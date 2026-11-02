"""Writes the synthetic dev/eval stroke sets under recognizer/src/test/resources/strokes/ (T28, LLD-11 §1.3).

Run from the repository root:
    /d/workspace/Keyboard/.venv/Scripts/python recognizer/src/test/resources/strokes/tools/gen_strokes.py

These are **synthetic** strokes, not collected handwriting: each sample takes a skeleton (a template stroke or
one of the hand-drawn writer variants below), then applies a smooth random warp, rotation, shear, aspect
change, corner rounding, size, position, uneven sampling and tremor. Dev and eval use the same distortion
settings with different seeds (dev 1, eval 2); the settings were fixed before the eval set was generated and
are not tuned on it (NFR-09). Real-device strokes replace or extend these sets later (GRAFFITI 7).
"""

import json
import math
import os
import random
import shutil
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from shapes import arc, join  # noqa: E402
import build_templates as bt  # noqa: E402

STROKES = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
LETTER_RECT = (0.0, 0.0, 300.0, 240.0)    # pad dp, letter : number = 2 : 1 (FR-GRF-01)
NUMBER_RECT = (300.0, 0.0, 450.0, 240.0)
PER_SKELETON = {"dev": 4, "eval": 8}
SEEDS = {"dev": 1, "eval": 2}

# Writer variants: other ways a person draws the same output (not template copies).
WRITER = {
    "ㄱ": [[(0, 0.05), (0.9, 0), (0.75, 1)]],
    "ㄴ": [[(0.05, 0), (0, 1), (1, 0.92)]],
    "ㄷ": [join([(1, 0.05), (0.15, 0)], arc(0.15, 0.15, 0.15, 0.15, -90, -180, 4), [(0, 0.85)],
                arc(0.15, 0.85, 0.15, 0.15, 180, 90, 4), [(1, 0.97)])],
    "ㄹ": [[(0, 0.05), (0.95, 0), (0.05, 0.9), (1, 1)]],
    "ㅁ": [[(0, 0), (0.03, 1), (1, 0.97), (0.97, 0), (0.1, 0.03)]],
    "ㅂ": [[(0, 0), (0.08, 0.95), (0.92, 0.95), (1, 0)]],
    "ㅅ": [[(0, 1), (0.4, 0), (1, 0.95)]],
    "ㅇ": [arc(0.5, 0.5, 0.5, 0.45, -100, -430, 22)],
    "ㅈ": [[(1, 0.05), (0.55, 0.6), (0.25, 0.95), (0.0, 0.7), (0.05, 0.3), (0.3, 0.05), (0.55, 0.4), (0.95, 1)]],
    "ㅊ": [join([(0, 0), (0, 1), (0, 0.65)], arc(0.45, 0.7, 0.45, 0.3, 180, 360, 10), [(0.9, 1)])],
    "ㅋ": [join(arc(0.4, 0.25, 0.55, 0.25, -150, 90, 12), arc(0.4, 0.73, 0.6, 0.27, -90, 150, 12))],
    "ㄲ": [[(0, 0), (0.4, 0), (0.35, 1), (0.6, 0.02), (1, 0), (0.95, 1)]],
    "ㅃ": [[(0, 0), (0.2, 1), (0.48, 0.1), (0.75, 1), (1, 0.05)]],
    "ㅆ": [[(0, 1), (0.2, 0), (0.48, 0.9), (0.78, 0), (1, 1)]],
    "SPACE_V": [[(0, 0), (0.45, 1), (1, 0.05)]],
    "a": [[(0, 1), (0.45, 0), (1, 1)]],
    "l": [[(0.05, 0), (0, 1), (0.9, 1)]],
    "t": [[(0, 0.05), (1, 0), (0.95, 1)]],
    "v": [[(0, 0), (0.45, 1), (0.85, 0.05), (1, 0.08)]],   # V ① with a shorter hook
    "z": [[(0, 0), (1, 0.05), (0.1, 0.95), (1, 1)]],
    "0": [arc(0.5, 0.5, 0.3, 0.5, -95, -440, 22)],
    "7": [[(0, 0.05), (1, 0), (0.6, 1)]],
}


def skeletons():
    """(key, area, lang, shift, expect, [skeletons]) grouped by output, template order kept."""
    groups = {}
    for set_name, tid, _kind, value, strokes, _src, _interim in bt.TEMPLATES:
        if isinstance(strokes, str):   # symbol-table rule entry: a tap or line, written in main()
            continue
        key = (set_name, value)
        if key not in groups:
            groups[key] = (tid, [])
        groups[key][1].extend(strokes)
    out = []
    for (set_name, value), (tid, sk) in groups.items():
        sk = sk + WRITER.get(value, [])
        out.append((tid, set_name, value, sk))
    return out


def chaikin(points, rounds):
    for _ in range(rounds):
        nxt = [points[0]]
        for a, b in zip(points, points[1:]):
            nxt.append((0.75 * a[0] + 0.25 * b[0], 0.75 * a[1] + 0.25 * b[1]))
            nxt.append((0.25 * a[0] + 0.75 * b[0], 0.25 * a[1] + 0.75 * b[1]))
        nxt.append(points[-1])
        points = nxt
    return points


def densify(points, step):
    out = [points[0]]
    for a, b in zip(points, points[1:]):
        n = max(1, int(math.dist(a, b) / step))
        out += [(a[0] + (b[0] - a[0]) * i / n, a[1] + (b[1] - a[1]) * i / n) for i in range(1, n + 1)]
    return out


def distort(rng, skel, rect):
    """Skeleton in 0..1 → pad dp points of one synthetic stroke inside rect."""
    pts = chaikin([(float(x), float(y)) for x, y in skel], rng.choice([0, 0, 1, 2]))
    pts = densify(pts, 0.02)
    # Smooth warp: two low-frequency waves per axis, amplitude up to 7 % of the shape.
    w = [(rng.uniform(-0.07, 0.07), rng.uniform(1.0, 3.0), rng.uniform(0, 6.3)) for _ in range(4)]
    pts = [(x + w[0][0] * math.sin(w[0][1] * y + w[0][2]) + w[1][0] * math.sin(w[1][1] * x + w[1][2]),
            y + w[2][0] * math.sin(w[2][1] * x + w[2][2]) + w[3][0] * math.sin(w[3][1] * y + w[3][2]))
           for x, y in pts]
    rot = math.radians(rng.uniform(-10, 10))
    shear = rng.uniform(-0.12, 0.12)
    aspect = math.exp(rng.uniform(-0.25, 0.25))
    pts = [((x - 0.5) * aspect + shear * (y - 0.5), (y - 0.5) / aspect) for x, y in pts]
    pts = [(x * math.cos(rot) - y * math.sin(rot), x * math.sin(rot) + y * math.cos(rot)) for x, y in pts]
    xs = [p[0] for p in pts]
    ys = [p[1] for p in pts]
    side = max(max(xs) - min(xs), max(ys) - min(ys))
    size = rng.uniform(40, 110)
    k = size / side
    w_dp = (max(xs) - min(xs)) * k
    h_dp = (max(ys) - min(ys)) * k
    left, top, right, bottom = rect
    ox = rng.uniform(left + 4, max(left + 4, right - 4 - w_dp))
    oy = rng.uniform(top + 4, max(top + 4, bottom - 4 - h_dp))
    pts = [(ox + (x - min(xs)) * k, oy + (y - min(ys)) * k) for x, y in pts]
    return sample(rng, pts)


def sample(rng, pts):
    """Uneven sampling along the path (1.5..4 dp apart) with ±0.5 dp tremor, 8 ms per point."""
    out = [pts[0]]
    acc = 0.0
    step = rng.uniform(1.5, 4.0)
    for a, b in zip(pts, pts[1:]):
        acc += math.dist(a, b)
        if acc >= step:
            out.append(b)
            acc = 0.0
            step = rng.uniform(1.5, 4.0)
    if out[-1] != pts[-1]:
        out.append(pts[-1])
    return [[round(x + rng.gauss(0, 0.5), 1), round(y + rng.gauss(0, 0.5), 1), i * 8] for i, (x, y) in enumerate(out)]


def line(rng, rect, angle_deg):
    """Slightly bowed straight stroke, 25..80 dp, direction within ±12° of angle_deg."""
    a = math.radians(angle_deg + rng.uniform(-12, 12))
    length = rng.uniform(25, 80)
    bow = rng.uniform(-0.03, 0.03) * length
    left, top, right, bottom = rect
    cx = rng.uniform(left + 45, right - 45)
    cy = rng.uniform(top + 45, bottom - 45)
    pts = []
    for i in range(21):
        t = i / 20
        along = (t - 0.5) * length
        side = bow * 4 * t * (1 - t)
        pts.append((cx + along * math.cos(a) - side * math.sin(a), cy + along * math.sin(a) + side * math.cos(a)))
    return sample(rng, pts)


def tap(rng, rect):
    left, top, right, bottom = rect
    x = rng.uniform(left + (right - left) * 0.2, right - 10)
    y = rng.uniform(top + (bottom - top) * 0.2, bottom - 10)
    return [[round(x + rng.uniform(-0.8, 0.8), 1), round(y + rng.uniform(-0.8, 0.8), 1), i * 30] for i in range(rng.randint(1, 4))]


SET_INFO = {   # set → (folder, area, langs, shift)
    "KO_LETTER": ("ko", "LETTER", ["KO"], None),
    "EN_LETTER": ("en", "LETTER", ["EN"], None),
    "NUMBER": ("num", "NUMBER", ["KO", "EN"], None),
    "PUNCT": ("punct", "NUMBER", ["KO", "EN"], "PUNCT"),
    "EXTENDED": ("ext", "LETTER", ["KO", "EN"], "EXTENDED"),
}

# Rule strokes (LLD-05 §3.1): (folder, key, area, lang, expect, kind)
RULES = [
    ("ko", "ko_i", "LETTER", "KO", "ㅣ", 90),
    ("ko", "ko_eu", "LETTER", "KO", "ㅡ", 0),
    ("ko", "ko_dot", "LETTER", "KO", "ㆍ", "tap"),
    ("en", "en_i", "LETTER", "EN", "i", 90),
    ("num", "num_1", "NUMBER", "KO", "1", 90),
]

# Screen angle of each straight-line rule (y down, so 90 = down), for the symbol-table rule entries.
RULE_ANGLE = {"LINE_DOWN": 90, "LINE_UP": -90, "LINE_RIGHT": 0, "DIAG_DOWN_LEFT": 135, "DIAG_DOWN_RIGHT": 45}


def write(path, area, lang, points, expect, shift):
    obj = {"area": area, "lang": lang, "points": points, "expect": expect}
    if shift:
        obj["shift"] = shift
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, ensure_ascii=False, separators=(",", ":"))
        f.write("\n")


def main():
    for split in sys.argv[1:] or list(PER_SKELETON):
        per = PER_SKELETON[split]
        rng = random.Random(SEEDS[split])
        root = os.path.join(STROKES, split)
        if os.path.isdir(root):
            shutil.rmtree(root)
        count = 0
        for key, set_name, value, sks in skeletons():
            folder, area, langs, shift = SET_INFO[set_name]
            os.makedirs(os.path.join(root, folder), exist_ok=True)
            rect = LETTER_RECT if area == "LETTER" else NUMBER_RECT
            n = 0
            for sk in sks:
                for _ in range(per):
                    n += 1
                    write(os.path.join(root, folder, f"{key}_{n:02d}.json"), area, rng.choice(langs),
                          distort(rng, sk, rect), value, shift)
            count += n
        for folder, key, area, lang, expect, kind in RULES:
            rect = LETTER_RECT if area == "LETTER" else NUMBER_RECT
            for n in range(1, per * 2 + 1):
                pts = tap(rng, rect) if kind == "tap" else line(rng, rect, kind)
                write(os.path.join(root, folder, f"{key}_{n:02d}.json"), area, lang, pts, expect, None)
            count += per * 2
        # Symbol-table rule entries (LLD-05 §3.1 step 3): drawn in either area and either language while the
        # shift is pending, since the tables go by shape only (OD-5 A).
        for set_name, tid, _kind, value, rule, _src, _interim in bt.TEMPLATES:
            if not isinstance(rule, str):
                continue
            folder, _area, langs, shift = SET_INFO[set_name]
            for n in range(1, per * 2 + 1):
                area = rng.choice(["LETTER", "NUMBER"])
                rect = LETTER_RECT if area == "LETTER" else NUMBER_RECT
                pts = tap(rng, rect) if rule == "TAP" else line(rng, rect, RULE_ANGLE[rule])
                write(os.path.join(root, folder, f"{tid}_{n:02d}.json"), area, rng.choice(langs), pts, value, shift)
            count += per * 2
        print(f"{split}: {count} strokes -> {root}")


if __name__ == "__main__":
    main()
