# Graffiti stroke sets (T28, T38, LLD-11 §1.3, NFR-09)

| folder | use |
|---|---|
| `dev/` | tuning the templates (`GraffitiEvalTest` "dev set" test prints accuracy, misses and the top-3 templates of each miss) |
| `eval/` | GR-EVAL accuracy only — never looked at while tuning (NFR-09) |
| `device/` | real tablet strokes (FSS#47, GR-36 `DeviceStrokeTest`): every one must be recognized; not part of GR-EVAL |
| `tools/` | `build_templates.py` (writes `recognizer/src/main/resources/graffiti/templates.json` and `TEMPLATES.md`), `gen_strokes.py` (writes `dev/`, `eval/`), `shapes.py` (shared shapes, matcher mirror for `corners`) |

Sub-folders: `ko` (KO letter area), `en` (EN letter area), `num` (number area), `punct` (punctuation shift pending: template symbols in the number area, tap and line symbols in either area), `ext` (extended shift pending: template symbols in the letter area, line symbols in either area).

## File format

One file = one stroke: `{ "area": "LETTER", "lang": "KO", "points": [[x, y, t], …], "expect": "ㄱ" }` (LLD-11 §1.3).
- `points`: pad dp (y down) and ms on a 450 × 240 dp pad split 2 : 1 — letter area x 0..300, number area x 300..450 (FR-GRF-01).
- `expect`: what the user gets — a template's `output.value` (jamo, lower-case letter, digit, symbol, `SPACE_V`, `FINISH_J`), or for rule strokes `ㅣ` `ㅡ` `ㆍ` (KO letter area), `i` (EN letter area), `1` (number area).
- `shift` (optional, LLD-11 §1.3 since LLD 0.7): `PUNCT` or `EXTENDED` = the `expect` passed to `Recognizer.recognize`; absent = `NORMAL`.

## Where the strokes come from

**Synthetic, not collected handwriting.** `gen_strokes.py` takes each template stroke plus a few hand-drawn writer variants and applies a smooth random warp (≤ 7 %), rotation (±10°), shear (±0.12), aspect change (×0.78..1.28), corner rounding (0–2 Chaikin rounds), size 40–110 dp, position, uneven sampling (1.5–4 dp) and ±0.5 dp tremor. Rule strokes (ㅣ ㅡ ㆍ i 1, and the tap and line symbols of the punctuation and extended tables, LLD-05 §3.1 step 3): lines 25–80 dp within ±12° with a small bow, taps of 1–4 points within ±0.8 dp. Dev uses seed 1 (4 samples per skeleton, 8 per rule stroke), eval seed 2 (8 and 16). The settings were fixed before `eval/` was generated.

So GR-EVAL measures robustness of the templates to distortion of the shapes they were drawn from, not accuracy on real users. Real-device strokes (GRAFFITI 7) should replace or join these sets before TBD-12 is set.

`device/ko/` is the exception: raw touches read with `adb shell getevent` on a tablet (density 360, so px / 2.25 = dp) while "갸" (ㄱ ㅣ ㆍ ㆍ) was written over and over, shifted into the letter area (taps on the auxiliary bar left out): 84 strokes, 23 of them ㅣ. The long ㅣ strokes end in a short move back up as the finger lifts (LLD-05 §3.1 step 2 lift-off tail).

Regenerate (repository root, project venv):

```
/d/workspace/Keyboard/.venv/Scripts/python recognizer/src/test/resources/strokes/tools/build_templates.py
/d/workspace/Keyboard/.venv/Scripts/python recognizer/src/test/resources/strokes/tools/gen_strokes.py dev
```

`gen_strokes.py eval` rewrites the eval set; the samples follow the template list, so do it only when the eval set is meant to change, not while tuning.
