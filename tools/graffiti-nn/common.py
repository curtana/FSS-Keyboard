"""Shared pieces of the graffiti network tools: repository paths, templates and letters, the stroke rules and the template
matcher (ports of `StrokeRules` and `TemplateMatcher`, for the parity fixture), and the repository's stroke files."""
import glob, json, math, os, re
import numpy as np

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..'))
TEMPLATES = os.path.join(ROOT, 'recognizer/src/main/resources/graffiti/templates.json')
STROKES = os.path.join(ROOT, 'recognizer/src/test/resources/strokes')
SETS = ['EN_LETTER', 'KO_LETTER', 'NUMBER', 'PUNCT', 'EXTENDED']
SHORT = dict(EN_LETTER='EN', KO_LETTER='KO', NUMBER='NUM', PUNCT='PUNCT', EXTENDED='EXT')
OUT = os.environ.get('GRAFFITI_NN_OUT', os.path.join(ROOT, 'build', 'graffiti-nn'))   # work files; never committed


def plen(p):
    return float(np.hypot(*np.diff(p, axis=0).T).sum()) if len(p) > 1 else 0.0


SECTORS = ((90, 'LineDown'), (-90, 'LineUp'), (0, 'LineRight'), (180, 'LineLeft'), (135, 'DiagDownLeft'), (45, 'DiagDownRight'))


def rule_of(xy):
    """'Tap' | 'LineDown' | ... | None. Corner tap is not separated (no stroke here starts in the corner)."""
    if plen(xy) < 8 and math.hypot(*(xy.max(0) - xy.min(0))) < 8:
        return 'Tap'
    d0 = np.hypot(*(xy - xy[0]).T)
    far = int(np.argmax(d0))
    cut = len(xy) - 1
    if far != len(xy) - 1 and plen(xy[far:]) <= plen(xy[:far + 1]) * 0.2:
        cut = far
    p = xy[:cut + 1]
    L = plen(p)
    if L <= 0:
        return None
    dx, dy = p[-1] - p[0]
    if math.hypot(dx, dy) / L < 0.92:
        return None
    th = math.degrees(math.atan2(dy, dx))
    for c, name in SECTORS:
        d = abs(th - c) % 360
        d = 360 - d if d > 180 else d
        if d <= 20:
            return name
    return None


RULE_NAME = dict(TAP='Tap', LINE_DOWN='LineDown', LINE_UP='LineUp', LINE_RIGHT='LineRight', DIAG_DOWN_LEFT='DiagDownLeft', DIAG_DOWN_RIGHT='DiagDownRight')

def set_of(st, normal=False):
    if st.get('shift') and not normal:
        return st['shift']
    if st['area'] == 'NUMBER':
        return 'NUMBER'
    return 'KO_LETTER' if st['lang'] == 'KO' else 'EN_LETTER'


N = 32
SQRT2 = math.sqrt(2)


def resample(pts, n=N):
    pts = [(float(a), float(b)) for a, b in pts]
    total = sum(math.dist(pts[i - 1], pts[i]) for i in range(1, len(pts)))
    if total == 0:
        return np.array([pts[0]] * n)
    interval = total / (n - 1)
    out = [pts[0]]
    acc = 0.0
    px, py = pts[0]
    for qx, qy in pts[1:]:
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
        out.append(pts[-1])
    return np.array(out)


def normalize(p):
    side = np.ptp(p, axis=0).max()
    k = 1.0 / side if side > 0 else 1.0
    return (p - p.mean(0)) * k


def corner_count(p):
    w = 2
    u = p[w:-w] - p[:-2 * w]
    v = p[2 * w:] - p[w:-w]
    nn = np.hypot(*u.T) * np.hypot(*v.T)
    with np.errstate(invalid='ignore', divide='ignore'):
        c = np.where(nn > 0, (u * v).sum(1) / nn, 1.0)
    turn = np.degrees(np.arccos(np.clip(c, -1, 1)))
    is_c = turn >= 60.0
    return int(is_c[0]) + int(((~is_c[:-1]) & is_c[1:]).sum())


def loop_of(xy):
    if len(xy) < 3:
        return None
    side = np.ptp(xy, axis=0).max()
    if side <= 0:
        return None
    r = resample(xy, 64)
    d = np.diff(r, axis=0)
    a = np.arctan2(d[:, 1], d[:, 0])
    t = np.degrees(np.diff(a))
    t = (t + 180) % 360 - 180
    t[t == -180] = 180
    cum = np.concatenate([[0, 0], np.cumsum(t)])    # cum[i+1] = turning up to point i
    total = abs(cum[-1])
    if total < 300 or total > 540:
        return None
    end = len(r) - 1
    if total > 360:
        end = next(i for i in range(1, len(r)) if abs(cum[i]) >= 360)
    lap = r[:end + 1]
    if math.dist(lap[0], lap[-1]) > 0.35 * side:
        return None
    for j in range(end + 1, len(r)):
        if np.hypot(*(lap - r[j]).T).min() > 0.15 * side:
            return None
    return np.concatenate([lap, r[:1]])


def cyclic(a, b):
    m = len(a) - 1
    aa = a[:m]
    best = -1e9
    for k in range(m):
        s = np.hypot(*(np.roll(aa, -k, axis=0) - b[:m]).T).sum()
        best = max(best, 1.0 - s / m / SQRT2)
    return best


class Inp:
    """A stroke prepared for matching."""
    __slots__ = ('n', 'c', 'lap')

    def __init__(self, xy):
        self.n = normalize(resample(xy))
        self.c = corner_count(self.n)
        lp = None if self.c >= 2 else loop_of(xy)
        self.lap = None if lp is None else normalize(resample(lp))


class Tmpl:
    __slots__ = ('id', 'value', 'corners', 'norm', 'closed')

    def __init__(self, id, value, corners, strokes):
        self.id, self.value, self.corners = id, value, corners
        self.norm = [normalize(resample(s)) for s in strokes]
        self.closed = [len(s) > 2 and math.dist(s[0], s[-1]) <= 0.05 for s in strokes]


def t_score(inp, t):
    """Template score for one input, corner aid included (float32 like Kotlin)."""
    best = -9.0
    for tn, cl in zip(t.norm, t.closed):
        s = 1.0 - np.hypot(*(inp.n - tn).T).mean() / SQRT2
        if inp.lap is not None and t.corners == 0 and cl:
            s = max(s, cyclic(inp.lap, tn))
        best = max(best, float(np.float32(s)))
    if abs(inp.c - t.corners) >= 2:
        best = float(np.float32(best) - np.float32(0.1))
    return best




def load_templates(path=TEMPLATES):
    """{set: [Tmpl]} of the template file; rule entries are left out (they are no letters)."""
    d = json.load(open(path, encoding='utf8'))
    out = {}
    for name, lst in d['sets'].items():
        out[name] = [Tmpl(t['id'], t['output']['value'], t['corners'], [np.array(s, dtype=np.float64) for s in t['strokes']])
                     for t in lst if not t.get('rule')]
    return out


def is_owner_tmpl(tid):
    return re.fullmatch(r'(ko|en)_.*_o\d+', tid) is not None


def classes(tsets=None):
    """{set: [values]}: the letters of each set = template outputs in file order (GM-02 checks the model has the same)."""
    tsets = tsets or load_templates()
    out = {}
    for k, lst in tsets.items():
        vs = []
        for t in lst:
            if t.value not in vs:
                vs.append(t.value)
        out[k] = vs
    return out


def _mk(xy, label, src, set_name, lang, area='LETTER', shift=None, name=None):
    xy = np.array([[p[0], p[1]] for p in xy], dtype=np.float64)
    return dict(xy=xy, label=label, src=src, set=set_name, lang=lang, area=area, shift=shift, name=name, rule=rule_of(xy))


def repo_strokes():
    """The repository's stroke files: dev/eval (synthetic), device/device_held (owner's device strokes), owner_en (owner's English strokes)."""
    S = []
    for split, src in (('dev', 'syn_dev'), ('eval', 'syn_eval'), ('device', 'device'), ('device_held', 'held')):
        for f in sorted(glob.glob(f'{STROKES}/{split}/*/*.json')):
            o = json.load(open(f, encoding='utf8'))
            S.append(_mk(o['points'], o['expect'], f'{src}_{os.path.basename(os.path.dirname(f))}', None, o['lang'], o['area'], o.get('shift'), name=os.path.basename(f)[:-5]))
            S[-1]['set'] = set_of(S[-1])
            S[-1]['file'] = os.path.relpath(f, STROKES).replace(os.sep, '/')
    for f in sorted(glob.glob(f'{STROKES}/owner_en/*/*.json')):
        o = json.load(open(f, encoding='utf8'))
        S.append(_mk(o['points'], o['expect'], 'owner_en_' + os.path.basename(os.path.dirname(f)), 'EN_LETTER', 'EN', name=os.path.basename(f)[:-5]))
        S[-1]['file'] = os.path.relpath(f, STROKES).replace(os.sep, '/')
    return S


def owner_exemplars(setname, S=None):
    """The owner's strokes that are built-in templates (ids _oN), already in the repository: owner_en/ (156) and
    tools/owner_strokes.json (117, ㅋ left out as in the templates). Shipped as exemplars, never trained on."""
    if setname == 'EN_LETTER':
        S = S or repo_strokes()
        return [s for s in S if s['src'].startswith('owner_en_') and re.fullmatch(r'en_[a-z]_o\d+', s['name']) and s['rule'] is None]
    if setname == 'KO_LETTER':
        d = json.load(open(os.path.join(STROKES, 'tools/owner_strokes.json'), encoding='utf8'))
        # In recording order: round by round, one stroke of each letter (the first round had no ㅎ, the last ㅍ came
        # after the rounds). The order changes nothing in recognition, only the bytes of the file.
        left = {v: list(lst) for v, lst in d.items() if v != 'ㅋ'}
        order = []
        for r in range(max(len(v) for v in left.values())):
            for v in left:
                if (v, r) == ('ㅎ', 0) or not left[v]:
                    continue
                if v == 'ㅎ' or r < len(d[v]) - (1 if v == 'ㅍ' else 0):
                    order.append((v, left[v].pop(0)))
        order += [(v, xy) for v in left for xy in left[v]]
        out = []
        for value, xy in order:
            s = _mk(xy, value, 'owner_ko', 'KO_LETTER', 'KO')
            if s['rule'] is None:
                out.append(s)
        return out
    return []
