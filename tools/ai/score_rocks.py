"""Scores every rock for balancing: how often a fighter with the rock wins against a random opponent, per level, next
to its target curve (rock_targets.py, BALANCING.md "Rock curves").

    python tools/ai/score_rocks.py                      # a new run (or carries on the last one if it was stopped)
    python tools/ai/score_rocks.py --run 2_milestones   # that run: carries it on, or starts it
    python tools/ai/score_rocks.py --rocks "Ruby,Healite" --per-level 800
    python tools/ai/score_rocks.py --plot-only          # just redraw the last run's graphs and report

At every level (0 to 25, and 30, 40 and 50 to see that nothing runs away), each rock fights --per-level fights
(aiTrain rocks): its side carries it and 0 to 3 other random rocks, the other side 1 to 4 random rocks without it,
both at that level with 0 to 2 random trinkets and no potions, both played by the trained net in the game
(game/reading/valueNet.txt) at --depth. Half the fights have the sides swapped with the same dice, so moving first
cancels out. 50% is an average rock.

Every run has its own folder in tools/ai/rock_scores, so older runs stay to look back at. In it:
  scores.csv      the results, saved after every rock and level, so a run can be stopped (Ctrl+C) and carried on
  ranking.png     every rock's overall win chance, best first
  levels.png      win chance per level, one small chart per rock (smoothed over neighbouring levels), its target
                  dashed (the targets shifted to average 50% at every level, as the results always do)
  report.txt      how far each rock is from its target, by level, and anything above 65% or below 35%
  gemstones.java  a copy of the rocks' moves as they were scored, and rock_targets.py the targets
"""

import argparse
import csv
import math
import shutil
import subprocess
import sys
import time
from datetime import datetime
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import train  # noqa: E402  (the Java build and run helpers)
import rock_targets  # noqa: E402

SCORES = train.ROOT / "tools" / "ai" / "rock_scores"
GEMSTONES = train.ROOT / "src" / "main" / "java" / "game" / "gemstones.java"
LEVELS = list(range(0, 26)) + [30, 40, 50]
HIGHEST, LOWEST = 65, 35


def options():
    p = argparse.ArgumentParser(description="Score every rock for balancing")
    p.add_argument("--run", default="", help="the run's folder name in rock_scores (default: carry on the last one if it "
                                             "was stopped, else a new one named by the time)")
    p.add_argument("--rocks", default="", help="only these rocks, comma separated (default: all)")
    p.add_argument("--levels", default="", help="comma separated levels (default: 0 to 25, 30, 40 and 50)")
    p.add_argument("--per-level", type=int, default=360, help="fights per rock and level")
    p.add_argument("--depth", type=int, default=2, help="how far the AI looks ahead")
    p.add_argument("--weights", default=str(train.GAME_WEIGHTS), help="the net to play with (default: the game's)")
    p.add_argument("--smooth", type=int, default=1, help="levels on each side pooled into each point of the level graphs")
    p.add_argument("--threads", type=int, default=0)
    p.add_argument("--plot-only", action="store_true")
    return p.parse_args()


def load(csv_path):
    """{(rock, level): [points, games]} from a run's results."""
    done = {}
    if csv_path.exists():
        for row in csv.DictReader(open(csv_path, encoding="utf-8")):
            key = (row["rock"], int(row["level"]))
            prev = done.get(key, [0.0, 0])
            done[key] = [prev[0] + float(row["points"]), prev[1] + int(row["games"])]
    return done


def save_row(csv_path, rock, level, points, games):
    new = not csv_path.exists()
    with open(csv_path, "a", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        if new:
            w.writerow(["rock", "level", "points", "games"])
        w.writerow([rock, level, points, games])


def runs():
    """The run folders, oldest first."""
    if not SCORES.exists():
        return []
    return sorted((d for d in SCORES.iterdir() if d.is_dir()), key=lambda d: d.stat().st_mtime)


def pick_run(args, names, levels):
    if args.run:
        return SCORES / args.run
    previous = runs()
    if previous:
        last = previous[-1]
        if args.plot_only:
            return last
        if (last / "unfinished").exists():
            saved = last / "gemstones.java"
            if saved.exists() and saved.read_bytes() != GEMSTONES.read_bytes():
                print(f"The stopped run {last.name} scored other rocks than gemstones.java has now: starting a new run")
            else:
                print(f"Carrying on the stopped run {last.name} (use --run to start another)")
                return last
    return SCORES / datetime.now().strftime("%Y-%m-%d_%H%M%S")


def score(args, folder, names, levels, java):
    folder.mkdir(parents=True, exist_ok=True)
    csv_path = folder / "scores.csv"
    # What was scored: the rocks' moves and the targets, the first time this run fights
    if not (folder / "gemstones.java").exists():
        shutil.copy(GEMSTONES, folder / "gemstones.java")
        shutil.copy(Path(rock_targets.__file__), folder / "rock_targets.py")
    if (folder / "gemstones.java").read_bytes() != GEMSTONES.read_bytes():
        print(f"Note: {folder.name} scored other rocks than gemstones.java has now; its results get mixed with these")
    done = load(csv_path)
    # Fights come in pairs (the sides swapped), so a level is full at an even number of games. A level that has some
    # already gets only what is missing, with a seed of its own (the number done), so no fight is played twice
    full = 2 * max(1, args.per_level // 2)
    todo = []
    for n in names:
        groups = {}
        for l in levels:
            have = done.get((n, l), [0, 0])[1]
            if have < full:
                groups.setdefault(have, []).append(l)
        for have, ls in sorted(groups.items()):
            todo.append((n, ls, have, full - have))
    total = sum(len(ls) for _, ls, _, _ in todo)
    print(f"Run {folder.name}: {len(names)} rocks, levels {levels[0]}-{levels[-1]}, {args.per_level} fights each: "
          f"{total} rock-levels to fight")
    if not total:
        return
    (folder / "unfinished").touch()
    threads = [f"threads={args.threads}"] if args.threads > 0 else []
    started, finished = time.time(), 0
    for rock, ls, have, missing in todo:
        cmd = [java, "--module-path", train.module_path() + ";" + str(train.CLASSES), "-m", "GemstoneGuardian/game.aiTrain",
               "rocks", f"rock={rock}", "levels=" + ",".join(map(str, ls)), f"perLevel={missing}", f"seed={4242 + have}",
               f"depth={args.depth}", f"weights={args.weights}"] + threads
        proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, cwd=train.ROOT)
        train.CURRENT["proc"] = proc
        other = []
        for line in proc.stdout:
            if not line.startswith("RESULT"):
                other = (other + [line.rstrip()])[-20:]
                continue
            _, level, points, games = line.split()
            save_row(csv_path, rock, int(level), float(points), int(games))
            finished += 1
            rate = (time.time() - started) / finished
            left = (total - finished) * rate / 60
            print(f"\r  {rock:<22} level {int(level):>2}: {float(points) / int(games):.1%}   "
                  f"({finished}/{total}, about {left:.0f} min left)   ", end="", flush=True)
        proc.wait()
        train.CURRENT["proc"] = None
        if proc.returncode != 0:
            print("\n" + "\n".join(other))
            sys.exit(f"aiTrain failed on {rock}")
    (folder / "unfinished").unlink(missing_ok=True)
    print()


# ---- Targets ----

def shifted_target(rock, level):
    """The rock's target at level (past 25 its level-25 one), shifted so all the targets average 50% there."""
    at = min(level, rock_targets.LAST)
    t = rock_targets.target(rock, at)
    if t is None:
        return None
    mean = sum(rock_targets.target(n, at) for n in rock_targets.TARGETS) / len(rock_targets.TARGETS)
    return t - (mean - 50)


# ---- Graphs and report ----

INK, MUTED, GRID, SURFACE, LINE, TARGET = "#0b0b0b", "#52514e", "#e4e3df", "#fcfcfb", "#2a78d6", "#d0632a"


def summarise(done):
    """Per rock: overall win chance (levels 0 to 25) with a 95% interval, and the per-level points."""
    rocks = {}
    for (rock, level), (points, games) in done.items():
        r = rocks.setdefault(rock, {"points": 0.0, "games": 0, "levels": {}})
        if level <= rock_targets.LAST:
            r["points"] += points
            r["games"] += games
        r["levels"][level] = (points, games)
    for r in rocks.values():
        p = r["points"] / max(1, r["games"])
        r["rate"] = p
        r["ci"] = 1.96 * math.sqrt(max(p * (1 - p), 1e-9) / max(1, r["games"]))
    return {n: r for n, r in rocks.items() if r["games"] > 0}


def near_levels(lv, level, smooth):
    """The measured levels pooled into the point at level: those within smooth of it, on the same side of 25."""
    if level > rock_targets.LAST:
        return [k for k in lv if k == level]
    return [k for k in lv if abs(k - level) <= smooth and k <= rock_targets.LAST]


def pooled_target(name, lv, level, smooth):
    """The target pooled the way the measured value is (weighted by the games at each level), None without one."""
    ks = near_levels(lv, level, smooth)
    g = sum(lv[k][1] for k in ks)
    ts = [(shifted_target(name, k), lv[k][1]) for k in ks]
    if g == 0 or any(t is None for t, _ in ts):
        return None
    return sum(t * w for t, w in ts) / g


def pooled(lv, level, smooth):
    """Win chance (%) and its 95% half width at level, pooled over the levels within smooth of it (past 25 alone)."""
    near = [lv[k] for k in near_levels(lv, level, smooth)]
    g = sum(x[1] for x in near)
    if g == 0:
        return None
    p = sum(x[0] for x in near) / g
    return p * 100, 1.96 * math.sqrt(max(p * (1 - p), 1e-9) / g) * 100


def style(ax):
    ax.set_facecolor(SURFACE)
    for side in ("top", "right"):
        ax.spines[side].set_visible(False)
    for side in ("left", "bottom"):
        ax.spines[side].set_color(GRID)
    ax.tick_params(colors=MUTED, labelsize=8, length=0)
    ax.grid(color=GRID, linewidth=0.8)
    ax.set_axisbelow(True)


def plot(folder, done, smooth):
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    from matplotlib.lines import Line2D

    rocks = summarise(done)
    if not rocks:
        print("Nothing scored yet")
        return
    order = sorted(rocks, key=lambda n: rocks[n]["rate"], reverse=True)

    # The ranking: one dot per rock with its 95% interval, best on top, 50% (an average rock) marked
    fig, ax = plt.subplots(figsize=(8, 0.32 * len(order) + 1.2), facecolor=SURFACE)
    style(ax)
    ys = list(range(len(order)))[::-1]
    rates = [rocks[n]["rate"] * 100 for n in order]
    cis = [rocks[n]["ci"] * 100 for n in order]
    ax.axvline(50, color=MUTED, linewidth=1, linestyle="--")
    ax.hlines(ys, [r - c for r, c in zip(rates, cis)], [r + c for r, c in zip(rates, cis)], color=LINE, linewidth=2, alpha=0.35)
    ax.scatter(rates, ys, s=40, color=LINE, edgecolor=SURFACE, linewidth=1.5, zorder=3)
    for y, r in zip(ys, rates):
        ax.text(r, y + 0.32, f"{r:.1f}%", ha="center", fontsize=7, color=MUTED)
    ax.set_yticks(ys)
    ax.set_yticklabels(order, fontsize=9, color=INK)
    ax.set_xlabel("chance to win against a random loadout, levels 0 to 25 (%)", color=MUTED, fontsize=9)
    ax.set_title(f"Rocks, best first ({folder.name}; 50% = an average rock; bars: 95% interval)", color=INK, fontsize=11, loc="left")
    ax.grid(axis="y", visible=False)
    fig.tight_layout()
    fig.savefig(folder / "ranking.png", dpi=110, facecolor=SURFACE)
    plt.close(fig)

    # Per level: one small chart per rock in the ranking's order, all on the same scale, the target dashed. The
    # levels past 25 are drawn closer together (each 5 levels one step), after a gap
    levels = sorted({l for r in rocks.values() for l in r["levels"]})
    xpos = lambda l: l if l <= rock_targets.LAST else rock_targets.LAST + 2 + (l - 30) / 5
    cols = 5
    rows = math.ceil(len(order) / cols)
    fig, axes = plt.subplots(rows, cols, figsize=(cols * 2.7, rows * 1.95), sharex=True, sharey=True, facecolor=SURFACE)
    axes = axes.flatten() if hasattr(axes, "flatten") else [axes]
    for ax, name in zip(axes, order):
        style(ax)
        lv = rocks[name]["levels"]
        ax.axhspan(HIGHEST, 100, color="#c43c3c", alpha=0.06, linewidth=0)
        ax.axhline(50, color=MUTED, linewidth=0.8, linestyle=":")
        for part in ([l for l in levels if l <= rock_targets.LAST], [l for l in levels if l > rock_targets.LAST]):
            pts = [(l, pooled(lv, l, smooth)) for l in part]
            pts = [(l, v) for l, v in pts if v is not None]
            if not pts:
                continue
            xs = [xpos(l) for l, _ in pts]
            ax.fill_between(xs, [v[0] - v[1] for _, v in pts], [v[0] + v[1] for _, v in pts], color=LINE, alpha=0.18, linewidth=0)
            ax.plot(xs, [v[0] for _, v in pts], color=LINE, linewidth=2, marker="o" if len(pts) < 5 else None, markersize=3)
            tl = [(xpos(l), pooled_target(name, lv, l, smooth)) for l, _ in pts]
            tl = [(x, t) for x, t in tl if t is not None]
            if tl:
                ax.plot([x for x, _ in tl], [t for _, t in tl], color=TARGET, linewidth=1.4, linestyle="--")
        ax.set_title(f"{name}  {rocks[name]['rate'] * 100:.1f}%", fontsize=8.5, color=INK, loc="left")
    for ax in axes[len(order):]:
        ax.axis("off")
    ticks = [l for l in (0, 5, 10, 15, 20, 25, 30, 40, 50) if l in levels or l <= rock_targets.LAST]
    for ax in axes[: len(order)]:
        ax.set_ylim(25, 80)
        ax.set_xticks([xpos(l) for l in ticks])
        ax.set_xticklabels([str(l) for l in ticks])
    fig.legend(handles=[Line2D([], [], color=LINE, linewidth=2, label="measured (band: 95% interval)"),
                        Line2D([], [], color=TARGET, linewidth=1.4, linestyle="--", label="target")],
               loc="upper right", frameon=False, fontsize=8, labelcolor=MUTED, ncol=2)
    fig.supxlabel("level (both fighters)", color=MUTED, fontsize=9)
    fig.supylabel("chance to win (%)", color=MUTED, fontsize=9)
    fig.suptitle(f"Win chance per level, {folder.name} (smoothed over +-{smooth} levels up to 25; red above {HIGHEST}%)",
                 color=INK, fontsize=11, x=0.01, ha="left")
    fig.tight_layout()
    fig.savefig(folder / "levels.png", dpi=110, facecolor=SURFACE)
    plt.close(fig)

    report(folder, rocks, levels, smooth, order)
    print(f"\nGraphs and report in {folder}")


def report(folder, rocks, levels, smooth, order):
    """report.txt: per rock its misses from the target by level, and what is out of the 35 to 65% band."""
    out = []
    head = [l for l in (0, 2, 5, 8, 10, 12, 15, 18, 20, 22, 25, 30, 40, 50) if l in levels]
    out.append(f"Run {folder.name}. Measured minus target, in points (targets shifted to average 50; both smoothed over +-{smooth} levels up to 25)")
    out.append(f"{'rock':<22}{'mean':>6}" + "".join(f"{l:>5}" for l in head) + "   rms  worst")
    total = []
    for name in order:
        lv = rocks[name]["levels"]
        misses = {}
        for l in levels:
            v, t = pooled(lv, l, smooth), pooled_target(name, lv, l, smooth)
            if v is not None and t is not None:
                misses[l] = v[0] - t
        if not misses:
            continue
        upto = [m for l, m in misses.items() if l <= rock_targets.LAST]
        total += upto
        rms = math.sqrt(sum(m * m for m in upto) / max(1, len(upto)))
        worst = max(misses, key=lambda l: abs(misses[l]))
        out.append(f"{name:<22}{rocks[name]['rate'] * 100 - 50:>+6.1f}" + "".join(f"{misses[l]:>+5.0f}" if l in misses else "    ." for l in head)
                   + f"  {rms:4.1f}  {misses[worst]:+.0f} at {worst}")
    if total:
        out.append(f"\nOverall rms miss (levels 0 to 25): {math.sqrt(sum(m * m for m in total) / len(total)):.1f} points")
    out.append(f"\nOutside {LOWEST} to {HIGHEST}% (one level alone, so with its noise):")
    flagged = False
    for name in order:
        for l, (p, g) in sorted(rocks[name]["levels"].items()):
            v = p / g * 100
            if v > HIGHEST or v < LOWEST:
                out.append(f"  {name} at level {l}: {v:.0f}%")
                flagged = True
    if not flagged:
        out.append("  nothing")
    text = "\n".join(out)
    (folder / "report.txt").write_text(text, encoding="utf-8")
    print("\n" + text)


def main():
    args = options()
    SCORES.mkdir(parents=True, exist_ok=True)
    levels = [int(x) for x in args.levels.split(",")] if args.levels else LEVELS
    if args.plot_only:
        folder = pick_run(args, [], levels)
        plot(folder, load(folder / "scores.csv"), args.smooth)
        return
    javac, java = train.java_tools()
    train.build(javac)
    names = [l[5:] for l in train.run_java(java, ["rocks", "list=true"]) if l.startswith("ROCK ")]
    if args.rocks:
        wanted = [n.strip() for n in args.rocks.split(",")]
        missing = [n for n in wanted if n not in names]
        if missing:
            sys.exit(f"No rock called {', '.join(missing)}. The rocks: {', '.join(names)}")
        names = wanted
    folder = pick_run(args, names, levels)
    train.keep_awake()
    try:
        score(args, folder, names, levels, java)
    except KeyboardInterrupt:
        proc = train.CURRENT["proc"]
        if proc is not None:
            proc.kill()
        print(f"\nStopped. What was done is saved in {folder.name}: run it again to carry on.")
    plot(folder, load(folder / "scores.csv"), args.smooth)


if __name__ == "__main__":
    main()
