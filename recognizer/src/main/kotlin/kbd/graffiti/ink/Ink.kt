package kbd.graffiti.ink

/** Graffiti pad ink types (LLD-05 §2). Coordinates are pad dp, time is ms. */
data class InkPoint(val x: Float, val y: Float, val t: Long)

/** Pad area rectangle in dp; recognizer must not depend on stroke-core (HLD ch. 2), hence its own rect. */
data class RectF2(val left: Float, val top: Float, val right: Float, val bottom: Float)

enum class PadArea { LETTER, NUMBER }

enum class GraffitiLang { KO, EN }

/** One stroke. [area] is where the stroke started (GRAFFITI 6.3); [areaRect] is that area, used for the corner test. */
data class InkStroke(val points: List<InkPoint>, val area: PadArea, val areaRect: RectF2)
