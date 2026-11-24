"""Tests of build_en_words.py on the tiny inputs in testdata/ (LLD-11 §7.12 (거) DT-01..DT-08).
Run: /d/workspace/Keyboard/.venv/Scripts/python -I tools/en-dict/test_build_en_words.py
DT-09 (`check` on the real sources) needs the downloads and is run by hand (README.md)."""
import contextlib
import gzip
import hashlib
import io
import json
import os
import re
import shutil
import socket
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)            # -I leaves the script folder off sys.path
import build_en_words as tool       # noqa: E402

DATA = os.path.join(HERE, 'testdata')


def build_args(out, **over):
    a = dict(work=os.path.join(DATA, 'agg'), scowl=os.path.join(DATA, 'scowl', 'final'),
             ldnoobw=os.path.join(DATA, 'ldnoobw_en'), sources=os.path.join(DATA, 'sources.json'),
             contractions=os.path.join(DATA, 'contractions.tsv'), restore=os.path.join(DATA, 'restore.txt'))
    a.update(over)
    argv = ['build']
    for k in ('work', 'scowl', 'ldnoobw', 'sources', 'contractions', 'restore'):
        argv += ['--' + k, a[k]]
    return argv + ['--out', out]


def run(argv):
    """(exit code, stdout + stderr)."""
    buf = io.StringIO()
    with contextlib.redirect_stdout(buf), contextlib.redirect_stderr(buf):
        code = tool.main(argv)
    return code, buf.getvalue()


def read(path):
    with open(path, 'rb') as f:
        return f.read()


def words_of(path):
    return [w for w in read(path).decode('utf-8').split('\n') if w and not w.startswith('#')]


class BuildTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp()
        self.out = os.path.join(self.tmp, 'en_words.txt')

    def tearDown(self):
        shutil.rmtree(self.tmp)

    def build(self, **over):
        code, log = run(build_args(self.out, **over))
        self.assertEqual(code, 0, log)
        return words_of(self.out)

    def write(self, name, text):
        path = os.path.join(self.tmp, name)
        with open(path, 'w', encoding='utf-8', newline='\n') as f:
            f.write(text)
        return path

    def copy_tree(self, sub):
        dst = os.path.join(self.tmp, sub.replace('/', '_'))
        shutil.copytree(os.path.join(DATA, sub), dst)
        return dst

    def assert_fails_without_writing(self, **over):
        code, log = run(build_args(self.out, **over))
        self.assertNotEqual(code, 0, log)
        self.assertFalse(os.path.exists(self.out), 'no file may be written')
        self.assertFalse(os.path.exists(self.out + '.tmp'))
        return log

    # DT-01
    def test_dt01_golden(self):
        self.build()
        self.assertEqual(read(self.out), read(os.path.join(DATA, 'expected_en_words.txt')))

    # DT-02
    def test_dt02_same_input_same_bytes_no_dates(self):
        self.build()
        first = read(self.out)
        os.remove(self.out)
        self.build()
        self.assertEqual(read(self.out), first)
        text = first.decode('utf-8')
        self.assertIsNone(re.search(r'\d{4}-\d{2}-\d{2}|\d{1,2}:\d{2}', text), 'no date or time')
        self.assertNotIn(socket.gethostname(), text)
        self.assertNotIn(b'\r', first)
        self.assertFalse(first.startswith(b'\xef\xbb\xbf'), 'no BOM')
        self.assertTrue(first.endswith(b'\n'))

    # DT-03
    def test_dt03_header(self):
        self.build()
        text = read(self.out).decode('utf-8')
        lines = text.split('\n')
        head = [l for l in lines if l.startswith('#')]
        self.assertEqual(lines[:len(head)], head, 'header lines come first')
        self.assertEqual([h.split(' ')[0] for h in head],
                         ['#fss-words', '#name', '#dict', '#tool', '#source', '#source', '#source',
                          '#contractions', '#words', '#sha256'])
        self.assertEqual(head[0], '#fss-words 1')
        self.assertEqual(head[1], '#name en')
        self.assertEqual(head[3], '#tool tools/en-dict/build_en_words.py 1')
        src = tool.load_sources(os.path.join(DATA, 'sources.json'))
        sha_list = hashlib.sha256('\n'.join(e['sha256'] for e in src['ngram']['files']).encode()).hexdigest()
        self.assertEqual(head[4], '#source ngram googlebooks-eng-all-1gram-20120701 years=1980-2008 min=2000 '
                                  'sha256-list=' + sha_list)
        self.assertEqual(head[5], '#source scowl 2020.12.07 level=60 sha256=' + src['scowl']['files'][0]['sha256'])
        self.assertEqual(head[6], '#source ldnoobw en commit=%s sha256=%s'
                         % (src['ldnoobw']['commit'], hashlib.sha256(read(os.path.join(DATA, 'ldnoobw_en'))).hexdigest()))
        self.assertEqual(head[7], '#contractions tools/en-dict/contractions.tsv sha256='
                         + hashlib.sha256(read(os.path.join(DATA, 'contractions.tsv'))).hexdigest())
        body = '\n'.join(lines[len(head):])
        words = [w for w in body.split('\n') if w]
        self.assertEqual(head[8], '#words %d' % len(words))
        self.assertEqual(head[9], '#sha256 ' + hashlib.sha256(body.encode('utf-8')).hexdigest())

    # DT-04
    def test_dt04_case_shapes(self):
        words = self.build()
        self.assertIn('God', words)        # lowercase share 0.1 < 0.2 and the most used form is in U
        self.assertNotIn('god', words)
        self.assertIn('march', words)      # lowercase share 0.3 >= 0.2
        self.assertNotIn('March', words)
        self.assertIn('Monday', words)     # not in L: the most used form that is in U (MONDAY is not)
        self.assertIn('Paris', words)      # PARIS is used more but is not in U
        self.assertIn('McDonald', words)   # tie between two U forms: the smaller string
        self.assertIn('bill', words)       # most used form tie Bill/BILL -> BILL, not in U -> stays lowercase
        self.assertIn('I', words)
        self.assertNotIn('i', words)
        for gone in ('xyzzy', 'zebu', 'Zorro'):   # in no list / only in levels above 60
            self.assertNotIn(gone, words)
        keys = [w.lower() for w in words]
        self.assertEqual(len(keys), len(set(keys)), 'one line per key')
        self.assertEqual(sorted(w for w in words if len(w) == 1), ['I', 'a'])

    # DT-05
    def test_dt05_ldnoobw_and_restore(self):
        words = self.build()
        for gone in ('badword', 'Badname', "hasn't"):
            self.assertNotIn(gone, words)
        words = self.build(restore=os.path.join(DATA, 'restore_badword.txt'))
        self.assertIn('badword', words)
        self.assertNotIn('Badname', words)

    # DT-06
    def test_dt06_contractions(self):
        words = self.build()
        self.assertIn("I'm", words)             # in C
        self.assertIn("don't", words)
        self.assertNotIn("they'll", words)      # only in a level above 60, extra 0
        self.assertIn("it's", words)            # extra = 1
        self.assertNotIn("mayn't", words)       # host has no count
        self.assertLess(words.index("we're"), words.index("you're"))   # same score: shape order
        # scores: I'm = 900100 * 50 // 1000 = 45005 sits between you (100000) and don't (15000)
        self.assertEqual(words[words.index('you') + 1:words.index('you') + 3], ["I'm", "don't"])

    # DT-07
    def test_dt07_regex(self):
        table = self.write('bad.tsv', tool.CONTRACTIONS_HEADER + "\nit'\tit\t10\t1\n")
        log = self.assert_fails_without_writing(contractions=table)
        self.assertIn("it'", log)

    def test_dt07_length(self):
        long_word = 'a' * 33
        agg = self.copy_tree('agg')
        with open(os.path.join(agg, 'a.tsv'), 'a', encoding='utf-8', newline='\n') as f:
            f.write(long_word + '\t9000000\n')
        scowl = self.copy_tree('scowl/final')
        with open(os.path.join(scowl, 'english-words.10'), 'a', encoding='latin-1', newline='\n') as f:
            f.write(long_word + '\n')
        self.assert_fails_without_writing(work=agg, scowl=scowl)
        shutil.rmtree(agg)          # the same word at 32 letters passes
        agg = self.copy_tree('agg')
        with open(os.path.join(agg, 'a.tsv'), 'a', encoding='utf-8', newline='\n') as f:
            f.write(long_word[:32] + '\t9000000\n')
        with open(os.path.join(scowl, 'english-words.10'), 'a', encoding='latin-1', newline='\n') as f:
            f.write(long_word[:32] + '\n')
        self.assertEqual(self.build(work=agg, scowl=scowl)[0], long_word[:32])

    def test_dt07_duplicate_key(self):
        table = self.write('dup.tsv', tool.CONTRACTIONS_HEADER + "\nDon't\tdo\t50\t0\ndon't\tdo\t50\t0\n")
        log = self.assert_fails_without_writing(contractions=table)
        self.assertIn("don't", log)

    # DT-08
    def test_dt08_sources_exactly_three(self):
        with open(os.path.join(DATA, 'sources.json'), encoding='utf-8') as f:
            src = json.load(f)
        self.assertEqual(sorted(src), ['ldnoobw', 'ngram', 'scowl'])
        self.assertEqual(sorted(tool.load_sources(os.path.join(HERE, 'sources.json'))), ['ldnoobw', 'ngram', 'scowl'])
        extra = dict(src, wordfreq=src['scowl'])
        path = self.write('extra.json', json.dumps(extra))
        self.assertRaises(tool.BuildError, tool.load_sources, path)
        self.assert_fails_without_writing(sources=path)
        missing = {k: v for k, v in src.items() if k != 'ldnoobw'}
        self.assertRaises(tool.BuildError, tool.load_sources, self.write('missing.json', json.dumps(missing)))

    def test_dt08_checksum_mismatch_stops_fetch(self):
        f = self.write('some.gz', 'not the recorded content')
        self.assertRaises(tool.BuildError, tool.verify_file, f, '0' * 64)
        tool.verify_file(f, hashlib.sha256(read(f)).hexdigest())
        dl = os.path.join(self.tmp, 'dl')       # a present file with the wrong content: fetch stops, nothing downloaded
        os.mkdir(dl)
        with open(os.path.join(dl, tool.NGRAM_NAME.format('a')), 'wb') as o:
            o.write(b'corrupt')
        code, log = run(['fetch', '--dl', dl, '--sources', os.path.join(DATA, 'sources.json')])
        self.assertNotEqual(code, 0)
        self.assertIn('sha256 mismatch', log)
        self.assertEqual(os.listdir(dl), [tool.NGRAM_NAME.format('a')])

    def test_dt08_ldnoobw_file_must_match_sources(self):
        other = self.write('en', 'another list\n')
        self.assert_fails_without_writing(ldnoobw=other)


class HelperTest(unittest.TestCase):
    """Extra checks of the two steps that DT-01..DT-08 do not reach (aggregate, contraction table)."""

    def test_aggregate_file(self):
        tmp = tempfile.mkdtemp()
        try:
            src = os.path.join(tmp, 'x.gz')
            lines = ['Word\t1979\t100000\t1', 'Word\t1980\t1500\t1', 'Word\t2008\t500\t1', 'Word\t2009\t9000\t1',
                     'word\t1990\t1999\t1', 'word_NOUN\t1990\t5000\t1', "don't\t1990\t5000\t1", 'a1\t1990\t5000\t1',
                     'x\ty\t1990\t5\t1', 'bad\tyear\t5000\t1', 'Abc\t1990\t2000\t3']
            with gzip.open(src, 'wb') as f:
                f.write(('\n'.join(lines) + '\n').encode('utf-8'))
            dst = os.path.join(tmp, 'x.tsv')
            name, n, bad, kept = tool.aggregate_file(src, dst)
            self.assertEqual((n, bad, kept), (11, 2, 2))
            self.assertEqual(read(dst), b'Abc\t2000\nWord\t2000\n')
        finally:
            shutil.rmtree(tmp)

    def test_contraction_table_from_scowl(self):
        out = os.path.join(tempfile.mkdtemp(), 'c.tsv')
        try:
            code, log = run(['contractions', '--scowl', os.path.join(DATA, 'scowl', 'final'), '--out', out])
            self.assertEqual(code, 0, log)
            self.assertEqual(read(out), read(os.path.join(DATA, 'expected_contractions.tsv')))
            self.assertIn("left out (host not determined): ain't h'm o'clock wouldn't've", log)
        finally:
            shutil.rmtree(os.path.dirname(out))

    def test_shipped_table_and_restore(self):
        rows = tool.read_contractions(tool.CONTRACTIONS_FILE)
        self.assertEqual(len({r[0].lower() for r in rows}), len(rows))
        self.assertEqual(sorted(r[0] for r in rows if r[3] == 1), sorted(tool.EXTRA_S_FORMS))
        self.assertEqual(read(tool.RESTORE_FILE), b'')


if __name__ == '__main__':
    unittest.main(verbosity=2)
