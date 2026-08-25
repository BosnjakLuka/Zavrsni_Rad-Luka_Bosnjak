package hr.lukabosnjak.geometry;

import hr.lukabosnjak.domain.entities.Shape;
import hr.lukabosnjak.domain.enums.ShapeSubtype;
import hr.lukabosnjak.domain.enums.ShapeType;
import hr.lukabosnjak.validation.ShapeValidator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToolPathBoundsCalculatorTest {
    private static final double TOLERANCE = 1e-9;

    private final ToolPathService toolPathService = new ToolPathService(new ShapeValidator());
    private final ToolPathBoundsCalculator calculator = new ToolPathBoundsCalculator();

    @Test
    void calculatesSquareBoundsFromLineSegments() {
        ToolPath path = toolPathService.generate(shape(ShapeType.SQUARE, null, 30.0, null));

        assertBounds(calculator.calculate(path), 0, 0, 30, 30);
    }

    @Test
    void calculatesRectangleBoundsFromLineSegments() {
        ToolPath path = toolPathService.generate(
                shape(ShapeType.RECTANGLE, null, 40.0, 20.0));

        assertBounds(calculator.calculate(path), 0, 0, 40, 20);
    }

    @Test
    void calculatesTriangleBoundsFromGeneratedVertices() {
        double side = 30.0;
        ToolPath path = toolPathService.generate(
                shape(ShapeType.TRIANGLE, ShapeSubtype.EQUILATERAL, side, null));

        assertBounds(calculator.calculate(path), 0, 0, side, Math.sqrt(3) / 2 * side);
    }

    @Test
    void includesCircleCardinalExtremaThatAreNotArcEndpoints() {
        ToolPath path = toolPathService.generate(shape(ShapeType.CIRCLE, null, 20.0, null));

        assertBounds(calculator.calculate(path), 0, 0, 20, 20);
    }

    @Test
    void calculatesBoundsOfTranslatedCircle() {
        ToolPath path = toolPathService
                .generate(shape(ShapeType.CIRCLE, null, 20.0, null))
                .translated(25, 40);

        assertBounds(calculator.calculate(path), 25, 40, 45, 60);
    }

    @Test
    void respectsClockwiseArcSweepWhenIncludingExtrema() {
        ToolPath path = new ToolPath(List.of(new ArcSegment(
                new Point2(1, 0),
                new Point2(0, 1),
                new Point2(0, 0),
                ArcDirection.CLOCKWISE)));

        assertBounds(calculator.calculate(path), -1, -1, 1, 1);
    }

    private void assertBounds(
            Bounds2 bounds,
            double minX,
            double minY,
            double maxX,
            double maxY
    ) {
        assertEquals(minX, bounds.minX(), TOLERANCE);
        assertEquals(minY, bounds.minY(), TOLERANCE);
        assertEquals(maxX, bounds.maxX(), TOLERANCE);
        assertEquals(maxY, bounds.maxY(), TOLERANCE);
        assertEquals(maxX - minX, bounds.width(), TOLERANCE);
        assertEquals(maxY - minY, bounds.height(), TOLERANCE);
    }

    private Shape shape(ShapeType type, ShapeSubtype subtype, Double dimensionA, Double dimensionB) {
        return new Shape(null, type, subtype, dimensionA, dimensionB, null, null, null, null);
    }
}
