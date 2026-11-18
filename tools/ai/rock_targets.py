"""The rocks' target curves from BALANCING.md ("Rock curves"): the chance to win, in %, that each rock should have at
each level (0 to 25) against a random opponent of the same level. score_rocks.py draws them next to what it measures.
Peaks sit at multiples of 5, where the rocks' level milestones are (moveEffects.from). Past level 25 a rock aims for its
level-25 value, and never above 65%.

    target("Rock", 10)  ->  55.0
"""
import math

LAST = 25

# shape, then its numbers (see BALANCING.md for the formulas)
TARGETS = {
    "Rock": ("linear", 65, 40),
    "Lava": ("bell", 45, 60, 5, 4),
    "Pebble": ("decay", 62, 47, 3),
    "Obsidian": ("skew", 45, 62, 5, 45, 2.5, 9),
    "Giant Bolder": ("bell", 45, 60, 10, 5),
    "Copper": ("bell", 45, 58, 5, 3),
    "Charged Copper": ("like", "Copper", 3),
    "Boltstone": ("flat", 55),
    "Discombobulate Shard": ("valley", 56, 47, 10, 5),
    "Uranium": ("mesa", 45, 60, 10, 20, 50),
    "Rainbow Uranium": ("like", "Uranium", 3),
    "Ice": ("bell", 45, 58, 15, 3),
    "Water": ("flat", 50),
    "Salt Water": ("log", 44, 56),
    "Saltstone": ("flat", 50),
    "Coal": ("flat", 47),
    "Sandstone": ("log", 42, 52),
    "Meteorite": ("log", 45, 60),
    "Healite": ("log", 44, 60),
    "Charged Healite": ("like", "Healite", 3),
    "Glass": ("scurve", 44, 58, 15, 1.5),
    "Magma": ("scurve", 42, 62, 15, 1.5),
    "Sunstone": ("linear", 42, 60),
    "Moonstone": ("skew", 43, 60, 20, 56, 5, 3),
    "Singularity": ("bloom", 40, 63, 4),
    "Mirror Shard": ("bloom", 40, 65, 3),
}


def _bump(x, peak, w, edge):
    """A bell's height at x (1 at the peak), stretched so it is 0 at edge (0 or LAST)."""
    g = math.exp(-(x - peak) ** 2 / (2 * w * w))
    g0 = math.exp(-(edge - peak) ** 2 / (2 * w * w))
    return (g - g0) / (1 - g0) if g0 < 1 else 1.0


def _ramp(t):
    """Smooth 0 to 1 for t in 0..1."""
    t = min(1.0, max(0.0, t))
    return t * t * (3 - 2 * t)


def target(rock, x):
    """The target win chance in % of rock at level x, or None if it has no target."""
    t = TARGETS.get(rock)
    if t is None:
        return None
    kind, *p = t
    if kind == "like":
        return min(65.0, target(p[0], x) + p[1])
    if kind == "flat":
        return float(p[0])
    if kind == "linear":
        a, b = p
        return a + (b - a) * x / LAST
    if kind == "bell":
        low, high, peak, w = p
        return low + (high - low) * _bump(x, peak, w, 0 if x < peak else LAST)
    if kind == "skew":
        start, high, peak, end, lw, rw = p
        if x < peak:
            return start + (high - start) * _bump(x, peak, lw, 0)
        return end + (high - end) * _bump(x, peak, rw, LAST)
    if kind == "valley":
        high, low, dip, w = p
        return high - (high - low) * _bump(x, dip, w, 0 if x < dip else LAST)
    if kind == "mesa":
        start, high, a, b, end = p
        if x < a:
            return start + (high - start) * _ramp(x / a)
        if x <= b:
            return float(high)
        return high + (end - high) * _ramp((x - b) / (LAST - b))
    if kind == "log":
        a, b = p
        return a + (b - a) * math.log(1 + x) / math.log(1 + LAST)
    if kind == "decay":
        a, floor, k = p
        return floor + (a - floor) * math.exp(-x / k)
    if kind == "bloom":
        a, b, k = p
        return a + (b - a) * (math.exp(x / k) - 1) / (math.exp(LAST / k) - 1)
    if kind == "scurve":
        low, high, switch, s = p
        return low + (high - low) / (1 + math.exp(-(x - switch) / s))
    raise ValueError(f"Unknown shape {kind} for {rock}")


if __name__ == "__main__":
    levels = list(range(0, LAST + 1, 5))
    print(f"{'rock':<22}" + "".join(f"{L:>6}" for L in levels))
    for name in TARGETS:
        print(f"{name:<22}" + "".join(f"{target(name, L):>6.0f}" for L in levels))
    print(f"{'mean':<22}" + "".join(f"{sum(target(n, L) for n in TARGETS) / len(TARGETS):>6.1f}" for L in levels))
