package hr.lukabosnjak.gcode;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GCodeFormatterTest {
    @Test
    void usesDecimalPointIndependentlyOfCroatianLocale() {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("hr-HR"));

            assertEquals("1234.500", new GCodeFormatter(3).format(1234.5));
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    void usesFixedPrecisionAndHalfUpRounding() {
        GCodeFormatter formatter = new GCodeFormatter(3);

        assertEquals(3, formatter.decimalPlaces());
        assertEquals("1.235", formatter.format(1.2345));
        assertEquals("2.000", formatter.format(2.0));
        assertEquals("0.000", formatter.format(-0.0004));
    }

    @Test
    void neverUsesScientificNotation() {
        String small = new GCodeFormatter(8).format(0.00000012);
        String large = new GCodeFormatter(2).format(1.0e20);

        assertEquals("0.00000012", small);
        assertEquals("100000000000000000000.00", large);
        assertFalse(small.contains("E"));
        assertFalse(large.contains("E"));
    }

    @Test
    void rejectsInvalidPrecisionAndNonFiniteValues() {
        assertThrows(IllegalArgumentException.class, () -> new GCodeFormatter(-1));

        GCodeFormatter formatter = new GCodeFormatter(3);
        assertThrows(IllegalArgumentException.class, () -> formatter.format(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> formatter.format(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> formatter.format(Double.NEGATIVE_INFINITY));
    }
}
