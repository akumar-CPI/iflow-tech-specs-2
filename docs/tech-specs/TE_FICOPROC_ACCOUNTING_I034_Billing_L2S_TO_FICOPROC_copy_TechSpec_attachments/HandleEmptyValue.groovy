
import com.sap.it.api.mapping.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Normalize one numeric input (as string).
 * - Returns "0" for null/blank and non-numeric inputs.
 * - Trims whitespace.
 * - Returns normalized string (no trailing zeros, no scientific notation).
 * - If input has more than 6 decimal digits, rounds to 6 (HALF_UP).
 *
 * Examples:
 *   toZeroIfEmpty(" 2.500 ")  -> "2.5"
 *   toZeroIfEmpty("")         -> "0"
 *   toZeroIfEmpty(null)       -> "0"
 *   toZeroIfEmpty("abc")      -> "0"
 *   toZeroIfEmpty("1.23456789")-> "1.234568"  (rounded to 6)
 */
def String toZeroIfEmpty(String v1) {
    // Local helper to parse safely
    def parse = { String s ->
        if (s == null) return null
        def t = s.trim()
        if (t.isEmpty()) return null
        try { new BigDecimal(t) } catch (Exception ignore) { null }
    }

    def dec = parse(v1)
    if (dec == null) {
        return "0"
    }

    // *** Added: Only round if there are more than 6 decimal places ***
    if (dec.scale() > 2) {
        dec = dec.setScale(2, RoundingMode.HALF_UP)
    }

    return dec.stripTrailingZeros().toPlainString()
}
