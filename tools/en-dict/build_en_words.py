"""Builds the shipped English word list `core-suggest/src/main/resources/suggest/en_words.txt` (LLD-12 §3, §16).

Sub-commands (standard library only; downloaded files are untrusted data, so run with `python -I`):
  fetch         --dl <dir>                          download the sources of sources.json one after another, check sha256,
                                                    unpack SCOWL `final/` and `Copyright` into <dir>/scowl
  aggregate     --dl <dir> --work <dir> [--jobs n]  Ngram 1-grams -> <work>/<letter>.tsv (token \t sum of 1980..2008)
  contractions  --scowl <final dir> --out <file>    write the contraction table from the SCOWL contraction lists
  build         --work <dir> --scowl <final dir> --ldnoobw <file> --out <file>
  check         --work <dir> --scowl <final dir> --ldnoobw <file> [--expected <file>]
                                                    build into a temporary file and compare it byte for byte
Same inputs give a byte-identical file: no dates, times, machine names or set/dict iteration order reach the output.
"""
import argparse
import glob
import gzip
import hashlib
import json
import os
import re
import shutil
import sys
import tarfile
import tempfile
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, '..', '..'))
WORDS_FILE = os.path.join(ROOT, 'core-suggest', 'src', 'main', 'resources', 'suggest', 'en_words.txt')
SOURCES_FILE = os.path.join(HERE, 'sources.json')
CONTRACTIONS_FILE = os.path.join(HERE, 'contractions.tsv')
RESTORE_FILE = os.path.join(HERE, 'restore.txt')

FORMAT_VERSION = 1          # `#fss-words`: raise when LLD-12 §3 changes
DICT_VERSION = '2026.11.1'  # `#dict`: raise when the data is rebuilt with other inputs or rules
TOOL_VERSION = 1            # `#tool`
YEAR_FROM, YEAR_TO, MIN_COUNT = 1980, 2008, 2000
SCOWL_LEVEL = 60
MAX_WORDS = 50000
MAX_LEN = 32                # WordChars.MAX_WORD
LOWER_SHARE = 0.2           # §16.2 ④: below this share of lowercase use a capitalised SCOWL form wins

SOURCE_KEYS = ('ngram', 'scowl', 'ldnoobw')
NGRAM_NAME = 'googlebooks-eng-all-1gram-20120701-{}.gz'
LETTERS = 'abcdefghijklmnopqrstuvwxyz'

WORD_RE = re.compile(r"[A-Za-z]+(?:'[A-Za-z]+)*")
NGRAM_TOKEN_RE = re.compile(rb'[A-Za-z]+')
LOWER_RE = re.compile(r'[a-z]+')
UPPER_RE = re.compile(r'[A-Z][A-Za-z]*')
SHA_RE = re.compile(r'[0-9a-f]{64}')
COMMIT_RE = re.compile(r'[0-9a-f]{40}')
NAME_RE = re.compile(r'[A-Za-z0-9][A-Za-z0-9._-]*')

# §16.2 ⑥ contraction table: suffix -> permille, irregular n't hosts, the `'s` forms SCOWL does not list
SUFFIX_PERMILLE = (("n't", 50), ("'m", 50), ("'re", 20), ("'ve", 20), ("'ll", 20), ("'d", 20), ("'s", 10))
IRREGULAR_HOSTS = {"can't": 'can', "won't": 'will', "shan't": 'shall'}
UNDETERMINED = ("ain't",)   # the part before n't is no word; a one-letter host other than i (h'm) is none either
EXTRA_S_FORMS = ("it's", "that's", "he's", "she's", "there's", "what's", "here's", "who's", "where's", "how's", "let's")
CONTRACTIONS_HEADER = 'form\thost\tpermille\textra'


class BuildError(Exception):
    """Stops the tool with a non-zero exit code; no output file is written."""


# ---------------------------------------------------------------- sources and checksums

def sha256_file(path):
    h = hashlib.sha256()
    with open(path, 'rb') as f:
        for block in iter(lambda: f.read(1 << 20), b''):
            h.update(block)
    return h.hexdigest()


def load_sources(path=SOURCES_FILE):
    """Reads sources.json and checks it names exactly the three sources (NFR-15: nothing else is read)."""
    with open(path, encoding='utf-8') as f:
        src = json.load(f)
    if not isinstance(src, dict) or sorted(src) != sorted(SOURCE_KEYS):
        raise BuildError('sources.json must name exactly %s, found %s'
                         % (', '.join(SOURCE_KEYS), ', '.join(sorted(src)) if isinstance(src, dict) else type(src).__name__))
    for key in SOURCE_KEYS:
        files = src[key].get('files') if isinstance(src[key], dict) else None
        if not files:
            raise BuildError('sources.json: %s has no files' % key)
        for e in files:
            if not NAME_RE.fullmatch(e.get('name', '')) or not str(e.get('url', '')).startswith('https://') \
                    or not SHA_RE.fullmatch(e.get('sha256', '')):
                raise BuildError('sources.json: bad entry in %s: %r' % (key, e))
    names = [e['name'] for e in src['ngram']['files']]
    if names != [NGRAM_NAME.format(c) for c in LETTERS]:
        raise BuildError('sources.json: ngram must list the 26 letter files a..z in order')
    if len(src['scowl']['files']) != 1 or len(src['ldnoobw']['files']) != 1:
        raise BuildError('sources.json: scowl and ldnoobw have one file each')
    if not COMMIT_RE.fullmatch(str(src['ldnoobw'].get('commit', ''))):
        raise BuildError('sources.json: ldnoobw needs the pinned 40-hex commit')
    return src


def verify_file(path, sha256):
    """The check of `fetch`: stops when the file's sha256 is not the recorded one."""
    actual = sha256_file(path)
    if actual != sha256:
        raise BuildError('sha256 mismatch for %s: expected %s, got %s' % (path, sha256, actual))


def cmd_fetch(args):
    src = load_sources(args.sources)
    os.makedirs(args.dl, exist_ok=True)
    for key in SOURCE_KEYS:                     # one file at a time: parallel writes corrupted a file before
        for e in src[key]['files']:
            path = os.path.join(args.dl, e['name'])
            if not os.path.exists(path):
                part = path + '.part'
                print('fetch', e['url'], flush=True)
                with urllib.request.urlopen(e['url']) as r, open(part, 'wb') as o:
                    shutil.copyfileobj(r, o, 1 << 20)
                try:
                    verify_file(part, e['sha256'])
                except BuildError:
                    os.remove(part)
                    raise
                os.replace(part, path)
            else:
                verify_file(path, e['sha256'])
            print('ok', e['name'], flush=True)
    unpack_scowl(os.path.join(args.dl, src['scowl']['files'][0]['name']), os.path.join(args.dl, 'scowl'))
    return 0


def unpack_scowl(archive, dest):
    """Unpacks only `final/` and `Copyright` (top folder stripped) with the tarfile `data` filter."""
    with tarfile.open(archive, 'r:gz') as tf:
        members = []
        for m in tf.getmembers():
            parts = m.name.split('/', 1)
            if len(parts) == 2 and m.isfile() and (parts[1] == 'Copyright' or parts[1].startswith('final/')):
                m.name = parts[1]
                members.append(m)
        tf.extractall(dest, members=members, filter='data')
    print('unpacked', len(members), 'files into', dest, flush=True)


# ---------------------------------------------------------------- aggregate (§16.2 ②)

def aggregate_file(src, dst):
    """Ngram lines `token \\t year \\t match_count \\t volume_count` -> `token \\t sum`, sorted by token."""
    sums = {}
    last, ok = None, False
    lines = bad = 0
    with gzip.open(src, 'rb') as f:
        for line in f:
            lines += 1
            parts = line.split(b'\t')
            if len(parts) != 4:
                bad += 1
                continue
            tok = parts[0]
            if tok != last:                     # lines of one token are consecutive: test it once
                last, ok = tok, NGRAM_TOKEN_RE.fullmatch(tok) is not None
            if not ok:
                continue
            try:
                year, count = int(parts[1]), int(parts[2])
            except ValueError:
                bad += 1
                continue
            if YEAR_FROM <= year <= YEAR_TO:
                sums[tok] = sums.get(tok, 0) + count
    kept = sorted((t.decode('ascii'), c) for t, c in sums.items() if c >= MIN_COUNT)
    with open(dst, 'w', encoding='utf-8', newline='\n') as o:
        for t, c in kept:
            o.write('%s\t%d\n' % (t, c))
    return os.path.basename(src), lines, bad, len(kept)


def cmd_aggregate(args):
    os.makedirs(args.work, exist_ok=True)
    jobs = []
    for c in LETTERS:
        src = os.path.join(args.dl, NGRAM_NAME.format(c))
        if not os.path.exists(src):
            raise BuildError('missing ' + src)
        jobs.append((src, os.path.join(args.work, c + '.tsv')))
    if args.jobs > 1:
        import multiprocessing
        with multiprocessing.Pool(args.jobs) as pool:
            for r in pool.starmap(aggregate_file, jobs, chunksize=1):
                print('%s lines=%d malformed=%d kept=%d' % r, flush=True)
    else:
        for job in jobs:
            print('%s lines=%d malformed=%d kept=%d' % aggregate_file(*job), flush=True)
    return 0


# ---------------------------------------------------------------- SCOWL (§16.2 ③)

def scowl_lines(final_dir, prefixes):
    out = []
    for p in prefixes:
        for path in sorted(glob.glob(os.path.join(final_dir, p + '.*'))):
            level = path.rsplit('.', 1)[1]
            if level.isdigit() and int(level) <= SCOWL_LEVEL:
                with open(path, encoding='latin-1') as f:
                    out.extend(line.rstrip('\r\n') for line in f)
    return out


def read_scowl(final_dir):
    """(L, U, C): lowercase words, capitalised words, contractions (lowercased); levels <= 60, read as latin-1."""
    if not os.path.isdir(final_dir):
        raise BuildError('SCOWL final/ folder not found: ' + final_dir)
    lower = {w for w in scowl_lines(final_dir, ('english-words', 'american-words')) if LOWER_RE.fullmatch(w)}
    upper = {w for w in scowl_lines(final_dir, ('english-upper', 'american-upper', 'english-proper-names',
                                                'american-proper-names')) if UPPER_RE.fullmatch(w)}
    contr = {w.lower() for w in scowl_lines(final_dir, ('english-contractions',)) if w}
    return lower, upper, contr


# ---------------------------------------------------------------- contraction table (§16.2 ⑥)

def contraction_rows(lower, contr):
    """Rows (form, host, permille, extra) of the fixed table and the SCOWL forms left out (host not determined)."""
    rows, left_out = [], []
    for c in sorted(contr):
        row = None
        if c.count("'") == 1:
            for suffix, permille in SUFFIX_PERMILLE:
                if c.endswith(suffix):
                    host = IRREGULAR_HOSTS.get(c, c[:-len(suffix)])
                    if c not in UNDETERMINED and (c in IRREGULAR_HOSTS or host == 'i' or (len(host) > 1 and host in lower)):
                        form = 'I' + c[1:] if host == 'i' else c
                        row = (form, host, permille, 0)
                    break
        if row:
            rows.append(row)
        else:
            left_out.append(c)
    have = {r[0].lower() for r in rows}
    for form in EXTRA_S_FORMS:
        if form not in have:
            rows.append((form, form[:-2], 10, 1))
    rows.sort()
    return rows, left_out


def cmd_contractions(args):
    lower, _, contr = read_scowl(args.scowl)
    rows, left_out = contraction_rows(lower, contr)
    with open(args.out, 'w', encoding='utf-8', newline='\n') as o:
        o.write(CONTRACTIONS_HEADER + '\n')
        for r in rows:
            o.write('%s\t%s\t%d\t%d\n' % r)
    print('rows=%d left out (host not determined): %s' % (len(rows), ' '.join(left_out)))
    return 0


def read_contractions(path):
    rows = []
    with open(path, encoding='utf-8') as f:
        lines = f.read().split('\n')
    if not lines or lines[0] != CONTRACTIONS_HEADER:
        raise BuildError('contractions table must start with the header: ' + CONTRACTIONS_HEADER)
    for n, line in enumerate(lines[1:], 2):
        if not line:
            continue
        cols = line.split('\t')
        if len(cols) != 4 or not cols[2].isdigit() or cols[3] not in ('0', '1'):
            raise BuildError('contractions table line %d is malformed: %r' % (n, line))
        rows.append((cols[0], cols[1], int(cols[2]), int(cols[3])))
    return rows


# ---------------------------------------------------------------- build (§16.2 ③-⑧)

def read_counts(work):
    """total[k] and forms[k][token] from <work>/<letter>.tsv."""
    total, forms = {}, {}
    paths = sorted(p for p in glob.glob(os.path.join(work, '*.tsv'))
                   if os.path.basename(p)[:-4] in LETTERS)
    if not paths:
        raise BuildError('no <letter>.tsv in ' + work)
    for path in paths:
        with open(path, encoding='utf-8') as f:
            for line in f:
                tok, count = line.rstrip('\n').split('\t')
                count = int(count)
                k = tok.lower()
                total[k] = total.get(k, 0) + count
                fk = forms.setdefault(k, {})
                fk[tok] = fk.get(tok, 0) + count
    return total, forms


def most_used(items):
    """The most used form; ties: the smaller string first."""
    return min(items, key=lambda fc: (-fc[1], fc[0]))[0]


def shape_of(k, total, forms, lower, upper):
    """§16.2 ④⑤: the one shape of key k, or None to drop it."""
    if k == 'i':
        return 'I'
    if len(k) == 1 and k != 'a':
        return None
    fk = forms[k]
    if k in lower:
        if len(k) > 1 and fk.get(k, 0) / total[k] < LOWER_SHARE:
            top = most_used(fk.items())
            if top in upper:
                return top
        return k
    in_upper = [(f, c) for f, c in fk.items() if f in upper]
    return most_used(in_upper) if in_upper else None


def read_word_set(path):
    """Lowercased lines without spaces (LDNOOBW `en`, restore.txt)."""
    with open(path, encoding='utf-8') as f:
        return {w for w in (line.strip().lower() for line in f) if w and ' ' not in w}


def select_words(total, forms, lower, upper, contr, contraction_table, bad, restore):
    scored = {}                                  # key -> (shape, score)
    for k in sorted(total):
        shape = shape_of(k, total, forms, lower, upper)
        if shape is not None:
            scored[k] = (shape, total[k])
    entries = [(shape, score) for _, (shape, score) in sorted(scored.items())]
    for form, host, permille, extra in contraction_table:
        if (form.lower() in contr or extra == 1) and host in total:
            entries.append((form, total[host] * permille // 1000))
    entries = [e for e in entries if e[0].lower() not in bad or e[0].lower() in restore]
    entries.sort(key=lambda e: (-e[1], e[0]))
    return [shape for shape, _ in entries[:MAX_WORDS]]


def validate(words):
    seen = set()
    for w in words:
        if not WORD_RE.fullmatch(w) or len(w) > MAX_LEN:
            raise BuildError('word does not fit the format (regex or length <= %d): %r' % (MAX_LEN, w))
        k = w.lower()
        if k in seen:
            raise BuildError('two words have the same lowercase key: %r' % k)
        seen.add(k)
    for need in ('a', 'I'):
        if need not in words:
            raise BuildError('the list must contain %r' % need)


def render(words, sources, contractions_sha):
    body = ''.join(w + '\n' for w in words)
    ng = sources['ngram']
    sha_list = hashlib.sha256('\n'.join(e['sha256'] for e in ng['files']).encode('ascii')).hexdigest()
    sc = sources['scowl']['files'][0]
    ld = sources['ldnoobw']
    head = [
        '#fss-words %d' % FORMAT_VERSION,
        '#name en',
        '#dict ' + DICT_VERSION,
        '#tool tools/en-dict/build_en_words.py %d' % TOOL_VERSION,
        '#source ngram googlebooks-eng-all-1gram-20120701 years=%d-%d min=%d sha256-list=%s'
        % (YEAR_FROM, YEAR_TO, MIN_COUNT, sha_list),
        '#source scowl 2020.12.07 level=%d sha256=%s' % (SCOWL_LEVEL, sc['sha256']),
        '#source ldnoobw en commit=%s sha256=%s' % (ld['commit'], ld['files'][0]['sha256']),
        '#contractions tools/en-dict/contractions.tsv sha256=%s' % contractions_sha,
        '#words %d' % len(words),
        '#sha256 ' + hashlib.sha256(body.encode('utf-8')).hexdigest(),
    ]
    return ('\n'.join(head) + '\n' + body).encode('utf-8')


def build_bytes(args):
    sources = load_sources(args.sources)
    verify_file(args.ldnoobw, sources['ldnoobw']['files'][0]['sha256'])
    lower, upper, contr = read_scowl(args.scowl)
    total, forms = read_counts(args.work)
    table = read_contractions(args.contractions)
    words = select_words(total, forms, lower, upper, contr, table,
                         read_word_set(args.ldnoobw), read_word_set(args.restore))
    validate(words)
    return render(words, sources, sha256_file(args.contractions)), len(words)


def write_bytes(path, data):
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    tmp = path + '.tmp'
    with open(tmp, 'wb') as o:
        o.write(data)
    os.replace(tmp, path)


def cmd_build(args):
    data, n = build_bytes(args)
    write_bytes(args.out, data)
    print('wrote %s: %d words, %d bytes, sha256 %s' % (args.out, n, len(data), hashlib.sha256(data).hexdigest()))
    return 0


def cmd_check(args):
    data, n = build_bytes(args)
    with tempfile.TemporaryDirectory() as tmp:     # rebuild into a temporary path, then compare with the file
        path = os.path.join(tmp, 'en_words.txt')
        write_bytes(path, data)
        with open(path, 'rb') as f:
            rebuilt = f.read()
    with open(args.expected, 'rb') as f:
        expected = f.read()
    if rebuilt == expected:
        print('check: identical to %s (%d words, %d bytes, sha256 %s)'
              % (args.expected, n, len(rebuilt), hashlib.sha256(rebuilt).hexdigest()))
        return 0
    hint = ' (only line ends differ: the file was checked out with CRLF)' \
        if expected.replace(b'\r\n', b'\n') == rebuilt else ''
    print('check: DIFFERENT from %s%s' % (args.expected, hint))
    return 1


# ---------------------------------------------------------------- command line

def parser():
    p = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    sub = p.add_subparsers(dest='cmd', required=True)
    f = sub.add_parser('fetch')
    f.add_argument('--dl', required=True)
    f.add_argument('--sources', default=SOURCES_FILE)
    a = sub.add_parser('aggregate')
    a.add_argument('--dl', required=True)
    a.add_argument('--work', required=True)
    a.add_argument('--jobs', type=int, default=1)
    c = sub.add_parser('contractions')
    c.add_argument('--scowl', required=True)
    c.add_argument('--out', default=CONTRACTIONS_FILE)
    for name in ('build', 'check'):
        b = sub.add_parser(name)
        b.add_argument('--work', required=True)
        b.add_argument('--scowl', required=True, help='the unpacked SCOWL final/ folder')
        b.add_argument('--ldnoobw', required=True)
        b.add_argument('--sources', default=SOURCES_FILE)
        b.add_argument('--contractions', default=CONTRACTIONS_FILE)
        b.add_argument('--restore', default=RESTORE_FILE)
        if name == 'build':
            b.add_argument('--out', default=WORDS_FILE)
        else:
            b.add_argument('--expected', default=WORDS_FILE)
    return p


COMMANDS = {'fetch': cmd_fetch, 'aggregate': cmd_aggregate, 'contractions': cmd_contractions,
            'build': cmd_build, 'check': cmd_check}


def main(argv=None):
    args = parser().parse_args(argv)
    try:
        return COMMANDS[args.cmd](args)
    except BuildError as e:
        print('error: %s' % e, file=sys.stderr)
        return 1


if __name__ == '__main__':
    sys.exit(main())
