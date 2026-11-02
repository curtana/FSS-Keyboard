package kbd.graffiti.recognize

/** Recognition result: shape only, independent of graffiti state (LLD-05 §2). */
sealed interface StrokeToken {
    data object Tap : StrokeToken
    data object CornerTap : StrokeToken        // tap in the letter area's top-left corner (FR-GRF-05a)
    data object LineDown : StrokeToken         // top → bottom
    data object LineUp : StrokeToken           // bottom → top (shift)
    data object LineRight : StrokeToken        // left → right
    data object LineLeft : StrokeToken         // right → left (backspace)
    data object DiagDownLeft : StrokeToken     // top-right → bottom-left (return)
    data object DiagDownRight : StrokeToken    // top-left → bottom-right (extended shift)
    data class Symbol(val id: String) : StrokeToken   // template match (template JSON id)
    data object Unknown : StrokeToken          // matches nothing (recognition failure, FR-GRF-08)
}
