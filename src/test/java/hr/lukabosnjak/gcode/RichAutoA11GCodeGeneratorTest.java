package hr.lukabosnjak.gcode;

import hr.lukabosnjak.domain.entities.MachiningParameters;
import hr.lukabosnjak.geometry.ArcDirection;
import hr.lukabosnjak.geometry.ArcSegment;
import hr.lukabosnjak.geometry.LineSegment;
import hr.lukabosnjak.geometry.Point2;
import hr.lukabosnjak.geometry.ToolPath;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static hr.lukabosnjak.gcode.RichAutoA11Profile.ArcCenterMode.RELATIVE_TO_ARC_START;
import static hr.lukabosnjak.gcode.RichAutoA11Profile.PositioningMode.ABSOLUTE;
import static hr.lukabosnjak.gcode.RichAutoA11Profile.Units.MILLIMETERS;
import static hr.lukabosnjak.gcode.RichAutoA11Profile.ZCoordinateConvention.MATERIAL_SURFACE_ZERO_NEGATIVE_CUT;
import static hr.lukabosnjak.gcode.RichAutoA11Profile.ZCoordinateConvention.MATERIAL_SURFACE_ZERO_POSITIVE_CUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RichAutoA11GCodeGeneratorTest {
    // Software-only fixtures. These are not confirmed machining values for ZK-1325 / RichAuto A11.
    private static final double SOFTWARE_TEST_SPINDLE_SPEED = 12_000.0;
    private static final double SOFTWARE_TEST_FEED_RATE = 900.0;
    private static final double SOFTWARE_TEST_PLUNGE_RATE = 250.0;
    private static final double SOFTWARE_TEST_CUT_DEPTH = 5.0;
    private static final double SOFTWARE_TEST_STEP_DOWN = 2.0;
    private static final double SOFTWARE_TEST_SAFE_Z = 6.0;
    private static final int SOFTWARE_TEST_PRECISION = 3;

    @Test
    void generatesDeterministicMultiPassProgramForSoftwareTestProfile() {
        RichAutoA11GCodeGenerator generator = new RichAutoA11GCodeGenerator(
                softwareTestProfile(MATERIAL_SURFACE_ZERO_NEGATIVE_CUT, true, true, true, true));

        GCodeProgram program = generator.generate(translatedRectangle(), softwareTestParameters());

        assertEquals("""
                G21
                G17
                G90
                G54
                S12000.000
                M03
                G00 Z6.000
                G00 X10.000 Y20.000
                G01 Z-2.000 F250.000
                G01 X30.000 Y20.000 F900.000
                G01 X30.000 Y40.000
                G01 X10.000 Y40.000
                G01 X10.000 Y20.000
                G00 Z6.000
                G00 X10.000 Y20.000
                G01 Z-4.000 F250.000
                G01 X30.000 Y20.000 F900.000
                G01 X30.000 Y40.000
                G01 X10.000 Y40.000
                G01 X10.000 Y20.000
                G00 Z6.000
                G00 X10.000 Y20.000
                G01 Z-5.000 F250.000
                G01 X30.000 Y20.000 F900.000
                G01 X30.000 Y40.000
                G01 X10.000 Y40.000
                G01 X10.000 Y20.000
                G00 Z6.000
                M05
                M30
                """, program.text());
    }

    @Test
    void usesPositiveCutConventionAndOmitsFeedWordsWhenDisabled() {
        RichAutoA11GCodeGenerator generator = new RichAutoA11GCodeGenerator(
                softwareTestProfile(MATERIAL_SURFACE_ZERO_POSITIVE_CUT, false, false, false, false));
        MachiningParameters onePassParameters = new MachiningParameters(
                null,
                SOFTWARE_TEST_SPINDLE_SPEED,
                SOFTWARE_TEST_FEED_RATE,
                SOFTWARE_TEST_PLUNGE_RATE,
                SOFTWARE_TEST_CUT_DEPTH,
                10.0,
                SOFTWARE_TEST_SAFE_Z);

        GCodeProgram program = generator.generate(triangle(), onePassParameters);

        assertEquals("""
                G21
                G17
                G90
                G00 Z-6.000
                G00 X0.000 Y0.000
                G01 Z5.000
                G01 X1.000 Y0.000
                G01 X0.500 Y1.000
                G01 X0.000 Y0.000
                G00 Z-6.000
                M30
                """, program.text());
        assertFalse(program.lines().stream().anyMatch(line -> line.contains(" F")));
    }

    @Test
    void emitsRelativeIjAndG03ForCounterclockwiseSemicirclesAcrossPasses() {
        RichAutoA11GCodeGenerator generator = generatorWithDefaultSoftwareTestProfile();

        GCodeProgram program = generator.generate(
                translatedCircle(ArcDirection.COUNTERCLOCKWISE),
                softwareTestParameters());

        assertEquals(List.of(
                        "G03 X20.000 Y25.000 I5.000 J0.000 F900.000",
                        "G03 X10.000 Y25.000 I-5.000 J0.000",
                        "G03 X20.000 Y25.000 I5.000 J0.000 F900.000",
                        "G03 X10.000 Y25.000 I-5.000 J0.000",
                        "G03 X20.000 Y25.000 I5.000 J0.000 F900.000",
                        "G03 X10.000 Y25.000 I-5.000 J0.000"),
                program.lines().stream().filter(line -> line.startsWith("G03")).toList());
    }

    @Test
    void emitsG02ForClockwiseSemicircles() {
        RichAutoA11GCodeGenerator generator = new RichAutoA11GCodeGenerator(
                softwareTestProfile(MATERIAL_SURFACE_ZERO_NEGATIVE_CUT, false, false, false, false));
        MachiningParameters onePassParameters = new MachiningParameters(
                null,
                SOFTWARE_TEST_SPINDLE_SPEED,
                SOFTWARE_TEST_FEED_RATE,
                SOFTWARE_TEST_PLUNGE_RATE,
                SOFTWARE_TEST_CUT_DEPTH,
                10.0,
                SOFTWARE_TEST_SAFE_Z);

        GCodeProgram program = generator.generate(
                translatedCircle(ArcDirection.CLOCKWISE),
                onePassParameters);

        assertEquals(List.of(
                        "G02 X20.000 Y25.000 I5.000 J0.000",
                        "G02 X10.000 Y25.000 I-5.000 J0.000"),
                program.lines().stream().filter(line -> line.startsWith("G02")).toList());
    }

    @Test
    void rejectsDisconnectedLinePath() {
        ToolPath disconnected = new ToolPath(List.of(
                new LineSegment(new Point2(0.0, 0.0), new Point2(1.0, 0.0)),
                new LineSegment(new Point2(2.0, 0.0), new Point2(0.0, 0.0))));
        RichAutoA11GCodeGenerator generator = generatorWithDefaultSoftwareTestProfile();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> generator.generate(disconnected, softwareTestParameters()));

        assertEquals("ToolPath must be connected", exception.getMessage());
    }

    @Test
    void rejectsOpenLinePath() {
        ToolPath open = new ToolPath(List.of(
                new LineSegment(new Point2(0.0, 0.0), new Point2(1.0, 0.0)),
                new LineSegment(new Point2(1.0, 0.0), new Point2(2.0, 0.0))));
        RichAutoA11GCodeGenerator generator = generatorWithDefaultSoftwareTestProfile();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> generator.generate(open, softwareTestParameters()));

        assertEquals("ToolPath must be closed", exception.getMessage());
    }

    private RichAutoA11GCodeGenerator generatorWithDefaultSoftwareTestProfile() {
        return new RichAutoA11GCodeGenerator(
                softwareTestProfile(MATERIAL_SURFACE_ZERO_NEGATIVE_CUT, false, false, false, true));
    }

    private RichAutoA11Profile softwareTestProfile(
            RichAutoA11Profile.ZCoordinateConvention zConvention,
            boolean emitG54,
            boolean emitSpindleSpeed,
            boolean emitSpindleCommands,
            boolean emitFeedRate
    ) {
        return new RichAutoA11Profile(
                emitFeedRate,
                emitSpindleSpeed,
                emitG54,
                emitSpindleCommands,
                MILLIMETERS,
                ABSOLUTE,
                SOFTWARE_TEST_PRECISION,
                zConvention,
                RELATIVE_TO_ARC_START,
                Set.of());
    }

    private MachiningParameters softwareTestParameters() {
        return new MachiningParameters(
                null,
                SOFTWARE_TEST_SPINDLE_SPEED,
                SOFTWARE_TEST_FEED_RATE,
                SOFTWARE_TEST_PLUNGE_RATE,
                SOFTWARE_TEST_CUT_DEPTH,
                SOFTWARE_TEST_STEP_DOWN,
                SOFTWARE_TEST_SAFE_Z);
    }

    private ToolPath translatedRectangle() {
        Point2 bottomLeft = new Point2(10.0, 20.0);
        Point2 bottomRight = new Point2(30.0, 20.0);
        Point2 topRight = new Point2(30.0, 40.0);
        Point2 topLeft = new Point2(10.0, 40.0);
        return new ToolPath(List.of(
                new LineSegment(bottomLeft, bottomRight),
                new LineSegment(bottomRight, topRight),
                new LineSegment(topRight, topLeft),
                new LineSegment(topLeft, bottomLeft)));
    }

    private ToolPath triangle() {
        Point2 pointA = new Point2(0.0, 0.0);
        Point2 pointB = new Point2(1.0, 0.0);
        Point2 pointC = new Point2(0.5, 1.0);
        return new ToolPath(List.of(
                new LineSegment(pointA, pointB),
                new LineSegment(pointB, pointC),
                new LineSegment(pointC, pointA)));
    }

    private ToolPath translatedCircle(ArcDirection direction) {
        Point2 left = new Point2(10.0, 25.0);
        Point2 right = new Point2(20.0, 25.0);
        Point2 center = new Point2(15.0, 25.0);
        return new ToolPath(List.of(
                new ArcSegment(left, right, center, direction),
                new ArcSegment(right, left, center, direction)));
    }
}
