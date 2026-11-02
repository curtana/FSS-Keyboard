package kbd.stroke.geom

/** Finds the key under a touch point, shared by every keyboard (LLD-04 §2). */
class KeyHitTester(val keys: List<KeyGeom>, val windowW: Float, val windowH: Float) {

    /**
     * The key whose rect contains (x, y) (left/top edges inclusive, right/bottom exclusive);
     * otherwise the key nearest by outside distance, ties going to the earlier key.
     * Null only when [keys] is empty. "No input" areas are filtered by the view beforehand.
     */
    fun hit(x: Float, y: Float): KeyGeom? {
        keys.firstOrNull { contains(it.rect, x, y) }?.let { return it }
        var best: KeyGeom? = null
        var bestD2 = Float.POSITIVE_INFINITY
        for (k in keys) {
            val d2 = outsideDistanceSq(k.rect, x, y)
            if (d2 < bestD2) {
                best = k
                bestD2 = d2
            }
        }
        return best
    }

    private fun contains(r: RectDp, x: Float, y: Float): Boolean =
        x >= r.left && x < r.right && y >= r.top && y < r.bottom

    private fun outsideDistanceSq(r: RectDp, x: Float, y: Float): Float {
        val dx = maxOf(r.left - x, 0f, x - r.right)
        val dy = maxOf(r.top - y, 0f, y - r.bottom)
        return dx * dx + dy * dy
    }
}
