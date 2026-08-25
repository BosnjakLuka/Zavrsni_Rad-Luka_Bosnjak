package hr.lukabosnjak.geometry;

import hr.lukabosnjak.domain.entities.Shape;
import hr.lukabosnjak.domain.enums.ShapeSubtype;
import hr.lukabosnjak.domain.enums.ShapeType;
import hr.lukabosnjak.validation.ShapeValidator;
import hr.lukabosnjak.validation.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ToolPathServiceTest {
    private static final double TOLERANCE = 1e-9;

    private final ToolPathService service = new ToolPathService(new ShapeValidator());

    @Test
    void generatesClosedSquareFromLocalOrigin() {
        ToolPath path = service.generate(shape(ShapeType.SQUARE, null, 30.0, null));

        assertEquals(4, path.segments().size());
        assertLine(path, 0, 0, 0, 30, 0);
        assertLine(path, 1, 30, 0, 30, 30);
        assertLine(path, 2, 30, 30, 0, 30);
        assertLine(path, 3, 0, 30, 0, 0);
        assertClosed(path);
    }

    @Test
    void generatesClosedRectangleFromLocalOrigin() {
        ToolPath path = service.generate(shape(ShapeType.RECTANGLE, null, 40.0, 20.0));

        assertEquals(4, path.segments().size());
        assertLine(path, 0, 0, 0, 40, 0);
        assertLine(path, 1, 40, 0, 40, 20);
        assertLine(path, 2, 40, 20, 0, 20);
        assertLine(path, 3, 0, 20, 0, 0);
        assertClosed(path);
    }

    @Test
    void generatesClosedEquilateralTriangleUsingExpectedHeight() {
        double side = 30.0;
        double expectedHeight = Math.sqrt(3) / 2 * side;

        ToolPath path = service.generate(
                shape(ShapeType.TRIANGLE, ShapeSubtype.EQUILATERAL, side, null));

        assertEquals(3, path.segments().size());
        assertLine(path, 0, 0, 0, side, 0);
        assertLine(path, 1, side, 0, side / 2, expectedHeight);
        assertLine(path, 2, side / 2, expectedHeight, 0, 0);
        assertClosed(path);
    }

    @Test
    void generatesCircleAsTwoCounterclockwiseSemicircles() {
        double diameter = 20.0;
        double radius = diameter / 2;

        ToolPath path = service.generate(shape(ShapeType.CIRCLE, null, diameter, null));

        assertEquals(2, path.segments().size());
        ArcSegment first = assertArc(path, 0, 0, radius, diameter, radius, radius, radius);
        ArcSegment second = assertArc(path, 1, diameter, radius, 0, radius, radius, radius);
        assertEquals(ArcDirection.COUNTERCLOCKWISE, first.direction());
        assertEquals(ArcDirection.COUNTERCLOCKWISE, second.direction());
        assertEquals(radius, distance(first.start(), first.center()), TOLERANCE);
        assertEquals(radius, distance(first.end(), first.center()), TOLERANCE);
        assertEquals(radius, distance(second.start(), second.center()), TOLERANCE);
        assertEquals(radius, distance(second.end(), second.center()), TOLERANCE);
        assertClosed(path);
    }

    @Test
    void delegatesInvalidShapeToShapeValidator() {
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.generate(shape(ShapeType.CIRCLE, null, 0.0, null)));

        assertEquals("Promjer kruga mora biti veći od 0 mm.", exception.getMessage());
    }

    private void assertLine(
            ToolPath path,
            int index,
            double startX,
            double startY,
            double endX,
            double endY
    ) {
        LineSegment line = assertInstanceOf(LineSegment.class, path.segments().get(index));
        assertPoint(line.start(), startX, startY);
        assertPoint(line.end(), endX, endY);
    }

    private ArcSegment assertArc(
            ToolPath path,
            int index,
            double startX,
            double startY,
            double endX,
            double endY,
            double centerX,
            double centerY
    ) {
        ArcSegment arc = assertInstanceOf(ArcSegment.class, path.segments().get(index));
        assertPoint(arc.start(), startX, startY);
        assertPoint(arc.end(), endX, endY);
        assertPoint(arc.center(), centerX, centerY);
        return arc;
    }

    private void assertClosed(ToolPath path) {
        assertPoint(
                path.segments().getLast().end(),
                path.startPoint().x(),
                path.startPoint().y());
    }

    private void assertPoint(Point2 point, double expectedX, double expectedY) {
        assertEquals(expectedX, point.x(), TOLERANCE);
        assertEquals(expectedY, point.y(), TOLERANCE);
    }

    private double distance(Point2 first, Point2 second) {
        return Math.hypot(first.x() - second.x(), first.y() - second.y());
    }

    private Shape shape(ShapeType type, ShapeSubtype subtype, Double dimensionA, Double dimensionB) {
        return new Shape(null, type, subtype, dimensionA, dimensionB, null, null, null, null);
    }
}
