package hr.lukabosnjak.service;

import hr.lukabosnjak.domain.entities.CncMachine;
import hr.lukabosnjak.domain.entities.MachiningParameters;
import hr.lukabosnjak.domain.entities.MaterialSheet;
import hr.lukabosnjak.domain.entities.Shape;
import hr.lukabosnjak.domain.entities.Tool;
import hr.lukabosnjak.domain.enums.ShapeType;
import hr.lukabosnjak.gcode.GCodeProgram;
import hr.lukabosnjak.geometry.ToolPath;
import hr.lukabosnjak.geometry.ToolPathBoundsCalculator;
import hr.lukabosnjak.geometry.ToolPathService;
import hr.lukabosnjak.geometry.CutSide;
import hr.lukabosnjak.geometry.ToolPathCompensationService;
import hr.lukabosnjak.validation.MachiningJobValidator;
import hr.lukabosnjak.validation.MachiningParametersValidator;
import hr.lukabosnjak.validation.MaterialSheetValidator;
import hr.lukabosnjak.validation.ShapeValidator;
import hr.lukabosnjak.validation.SingleShapeFitValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProgramGenerationServiceTest {

    @Test
    void validatesGeneratesFitsAndDelegatesOneToolPath() {
        CapturingGenerator generator = new CapturingGenerator();
        ProgramGenerationService service = service(generator);

        GCodeProgram result = service.generate(validRequest());

        assertEquals("PREVIEW\n", result.text());
        assertEquals(1, generator.callCount);
        assertEquals(4, generator.toolPath.segments().size());
        assertEquals(500.0, generator.parameters.getFeedRate());
        assertEquals(200.0, generator.parameters.getPlungeRate());
    }

    @Test
    void rejectsShapeThatDoesNotFitMaterialSheet() {
        ProgramGenerationService service = service((toolPath, parameters) -> new GCodeProgram(java.util.List.of("UNUSED")));
        ProgramGenerationRequest validRequest = validRequest();
        ProgramGenerationRequest request = new ProgramGenerationRequest(
                validRequest.machine(), validRequest.tool(), new MaterialSheet(null, null, 5, 5, 2, null, null),
                validRequest.machiningParameters(), validRequest.shape(), validRequest.cutSide());

        assertThrows(IllegalArgumentException.class, () -> service.generate(request));
    }

    @Test
    void rejectsToolThatDoesNotBelongToSelectedMachine() {
        ProgramGenerationService service = service((toolPath, parameters) -> new GCodeProgram(java.util.List.of("UNUSED")));
        ProgramGenerationRequest request = validRequest();
        CncMachine otherMachine = machine(2L);
        Tool wrongTool = new Tool(2L, otherMachine, 2, "Drugi alat", "", 3, 10, 2, true, null, null);

        assertThrows(IllegalArgumentException.class, () -> service.generate(new ProgramGenerationRequest(
                request.machine(), wrongTool, request.materialSheet(), request.machiningParameters(),
                request.shape(), request.cutSide())));
    }

    private ProgramGenerationService service(hr.lukabosnjak.gcode.GCodeGenerator generator) {
        ShapeValidator shapeValidator = new ShapeValidator();
        MaterialSheetValidator sheetValidator = new MaterialSheetValidator();
        return new ProgramGenerationService(
                new MachiningJobValidator(shapeValidator, sheetValidator, new MachiningParametersValidator()),
                new ToolPathService(shapeValidator),
                new SingleShapeFitValidator(new ToolPathBoundsCalculator(), sheetValidator),
                new ToolPathCompensationService(),
                generator);
    }

    private ProgramGenerationRequest validRequest() {
        CncMachine machine = machine(1L);
        return new ProgramGenerationRequest(
                machine,
                new Tool(1L, machine, 1, "Testni alat", "", 3, 10, 2, true, null, null),
                new MaterialSheet(null, null, 100, 100, 2, null, null),
                new MachiningParameters(null, 12000, 500, 200, 2, 1, 5),
                new Shape(null, ShapeType.SQUARE, null, 10.0, null, null, null, null, null),
                CutSide.INSIDE);
    }

    private CncMachine machine(Long id) {
        return new CncMachine(
                id, "Testni stroj", null, null, "", 200, 200,
                50.0, 1000.0, 1000.0, 24000.0, null, null);
    }

    private static final class CapturingGenerator implements hr.lukabosnjak.gcode.GCodeGenerator {
        private int callCount;
        private ToolPath toolPath;
        private MachiningParameters parameters;

        @Override
        public GCodeProgram generate(ToolPath toolPath, MachiningParameters parameters) {
            callCount++;
            this.toolPath = toolPath;
            this.parameters = parameters;
            return new GCodeProgram(java.util.List.of("PREVIEW"));
        }
    }
}
