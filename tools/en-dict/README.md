# English word list builder (`tools/en-dict`)

Rebuilds `core-suggest/src/main/resources/suggest/en_words.txt`, the 50,000-word list behind the English word
suggestions (format: LLD-12 §3; processing: LLD-12 §16.2). The same inputs give a byte-identical file.

## Files

| File | What it is |
|---|---|
| `build_en_words.py` | The tool (Python standard library only). Sub-commands `fetch`, `aggregate`, `contractions`, `build`, `check`. |
| `sources.json` | The three sources and nothing else: download URL, version and sha256 of each file (26 Ngram letter files, the SCOWL archive, the LDNOOBW `en` file at a pinned commit). The tool stops if another source is named. |
| `contractions.tsv` | Contraction table (`form`, `host`, `permille`, `extra`), written by `contractions` from the SCOWL contraction lists (LLD-12 §16.2 ⑥). Its sha256 goes into the word list header. |
| `restore.txt` | Lowercase keys to keep although LDNOOBW lists them. Empty in stage 1. |
| `test_build_en_words.py`, `testdata/` | Tests DT-01..DT-08 on tiny inputs (golden file `testdata/expected_en_words.txt`). |
| `.gitattributes` | Keeps every file here byte-exact on checkout (hashes and the golden file). |

The notices for the shipped list are the repository `NOTICE` and `core-suggest/src/main/resources/suggest/en_words_NOTICE.txt`.

## Sources (pinned)

| Source | Version | Licence | sha256 |
|---|---|---|---|
| Google Books Ngram, English 1-grams `googlebooks-eng-all-1gram-20120701-<a..z>.gz` (26 files, 4,791,444,099 bytes) | 20120701 | CC BY 3.0 Unported | per file in `sources.json`; `sha256-list` in the header = `5319088452bd5d14fec23025a07a8e4422f8c8a518e3d39b6da40a11c9568121` |
| SCOWL `scowl-2020.12.07.tar.gz` (only `final/` and `Copyright` are unpacked) | 2020.12.07 | SCOWL `Copyright` (MIT-like, WordNet notice included) | `5587667caa20c4891390c2d42dbb4d5c4c3f41bee77af1457ece3ba23fb859cc` (2,569,810 bytes) |
| LDNOOBW `en` | commit `5faf2ba42d7b1c0977169ec3611df25a3c08eb13` | CC BY 4.0 | `af851ecef1d5f212caba17339b12ac39cc2fef7d78c74876f67237644fcee8bd` (3,777 bytes) |

Nothing else is read: no subtitle frequency lists or other lists under CC BY-SA or unclear terms (NFR-15).

## Rebuild

Downloads are untrusted data: put them in a new empty folder outside the repository, run Python with `-I`, never run
anything from that folder. **Downloads and intermediate files are never committed.** Used: Python 3.14.5 (Windows, the
project venv); only the standard library.

```bash
PY=/d/workspace/Keyboard/.venv/Scripts/python      # any Python 3.12+ works
D=/d/tmp/Keyboard/T39-en-dict                      # outside the repository, deleted afterwards
$PY -I tools/en-dict/build_en_words.py fetch --dl $D/dl                     # ~4.8 GB, one file after another, sha256 checked
$PY -I tools/en-dict/build_en_words.py aggregate --dl $D/dl --work $D/work --jobs 4   # Ngram -> $D/work/<letter>.tsv
$PY -I tools/en-dict/build_en_words.py contractions --scowl $D/dl/scowl/final   # rewrites contractions.tsv (only when SCOWL changes)
$PY -I tools/en-dict/build_en_words.py build --work $D/work --scowl $D/dl/scowl/final --ldnoobw $D/dl/en
$PY -I tools/en-dict/build_en_words.py check --work $D/work --scowl $D/dl/scowl/final --ldnoobw $D/dl/en
$PY -I tools/en-dict/test_build_en_words.py                                   # DT-01..DT-08
```

`fetch` keeps files that are already present after checking their sha256, so an interrupted download can be resumed.
`check` rebuilds into a temporary file and compares it byte for byte with the committed list (DT-09).

## Rules in short (LLD-12 §16.2)

1. Ngram: years 1980..2008 summed per token (case-sensitive); tokens with `_` or other than `^[A-Za-z]+$` dropped;
   sums below 2000 dropped.
2. SCOWL levels up to 60, read as latin-1: lowercase words L (`english-words`, `american-words`), capitalised words U
   (`english-upper`, `american-upper`, `english-proper-names`, `american-proper-names`), contractions C.
3. One shape per lowercase key: `i` is `I`; a key in L stays lowercase unless its lowercase share is below 0.2 and its
   most used form is in U; a key not in L takes its most used form that is in U, or is dropped. Single letters: only
   `a` and `I`.
4. Contractions: score = count of the host word × permille // 1000 (`n't`, `'m` 50; `'re`, `'ve`, `'ll`, `'d` 20;
   `'s` 10). The table holds the SCOWL forms whose host is determined plus eleven `'s` forms SCOWL does not list.
5. Keys listed in LDNOOBW `en` are dropped, except those in `restore.txt`. The filter only keeps words out of the
   suggestions; typing them is never blocked.
6. Sorted by score (descending), then by shape; the first 50,000 are written after the format check.

To raise the data version after a rule or source change, change `DICT_VERSION` in `build_en_words.py` (`#dict`).

## Contraction table

`contractions` keeps the SCOWL contraction forms (levels up to 60) that end in exactly one of `n't`, `'m`, `'re`,
`'ve`, `'ll`, `'d`, `'s` and whose host is determined: the form without the suffix when that is a lowercase SCOWL word
of two or more letters, or `i`; fixed hosts for `can't` (can), `won't` (will), `shan't` (shall). It adds the eleven
`'s` forms with `extra = 1`. Left out for SCOWL 2020.12.07 (host not determined):
`ain't e'en e'er entr'acte h'm ma'am ne'er nor'easter o'clock o'er san'a sou'wester y'all`.

## Last build

| | |
|---|---|
| Aggregate | 1,201,784,959 Ngram lines, 0 malformed, 567,395 tokens kept (`aggregate --jobs 4`, 358 s) |
| `en_words.txt` | 50,000 words, 447,259 bytes, file sha256 `169e24ee9143e2c6639c50d69cbd3c0396eefff15a4bd8bad13551d7ed9e6520` |
| `#sha256` (word lines) | `4174ef5ec96f82fcd66496022a291c6d8af23fe9b1b607f729213a60dd4423c2` |

`check` compares bytes. A Git checkout that converts line ends (`core.autocrlf=true` on Windows) turns the committed
`en_words.txt` into CRLF; `check` then reports "only line ends differ". The files in this folder are kept byte-exact by
`.gitattributes`.
