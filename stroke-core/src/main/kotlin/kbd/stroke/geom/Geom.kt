package kbd.stroke.geom

/*
 * Shared geometry types (LLD-03 §3.3). Every engine coordinate and distance is in dp,
 * every time in ms (`MotionEvent.getEventTime()` base); the app converts px to dp (D-03).
 */

/** Screen direction; y grows downwards. */
enum class Dir { UP, DOWN, LEFT, RIGHT }

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
