"""Trains the trained judge (valueNet) for the computer's fighters. Press play (VS Code: pick "Train AI" in Run and
Debug, then the green arrow), double click tools\\ai\\train.bat, or run:

    python tools/ai/train.py            # trains until stopped (Ctrl+C), picking up where the last run stopped
    python tools/ai/train.py --hours 3  # stops by itself after 3 hours
    python tools/ai/train.py --until 07:00  # stops by itself at 7 in the morning (left running overnight)
    python tools/ai/train.py --run fresh  # a new run in tools/ai/runs/fresh, the old one is kept

Each round:
1. self-play: the game (aiTrain, Java) plays fights with the newest net against a pool: itself, the calculated AI
   (40% of the fights in round 2, falling to 20% by round 6) and older nets (10%, kept every few rounds). Round 1,
   with no net yet, is the calculated AI against itself. Every position is written with how its fight ended
2. training: the net learns the chance to win from those positions (the last BUFFER rounds of them), a few epochs,
   with 10% of each round's fights kept apart for good to check it on (val), never trained on
3. eval: the net at --eval-depth plays the calculated AI at depth 3. A net that beats both 50% and the net already
   in the game is put in the game (src/main/resources/game/reading/valueNet.txt, and target/classes when built)

Everything is in tools/ai/runs/<run>/: checkpoint.pt (picked up again next time), data/ (the rounds' positions),
log.csv (every epoch's train and val scores, every round's win rate), progress.png (the same as graphs, redrawn as it
goes: open it in VS Code to watch), latest.txt and best.txt (the weights).
"""

import argparse
import csv
import glob
import os
import shutil
import signal
import subprocess
import sys
import time
from pathlib import Path

import numpy as np
import torch
import torch.nn.functional as fn

sys.path.insert(0, str(Path(__file__).parent))
import net  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
CLASSES = ROOT / "target" / "ai-classes"
GAME_WEIGHTS = ROOT / "src" / "main" / "resources" / "game" / "reading" / "valueNet.txt"
BUILT_WEIGHTS = ROOT / "target" / "classes" / "game" / "reading" / "valueNet.txt"
ADOPTIUM = Path(r"C:\Program Files\Eclipse Adoptium\jdk-17.0.6.10-hotspot")


def options():
    p = argparse.ArgumentParser(description="Train the computer fighters' judge")
    p.add_argument("--run", default="main", help="name of the run (folder in tools/ai/runs)")
    p.add_argument("--hours", type=float, default=0, help="stop after this many hours (0: until stopped)")
    p.add_argument("--until", default="", help="stop at this time of day, HH:MM (the next time it comes)")
    p.add_argument("--rounds", type=int, default=0, help="stop after this many rounds this time (0: until stopped)")
    p.add_argument("--fights", type=int, default=15000, help="self-play fights per round")
    p.add_argument("--first-fights", type=int, default=40000, help="self-play fights in the first round (calculated judge)")
    p.add_argument("--selfplay-depth", type=int, default=2, help="how far the AI looks ahead in self-play")
    p.add_argument("--epsilon", type=float, default=0.1, help="share of random actions in self-play")
    p.add_argument("--varied", type=float, default=0.25, help="share of self-play rocks and potions with their numbers changed a little")
    p.add_argument("--invented", type=float, default=0.2, help="share of self-play rocks and potions made up from scratch")
    p.add_argument("--lambda", dest="lam", type=float, default=0.8,
                   help="TD(lambda) targets: 1 learns from fight results only, lower leans on the net's value of the next positions")
    p.add_argument("--calc-start", type=float, default=0.4, help="share of self-play fights against the calculated AI in round 2")
    p.add_argument("--calc-end", type=float, default=0.2, help="... falling to this by round 6, and staying there")
    p.add_argument("--old-share", type=float, default=0.1, help="share of self-play fights against an older net")
    p.add_argument("--snapshot-every", type=int, default=3, help="keep the net as an older opponent every this many rounds")
    p.add_argument("--snapshots", type=int, default=5, help="older nets kept as opponents")
    p.add_argument("--buffer", type=int, default=3, help="rounds of positions trained on")
    p.add_argument("--epochs", type=int, default=3, help="epochs over the buffer each round (the one with the best val is kept)")
    p.add_argument("--width", type=int, default=None, choices=[1, 2],
                   help="net width for a new run (1: the first nets, 2: twice as wide, the default); a run keeps the width it started with")
    p.add_argument("--reset-bar", action="store_true",
                   help="measure the net in the game again before training (after a balancing update)")
    p.add_argument("--search-weight", type=float, default=0.5,
                   help="share of the target taken from the search's value where a net chose (0: TD(lambda) only)")
    p.add_argument("--deep-after", type=int, default=0,
                   help="from this round on, self-play looks one action further ahead (0: never)")
    p.add_argument("--weight-decay", type=float, default=1e-3)
    p.add_argument("--batch", type=int, default=4096)
    p.add_argument("--lr", type=float, default=None,
                   help="learning rate (default: 1e-3 at the start, halved by itself each round that brings no improvement)")
    p.add_argument("--min-lr", type=float, default=5e-5)
    p.add_argument("--eval-fights", type=int, default=400)
    p.add_argument("--eval-depth", type=int, default=2, help="the net's depth in the eval against calculated depth 3")
    p.add_argument("--threads", type=int, default=0, help="Java threads (0: all cores but one)")
    p.add_argument("--no-install", action="store_true", help="never put the best net in the game (for trying things)")
    return p.parse_args()


# ---- The Java side ----

def java_tools():
    javac = shutil.which("javac")
    java = None
    if javac:
        cand = Path(javac).with_name("java.exe" if os.name == "nt" else "java")
        if cand.exists():
            java = cand
    if (java is None or not _is_17(java)) and (ADOPTIUM / "bin" / "java.exe").exists():
        java = ADOPTIUM / "bin" / "java.exe"
        javac = str(ADOPTIUM / "bin" / "javac.exe")
    if not javac or java is None:
        sys.exit("No JDK 17 found: put its bin folder on PATH")
    return str(javac), str(java)


def _is_17(java):
    try:
        out = subprocess.run([str(java), "-version"], capture_output=True, text=True).stderr
        return '"1.' not in out
    except OSError:
        return False


def module_path():
    fx = Path.home() / ".m2" / "repository" / "org" / "openjfx"
    jars = [fx / f"javafx-{m}" / "19" / f"javafx-{m}-19-win.jar" for m in ("base", "controls", "fxml", "graphics")]
    for jar in jars:
        if not jar.exists():
            sys.exit(f"Missing JavaFX jar {jar}")
    return os.pathsep.join(str(j) for j in jars)


def build(javac):
    """Compiles the game into target/ai-classes (not target/classes, so a game that is running is not disturbed)."""
    print("Building the game for self-play...")
    sources = [str(p) for p in (ROOT / "src" / "main" / "java").rglob("*.java")]
    CLASSES.mkdir(parents=True, exist_ok=True)
    listing = CLASSES.parent / "ai-sources.txt"
    listing.write_text("\n".join(f'"{s}"' for s in sources).replace("\\", "/"), encoding="utf-8")
    r = subprocess.run([javac, "--release", "17", "-encoding", "UTF-8", "--module-path", module_path(),
                        "-d", str(CLASSES), "@" + str(listing)], capture_output=True, text=True)
    if r.returncode != 0:
        sys.exit("javac failed:\n" + r.stdout + r.stderr)
    shutil.copytree(ROOT / "src" / "main" / "resources", CLASSES, dirs_exist_ok=True)


CURRENT = {"proc": None}


def keep_awake():
    """Asks Windows not to go to sleep while training runs (the screen may still turn off)."""
    if os.name != "nt":
        return
    try:
        import ctypes
        ES_CONTINUOUS, ES_SYSTEM_REQUIRED = 0x80000000, 0x00000001
        ctypes.windll.kernel32.SetThreadExecutionState(ES_CONTINUOUS | ES_SYSTEM_REQUIRED)
    except (OSError, AttributeError):
        pass


class tee:
    """Everything printed also goes to console.log in the run's folder, to read after a night of training."""
    def __init__(self, stream, path):
        self.stream, self.file = stream, open(path, "a", encoding="utf-8")

    def write(self, text):
        self.stream.write(text)
        self.file.write(text)
        self.file.flush()

    def flush(self):
        self.stream.flush()
        self.file.flush()


def deadline(until):
    """The next time the clock shows until (HH:MM), as a time.time() value; 0 when none is given."""
    if not until:
        return 0
    import datetime
    h, m = (int(x) for x in until.split(":"))
    now = datetime.datetime.now()
    at = now.replace(hour=h, minute=m, second=0, microsecond=0)
    if at <= now:
        at += datetime.timedelta(days=1)
    return at.timestamp()


def run_java(java, args, show_progress=False):
    cmd = [java, "--module-path", module_path() + os.pathsep + str(CLASSES), "-m", "GemstoneGuardian/game.aiTrain"] + args
    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, cwd=ROOT)
    CURRENT["proc"] = proc
    lines = []
    for line in proc.stdout:
        line = line.rstrip()
        lines.append(line)
        if line.startswith("PROGRESS") and show_progress:
            _, done, total = line.split()
            print(f"\r  self-play {done}/{total} fights", end="", flush=True)
    proc.wait()
    CURRENT["proc"] = None
    if show_progress:
        print()
    if proc.returncode != 0:
        raise RuntimeError("aiTrain failed:\n" + "\n".join(lines[-30:]))
    return lines


# ---- Data ----

def load_buffer(data_dir, rounds):
    files = sorted(glob.glob(str(data_dir / "round_*.npz")))[-rounds:]
    parts = [dict(np.load(f)) for f in files]
    if not parts:
        return None
    # Rounds saved before the search's values were recorded have none (NaN: the plain TD target is used there)
    for p in parts:
        if "search" not in p:
            p["search"] = np.full(len(p["target"]), np.nan, dtype=np.float32)
    out = {}
    offset = 0
    for p in parts:
        p["fight"] = p["fight"] + offset
        offset += len(p["fight_static"])
    for k in parts[0]:
        out[k] = np.concatenate([p[k] for p in parts])
    return out


VAL_SHARE = 0.1


def mark_val(data, rng):
    """Picks a round's val fights once, when it is saved: they stay val in every round they are in the buffer."""
    data["fight_val"] = rng.random(len(data["fight_static"])) < VAL_SHARE


def split(data):
    """Train and val by fight, so positions of one fight are never on both sides."""
    is_val = data["fight_val"][data["fight"]]
    return np.where(~is_val)[0], np.where(is_val)[0]


def to_device(data, idx, device, target="target"):
    keys = ("now", "rocks", "items", "stats", "next", "ender")
    b = {k: torch.from_numpy(data[k][idx]).to(device) for k in keys}
    b["target"] = torch.from_numpy(data[target][idx]).to(device)
    b["static"] = torch.from_numpy(data["fight_static"][data["fight"][idx]]).to(device)
    return b


@torch.no_grad()
def side0_values(model, data, device, batch=16384):
    """The net's chance for side 0 at every position: the average of its side 0 value and 1 minus its side 1 value."""
    model.eval()
    out = np.empty(len(data["target"]), dtype=np.float32)
    idx = np.arange(len(out))
    for s in range(0, len(idx), batch):
        b = to_device(data, idx[s:s + batch], device)
        p = torch.sigmoid(model(b))
        out[s:s + batch] = ((p[:, 0] + 1 - p[:, 1]) / 2).cpu().numpy()
    model.train()
    return out


def blend_search(data, td, weight):
    """Where a net chose, part of the target is its search's value of the position: what looking ahead over every action
    and roll found, with no luck in it. The rest stays the TD(lambda) target, so the real results keep it honest."""
    search = data["search"]
    have = np.isfinite(search)
    out = td.copy()
    out[have] = (1 - weight) * td[have] + weight * search[have]
    return out, have.mean()


def td_targets(data, values, lam):
    """TD(lambda): each position learns from what came right after it rather than only from how the fight ended.
    Backwards through each fight: the last position's target is the result; before it,
    target(t) = (1 - lam) * value(t+1) + lam * target(t+1). lam = 1 is the plain result."""
    target, fight, result = data["target"], data["fight"], data["target"]
    out = np.empty_like(target)
    n = len(target)
    for t in range(n - 1, -1, -1):
        if t == n - 1 or fight[t + 1] != fight[t]:
            out[t] = result[t]
        else:
            out[t] = (1 - lam) * values[t + 1] + lam * out[t + 1]
    return out


def losses(model, batch):
    """Binary cross-entropy of both sides' chance against how the fight ended (side 1's is 1 minus side 0's)."""
    logits = model(batch)
    t0 = batch["target"].float()
    target = torch.stack([t0, 1 - t0], 1)
    loss = fn.binary_cross_entropy_with_logits(logits, target)
    p = torch.sigmoid(logits)
    brier = ((p - target) ** 2).mean()
    decided = (t0 != 0.5)
    acc = ((p[:, 0] > 0.5) == (t0 > 0.5))[decided].float().mean() if decided.any() else torch.tensor(0.0)
    return loss, brier, acc


@torch.no_grad()
def evaluate(model, data, idx, device, batch=8192):
    model.eval()
    total, brier, acc, n = 0.0, 0.0, 0.0, 0
    for s in range(0, len(idx), batch):
        b = to_device(data, idx[s:s + batch], device)
        l, br, a = losses(model, b)
        k = len(b["target"])
        total += l.item() * k
        brier += br.item() * k
        acc += a.item() * k
        n += k
    model.train()
    return total / n, brier / n, acc / n


# ---- Progress ----

LOG_FIELDS = ["round", "epoch", "time_min", "positions", "train_loss", "val_loss", "val_brier", "val_acc", "eval_winrate", "ms_per_decision"]


def log_row(path, row):
    new = not path.exists()
    with open(path, "a", newline="") as f:
        w = csv.DictWriter(f, fieldnames=LOG_FIELDS)
        if new:
            w.writeheader()
        w.writerow({k: row.get(k, "") for k in LOG_FIELDS})


def plot(log_path, png_path):
    try:
        import matplotlib
        matplotlib.use("Agg")
        import matplotlib.pyplot as plt
    except ImportError:
        return
    rows = list(csv.DictReader(open(log_path)))
    ep = [r for r in rows if r["train_loss"]]
    ev = [r for r in rows if r["eval_winrate"]]
    fig, (a, b) = plt.subplots(1, 2, figsize=(11, 4))
    x = list(range(1, len(ep) + 1))
    a.plot(x, [float(r["train_loss"]) for r in ep], label="train loss")
    a.plot(x, [float(r["val_loss"]) for r in ep], label="val loss")
    a.set_xlabel("epoch")
    a.set_ylabel("cross-entropy (lower is better)")
    a.set_title("Chance to win: train and val")
    a.grid(alpha=0.3)
    a.legend()
    a2 = a.twinx()
    a2.plot(x, [float(r["val_acc"]) for r in ep], color="tab:green", alpha=0.5, label="val accuracy")
    a2.set_ylabel("val accuracy (winner picked)", color="tab:green")
    b.plot([int(r["round"]) for r in ev], [float(r["eval_winrate"]) for r in ev], marker="o")
    b.axhline(0.5, color="grey", linestyle="--", linewidth=1)
    b.set_xlabel("round")
    b.set_ylabel("win rate")
    b.set_title("Net against the calculated AI (depth 3)")
    b.grid(alpha=0.3)
    fig.tight_layout()
    tmp = str(png_path) + ".tmp.png"
    fig.savefig(tmp, dpi=90)
    plt.close(fig)
    os.replace(tmp, png_path)


# ---- Checkpoints ----

def save_checkpoint(path, model, opt, state):
    tmp = str(path) + ".tmp"
    torch.save({"model": model.state_dict(), "opt": opt.state_dict(), "state": state}, tmp)
    os.replace(tmp, path)


def check_java_matches(java, model, run_dir, weights, threads):
    """The game's forward pass (valueNet.java) must give what this one gives, on the same positions."""
    data_path, values_path = run_dir / "check.bin", run_dir / "check.txt"
    run_java(java, ["check", f"weights={weights}", f"out={data_path}", f"values={values_path}", "fights=4"])
    # Exactly as written (float32), not rounded to float16 like the training data
    data = net.read_positions(data_path, dtype=np.float32)
    java_values = np.loadtxt(values_path, ndmin=2)
    device = next(model.parameters()).device
    model.eval()
    with torch.no_grad():
        b = to_device(data, np.arange(len(data["target"])), device)
        mine = torch.sigmoid(model(b)).cpu().numpy()
    model.train()
    diff = np.abs(mine - java_values).max()
    data_path.unlink(missing_ok=True)
    values_path.unlink(missing_ok=True)
    if not np.isfinite(diff) or diff > 2e-3:
        raise RuntimeError(f"The game's net and this one disagree by {diff:.4f}: valueNet.java and net.py differ")
    return diff


def eval_net(java, weights, args, threads):
    out = run_java(java, ["eval", f"weights={weights}", f"fights={args.eval_fights}", f"depth={args.eval_depth}",
                          "opponentDepth=3"] + threads)
    parts = [l for l in out if l.startswith("winrate")][-1].split()
    return float(parts[1]), float(parts[-1])


def calc_share(args, rnd):
    """The share of self-play fights against the calculated AI: calc_start in round 2, down to calc_end by round 6."""
    t = min(1.0, max(0.0, (rnd - 2) / 4))
    return args.calc_start + (args.calc_end - args.calc_start) * t


def main():
    args = options()
    run_dir = ROOT / "tools" / "ai" / "runs" / args.run
    data_dir = run_dir / "data"
    data_dir.mkdir(parents=True, exist_ok=True)
    # Self-play cut off by a stop (VS Code's stop button ends the script at once) left half a file: it is not used
    for leftover in data_dir.glob("*.bin"):
        leftover.unlink()
    ckpt, log_path, png_path = run_dir / "checkpoint.pt", run_dir / "log.csv", run_dir / "progress.png"
    sys.stdout = tee(sys.stdout, run_dir / "console.log")
    # Errors too, so a crash in the night can be read in the morning
    sys.stderr = tee(sys.stderr, run_dir / "console.log")
    print(f"\n---- {time.strftime('%Y-%m-%d %H:%M')}: train.py {' '.join(sys.argv[1:])}")
    keep_awake()
    # The stop time is kept in the run's folder, so a restart after a crash keeps it (and does not turn 07:00 into
    # tomorrow's). Removed when the run ends by itself or is stopped
    stop_file = run_dir / "stop_at.txt"
    if args.until or args.hours:
        stop_at = float(stop_file.read_text()) if stop_file.exists() else 0
        # A stop time more than an hour gone is from an earlier night (a run that crashed out): a new one is set
        if stop_at and time.time() - stop_at > 3600:
            stop_at = 0
        if not stop_at:
            stop_at = deadline(args.until) if args.until else time.time() + args.hours * 3600
            stop_file.write_text(str(stop_at))
    else:
        stop_at = 0
        stop_file.unlink(missing_ok=True)
    if stop_at:
        print(f"Stops by itself at {time.strftime('%H:%M', time.localtime(stop_at))}")
    latest, best = run_dir / "latest.txt", run_dir / "best.txt"
    snapshots = run_dir / "snapshots"
    snapshots.mkdir(exist_ok=True)

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"Training on {torch.cuda.get_device_name(0) if device.type == 'cuda' else 'the CPU'}")
    # The width is fixed when a run starts (older checkpoints are width 1)
    width = args.width or 2
    if ckpt.exists():
        width = torch.load(ckpt, map_location="cpu")["state"].get("width", 1)
        if args.width is not None and args.width != width:
            sys.exit(f"Run '{args.run}' is a width {width} net; --width {args.width} needs a new run (--run <new name>)")
    model = net.ValueNet(width).to(device)
    print(f"Net width {width} ({sum(p.numel() for p in model.parameters())} weights)")
    opt = torch.optim.AdamW(model.parameters(), lr=1e-3, weight_decay=args.weight_decay)
    # best: this run's best win rate (best.txt); installed: the win rate of the net in the game, which a net must beat
    # (and 50%) to replace it
    state = {"round": 0, "epochs": 0, "best": -1.0, "installed": None, "minutes": 0.0, "lr": 1e-3, "width": width}
    if ckpt.exists():
        saved = torch.load(ckpt, map_location=device)
        model.load_state_dict(saved["model"])
        opt.load_state_dict(saved["opt"])
        state.update(saved["state"])
        print(f"Picking up run '{args.run}' after round {state['round']} ({state['minutes']:.0f} min trained, best win rate {state['best']:.3f})")
    else:
        print(f"Starting run '{args.run}' in {run_dir}")
    if args.lr is not None:
        state["lr"] = args.lr

    def set_lr():
        for group in opt.param_groups:
            group["lr"] = state["lr"]
            group["weight_decay"] = args.weight_decay
    set_lr()
    print(f"Learning rate {state['lr']:.1e}")

    javac, java = java_tools()
    build(javac)
    threads = [f"threads={args.threads}"] if args.threads > 0 else []
    if args.reset_bar:
        state["installed"] = None
    if state["installed"] is None:
        state["installed"] = 0.5
        if GAME_WEIGHTS.exists() and not args.no_install:
            print("Measuring the net already in the game, so only a better one replaces it...")
            measured = eval_net(java, GAME_WEIGHTS, args, threads)[0]
            state["installed"] = max(0.5, measured)
            print(f"  it wins {measured:.3f} against the calculated AI; a new net must beat {state['installed']:.3f}")

    started = time.time()
    minutes_before = state["minutes"]
    first_round = state["round"] + 1

    def minutes():
        return minutes_before + (time.time() - started) / 60

    def stop_now():
        if stop_at and time.time() >= stop_at:
            return True
        return bool(args.rounds) and state["round"] - first_round + 1 >= args.rounds

    try:
        while not stop_now():
            rnd = state["round"] + 1
            print(f"\n=== Round {rnd} ===")
            # 1. Self-play: with the newest net, or the calculated judge when there is none yet
            have_net = latest.exists()
            fights = args.fights if have_net else args.first_fights
            raw = data_dir / f"round_{rnd:04d}.bin"
            depth = args.selfplay_depth + (1 if args.deep_after and rnd >= args.deep_after else 0)
            jargs = ["selfplay", f"out={raw}", f"fights={fights}", f"depth={depth}",
                     f"epsilon={args.epsilon}", f"seed={rnd * 7919 + int(time.time()) % 1000}",
                     f"varied={args.varied}", f"invented={args.invented}"] + threads
            if have_net:
                olds = sorted(snapshots.glob("round_*.txt"))
                share = calc_share(args, rnd)
                old_share = args.old_share if olds else 0
                jargs += [f"weights={latest}", f"calcShare={share:.3f}", f"oldShare={old_share}",
                          "old=" + ",".join(str(o) for o in olds)]
                print(f"Self-play with the net (depth {depth}): {share:.0%} of fights against the calculated AI, "
                      f"{old_share:.0%} against {len(olds)} older nets, the rest against itself")
            else:
                print(f"Self-play with the calculated AI (depth {depth}), as there is no net yet")
            out = run_java(java, jargs, show_progress=True)
            for line in out:
                if line.startswith(("OPPONENTS", "DONE")):
                    print("  " + line)
            data = net.read_positions(raw)
            mark_val(data, np.random.default_rng(rnd))
            np.savez_compressed(data_dir / f"round_{rnd:04d}.npz", **data)
            del data
            raw.unlink()
            # Old rounds beyond the buffer are not needed any more
            for old in sorted(glob.glob(str(data_dir / "round_*.npz")))[:-args.buffer]:
                os.remove(old)

            # 2. Training on the buffer
            buf = None
            buf = load_buffer(data_dir, args.buffer)
            tr, va = split(buf)
            print(f"Training on {len(tr)} positions ({len(va)} kept for val)")
            # The epoch with the best val is kept: later epochs over the same fights can learn them by heart
            saved_before = {k: v.detach().clone() for k, v in model.state_dict().items()}
            best_val, best_weights, best_epoch = evaluate(model, buf, va, device)[0], None, None
            print(f"  before training: val {best_val:.4f}")
            # The targets: TD(lambda) from the net as it is now, or the fight results while there is no trained net
            if args.lam < 1 and state["epochs"] > 0:
                buf["td"] = td_targets(buf, side0_values(model, buf, device), args.lam)
                print(f"  targets: TD(lambda {args.lam}) from the net's values of the next positions; val is still scored on the real results")
            else:
                buf["td"] = buf["target"]
            if args.search_weight > 0 and "search" in buf:
                buf["td"], share = blend_search(buf, buf["td"], args.search_weight)
                print(f"  {args.search_weight:.0%} of the target from the search's value at {share:.0%} of positions (where a net chose)")
            for e in range(args.epochs):
                perm = np.random.permutation(tr)
                total, n = 0.0, 0
                for s in range(0, len(perm), args.batch):
                    b = to_device(buf, perm[s:s + args.batch], device, target="td")
                    loss, _, _ = losses(model, b)
                    opt.zero_grad()
                    loss.backward()
                    opt.step()
                    total += loss.item() * len(b["target"])
                    n += len(b["target"])
                train_loss = total / n
                if not np.isfinite(train_loss):
                    # Something blew up: this round's training is thrown away, the net from before it is kept
                    print("  the loss is not a number: this round's training is thrown away")
                    model.load_state_dict(saved_before)
                    best_weights, best_epoch = None, None
                    break
                val_loss, val_brier, val_acc = evaluate(model, buf, va, device)
                state["epochs"] += 1
                print(f"  epoch {state['epochs']}: train {train_loss:.4f}  val {val_loss:.4f}  val accuracy {val_acc:.3f}")
                if val_loss < best_val:
                    best_val, best_epoch = val_loss, state["epochs"]
                    best_weights = {k: v.detach().clone() for k, v in model.state_dict().items()}
                log_row(log_path, {"round": rnd, "epoch": state["epochs"], "time_min": f"{minutes():.1f}", "positions": len(tr),
                                   "train_loss": f"{train_loss:.5f}", "val_loss": f"{val_loss:.5f}",
                                   "val_brier": f"{val_brier:.5f}", "val_acc": f"{val_acc:.4f}"})
                plot(log_path, png_path)
                state["minutes"] = minutes()
                save_checkpoint(ckpt, model, opt, state)
            if best_weights is None:
                print("  no epoch beat the net from before this round: it is kept")
                model.load_state_dict(saved_before)
                # The steps are too big now: smaller ones from the next round on
                if state["lr"] > args.min_lr:
                    state["lr"] = max(args.min_lr, state["lr"] / 2)
                    set_lr()
                    print(f"  learning rate halved to {state['lr']:.1e}")
            elif best_epoch != state["epochs"]:
                print(f"  kept epoch {best_epoch} (best val {best_val:.4f})")
                model.load_state_dict(best_weights)
            save_checkpoint(ckpt, model, opt, state)
            net.export(model, latest)

            # The game's copy of the net must work out the same numbers
            diff = check_java_matches(java, model, run_dir, latest, threads)
            print(f"  the game's net matches (largest difference {diff:.1e})")

            # 3. Eval against the calculated AI
            winrate, ms_eval = eval_net(java, latest, args, threads)
            print(f"  against the calculated AI: {winrate:.3f} ({ms_eval:.1f} ms a decision at depth {args.eval_depth})")
            state["round"] = rnd
            state["minutes"] = minutes()
            log_row(log_path, {"round": rnd, "time_min": f"{minutes():.1f}", "eval_winrate": f"{winrate:.4f}", "ms_per_decision": f"{ms_eval:.2f}"})
            plot(log_path, png_path)
            if winrate > state["best"]:
                state["best"] = winrate
                shutil.copy(latest, best)
                print("  new best of this run (best.txt)")
            if winrate > state["installed"]:
                if args.no_install:
                    print("  better than the game's net, but not put in the game (--no-install)")
                else:
                    state["installed"] = winrate
                    shutil.copy(latest, GAME_WEIGHTS)
                    if BUILT_WEIGHTS.parent.exists():
                        shutil.copy(latest, BUILT_WEIGHTS)
                    print(f"  put in the game ({GAME_WEIGHTS.relative_to(ROOT)})")
            # Now and then the net is kept as an older opponent for later rounds
            if rnd % args.snapshot_every == 0:
                shutil.copy(latest, snapshots / f"round_{rnd:04d}.txt")
                for extra in sorted(snapshots.glob("round_*.txt"))[:-args.snapshots]:
                    extra.unlink()
            save_checkpoint(ckpt, model, opt, state)
    except KeyboardInterrupt:
        proc = CURRENT["proc"]
        if proc is not None:
            proc.kill()
            proc.wait()
        state["minutes"] = minutes()
        save_checkpoint(ckpt, model, opt, state)
        for leftover in data_dir.glob("*.bin"):
            try:
                leftover.unlink()
            except OSError:
                pass  # removed at the next start
        stop_file.unlink(missing_ok=True)
        print("\nStopped. Everything is saved: press play again to go on from here.")
        return
    stop_file.unlink(missing_ok=True)
    print(f"\nDone after round {state['round']}. Best win rate against the calculated AI: {state['best']:.3f}")


if __name__ == "__main__":
    signal.signal(signal.SIGINT, signal.default_int_handler)
    main()
