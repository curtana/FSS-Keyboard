"""Step 1: the synthetic training pool (LLD-05 7.1). Only generated strokes: the repository's dev folder plus the stroke
generator of strokes/tools/gen_strokes.py with other seeds, and loops at any start for the closed shapes. No human stroke.
Writes $GRAFFITI_NN_OUT/pool.pkl. Usage: python pool.py"""
import os, pickle, random, sys
import numpy as np
from common import OUT, STROKES, is_owner_tmpl, load_templates, rule_of, repo_strokes, classes, SETS

sys.path.insert(0, os.path.join(STROKES, 'tools'))
import gen_strokes as gs        # noqa: E402  (functions only; its main() rewrites dev/eval)
import build_templates as bt    # noqa: E402
import math                     # noqa: E402

PER = 16                 # strokes per skeleton stroke and seed
POOL_SEEDS = [101, 102]
LOOP_SEED = 103
LOOPS_PER_DIR = 40


def skeletons():
    sk = list(gs.skeletons())
    for set_name, tid, _k, value, strokes, _s, _i in bt.EXTRA_TEMPLATES:   # en_q2 is a drawn template the generator leaves out
        for i, (a, b, val, lst) in enumerate(sk):
            if b == set_name and val == value:
                sk[i] = (a, b, val, lst + strokes)
    return sk


def rect_of(set_name):
    return gs.NUMBER_RECT if gs.SET_INFO[set_name][1] == 'NUMBER' else gs.LETTER_RECT


def arc_stroke(rng, rect, sign):
    """A hand-like ellipse lap at a random start, short of closing or past it."""
    start = rng.uniform(0, 360)
    sweep = rng.uniform(320, 420) * sign
    rx = 0.5
    ry = 0.5 * math.exp(rng.uniform(-0.3, 0.3))
    pts = [(rx * math.cos(math.radians(start + sweep * i / 60)), ry * math.sin(math.radians(start + sweep * i / 60))) for i in range(61)]
    return gs.distort(rng, pts, rect)


def generated():
    pool = []
    sk = skeletons()
    for seed in POOL_SEEDS:
        rng = random.Random(seed)
        for tid, set_name, value, strokes in sk:
            rect = rect_of(set_name)
            for s in strokes:
                for _ in range(PER):
                    xy = np.array([[p[0], p[1]] for p in gs.distort(rng, s, rect)])
                    if rule_of(xy) is None:
                        pool.append(dict(xy=xy, label=value, set=set_name, kind='gen'))
    rng = random.Random(LOOP_SEED)
    closed = sorted({(name, t.value) for name, lst in load_templates().items() for t in lst
                     if not is_owner_tmpl(t.id) and t.corners == 0 and any(t.closed)})
    for name, value in closed:
        for sign in ([-1] if value == '@' else [-1, 1]):   # @ is drawn one way only
            for _ in range(LOOPS_PER_DIR):
                xy = np.array([[p[0], p[1]] for p in arc_stroke(rng, rect_of(name), sign)])
                if rule_of(xy) is None:
                    pool.append(dict(xy=xy, label=value, set=name, kind='loop'))
    return pool


def training_strokes(pool):
    """{set: [(xy, label)]}: the dev folder (synthetic) then the generated pool."""
    S = repo_strokes()
    cl = classes()
    folder = dict(EN_LETTER='en', KO_LETTER='ko', NUMBER='num', PUNCT='punct', EXTENDED='ext')
    out = {}
    for s in SETS:
        items = [(x['xy'], x['label']) for x in S if x['src'] == 'syn_dev_' + folder[s] and x['rule'] is None and x['label'] in cl[s]]
        items += [(p['xy'], p['label']) for p in pool if p['set'] == s]
        out[s] = items
    return out


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    pool = generated()
    pickle.dump(pool, open(os.path.join(OUT, 'pool.pkl'), 'wb'))
    import collections
    print('pool', len(pool), dict(collections.Counter(p['set'] for p in pool)))
