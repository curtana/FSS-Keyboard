"""Stroke features of the graffiti network (LLD-05 3.4): 8 x 8 grid x (8 directions + 1 centre) = 576 values.

The Kotlin side (`kbd.graffiti.nn.StrokeFeatures`) computes the same values in the same order; the shipped weights were
trained on what this file gives, so change both or neither.
"""
import numpy as np

GRID = 8
DIRECTIONS = 8
FILL = 0.875
RESAMPLE = 12 * GRID          # 96 points by path length
CENTRE_STEP_DP = 1.0          # a raw step shorter than this counts in the centre channel
CENTRE_GAIN = 0.25            # 4 such steps = 1


def lin_resample(xy, n):
    """n points at equal path length along xy (linear between the raw points); all the first point when the path is 0."""
    seg = np.hypot(*np.diff(xy, axis=0).T)
    cum = np.concatenate([[0], np.cumsum(seg)])
    if cum[-1] == 0:
        return np.repeat(xy[:1], n, 0)
    t = np.linspace(0, cum[-1], n)
    return np.stack([np.interp(t, cum, xy[:, 0]), np.interp(t, cum, xy[:, 1])], 1)


def directions(xy, G=GRID, D=DIRECTIONS):
    """[D][G][G]: path length per cell, each step's angle split between its two neighbouring directions, clipped to 1."""
    side = max(np.ptp(xy[:, 0]), np.ptp(xy[:, 1]))
    c = (xy.max(0) + xy.min(0)) / 2
    q = (lin_resample(xy, 12 * G) - c) / (side if side > 0 else 1) * (FILL * G) + G / 2
    d = np.diff(q, axis=0)
    ln = np.hypot(d[:, 0], d[:, 1])
    mid = (q[1:] + q[:-1]) / 2
    cx = np.clip(mid[:, 0].astype(int), 0, G - 1)
    cy = np.clip(mid[:, 1].astype(int), 0, G - 1)
    a = np.mod(np.arctan2(d[:, 1], d[:, 0]), 2 * np.pi) / (2 * np.pi / D)
    b0 = np.floor(a).astype(int) % D
    fr = a - np.floor(a)
    img = np.zeros((D, G, G), np.float32)
    np.add.at(img, (b0, cy, cx), ln * (1 - fr))          # all first shares, then all second shares (as Kotlin)
    np.add.at(img, ((b0 + 1) % D, cy, cx), ln * fr)
    return np.minimum(img, 1.0)


def features(xy, G=GRID):
    """[9][G][G] float32 of one stroke: the raw sample points in dp, before any resampling."""
    xy = np.asarray(xy, np.float64)
    img = directions(xy, G, DIRECTIONS)
    side = max(np.ptp(xy[:, 0]), np.ptp(xy[:, 1]))
    c = (xy.max(0) + xy.min(0)) / 2
    q = (xy - c) / (side if side > 0 else 1) * (FILL * G) + G / 2
    step = np.hypot(*np.diff(xy, axis=0).T)
    mid = (q[1:] + q[:-1]) / 2
    cx = np.clip(mid[:, 0].astype(int), 0, G - 1)
    cy = np.clip(mid[:, 1].astype(int), 0, G - 1)
    cen = np.zeros((1, G, G), np.float32)
    s = step < CENTRE_STEP_DP
    np.add.at(cen, (0, cy[s], cx[s]), CENTRE_GAIN)
    return np.concatenate([img, np.minimum(cen, 1.0)])


def batch(xys):
    return np.stack([features(x) for x in xys]) if len(xys) else np.zeros((0, 9, GRID, GRID), np.float32)
