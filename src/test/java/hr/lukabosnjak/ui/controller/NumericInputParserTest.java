package hr.lukabosnjak.ui.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NumericInputParserTest {

    @Test
    void parsesFiniteDecimalNumbersIncludingCroatianDecimalSeparator() {
        assertEquals(12.5, NumericInputParser.parseRequiredFinite(" 12,5 ", "Širina"));
    }

    @Test
    void rejectsBlankInput() {
        assertThrows(IllegalArgumentException.class,
                () -> NumericInputParser.parseRequiredFinite(" ", "Širina"));
    }

    @Test
    void rejectsNonNumericInput() {
        assertThrows(IllegalArgumentException.class,
                () -> NumericInputParser.parseRequiredFinite("dvanaest", "Širina"));
    }

    @Test
    void rejectsNonFiniteInput() {
        assertThrows(IllegalArgumentException.class,
                () -> NumericInputParser.parseRequiredFinite("NaN", "Širina"));
        assertThrows(IllegalArgumentException.class,
                () -> NumericInputParser.parseRequiredFinite("Infinity", "Širina"));
    }
}
