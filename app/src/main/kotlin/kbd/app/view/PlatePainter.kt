package kbd.app.view

import android.graphics.Canvas
import android.graphics.Paint
import kbd.app.store.PxRect

/**
 * Paints a keyboard plate (SRS FR-CMN-14 ⑫, HLD 5A, LLD-08 §7.3, FSS#85): the view that has the plate has no
 * background of its own and fills only this rectangle with the keyboard colour, so the view draws nothing outside it
 * (what shows there is the window's and the platform's part, not confirmed on a device: LLD-08 §7.3).
 */
internal class PlatePainter {
    private val paint = Paint().apply { color = KeyboardRootView.BACKGROUND_COLOR }

    fun draw(canvas: Canvas, plate: PxRect) {
        if (plate.isEmpty) return
        canvas.drawRect(plate.left.toFloat(), plate.top.toFloat(), plate.right.toFloat(), plate.bottom.toFloat(), paint)
    }
}

/** A view that has no background of its own and paints a keyboard plate. */
internal interface PlateView {
    /** The plate in the view's coordinates, px; empty until the first layout. */
    val plate: PxRect
}
