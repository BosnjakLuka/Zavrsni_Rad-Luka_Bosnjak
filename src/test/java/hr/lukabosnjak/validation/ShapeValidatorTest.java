package hr.lukabosnjak.validation;

import hr.lukabosnjak.domain.entities.Shape;
import hr.lukabosnjak.domain.enums.ShapeSubtype;
import hr.lukabosnjak.domain.enums.ShapeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShapeValidatorTest {
    private final ShapeValidator validator = new ShapeValidator();

    @Test
    void acceptsAllSupportedShapes() {
        assertDoesNotThrow(() -> validator.validate(shape(ShapeType.SQUARE, null, 10.0, null)));
        assertDoesNotThrow(() -> validator.validate(shape(ShapeType.RECTANGLE, null, 10.0, 20.0)));
        assertDoesNotThrow(() -> validator.validate(shape(ShapeType.CIRCLE, null, 10.0, null)));
        assertDoesNotThrow(() -> validator.validate(
                shape(ShapeType.TRIANGLE, ShapeSubtype.EQUILATERAL, 10.0, null)));
    }

    @Test
    void ignoresDimensionsThatSelectedShapeDoesNotUse() {
        assertDoesNotThrow(() -> validator.validate(
                shape(ShapeType.CIRCLE, ShapeSubtype.EQUILATERAL, 10.0, -1.0)));
    }

    @Test
    void rejectsMissingShapeAndType() {
        assertMessage("Oblik mora biti zadan.", null);
        assertMessage("Vrsta oblika mora biti odabrana.", shape(null, null, 10.0, null));
    }

    @Test
    void rejectsInvalidSquareSide() {
        assertMessage("Duljina stranice mora biti veća od 0 mm.",
                shape(ShapeType.SQUARE, null, 0.0, null));
        assertMessage("Duljina stranice mora biti veća od 0 mm.",
                shape(ShapeType.SQUARE, null, -1.0, null));
    }

    @Test
    void rejectsInvalidRectangleDimensions() {
        assertMessage("Širina pravokutnika mora biti veća od 0 mm.",
                shape(ShapeType.RECTANGLE, null, 0.0, 10.0));
        assertMessage("Visina pravokutnika mora biti veća od 0 mm.",
                shape(ShapeType.RECTANGLE, null, 10.0, null));
    }

    @Test
    void rejectsInvalidCircleDiameter() {
        assertMessage("Promjer kruga mora biti veći od 0 mm.",
                shape(ShapeType.CIRCLE, null, Double.NaN, null));
        assertMessage("Promjer kruga mora biti veći od 0 mm.",
                shape(ShapeType.CIRCLE, null, Double.POSITIVE_INFINITY, null));
    }

    @Test
    void rejectsUnsupportedTriangleSubtypeAndInvalidSide() {
        assertMessage("Za trokut je podržan samo podtip EQUILATERAL.",
                shape(ShapeType.TRIANGLE, null, 10.0, null));
        assertMessage("Duljina stranice trokuta mora biti veća od 0 mm.",
                shape(ShapeType.TRIANGLE, ShapeSubtype.EQUILATERAL, 0.0, null));
    }

    private void assertMessage(String expectedMessage, Shape shape) {
        ValidationException exception = assertThrows(
                ValidationException.class, () -> validator.validate(shape));
        assertEquals(expectedMessage, exception.getMessage());
    }

    private Shape shape(ShapeType type, ShapeSubtype subtype, Double dimensionA, Double dimensionB) {
        return new Shape(null, type, subtype, dimensionA, dimensionB, null, null, null, null);
    }
}
