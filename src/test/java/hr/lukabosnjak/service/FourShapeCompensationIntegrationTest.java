package hr.lukabosnjak.service;

import hr.lukabosnjak.domain.entities.CncMachine;
import hr.lukabosnjak.domain.entities.MachiningParameters;
import hr.lukabosnjak.domain.entities.MaterialSheet;
import hr.lukabosnjak.domain.entities.Shape;
import hr.lukabosnjak.domain.entities.Tool;
import hr.lukabosnjak.domain.enums.ShapeSubtype;
import hr.lukabosnjak.domain.enums.ShapeType;
import hr.lukabosnjak.gcode.GCodeGenerator;
import hr.lukabosnjak.gcode.GCodeProgram;
import hr.lukabosnjak.gcode.RichAutoA11GCodeGenerator;
import hr.lukabosnjak.gcode.RichAutoA11Profile;
import hr.lukabosnjak.geometry.CutSide;
import hr.lukabosnjak.geometry.ToolPath;
import hr.lukabosnjak.geometry.ToolPathBoundsCalculator;
import hr.lukabosnjak.geometry.ToolPathCompensationService;
import hr.lukabosnjak.geometry.ToolPathService;
import hr.lukabosnjak.validation.MachiningJobValidator;
import hr.lukabosnjak.validation.MachiningParametersValidator;
import hr.lukabosnjak.validation.MaterialSheetValidator;
import hr.lukabosnjak.validation.ShapeValidator;
import hr.lukabosnjak.validation.SingleShapeFitValidator;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FourShapeCompensationIntegrationTest {
    private static final double TOOL_DIAMETER = 6.0;
    private static final double SHEET_SIZE = 500.0;
    private static final double CUT_DEPTH = 5.0;
    private static final double STEP_DOWN = 2.0;

    @Test
    void validatesCompensatesFitsAndGeneratesDeterministicProgramsForAllV1Shapes() {
        ShapeValidator shapeValidator = new ShapeValidator();
        ToolPathService toolPathService = new ToolPathService(shapeValidator);
        ToolPathCompensationService compensationService = new ToolPathCompensationService();
        MaterialSheetValidator sheetValidator = new MaterialSheetValidator();
        SingleShapeFitValidator fitValidator = new SingleShapeFitValidator(
                new ToolPathBoundsCalculator(), sheetValidator);
        CncMachine machine = machine();
        Tool tool = new Tool(1L, machine, 1, "Software test tool", null,
                TOOL_DIAMETER, null, null, true, null, null);
        MaterialSheet sheet = new MaterialSheet(null, null, SHEET_SIZE, SHEET_SIZE, 18.0, null, null);
        MachiningParameters parameters = new MachiningParameters(
                null, 12000.0, 500.0, 150.0, CUT_DEPTH, STEP_DOWN, 5.0);
        ProgramGenerationService generationService = generationService(
                shapeValidator, sheetValidator, toolPathService, fitValidator);

        Map<ShapeType, Shape> shapes = shapes();
        Map<ShapeType, String> outputs = new EnumMap<>(ShapeType.class);
        for (Map.Entry<ShapeType, Shape> entry : shapes.entrySet()) {
            Shape shape = entry.getValue();
            ToolPath programmedContour = toolPathService.generate(shape);
            ToolPath cutterCenterPath = compensationService.compensate(
                    programmedContour, tool, CutSide.INSIDE);

            assertTrue(programmedContour.segments().size() > 0);
            assertEquals(programmedContour.startPoint(), programmedContour.segments().getLast().end());
            fitValidator.validate(cutterCenterPath, sheet, machine);

            ProgramGenerationRequest request = new ProgramGenerationRequest(
                    machine, tool, sheet, parameters, shape, CutSide.INSIDE);
            GCodeProgram first = generationService.generate(request);
            GCodeProgram second = generationService.generate(request);
            assertEquals(first.text(), second.text());
            assertFalse(first.text().isBlank());
            assertEquals(3, countLines(first, "G01 Z"));
            assertEquals(4, countLines(first, "G00 Z5.000"));
            assertTrue(first.text().contains("F150.000"));
            assertTrue(first.text().contains("F500.000"));
            outputs.put(entry.getKey(), first.text());
        }

        assertEquals(4, outputs.size());
        assertTrue(outputs.values().stream().allMatch(output -> output.endsWith("M30\n")));
    }

    private ProgramGenerationService generationService(
            ShapeValidator shapeValidator,
            MaterialSheetValidator sheetValidator,
            ToolPathService toolPathService,
            SingleShapeFitValidator fitValidator
    ) {
        GCodeGenerator generator = new RichAutoA11GCodeGenerator(
                RichAutoA11Profile.referenceProgramProfile());
        return new ProgramGenerationService(
                new MachiningJobValidator(
                        shapeValidator, sheetValidator, new MachiningParametersValidator()),
                toolPathService,
                fitValidator,
                new ToolPathCompensationService(),
                generator);
    }

    private Map<ShapeType, Shape> shapes() {
        Map<ShapeType, Shape> shapes = new EnumMap<>(ShapeType.class);
        shapes.put(ShapeType.SQUARE, new Shape(
                null, ShapeType.SQUARE, null, 80.0, null, null, null, null, null));
        shapes.put(ShapeType.RECTANGLE, new Shape(
                null, ShapeType.RECTANGLE, null, 100.0, 120.0, null, null, null, null));
        shapes.put(ShapeType.CIRCLE, new Shape(
                null, ShapeType.CIRCLE, null, 100.0, null, null, null, null, null));
        shapes.put(ShapeType.TRIANGLE, new Shape(
                null, ShapeType.TRIANGLE, ShapeSubtype.EQUILATERAL,
                80.0, null, null, null, null, null));
        return shapes;
    }

    private int countLines(GCodeProgram program, String prefix) {
        return (int) program.lines().stream().filter(line -> line.startsWith(prefix)).count();
    }

    private CncMachine machine() {
        return new CncMachine(
                1L, "Software test machine", null, null, "RichAuto A11",
                SHEET_SIZE, SHEET_SIZE, 50.0, 1000.0, 1000.0, 24000.0,
                null, null);
    }
}
