package hr.lukabosnjak.validation;

import hr.lukabosnjak.domain.entities.MaterialSheet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MaterialSheetValidatorTest {
    private final MaterialSheetValidator validator = new MaterialSheetValidator();

    @Test
    void acceptsPositiveFiniteDimensions() {
        assertDoesNotThrow(() -> validator.validate(sheet(600.0, 400.0, 18.0)));
    }

    @Test
    void rejectsMissingSheet() {
        assertMessage("Ploča materijala mora biti zadana.", null);
    }

    @Test
    void rejectsZeroDimensions() {
        assertMessage("Širina ploče mora biti veća od 0 mm.", sheet(0.0, 400.0, 18.0));
        assertMessage("Visina ploče mora biti veća od 0 mm.", sheet(600.0, 0.0, 18.0));
        assertMessage("Debljina ploče mora biti veća od 0 mm.", sheet(600.0, 400.0, 0.0));
    }

    @Test
    void rejectsNegativeAndNonFiniteDimensions() {
        assertMessage("Širina ploče mora biti veća od 0 mm.", sheet(-1.0, 400.0, 18.0));
        assertMessage("Visina ploče mora biti veća od 0 mm.",
                sheet(600.0, Double.NaN, 18.0));
        assertMessage("Debljina ploče mora biti veća od 0 mm.",
                sheet(600.0, 400.0, Double.POSITIVE_INFINITY));
    }

    private void assertMessage(String expectedMessage, MaterialSheet sheet) {
        ValidationException exception = assertThrows(
                ValidationException.class, () -> validator.validate(sheet));
        assertEquals(expectedMessage, exception.getMessage());
    }

    private MaterialSheet sheet(double width, double height, double thickness) {
        return new MaterialSheet(null, null, width, height, thickness, null, null);
    }
}
