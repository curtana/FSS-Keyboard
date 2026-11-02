"""Shape helpers shared by build_templates.py and gen_strokes.py (T28).

Coordinates are screen-like: x grows right, y grows down. Angles are screen angles in degrees
(0 = right, 90 = down, -90 = up); a decreasing angle runs counter-clockwise on screen.
Everything here is drawn from the stroke descriptions in the graffiti rules document (kept outside the repository,
issue #249; sections 4.2, 6.1, 6.3, 6.4); no GPL code and no copied figures (SRS CON-6).
"""

import math


def arc(cx, cy, rx, ry, a0, a1, steps=None):
    """Ellipse arc from a0 to a1 (screen angles), both ends included."""
    if steps is None:
        steps = max(4, int(abs(a1 - a0) / 15))
    return [
        (cx + rx * math.cos(math.radians(a0 + (a1 - a0) * i / steps)),
         cy + ry * math.sin(math.radians(a0 + (a1 - a0) * i / steps)))
        for i in range(steps + 1)
    ]


def join(*parts):
    """Concatenate point lists, dropping a point equal to the previous one."""
    out = []
    for part in parts:
        for p in part:
            if not out or abs(out[-1][0] - p[0]) > 1e-9 or abs(out[-1][1] - p[1]) > 1e-9:
                out.append(p)
    return out


def fit_box(points):
    """Shift to the origin and scale the long side to 1, same factor on both axes (stays inside 0..1)."""
    xs = [p[0] for p in points]
    ys = [p[1] for p in points]
    side = max(max(xs) - min(xs), max(ys) - min(ys)) or 1.0
    return [((x - min(xs)) / side, (y - min(ys)) / side) for x, y in points]


# --- Matcher mirror (TemplateMatcher.resample / normalize / cornerCount, LLD-05 §3.2) ---------------

N = 32
CORNER_WINDOW = 2
CORNER_MIN_DEG = 60.0


def resample(points, n=N):
    total = sum(math.dist(points[i - 1], points[i]) for i in range(1, len(points)))
    if total == 0:
        return [points[0]] * n
    interval = total / (n - 1)
    out = [points[0]]
    acc = 0.0
    px, py = points[0]
    for qx, qy in points[1:]:
        d = math.hypot(qx - px, qy - py)
        while acc + d >= interval and len(out) < n - 1:
            f = (interval - acc) / d
            px += f * (qx - px)
            py += f * (qy - py)
            out.append((px, py))
            d = math.hypot(qx - px, qy - py)
            acc = 0.0
        acc += d
        px, py = qx, qy
    while len(out) < n:
        out.append(points[-1])
    return out


def normalize(points):
    cx = sum(p[0] for p in points) / len(points)
    cy = sum(p[1] for p in points) / len(points)
    xs = [p[0] for p in points]
    ys = [p[1] for p in points]
    side = max(max(xs) - min(xs), max(ys) - min(ys))
    k = 1.0 / side if side > 0 else 1.0
    return [((x - cx) * k, (y - cy) * k) for x, y in points]


def _turn(a, b, c):
    ux, uy = b[0] - a[0], b[1] - a[1]
    vx, vy = c[0] - b[0], c[1] - b[1]
    norm = math.hypot(ux, uy) * math.hypot(vx, vy)
    if norm == 0:
        return 0.0
    return math.degrees(math.acos(max(-1.0, min(1.0, (ux * vx + uy * vy) / norm))))


def corner_count(points):
    pts = normalize(resample(points))
    corners = 0
    in_run = False
    for i in range(CORNER_WINDOW, len(pts) - CORNER_WINDOW):
        is_corner = _turn(pts[i - CORNER_WINDOW], pts[i], pts[i + CORNER_WINDOW]) >= CORNER_MIN_DEG
        if is_corner and not in_run:
            corners += 1
        in_run = is_corner
    return corners
