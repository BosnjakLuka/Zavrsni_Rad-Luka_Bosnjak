package hr.lukabosnjak.validation;

import hr.lukabosnjak.domain.entities.CncMachine;
import hr.lukabosnjak.domain.entities.MaterialSheet;
import hr.lukabosnjak.domain.entities.Shape;
import hr.lukabosnjak.domain.enums.ShapeType;
import hr.lukabosnjak.geometry.ToolPath;
import hr.lukabosnjak.geometry.ToolPathBoundsCalculator;
import hr.lukabosnjak.geometry.ToolPathService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SingleShapeFitValidatorTest {
    private final ToolPathService toolPathService = new ToolPathService(new ShapeValidator());
    private final SingleShapeFitValidator validator = new SingleShapeFitValidator(
            new ToolPathBoundsCalculator(), new MaterialSheetValidator());

    @Test
    void acceptsShapeInsideMaterialAndMachineBounds() {
        ToolPath path = circlePath(80.0);

        assertDoesNotThrow(() -> validator.validate(
                path, materialSheet(100, 100), machine(1250, 2500)));
    }

    @Test
    void acceptsShapeExactlyOnMaterialAndMachineBoundaries() {
        ToolPath path = circlePath(80.0);

        assertDoesNotThrow(() -> validator.validate(
                path, materialSheet(80, 80), machine(80, 80)));
    }

    @Test
    void rejectsShapeWiderOrHigherThanMaterialSheet() {
        ToolPath path = circlePath(80.0);

        assertMessage(
                "Oblik ne stane na odabranu ploču materijala.",
                path, materialSheet(79.9, 100), machine(1250, 2500));
        assertMessage(
                "Oblik ne stane na odabranu ploču materijala.",
                path, materialSheet(100, 79.9), machine(1250, 2500));
    }

    @Test
    void rejectsPathThatStartsOutsideMaterialOrigin() {
        ToolPath path = circlePath(20.0).translated(-0.1, 0);

        assertMessage(
                "Oblik ne stane na odabranu ploču materijala.",
                path, materialSheet(100, 100), machine(1250, 2500));
    }

    @Test
    void rejectsShapeOutsideMachineWorkArea() {
        ToolPath path = circlePath(80.0);

        assertMessage(
                "Planirani XY opseg ne stane u radno područje odabranog CNC stroja.",
                path, materialSheet(100, 100), machine(79.9, 100));
        assertMessage(
                "Planirani XY opseg ne stane u radno područje odabranog CNC stroja.",
                path, materialSheet(100, 100), machine(100, 79.9));
    }

    @Test
    void rejectsMissingPathAndMachineWithConcreteMessages() {
        assertMessage(
                "Putanja alata mora biti zadana.",
                null, materialSheet(100, 100), machine(1250, 2500));
        assertMessage(
                "CNC stroj mora biti odabran.",
                circlePath(80.0), materialSheet(100, 100), null);
    }

    @Test
    void delegatesInvalidMaterialSheetToExistingValidator() {
        assertMessage(
                "Širina ploče mora biti veća od 0 mm.",
                circlePath(80.0), materialSheet(0, 100), machine(1250, 2500));
    }

    private void assertMessage(
            String expectedMessage,
            ToolPath path,
            MaterialSheet materialSheet,
            CncMachine machine
    ) {
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validator.validate(path, materialSheet, machine));
        assertEquals(expectedMessage, exception.getMessage());
    }

    private ToolPath circlePath(double diameter) {
        Shape circle = new Shape(
                null, ShapeType.CIRCLE, null, diameter, null, null, null, null, null);
        return toolPathService.generate(circle);
    }

    private MaterialSheet materialSheet(double width, double height) {
        return new MaterialSheet(null, null, width, height, 18.0, null, null);
    }

    private CncMachine machine(double workAreaX, double workAreaY) {
        return new CncMachine(
                1L, "Test machine", null, null, "Test controller",
                workAreaX, workAreaY, 150.0, 5000.0, 1000.0, 24000.0, null, null);
    }
}
