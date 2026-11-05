"""Writes recognizer/src/main/resources/graffiti/templates.json (LLD-05 §7) and strokes/TEMPLATES.md (T28, T38).

Run from the repository root:
    /d/workspace/Keyboard/.venv/Scripts/python recognizer/src/test/resources/strokes/tools/build_templates.py

Every template is drawn here from the stroke description quoted in its `src`: GRAFFITI 0.13 §4.2 (Korean
consonants) and the stroke tables written out in words in §6.1 (letters), §6.3 (digits) and §6.4
(punctuation, extended symbols) — owner decisions #79 OD-1..OD-7 A. The point lists are drawn by hand from
those words; no third-party figure and no GPL code is copied (SRS CON-6).

`corners` is the corner count TemplateMatcher sees on the first stroke (shapes.corner_count mirrors LLD-05
§3.2 step 5); alternative strokes of one id must agree, so an alternative (②) with another corner count gets
its own id. Taps and straight lines are rules (LLD-05 §3.1): they have no template, except the rule entries
of the two symbol tables (`rule`, LLD-05 §7), which say which symbol a tap or line is while that shift is
pending. `interim` = GRAFFITI marks a detail of the stroke unconfirmed and the T28 interim stroke is kept
(LLD-05 §7); strokes GRAFFITI marks unconfirmed that T28 did not have are left out (see OMITTED).
"""

import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from shapes import arc, corner_count, fit_box, join, resample  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), *[".."] * 6))
OUT_JSON = os.path.join(ROOT, "recognizer", "src", "main", "resources", "graffiti", "templates.json")
OUT_MD = os.path.join(ROOT, "recognizer", "src", "test", "resources", "strokes", "TEMPLATES.md")
OWNER_STROKES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "owner_strokes.json")


def rev(points):
    """The same shape drawn from the other end."""
    return list(reversed(points))


def mirror_x(points):
    return [(1 - x, y) for x, y in points]


def half(points, x0):
    """Squeeze into the half [x0, x0 + 0.5] of the box (two shapes in a row)."""
    return [(x0 + x * 0.5, y) for x, y in points]


# --- shapes ----------------------------------------------------------------------------------------

G7 = [(0, 0), (1, 0), (1, 1)]                                    # 'ㄱ' / '7' / T: →, bend ↓
L = [(0, 0), (0, 1), (1, 1)]                                     # ↓, bend →
GAMMA_UP = [(0, 1), (0, 0), (1, 0)]                              # 'Γ' from the bottom: ↑, bend →
C_LEFT_OPEN = [(1, 0), (0, 0), (0, 1), (1, 1)]                   # 'ㄷ': ←, ↓, →
C_LEFT_OPEN_ROUND = join([(1, 0)], arc(0.3, 0.3, 0.3, 0.3, -90, -180, 4), arc(0.3, 0.7, 0.3, 0.3, 180, 90, 4), [(1, 1)])
Z = [(0, 0), (1, 0), (0, 1), (1, 1)]
BOX_DOWN_FIRST = [(0, 0), (0, 1), (1, 1), (1, 0), (0, 0)]
BOX_RIGHT_FIRST = [(0, 0), (1, 0), (1, 1), (0, 1), (0, 0)]
U_SQUARE = [(0, 0), (0, 1), (1, 1), (1, 0)]                      # 'ㅂ': ↓, →, ↑
U_ROUND = join([(0, 0), (0, 0.55)], arc(0.5, 0.55, 0.5, 0.45, 180, 0), [(1, 0)])
LAMBDA = [(0, 1), (0.5, 0), (1, 1)]
V = [(0, 0), (0.5, 1), (1, 0)]
CIRCLE_CCW = arc(0.5, 0.5, 0.5, 0.5, -90, -450, 24)              # from the top, leftwards first
CIRCLE_CW = arc(0.5, 0.5, 0.5, 0.5, -90, 270, 24)                # from the top, rightwards first
CIRCLE_BR_CCW = arc(0.5, 0.5, 0.5, 0.5, 45, -315, 24)            # from the bottom-right, upwards first
CIRCLE_BR_CW = arc(0.5, 0.5, 0.5, 0.5, 45, 405, 24)              # from the bottom-right, downwards first
ALPHA = [(1, 0), (0.6, 0.55), (0.3, 0.95), (0.05, 0.75), (0.05, 0.25), (0.3, 0.05), (0.6, 0.45), (1, 1)]
ALPHA_MIRROR = mirror_x(ALPHA)                                   # K mirrored: from the top-left, loop on the right
H = join([(0, 0), (0, 1), (0, 0.75)], arc(0.5, 0.75, 0.5, 0.35, 180, 360, 10), [(1, 1)])
THREE = join(arc(0.45, 0.25, 0.5, 0.25, -160, 90, 12), arc(0.45, 0.75, 0.55, 0.25, -90, 160, 12))
EPSILON = join(arc(0.55, 0.27, 0.45, 0.25, -40, -270, 10), arc(0.55, 0.75, 0.45, 0.25, -90, -320, 10))
GAMMA = [(0, 0), (0.5, 0.55), (0.68, 0.8), (0.5, 1.0), (0.32, 0.8), (0.5, 0.55), (1, 0)]
TWO_GAMMAS = half(GAMMA, 0) + half(GAMMA, 0.5)                   # '%' / '÷': two loops in a row
HORSESHOE = join([(0.1, 0), (0.28, 0.12), (0.18, 0.5)], arc(0.5, 0.5, 0.32, 0.45, 180, 0, 12),
                 [(0.72, 0.12), (0.9, 0)])                        # 'ʊ': open top, ends turned out
SIX = join([(0.75, 0), (0.4, 0.1), (0.15, 0.4)], arc(0.45, 0.72, 0.33, 0.28, 180, -150, 14))
C_SHAPE = arc(0.5, 0.5, 0.5, 0.5, -50, -310, 16)
J = join([(0.8, 0), (0.8, 0.65)], arc(0.45, 0.65, 0.35, 0.35, 0, 180, 8))
B = join([(0, 0), (0, 1), (0, 0)], arc(0, 0.25, 0.8, 0.25, -90, 90, 10), arc(0, 0.75, 0.9, 0.25, -90, 90, 10))
B_UP = join([(0, 1), (0, 0)], arc(0, 0.25, 0.8, 0.25, -90, 90, 10), arc(0, 0.75, 0.9, 0.25, -90, 90, 10))  # 'ß'
D = join([(0, 0), (0, 1), (0, 0)], arc(0, 0.5, 0.9, 0.5, -90, 90, 14))
D_UP = join([(0, 1), (0, 0)], arc(0, 0.5, 0.9, 0.5, -90, 90, 14))
F = [(1, 0), (0, 0), (0, 1)]
G = join(arc(0.5, 0.5, 0.5, 0.5, -50, -360, 18), [(0.55, 0.5)])
EN_M = join([(0, 1)], arc(0.25, 0.35, 0.25, 0.35, 180, 360, 8), [(0.5, 0.85)],
            arc(0.75, 0.35, 0.25, 0.35, 180, 360, 8), [(1, 1)])          # 'm': ↑, two round humps of one height
EN_M_DOWN_FIRST = join([(0, 0.35), (0, 1)], EN_M)                 # the left leg drawn down first, then the same
N = [(0, 1), (0, 0), (1, 1), (1, 0)]
N_MIRROR = [(0, 0), (0, 1), (1, 0), (1, 1)]                      # 'И' from the top-left
P = join([(0, 1), (0, 0)], arc(0, 0.27, 0.8, 0.27, -90, 90, 10))
Q = join(CIRCLE_CCW, [(0.95, 0.0)])                              # 'σ': circle, then a short tail → at the top
R = join(P, [(1, 1)])
S = join(arc(0.5, 0.25, 0.5, 0.25, -30, -270, 10), arc(0.5, 0.75, 0.5, 0.25, -90, 150, 10))
V_HOOK = [(0, 0), (0.4, 1), (0.8, 0), (1, 0.03)]                 # '√': V whose end turns right
X = [(0, 0), (1, 1), (1, 0), (0, 1)]
Y_SCRIPT = join([(0, 0), (0, 0.25)], arc(0.4, 0.25, 0.4, 0.25, 180, 0, 8), [(0.8, 0), (0.8, 1.2)])
ELLIPSE_CCW = arc(0.5, 0.5, 0.35, 0.5, -90, -450, 24)
ELLIPSE_CW = arc(0.5, 0.5, 0.35, 0.5, -90, 270, 24)
TWO = join(arc(0.5, 0.28, 0.45, 0.28, -160, 30, 10), [(0, 1), (1, 1)])
FIVE = join([(0.9, 0), (0.1, 0), (0.1, 0.42)], arc(0.45, 0.68, 0.45, 0.3, -150, 140, 14))
FIVE_SHORT = join([(0.1, 0), (0.1, 0.42)], arc(0.45, 0.68, 0.45, 0.3, -150, 140, 14))
SEVEN_SLANT = [(0, 0), (1, 0), (0.45, 1)]
EIGHT = join(S, [(0.93, 0.12)])
NINE = join(arc(0.45, 0.3, 0.45, 0.3, -20, -360, 16), [(0.9, 1)])
QUESTION = join(arc(0.5, 0.3, 0.45, 0.3, -180, 60, 10), [(0.5, 0.65), (0.5, 1)])
LPAREN = arc(1, 0.5, 0.6, 0.5, -115, -245, 10)
RPAREN = arc(0, 0.5, 0.6, 0.5, -65, 65, 10)
SLASH = [(0, 1), (1, 0)]
LT = [(1, 1), (0, 0.5), (1, 0)]                                  # from the bottom end
GT = [(0, 1), (1, 0.5), (0, 0)]
REG = join([(0, 0)], R)                                          # ®: stem ↓ from the top, then R's bump and leg

# --- templates: (set, id, output type, value, [strokes], source, interim?) --------------------------

KO = "GRAFFITI 4.2 consonant table"
EN = "GRAFFITI 6.1"
NUM = "GRAFFITI 6.3"
PU = "GRAFFITI 6.4 punctuation"
EX = "GRAFFITI 6.4 extended"

TEMPLATES = [
    ("KO_LETTER", "ko_g", "jamo", "ㄱ", [G7], f"{KO}: '7' — top-left →, bend ↓", False),
    ("KO_LETTER", "ko_n", "jamo", "ㄴ", [L], f"{KO}: 'L' — ↓ from the top, bend →", False),
    ("KO_LETTER", "ko_d", "jamo", "ㄷ", [C_LEFT_OPEN], f"{KO}: 'ㄷ' (C closed on the left) — top-right ←, ↓, →", False),
    ("KO_LETTER", "ko_d2", "jamo", "ㄷ", [C_LEFT_OPEN_ROUND], f"{KO}: 'ㄷ' written with round corners", False),
    ("KO_LETTER", "ko_r", "jamo", "ㄹ", [Z], f"{KO}: 'Z' — top-left →, ↙, →", False),
    ("KO_LETTER", "ko_m", "jamo", "ㅁ", [BOX_DOWN_FIRST], f"{KO}: 'ㅁ' in one go, starting on the left (↓ first)", False),
    ("KO_LETTER", "ko_m2", "jamo", "ㅁ", [BOX_RIGHT_FIRST], f"{KO}: ㅁ alternative — closed box (→ first)", False),
    ("KO_LETTER", "ko_b", "jamo", "ㅂ", [U_SQUARE], f"{KO}: 'U' — top-left ↓, →, ↑", False),
    ("KO_LETTER", "ko_b2", "jamo", "ㅂ", [U_ROUND], f"{KO}: 'U' written round", False),
    ("KO_LETTER", "ko_s", "jamo", "ㅅ", [LAMBDA], f"{KO}: 'Λ' — bottom-left ↗, ↘", False),
    ("KO_LETTER", "ko_ieung", "jamo", "ㅇ", [CIRCLE_CCW, CIRCLE_CW], f"{KO}: circle starting at the top", False),
    ("KO_LETTER", "ko_j", "jamo", "ㅈ", [ALPHA], f"{KO}: 'α' loop from the top-right tail", False),
    ("KO_LETTER", "ko_ch", "jamo", "ㅊ", [H], f"{KO}: 'h' — ↓ from the top, then the loop", False),
    ("KO_LETTER", "ko_k", "jamo", "ㅋ", [THREE], f"{KO}: '3' from the top-left", False),
    ("KO_LETTER", "ko_t", "jamo", "ㅌ", [EPSILON], f"{KO}: 'ε' from the top-right", False),
    ("KO_LETTER", "ko_p", "jamo", "ㅍ", [GAMMA], f"{KO}: 'γ' loop from the top-left", False),
    ("KO_LETTER", "ko_h", "jamo", "ㅎ", [HORSESHOE], f"{KO}: loop open at the top ('ʊ'-like), from the top", False),
    ("KO_LETTER", "ko_h6", "jamo", "ㅎ", [SIX], f"{KO}: ㅎ alternative — '6'", False),
    ("KO_LETTER", "ko_space_v", "control", "SPACE_V", [V], "GRAFFITI 4.2/4.4, 6.3-4: Korean space = 'V'", False),
    ("KO_LETTER", "ko_finish_j", "control", "FINISH_J", [J], "GRAFFITI 4.2: finish composing = 'J'", False),

    ("EN_LETTER", "en_a", "text", "a", [LAMBDA], f"{EN} A: bottom-left ↑, round apex, ↘ (Λ, no bar)", False),
    ("EN_LETTER", "en_b", "text", "b", [B], f"{EN} B ①: top-left ↓, two bumps to the right (retrace of the stem unconfirmed; T28 stroke kept)", True),
    ("EN_LETTER", "en_b2", "text", "b", [B_UP], f"{EN} B ②: bottom-left ↑, two bumps clockwise, back to the bottom-left (ß)", False),
    ("EN_LETTER", "en_c", "text", "c", [C_SHAPE], f"{EN} C: top-right, counter-clockwise, open on the right", False),
    ("EN_LETTER", "en_d", "text", "d", [D], f"{EN} D ①: top-left ↓, back up, one big bump clockwise (retrace unconfirmed; T28 stroke kept)", True),
    ("EN_LETTER", "en_d2", "text", "d", [D_UP], f"{EN} D ②: bottom-left ↑, one big bump clockwise to the bottom", False),
    ("EN_LETTER", "en_e", "text", "e", [EPSILON], f"{EN} E: top-right, round to the left, fold in the middle (ε)", False),
    ("EN_LETTER", "en_f", "text", "f", [F], f"{EN} F ①: top-right ←, bend ↓ (Γ)", False),
    ("EN_LETTER", "en_f2", "text", "f", [GAMMA_UP], f"{EN} F ②: bottom-left ↑, bend → (Γ from the bottom)", False),
    ("EN_LETTER", "en_g", "text", "g", [G], f"{EN} G ①: C counter-clockwise up to the middle, then the bar (bar direction unconfirmed; T28 stroke kept)", True),
    ("EN_LETTER", "en_g2", "text", "g", [SIX], f"{EN} G ②: '6' — top-right, counter-clockwise, closed loop", False),
    ("EN_LETTER", "en_h", "text", "h", [H], f"{EN} H: top-left ↓, back up half way, arch, ↓ (h) (retrace unconfirmed; T28 stroke kept)", True),
    ("EN_LETTER", "en_j", "text", "j", [J], f"{EN} J: top-right ↓, hook to the left", False),
    ("EN_LETTER", "en_k", "text", "k", [ALPHA], f"{EN} K: top-right, small loop (α) on the left, cross, ↘ (same shape as ㅈ, OD-6 A)", False),
    ("EN_LETTER", "en_l", "text", "l", [L], f"{EN} L: top-left ↓, bend →", False),
    ("EN_LETTER", "en_m", "text", "m", [EN_M], f"{EN} M ①: bottom-left ↑, two round humps of one height, ↓ (m)", False),
    ("EN_LETTER", "en_m2", "text", "m", [EN_M_DOWN_FIRST], f"{EN} M ②: the left leg ↓ first, back up, then the same", False),
    ("EN_LETTER", "en_n", "text", "n", [N], f"{EN} N: bottom-left ↑, diagonal ↘, ↑", False),
    ("EN_LETTER", "en_o", "text", "o", [CIRCLE_CCW, CIRCLE_CW], f"{EN} O ①②: circle from the top, counter-clockwise / clockwise (direction unconfirmed; both kept)", True),
    ("EN_LETTER", "en_p", "text", "p", [P], f"{EN} P ②: bottom-left ↑, top bump clockwise", False),
    ("EN_LETTER", "en_q", "text", "q", [Q], f"{EN} Q: circle counter-clockwise from the top, then a short tail → at the top (σ)", False),
    ("EN_LETTER", "en_r", "text", "r", [R], f"{EN} R ②: bottom-left ↑, top bump, leg ↘", False),
    ("EN_LETTER", "en_s", "text", "s", [S], f"{EN} S: top-right, S curve to the bottom-left", False),
    ("EN_LETTER", "en_t", "text", "t", [G7], f"{EN} T: top-left →, ↓ from its end (˥)", False),
    ("EN_LETTER", "en_u", "text", "u", [U_ROUND], f"{EN} U: top-left ↓, round, up to the top-right", False),
    ("EN_LETTER", "en_v", "text", "v", [V_HOOK], f"{EN} V ①: top-left, sharp V, the end turns right (√)", False),
    ("EN_LETTER", "en_v2", "text", "v", [rev(V)], f"{EN} V ②: V from the top-right to the top-left", False),
    ("EN_LETTER", "en_w", "text", "w", [[(0, 0), (0.25, 1), (0.5, 0.45), (0.75, 1), (1, 0)]], f"{EN} W: top-left ↓, middle peak lower than the arms, long right arm", False),
    ("EN_LETTER", "en_x", "text", "x", [X], f"{EN} X ①: two crossing diagonals from the top-left (one-stroke path unconfirmed; T28 stroke ↘ ↑ ↙ kept)", True),
    ("EN_LETTER", "en_x2", "text", "x", [ALPHA_MIRROR], f"{EN} X ②: top-left ↘, loop on the right, cross, to the bottom-left (K mirrored)", False),
    ("EN_LETTER", "en_y", "text", "y", [Y_SCRIPT], f"{EN} Y ①: top-left ↓, round up to the top-right, tail straight ↓ (script y)", False),
    ("EN_LETTER", "en_y2", "text", "y", [GAMMA], f"{EN} Y ②: top-left ↘, loop, to the top-right (γ)", False),
    ("EN_LETTER", "en_z", "text", "z", [Z], f"{EN} Z: →, ↙, →", False),

    ("NUMBER", "num_0", "text", "0", [ELLIPSE_CCW, ELLIPSE_CW], f"{NUM} 0 ①②: narrow circle from the top, counter-clockwise / clockwise (direction unconfirmed; both kept)", True),
    ("NUMBER", "num_2", "text", "2", [TWO], f"{NUM} 2: top-left, arc clockwise, ↙, bottom line →", False),
    ("NUMBER", "num_3", "text", "3", [THREE], f"{NUM} 3: top-left, two bumps clockwise", False),
    ("NUMBER", "num_4", "text", "4", [L], f"{NUM} 4: top-left ↓, bend → (same shape as L)", False),
    ("NUMBER", "num_5", "text", "5", [FIVE], f"{NUM} 5 ①: top-right ←, ↓, belly clockwise", False),
    ("NUMBER", "num_5b", "text", "5", [FIVE_SHORT], f"{NUM} 5 ②: top-left short ↓, belly clockwise", False),
    ("NUMBER", "num_6", "text", "6", [SIX], f"{NUM} 6: top-right, counter-clockwise, closed loop", False),
    ("NUMBER", "num_7", "text", "7", [G7, SEVEN_SLANT], f"{NUM} 7: top-left →, bend ↓ (as T, the end may lean left)", False),
    ("NUMBER", "num_8", "text", "8", [EIGHT], f"{NUM} 8 ①: one-stroke 8 from the top, right of the middle (direction unconfirmed; T28 stroke kept)", True),
    ("NUMBER", "num_9", "text", "9", [NINE], f"{NUM} 9: top loop counter-clockwise, then straight ↓ on the right", False),

    ("PUNCT", "p_period", "text", ".", "TAP", f"{PU} .: tap (tap twice in all)", False),
    ("PUNCT", "p_comma", "text", ",", "DIAG_DOWN_LEFT", f"{PU} ,: top-right ↙ (length not used)", False),
    ("PUNCT", "p_apostrophe", "text", "'", "LINE_DOWN", f"{PU} ': ↓ (length not used)", False),
    ("PUNCT", "p_hyphen", "text", "-", "LINE_RIGHT", f"{PU} -: →", False),
    ("PUNCT", "p_excl", "text", "!", "LINE_UP", f"{PU} !: ↑", False),
    ("PUNCT", "p_backslash", "text", "\\", "DIAG_DOWN_RIGHT", f"{PU} \\: ↘ (OD-4 A)", False),
    ("PUNCT", "p_lparen", "text", "(", [LPAREN], f"{PU} (: top-right, counter-clockwise curve down", False),
    ("PUNCT", "p_rparen", "text", ")", [RPAREN], f"{PU} ): top-left, clockwise curve down", False),
    ("PUNCT", "p_question", "text", "?", [QUESTION], f"{PU} ?: top-left, small arc clockwise, down the middle", False),
    ("PUNCT", "p_slash", "text", "/", [SLASH], f"{PU} /: bottom-left ↗ (not a rule direction)", False),
    ("PUNCT", "p_dollar", "text", "$", [S], f"{PU} $: as S, from the top-right", False),
    ("PUNCT", "p_at", "text", "@", [CIRCLE_CCW], f"{PU} @: circle counter-clockwise from the top (as O)", False),
    ("PUNCT", "p_hash", "text", "#", [N_MIRROR], f"{PU} #: top-left ↓, diagonal ↗, ↓ (И)", False),
    ("PUNCT", "p_percent", "text", "%", [TWO_GAMMAS], f"{PU} %: two loops in a row from the top-left", False),
    ("PUNCT", "p_caret", "text", "^", [LAMBDA], f"{PU} ^: as A", False),
    ("PUNCT", "p_amp", "text", "&", [EIGHT], f"{PU} &: one-stroke 8 from the top-right", False),
    ("PUNCT", "p_star", "text", "*", [ALPHA_MIRROR], f"{PU} *: as X ② — top-left ↘, loop on the right, to the bottom-left", False),
    ("PUNCT", "p_lt", "text", "<", [LT], f"{PU} <: from the bottom-right, left apex, to the top-right", False),
    ("PUNCT", "p_gt", "text", ">", [GT], f"{PU} >: from the bottom-left, right apex, to the top-left", False),
    ("PUNCT", "p_plus", "text", "+", [ALPHA], f"{PU} +: α loop from the top-right (as K)", False),
    ("PUNCT", "p_equals", "text", "=", [Z], f"{PU} =: as Z", False),
    ("PUNCT", "p_lbrace", "text", "{", [EPSILON], f"{PU} {{: as E (ε)", False),
    ("PUNCT", "p_rbrace", "text", "}", [THREE], f"{PU} }}: as 3, from the top-left", False),
    ("PUNCT", "p_lbracket", "text", "[", [rev(EPSILON)], f"{PU} [: ε backwards, from the bottom-right", False),
    ("PUNCT", "p_rbracket", "text", "]", [rev(THREE)], f"{PU} ]: 3 backwards, from the bottom-left", False),
    ("PUNCT", "p_tilde", "text", "~", [rev(N)], f"{PU} ~: N backwards, from the top-right", False),
    ("PUNCT", "p_quote", "text", "\"", [N], f"{PU} \": as N, from the bottom-left", False),
    ("PUNCT", "p_tab", "text", "\t", [GAMMA_UP], f"{PU} tab character: bottom-left ↑, bend → (as F ②)", False),

    ("EXTENDED", "x_plusminus", "text", "±", "LINE_RIGHT", f"{EX} ±: →", False),
    ("EXTENDED", "x_times", "text", "×", "DIAG_DOWN_LEFT", f"{EX} ×: top-right ↙ (length not used)", False),
    ("EXTENDED", "x_iexcl", "text", "¡", "LINE_UP", f"{EX} ¡: ↑", False),
    ("EXTENDED", "x_trademark", "text", "™", [EN_M], f"{EX} ™: bottom-left ↑, two humps (m)", False),
    ("EXTENDED", "x_registered", "text", "®", [REG], f"{EX} ®: top-left stem ↓, then R's bump and leg", False),
    ("EXTENDED", "x_copyright", "text", "©", [C_SHAPE], f"{EX} ©: as C", False),
    ("EXTENDED", "x_lsquo", "text", "‘", [GAMMA_UP], f"{EX} ‘: bottom-left ↑, bend → (Γ from the bottom)", False),
    ("EXTENDED", "x_rsquo", "text", "’", [[(1, 1), (1, 0), (0, 0)]], f"{EX} ’: bottom-right ↑, bend ← (˥ from the bottom)", False),
    ("EXTENDED", "x_ldquo", "text", "“", [N], f"{EX} “: as N, from the bottom-left", False),
    ("EXTENDED", "x_rdquo", "text", "”", [mirror_x(N)], f"{EX} ”: И from the bottom-right", False),
    ("EXTENDED", "x_section", "text", "§", [rev(S)], f"{EX} §: S from the bottom-left", False),
    ("EXTENDED", "x_degree", "text", "°", [CIRCLE_BR_CCW, CIRCLE_BR_CW], f"{EX} °: O from the bottom-right (direction and the difference from Ø unconfirmed; both directions)", True),
    ("EXTENDED", "x_plus", "text", "+", [ALPHA], f"{EX} +: α loop from the top-right (as K)", False),
    ("EXTENDED", "x_divide", "text", "÷", [TWO_GAMMAS], f"{EX} ÷: two loops from the top-left (as %)", False),
    ("EXTENDED", "x_equals", "text", "=", [Z], f"{EX} =: as Z", False),
    ("EXTENDED", "x_cent", "text", "¢", [rev(C_SHAPE)], f"{EX} ¢: C from the bottom-right, clockwise", False),
    ("EXTENDED", "x_yen", "text", "¥", [GAMMA], f"{EX} ¥: γ (as Y ②)", False),
    ("EXTENDED", "x_pound", "text", "£", [L], f"{EX} £: as L", False),
    ("EXTENDED", "x_iquest", "text", "¿", [rev(L)], f"{EX} ¿: L from the bottom-right", False),
    ("EXTENDED", "x_sz", "text", "ß", [B_UP], f"{EX} ß: as B ②", False),
    ("EXTENDED", "x_micro", "text", "µ", [rev(EN_M)], f"{EX} µ: m from the bottom-right", False),
    ("EXTENDED", "x_euro", "text", "€", [EPSILON], f"{EX} €: stroke unconfirmed (not in the stroke tables); T28 interim 'ε' kept", True),
]

# Strokes GRAFFITI marks unconfirmed that had no T28 template: not recognized until confirmed (GRAFFITI 6.4,
# LLD-05 §7).
OMITTED = [
    ("PUNCT", "_ | ` ; :", "wedge symbols — the retrace point is unconfirmed"),
    ("EN_LETTER", "P ①, R ①", "stem ↓ then retraced — the retrace is unconfirmed (P ②, R ② are kept)"),
    ("NUMBER", "8 ②", "one-stroke 8 from the top-left — the direction is unconfirmed"),
    ("EXTENDED", "∫", "the symbol itself is unconfirmed (the print is broken)"),
    ("EXTENDED", "Ø", "reads as the same shape as °"),
]


def owner_templates():
    """The owner's own consonant strokes (FSS#64), one id each because every stroke has its own corner count.
    They come after the drawn templates, which keep their ids: the stroke guide shows the drawn shapes."""
    with open(OWNER_STROKES, encoding="utf-8") as f:
        strokes = json.load(f)
    base = {t[3]: t[1] for t in TEMPLATES if t[0] == "KO_LETTER"}

    def stored(s):
        # As the JSON will hold it, so `corners` is counted on the same numbers the matcher reads.
        return [(round(x, 3), round(y, 3)) for x, y in fit_box(resample([tuple(p) for p in s]))]

    return [
        ("KO_LETTER", f"{base[c]}_o{i}", "jamo", c, [stored(s)], "owner's handwriting (FSS#64)", False)
        for c, ss in strokes.items() for i, s in enumerate(ss, start=1)
    ]


ALL_TEMPLATES = TEMPLATES + owner_templates()
SET_ORDER = ["KO_LETTER", "EN_LETTER", "NUMBER", "PUNCT", "EXTENDED"]
RULES = {"TAP", "LINE_DOWN", "LINE_UP", "LINE_RIGHT", "DIAG_DOWN_LEFT", "DIAG_DOWN_RIGHT"}


def is_rule(t):
    return isinstance(t[4], str)


def corners_of(t):
    return 0 if is_rule(t) else corner_count(fit_box(t[4][0]))


def fmt_stroke(stroke):
    return "[" + ", ".join(f"[{x:.3f}, {y:.3f}]".replace(".000", ".0") for x, y in fit_box(stroke)) + "]"


def main():
    ids = [t[1] for t in ALL_TEMPLATES]
    assert len(ids) == len(set(ids)), "duplicate id"
    for t in ALL_TEMPLATES:
        if is_rule(t):
            assert t[0] in ("PUNCT", "EXTENDED") and t[4] in RULES, t[1]
            continue
        counts = {corner_count(fit_box(s)) for s in t[4]}
        assert len(counts) == 1, f"{t[1]}: alternative strokes with different corner counts {counts}"
    for name in ("PUNCT", "EXTENDED"):
        rules = [t[4] for t in ALL_TEMPLATES if t[0] == name and is_rule(t)]
        assert len(rules) == len(set(rules)), f"{name}: rule twice"
    lines = ['{', '  "version": 1,', '  "sets": {']
    for si, name in enumerate(SET_ORDER):
        rows = []
        for t in ALL_TEMPLATES:
            set_name, tid, kind, value, strokes, _src, _interim = t
            if set_name != name:
                continue
            out = json.dumps({"type": kind, "value": value}, ensure_ascii=False)
            if is_rule(t):
                rows.append(f'      {{"id": "{tid}", "output": {out}, "corners": 0, "rule": "{strokes}", "strokes": []}}')
            else:
                body = ", ".join(fmt_stroke(s) for s in strokes)
                rows.append(f'      {{"id": "{tid}", "output": {out}, "corners": {corners_of(t)}, "strokes": [{body}]}}')
        lines.append(f'    "{name}": [')
        lines.append(",\n".join(rows))
        lines.append("    ]" + ("," if si < len(SET_ORDER) - 1 else ""))
    lines += ["  }", "}", ""]
    with open(OUT_JSON, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines))

    md = [
        "# Graffiti templates — source per id (T28, T38)",
        "",
        "Generated by `tools/build_templates.py` together with "
        "`recognizer/src/main/resources/graffiti/templates.json`; edit the script, not the JSON. "
        "Korean consonants follow GRAFFITI 0.13 §4.2; English letters, digits, punctuation and extended symbols follow "
        "the stroke tables written out in words in GRAFFITI 0.13 §6.1, §6.3 and §6.4 (owner decisions "
        "#79 OD-1..OD-7 A). The point lists are drawn by hand from those words (no GPL code, no third-party figures, CON-6). "
        "`rule` = a symbol-table entry for a tap or straight line, used only while that shift is pending "
        "(LLD-05 §3.1 step 3, §7). `interim` = GRAFFITI marks a detail of the stroke unconfirmed and the T28 "
        "interim stroke is kept until real-device strokes settle it (GRAFFITI 7, LLD-05 §7). "
        "Ids ending in `_oN` are the owner's own strokes (`tools/owner_strokes.json`, FSS#64). "
        "Double consonants have no template: they are shift (↑) + consonant (FSS#64).",
        "",
        "| set | id | output | rule | alt strokes | corners | interim | source |",
        "|---|---|---|---|---|---|---|---|",
    ]
    for t in ALL_TEMPLATES:
        set_name, tid, kind, value, strokes, src, interim = t
        shown = {"\t": "tab (`\\t`)", "|": "\\|"}.get(value, value)
        rule = strokes if is_rule(t) else ""
        n = 0 if is_rule(t) else len(strokes)
        md.append(f"| {set_name} | `{tid}` | {shown} | {rule} | {n} | {corners_of(t)} | {'yes' if interim else ''} | {src.replace('|', chr(92) + '|')} |")
    md += [
        "",
        "Left out — GRAFFITI marks the stroke unconfirmed and T28 had no template, so it is not recognized until "
        "the stroke is settled (GRAFFITI 6.4, LLD-05 §7):",
        "",
        "| set | strokes | why |",
        "|---|---|---|",
    ]
    for set_name, what, why in OMITTED:
        md.append(f"| {set_name} | {what.replace('|', chr(92) + '|')} | {why} |")
    md += [
        "",
        "Not in the tables on purpose: the closed 4 (not in Graffiti 1), `•` (extended + tap is the language "
        "toggle, OD-7 A), ⌫ ← in any symbol table (OD-3 A).",
        "",
        "No template (rules, LLD-05 §3.1): tap (ㆍ, punctuation shift, en/number tap), the six straight lines "
        "(ㅣ / i / 1 = ↓, ㅡ / space = →, ⌫ = ←, shift = ↑, return = ↙, extended shift = ↘), corner tap. "
        "While a shift is pending, the `rule` entries above come first.",
        "",
    ]
    with open(OUT_MD, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(md))
    print(f"{len(ALL_TEMPLATES)} templates -> {OUT_JSON}")


if __name__ == "__main__":
    main()
