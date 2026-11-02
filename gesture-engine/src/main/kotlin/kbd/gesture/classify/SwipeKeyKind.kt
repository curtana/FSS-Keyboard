package kbd.gesture.classify

/**
 * Kind of a swipe-layout key (LLD-04 §4.1). Created early by T09 for the layout data; T15 owns the rest of `classify/`.
 * [CHEONJIIN] = the cheonjiin key (row 3 col 0, SWIPE 5.4, issue #160): one stroke = one element.
 */
enum class SwipeKeyKind { CONSONANT, NUMBER, SYMBOL, FUNCTION, EMPTY, CHEONJIIN }
