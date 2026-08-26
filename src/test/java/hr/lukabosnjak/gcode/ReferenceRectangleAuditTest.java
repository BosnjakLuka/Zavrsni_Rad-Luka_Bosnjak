package hr.lukabosnjak.gcode;

import hr.lukabosnjak.domain.entities.MachiningParameters;
import hr.lukabosnjak.domain.entities.Shape;
import hr.lukabosnjak.domain.entities.Tool;
import hr.lukabosnjak.domain.enums.ShapeType;
import hr.lukabosnjak.geometry.Bounds2;
import hr.lukabosnjak.geometry.CutSide;
import hr.lukabosnjak.geometry.ToolPath;
import hr.lukabosnjak.geometry.ToolPathBoundsCalculator;
import hr.lukabosnjak.geometry.ToolPathCompensationService;
import hr.lukabosnjak.geometry.ToolPathService;
import hr.lukabosnjak.validation.ShapeValidator;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceRectangleAuditTest {
    private static final Path REFERENCE = Path.of(
            "Dokumentacija", "reference", "referentni-pravokutnik-100x200.nc");

    @Test
    void auditsCompensatedRectangleAgainstReferenceProgram() throws Exception {
        Shape shape = new Shape(null, ShapeType.RECTANGLE, null,
                100.0, 200.0, null, null, null, null);
        ToolPath nominalPath = new ToolPathService(new ShapeValidator()).generate(shape);
        ToolPath compensatedPath = new ToolPathCompensationService().compensate(
                nominalPath, new Tool(1L, null, 9006, "Software test tool", null,
                        6.0, null, null, true, null, null), CutSide.INSIDE);
        Bounds2 bounds = new ToolPathBoundsCalculator().calculate(compensatedPath);

        assertEquals(100.0, nominalPath.segments().get(1).start().x());
        assertEquals(200.0, nominalPath.segments().get(2).start().y());
        assertEquals(3.0, bounds.minX(), 1e-9);
        assertEquals(3.0, bounds.minY(), 1e-9);
        assertEquals(97.0, bounds.maxX(), 1e-9);
        assertEquals(197.0, bounds.maxY(), 1e-9);

        GCodeProgram generated = new RichAutoA11GCodeGenerator(
                RichAutoA11Profile.referenceProgramProfile()).generate(
                        compensatedPath, new MachiningParameters(
                                null, 18000.0, 500.0, 150.0, 1.0, 1.0, 5.0));
        String actual = generated.text();
        String reference = Files.readString(REFERENCE).replace("\r\n", "\n");

        assertTrue(actual.startsWith("G21\nG17\nG90\nG54\nM03\n"));
        assertTrue(actual.contains("G00 Z5.000\n"));
        assertTrue(actual.contains("G00 X3.000 Y3.000\n"));
        assertTrue(actual.contains("G01 Z-1.000 F150.000\n"));
        assertTrue(actual.contains("G01 X97.000 Y3.000 F500.000\n"));
        assertTrue(actual.contains("G01 X97.000 Y197.000\n"));
        assertTrue(actual.endsWith("G00 Z5.000\nM05\nM30\n"));
        assertTrue(actual.contains("F150.000"));
        assertTrue(actual.contains("F500.000"));
        assertTrue(actual.contains("M03"));
        assertTrue(actual.contains("M05"));
        assertTrue(actual.endsWith("M30\n"));
        assertTrue(reference.startsWith("G90 G54\nM03\n"));
        assertTrue(reference.contains("G01 X100.000 Y200.000\n"));
        assertNotEquals(reference, actual);
    }
}
