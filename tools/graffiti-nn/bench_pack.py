"""Helper of device_bench.sh: packs the dex and the recognizer's resources into one jar and writes the strokes to time.

Usage (repository root): bench_pack.py <classes.dex> <out jar> <out strokes.jsonl>
The strokes are the repository's own files: the real strokes of device/ and device_held/ and the synthetic eval set.
"""
import glob
import json
import os
import sys
import zipfile

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RESOURCES = os.path.join(ROOT, "recognizer", "src", "main", "resources")
STROKES = os.path.join(ROOT, "recognizer", "src", "test", "resources", "strokes")


def main(dex, out_jar, out_strokes):
    with zipfile.ZipFile(out_jar, "w", zipfile.ZIP_DEFLATED) as z:
        z.write(dex, "classes.dex")
        for name in ("templates.json", "graffiti-nn.bin"):
            z.write(os.path.join(RESOURCES, "graffiti", name), "graffiti/" + name)
    n = 0
    with open(out_strokes, "w", encoding="utf-8", newline="\n") as f:
        for split in ("device", "device_held", "eval"):
            for path in sorted(glob.glob(os.path.join(STROKES, split, "*", "*.json"))):
                with open(path, encoding="utf-8") as g:
                    o = json.load(g)
                row = dict(area=o["area"], lang=o["lang"], shift=o.get("shift", "NORMAL"), expect=o["expect"],
                           group=split + "/" + os.path.basename(os.path.dirname(path)), points=[p[:2] for p in o["points"]])
                f.write(json.dumps(row, ensure_ascii=False, separators=(",", ":")) + "\n")
                n += 1
    print(f"bench jar {os.path.getsize(out_jar)} bytes, {n} strokes")


if __name__ == "__main__":
    main(*sys.argv[1:4])
