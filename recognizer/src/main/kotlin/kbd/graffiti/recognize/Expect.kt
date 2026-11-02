package kbd.graffiti.recognize

/** Which set `recognize` searches first: punctuation shift or extended shift pending (LLD-05 §3.1 steps 3–4, §3.2). */
enum class Expect { NORMAL, PUNCT, EXTENDED }
