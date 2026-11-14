# Graffiti network tools

The scripts that make the graffiti model file `recognizer/src/main/resources/graffiti/graffiti-nn.bin` and its parity
fixture `recognizer/src/test/resources/graffiti/nn-parity.json` (LLD-05 3.4, 3.5, 7.1; LLD-11 6.7), and the device timing
tool. Nothing here runs in the app or in the Gradle build.

| File | What it does |
|---|---|
| `features.py` | Stroke features: 8 × 8 grid × (8 directions + centre) = 576 values. Same values and order as `kbd.graffiti.nn.StrokeFeatures` |
| `common.py` | Paths, templates and letters, ports of the stroke rules and the template matcher (for the fixture), the repository's stroke files, the owner exemplars |
| `pool.py` | Step 1: synthetic training pool (the stroke generator of `strokes/tools/gen_strokes.py`, seeds 101·102, and loops at any start, seed 103) |
| `train.py` | Step 2: one body per letter set, 3,000 steps, seed 0, on the dev folder + the pool (synthetic strokes only) |
| `export.py` | Step 3: model file (bodies + exemplars) and parity fixture |
| `device_bench.sh`, `bench_pack.py` | Time one stroke on a connected device without touching the installed keyboard (LLD-11 6.7 (마)) |

## Making the model again

Needs Python 3 with NumPy and PyTorch (CPU). Measured with Python 3.14, NumPy 2.5.4, PyTorch 2.15.0+cpu. From this folder:

```
GRAFFITI_NN_OUT=<work folder> python pool.py
GRAFFITI_NN_OUT=<work folder> TORCH_THREADS=1 python train.py EN_LETTER KO_LETTER
GRAFFITI_NN_OUT=<work folder> TORCH_THREADS=4 python train.py NUMBER PUNCT EXTENDED
GRAFFITI_NN_OUT=<work folder> python export.py
```

then copy `graffiti-nn.bin` and `nn-parity.json` from the work folder to the two places above **together** (they are one
pair) and run `./gradlew :recognizer:test`. The work folder (default `build/graffiti-nn` at the repository root) is never
committed.

- **What goes into the weights:** generated strokes only — the repository's `dev` folder and the generator's other seeds.
  No owner or user stroke is trained on (SRS FR-GRF-13 ⑤).
- **Exemplars:** 32 generated strokes per letter (fixed seed) and the owner strokes that are built-in templates: the
  `en_<letter>_oN` files of `strokes/owner_en/` (156) and `strokes/tools/owner_strokes.json` without ㅋ (117).
- **Same bits:** the thread count changes the last bits of the trained weights. The shipped file's English and Korean
  bodies were trained with one thread, the other three with four (the commands above). With these versions the steps
  give the shipped pool, all five bodies and every exemplar except the 117 Korean owner exemplars bit for bit. Those 117
  come from the original recording; `owner_strokes.json` is its copy rounded to 0.1 dp, so its embeddings differ by up
  to 0.021 per value. On the 440 fixture strokes the answers are all the same and the scores differ by at most 0.0022
  (letter network score) — so the fixture of the shipped file is not the one `export.py` writes today. Other library
  versions may change more bits; a changed model always goes in with its own fixture.
- If the templates' letters or the built-in owner strokes change, the model and the fixture must be made again
  (`GraffitiModelTest` GM-02 fails otherwise).
