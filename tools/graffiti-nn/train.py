"""Step 2: train one network body per set (LLD-05 3.5) on the synthetic pool only. Needs PyTorch (CPU is enough).
Writes $GRAFFITI_NN_OUT/body_<SET>.pt. Usage: python train.py [set ...]   (default: all five)"""
import os, pickle, sys
import numpy as np
from common import OUT, SETS, classes
from features import batch
from pool import training_strokes

STEPS = 3000
SEED = 0
EMB = 32


def make_body():
    import torch.nn as nn
    return nn.Sequential(nn.Conv2d(9, 16, 3, padding=1), nn.ReLU(),
                         nn.Conv2d(16, 32, 3, padding=1), nn.ReLU(), nn.MaxPool2d(2), nn.Flatten(),
                         nn.Linear(32 * 16, EMB), nn.ReLU(), nn.Dropout(0.2))


def train(setname, X, y, steps=STEPS, seed=SEED):
    """Body + a classification head (thrown away after training), AdamW, cosine schedule, label smoothing 0.05."""
    import torch, torch.nn.functional as F
    torch.manual_seed(seed * 13 + 1)
    body = make_body()
    head = torch.nn.Linear(EMB, len(classes()[setname]))
    Xt, yt = torch.from_numpy(X), torch.from_numpy(y)
    opt = torch.optim.AdamW(list(body.parameters()) + list(head.parameters()), lr=3e-3, weight_decay=0.01)
    sch = torch.optim.lr_scheduler.CosineAnnealingLR(opt, steps)
    g = torch.Generator().manual_seed(seed)
    body.train()
    bs = 64
    for _ in range(steps):
        idx = torch.randint(len(Xt), (bs,), generator=g)
        loss = F.cross_entropy(head(body(Xt[idx])), yt[idx], label_smoothing=0.05)
        opt.zero_grad(); loss.backward(); opt.step(); sch.step()
    body.eval()
    return body


if __name__ == '__main__':
    import torch
    torch.set_num_threads(int(os.environ.get("TORCH_THREADS", "4")))   # the thread count changes the last bits of the weights
    pool = pickle.load(open(os.path.join(OUT, 'pool.pkl'), 'rb'))
    data = training_strokes(pool)
    for s in sys.argv[1:] or SETS:
        cl = classes()[s]
        X = batch([a for a, _ in data[s]])
        y = np.array([cl.index(b) for _, b in data[s]])
        body = train(s, X, y)
        torch.save(body.state_dict(), os.path.join(OUT, f'body_{s}.pt'))
        print(s, 'strokes', len(y), 'saved')
