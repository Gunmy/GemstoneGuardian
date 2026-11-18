"""The trained judge's network (valueNet.java runs the same net in the game) and the reader for the positions aiTrain
writes. Nothing here knows the game's rules: every input is made by valueNet.java, this only puts them together.

A position is seen from both sides: the net gives side 0's chance to win and side 1's. Per side:
- every move row: the move's fixed inputs (S), how it stands now (R), and the situation: the side's stats, the other
  side's stats (F each), whether the side acts next and whether effects tick right after the next action
- a shared move net reads each row; a rock is the sum of its moves plus whether it has uses, through the rock layer
- items the same, through their own net
- the side's summary: held rock, other rocks (mean and max), items (mean and max), its stats
- the head reads [my summary, their summary, I act next, tick next] and gives the chance to win
"""

import struct

import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as fn

# Must match valueNet.java
S, R, F = 20, 11, 16
ROCKS, MOVES, ITEMS = 4, 3, 4
CTX = 2 * F + 2
ROW = S + R + CTX
ROWS = ROCKS * MOVES + ITEMS
MAGIC = 0x47475632

# Layer sizes: width 1 is the first nets' (v1 to v3), width 2 twice as wide. valueNet.java reads them from the weights
SIZES = {
    1: dict(move_h=32, move_out=32, item_h=32, item_out=16, rock_out=32, head_h=128),
    2: dict(move_h=64, move_out=64, item_h=64, item_out=32, rock_out=64, head_h=256),
}

STATIC_FLOATS = 2 * ROWS * (1 + S)
SIDE_FLOATS = ROWS * R + ROCKS * 3 + ITEMS + F
POSITION_FLOATS = 3 + 2 * SIDE_FLOATS


def read_positions(path, dtype=np.float16):
    """Reads an aiTrain data file into arrays (float16 to keep them small). One entry per position:
    fight_static [fights, 2, ROWS, 1+S] (once per fight), now [N, 2, ROWS, R], rocks [N, 2, ROCKS, 3], items [N, 2, ITEMS],
    stats [N, 2, F], next [N], ender [N], search [N] (the acting net's search value for side 0, NaN when the
    calculated AI acted), target [N] (side 0's result), fight [N] (which fight, for the split)."""
    raw = open(path, "rb").read()
    head = np.frombuffer(raw, "<i4", count=7)
    if head[0] != MAGIC or tuple(head[1:]) != (S, R, F, ROCKS, MOVES, ITEMS):
        raise ValueError(f"{path}: made by another version of valueNet.java ({tuple(head)})")
    off = 28
    statics, positions, results, counts = [], [], [], []
    while off < len(raw):
        n = struct.unpack_from("<i", raw, off)[0]
        result = struct.unpack_from("<f", raw, off + 4)[0]
        off += 8
        statics.append(np.frombuffer(raw, "<f4", count=STATIC_FLOATS, offset=off))
        off += 4 * STATIC_FLOATS
        positions.append(np.frombuffer(raw, "<f4", count=n * POSITION_FLOATS, offset=off).reshape(n, POSITION_FLOATS))
        off += 4 * n * POSITION_FLOATS
        results.append(result)
        counts.append(n)
    counts = np.array(counts)
    fight = np.repeat(np.arange(len(counts)), counts)
    static = np.stack(statics).reshape(-1, 2, ROWS, 1 + S)
    pos = np.concatenate(positions)
    ints = pos[:, :2].copy().view("<i4")
    search = pos[:, 2].astype(np.float32)
    body = pos[:, 3:].reshape(-1, 2, SIDE_FLOATS)
    a = ROWS * R
    b = a + ROCKS * 3
    c = b + ITEMS
    return {
        "fight_static": static.astype(dtype),
        "now": body[:, :, :a].reshape(-1, 2, ROWS, R).astype(dtype),
        "rocks": body[:, :, a:b].reshape(-1, 2, ROCKS, 3).astype(dtype),
        "items": body[:, :, b:c].astype(dtype),
        "stats": body[:, :, c:].astype(dtype),
        "next": ints[:, 0].astype(np.int8),
        "ender": ints[:, 1].astype(np.int8),
        "search": search,
        "target": np.repeat(np.array(results, dtype=np.float32), counts),
        "fight": fight.astype(np.int32),
    }


class ValueNet(nn.Module):
    def __init__(self, width=1):
        super().__init__()
        z = SIZES[width]
        self.width = width
        self.move_out, self.rock_out = z["move_out"], z["rock_out"]
        side = z["rock_out"] * 3 + z["item_out"] * 2 + F
        self.move1 = nn.Linear(ROW, z["move_h"])
        self.move2 = nn.Linear(z["move_h"], z["move_out"])
        self.item1 = nn.Linear(ROW, z["item_h"])
        self.item2 = nn.Linear(z["item_h"], z["item_out"])
        self.rock = nn.Linear(z["move_out"] + 1, z["rock_out"])
        self.head1 = nn.Linear(2 * side + 2, z["head_h"])
        self.head2 = nn.Linear(z["head_h"], z["head_h"])
        self.out = nn.Linear(z["head_h"], 1)

    def side(self, static, now, rocks, items, mine, theirs, acts_next, tick_next):
        """Side o's summary. static [B, ROWS, 1+S], now [B, ROWS, R], rocks [B, ROCKS, 3], items [B, ITEMS],
        mine / theirs [B, F], acts_next / tick_next [B]."""
        b = static.shape[0]
        ctx = torch.cat([mine, theirs, acts_next[:, None], tick_next[:, None]], 1)
        rows = torch.cat([static[:, :, 1:], now, ctx[:, None, :].expand(b, ROWS, CTX)], 2)
        present = static[:, :, 0:1]

        m = fn.relu(self.move2(fn.relu(self.move1(rows[:, : ROCKS * MOVES])))) * present[:, : ROCKS * MOVES]
        m = m.view(b, ROCKS, MOVES, self.move_out).sum(2)
        r = fn.relu(self.rock(torch.cat([m, rocks[:, :, 1:2]], 2)))
        held = rocks[:, :, 2:3]
        others = rocks[:, :, 0:1] * (1 - held)
        held_vec = (r * held).sum(1)
        n = others.sum(1).clamp(min=1)
        mean = (r * others).sum(1) / n
        # The outputs are 0 or more, so the max with the rest set to 0 is the max over the others (0 if none)
        top = (r * others).max(1).values

        it = fn.relu(self.item2(fn.relu(self.item1(rows[:, ROCKS * MOVES :]))))
        mask = items[:, :, None]
        k = mask.sum(1).clamp(min=1)
        it_mean = (it * mask).sum(1) / k
        it_top = (it * mask).max(1).values
        return torch.cat([held_vec, mean, top, it_mean, it_top, mine], 1)

    def forward(self, batch):
        """The logits of side 0's and side 1's chance to win, [B, 2]."""
        static, now, rocks, items, stats = (batch[k].float() for k in ("static", "now", "rocks", "items", "stats"))
        nxt, ender = batch["next"].long(), batch["ender"].long()
        tick = (nxt == ender).float()
        sides = []
        for o in (0, 1):
            acts = (nxt == o).float()
            sides.append(self.side(static[:, o], now[:, o], rocks[:, o], items[:, o], stats[:, o], stats[:, 1 - o], acts, tick))
        logits = []
        for i in (0, 1):
            x = torch.cat([sides[i], sides[1 - i], (nxt == i).float()[:, None], tick[:, None]], 1)
            h = fn.relu(self.head2(fn.relu(self.head1(x))))
            logits.append(self.out(h)[:, 0])
        return torch.stack(logits, 1)


def export(model, path):
    """Writes the weights as valueNet.java reads them: "valueNet 1", then per tensor "<name> <rows> <cols>" and the
    numbers row by row (a bias is one row)."""
    lines = ["valueNet 1"]
    for name, t in model.state_dict().items():
        a = t.detach().cpu().double().numpy()
        if a.ndim == 1:
            a = a[None, :]
        lines.append(f"{name} {a.shape[0]} {a.shape[1]}")
        for row in a:
            lines.append(" ".join(repr(float(v)) for v in row))
    tmp = str(path) + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")
    import os
    os.replace(tmp, path)
