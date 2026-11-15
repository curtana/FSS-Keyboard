package kbd.stroke.geom

/*
 * Shared geometry types (LLD-03 §3.3). Every engine coordinate and distance is in dp,
 * every time in ms (`MotionEvent.getEventTime()` base); the app converts px to dp (D-03).
 */

/**
 * One of the eight stroke directions of a tap-keyboard key (LLD-05A §2.1, SRS FR-TAP-02e, FSS#109): the four of [Dir]
 * or a [Diag]. [dx], [dy] are the signs of the direction in screen coordinates (y grows downwards): UP = (0, −1).
 * They are also the (sign of the column difference, sign of the row difference) of the compound-final rule
 * (SRS FR-TAP-02c ①).
 */
sealed interface Way {
    val dx: Int
    val dy: Int
}

/** Screen direction; y grows downwards. */
enum class Dir(override val dx: Int, override val dy: Int) : Way { UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0) }

/** A diagonal stroke direction (FSS#109): only keys with a diagonal assignment are judged in eight directions. */
enum class Diag(override val dx: Int, override val dy: Int) : Way {
    UP_LEFT(-1, -1), UP_RIGHT(1, -1), DOWN_LEFT(-1, 1), DOWN_RIGHT(1, 1)
}

/** All eight directions: the four of [Dir], then the four of [Diag]. */
val WAYS: List<Way> = Dir.entries + Diag.entries

/** The direction whose signs are ([dx], [dy]), or null for (0, 0). */
fun wayOfSigns(dx: Int, dy: Int): Way? = WAYS.firstOrNull { it.dx == dx && it.dy == dy }

enum class Axis { HORIZONTAL, VERTICAL }

/** A key edge, named by the direction a stroke leaves through it. */
enum class Edge(val dir: Dir) { TOP(Dir.UP), BOTTOM(Dir.DOWN), LEFT(Dir.LEFT), RIGHT(Dir.RIGHT) }

data class RectDp(val left: Float, val top: Float, val right: Float, val bottom: Float)

data class StrokePoint(val x: Float, val y: Float, val t: Long)

val Dir.axis: Axis
    get() = when (this) {
        Dir.LEFT, Dir.RIGHT -> Axis.HORIZONTAL
        Dir.UP, Dir.DOWN -> Axis.VERTICAL
    }

val Dir.opposite: Dir
    get() = when (this) {
        Dir.UP -> Dir.DOWN
        Dir.DOWN -> Dir.UP
        Dir.LEFT -> Dir.RIGHT
        Dir.RIGHT -> Dir.LEFT
    }

/** Unit vector (dx, dy) in screen coordinates: UP = (0, −1). */
val Dir.unit: Pair<Float, Float>
    get() = when (this) {
        Dir.UP -> 0f to -1f
        Dir.DOWN -> 0f to 1f
        Dir.LEFT -> -1f to 0f
        Dir.RIGHT -> 1f to 0f
    }

val Edge.axis: Axis get() = dir.axis
