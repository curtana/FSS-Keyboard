"""Step 3: write the model file and its parity fixture (LLD-05 7.1). They are one pair: retraining may change weights in
the last bits, so both go into the repository together.
  graffiti-nn.bin  little endian: "FSGN", u32 version 1, u32 grid 8, u32 directions 8, u32 centre 1, f32 centre step 1.0,
                   f32 centre gain 0.25, u32 embedding 32, u32 bodies, per body f32: conv1.w(16,9,3,3) conv1.b(16)
                   conv2.w(32,16,3,3) conv2.b(32) fc.w(32,512) fc.b(32) (PyTorch order); u32 sets, per set: u8 length +
                   ASCII name, u32 body, u32 letters, per letter u16 length + UTF-8 value, u32 exemplars, per exemplar
                   u16 letter + f32[32] unit-length embedding
  nn-parity.json   strokes of the repository's test resources with what this side computes (features, embedding, best
                   letters and scores, final answer); NnParityTest compares the Kotlin side with it.
Exemplars: 32 synthetic strokes per letter from the training strokes (fixed seed) + the owner strokes that are built-in
templates (common.owner_exemplars). Usage: python export.py  -> $GRAFFITI_NN_OUT/graffiti-nn.bin, nn-parity.json"""
import json, os, pickle, struct
import numpy as np
from common import OUT, SETS, Inp, classes, load_templates, owner_exemplars, repo_strokes, set_of, t_score
from features import batch, features
from pool import training_strokes
from train import make_body

A, SUM_MIN, TEMPLATE_MIN = 0.5, 0.80, 0.78
PER_LETTER = 32


class Model:
    def __init__(self):
        import torch
        self.cl = classes()
        self.bodies = {}
        for s in SETS:
            b = make_body()
            b.load_state_dict(torch.load(os.path.join(OUT, f'body_{s}.pt')))
            b.eval()
            self.bodies[s] = b
        pool = pickle.load(open(os.path.join(OUT, 'pool.pkl'), 'rb'))
        data = training_strokes(pool)
        S = repo_strokes()
        self.ex = {}
        for s in SETS:
            cl = self.cl[s]
            y = np.array([cl.index(b) for _, b in data[s]])
            rng = np.random.default_rng(0)
            idx = np.concatenate([rng.permutation(np.where(y == c)[0])[:PER_LETTER] for c in range(y.max() + 1)])
            own = owner_exemplars(s, S)
            bz = self.embed(s, batch([data[s][i][0] for i in idx]))       # synthetic and owner exemplars embedded apart
            if own:
                bz = np.concatenate([bz, self.embed(s, batch([o['xy'] for o in own]))])
            ys = np.concatenate([y[idx], np.array([cl.index(o['label']) for o in own], int)])
            self.ex[s] = (bz, ys)

    def embed(self, s, X, bs=512):
        """Unit-length embeddings, in chunks of 512 and normalised by PyTorch (this is how the shipped file was made: other
        chunking or normalising changes the last bits)."""
        import torch, torch.nn.functional as F
        with torch.no_grad():
            X = torch.from_numpy(X)
            z = torch.cat([self.bodies[s](X[i:i + bs]) for i in range(0, len(X), bs)]) if len(X) else torch.zeros((0, 32))
            return F.normalize(z, dim=1).numpy()

    def scores(self, s, xy, inp, tsets):
        cl = self.cl[s]
        t = np.full(len(cl), -9.0, np.float32)
        for tm in tsets[s]:
            c = cl.index(tm.value)
            t[c] = max(t[c], t_score(inp, tm))
        z = self.embed(s, features(xy)[None])[0]
        bz, by = self.ex[s]
        sim = bz @ z
        n = np.array([sim[by == c].max() for c in range(len(cl))], np.float32)
        return t, n, z


def body_arrays(b):
    sd = b.state_dict()
    return [sd[k].numpy().astype('<f4') for k in ('0.weight', '0.bias', '2.weight', '2.bias', '6.weight', '6.bias')]


if __name__ == '__main__':
    m = Model()
    with open(os.path.join(OUT, 'graffiti-nn.bin'), 'wb') as f:
        f.write(b'FSGN' + struct.pack('<IIIIffI', 1, 8, 8, 1, 1.0, 0.25, 32))
        f.write(struct.pack('<I', len(SETS)))
        for s in SETS:
            for a in body_arrays(m.bodies[s]):
                f.write(a.tobytes())
        f.write(struct.pack('<I', len(SETS)))
        for i, s in enumerate(SETS):
            cl = m.cl[s]
            bz, by = m.ex[s]
            f.write(struct.pack('<B', len(s)) + s.encode('ascii') + struct.pack('<II', i, len(cl)))
            for v in cl:
                b = v.encode('utf8')
                f.write(struct.pack('<H', len(b)) + b)
            f.write(struct.pack('<I', len(bz)))
            for z, y in zip(bz, by):
                f.write(struct.pack('<H', int(y)) + z.astype('<f4').tobytes())
    print('model bytes', os.path.getsize(os.path.join(OUT, 'graffiti-nn.bin')))

    # parity fixture: every 6th synthetic dev/eval stroke of a folder, every 4th owner_en stroke, all device strokes; no rule strokes
    tsets = load_templates()
    rows, cnt = [], {}
    for s in repo_strokes():
        src = s['src']
        if s['rule'] is not None:
            continue
        cnt[src] = cnt.get(src, 0) + 1
        if src.startswith('syn_') and cnt[src] % 6 != 1:
            continue
        if src.startswith('owner_en_') and cnt[src] % 4 != 1:
            continue
        xy = np.asarray(s['xy'], '<f4').astype(np.float64)   # as Kotlin reads them
        inp = Inp(xy)
        sets = []
        for name in ([s['shift']] if s.get('shift') else []) + [set_of(s, True)]:
            cl = m.cl[name]
            t, n, z = m.scores(name, xy, inp, tsets)
            w = A * t + (1 - A) * n
            feat = features(xy)
            sets.append(dict(set=name, featSum=round(float(feat.sum()), 5), centreSum=round(float(feat[8].sum()), 5), emb=[round(float(v), 6) for v in z],
                             nn=cl[int(n.argmax())], nnScore=round(float(n.max()), 6), sum=cl[int(w.argmax())], sumScore=round(float(w.max()), 6),
                             tmpl=cl[int(t.argmax())], tmplScore=round(float(t.max()), 6)))
        ans = next((x['sum'] for x in sets if x['sumScore'] >= SUM_MIN and x['tmplScore'] >= TEMPLATE_MIN), 'Unknown')
        rows.append(dict(file=s['file'], expect=s['label'], answer=ans, sets=sets))
    json.dump(dict(a=A, sumMin=SUM_MIN, templateMin=TEMPLATE_MIN, samples=rows), open(os.path.join(OUT, 'nn-parity.json'), 'w', encoding='utf8'),
              ensure_ascii=False, separators=(',', ':'))
    print('parity samples', len(rows))
