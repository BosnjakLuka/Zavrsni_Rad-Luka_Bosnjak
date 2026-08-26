package hr.lukabosnjak.ui.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NumericInputParserTest {

    @Test
    void parsesFiniteDecimalNumbersIncludingCroatianDecimalSeparator() {
        assertEquals(12.5, NumericInputParser.parseRequiredFinite(" 12,5 ", "Širina"));
    }

    @Test
    void parsesOptionalFiniteNumberAndMapsBlankToNull() {
        assertNull(NumericInputParser.parseOptionalFinite(" ", "Maksimalni posmak"));
        assertNull(NumericInputParser.parseOptionalFinite(null, "Maksimalni posmak"));
        assertEquals(1250.5, NumericInputParser.parseOptionalFinite(" 1250,5 ", "Maksimalni posmak"));
        assertThrows(IllegalArgumentException.class,
                () -> NumericInputParser.parseOptionalFinite("NaN", "Maksimalni posmak"));
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

    @Test
    void parsesIntegerAndRejectsDecimalIntegerInput() {
        assertEquals(2, NumericInputParser.parseRequiredInteger(" 2 ", "Broj oštrica"));
        assertThrows(IllegalArgumentException.class,
                () -> NumericInputParser.parseRequiredInteger("2.5", "Broj oštrica"));
    }
}
