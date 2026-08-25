package hr.lukabosnjak.geometry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ToolPathTranslationTest {
    private static final double TOLERANCE = 1e-9;

    @Test
    void translatesLineEndpointsWithoutChangingOriginalPath() {
        ToolPath original = new ToolPath(List.of(
                new LineSegment(new Point2(0, 0), new Point2(10, 5))));

        ToolPath translated = original.translated(25, 40);

        LineSegment originalLine = assertInstanceOf(LineSegment.class, original.segments().getFirst());
        assertPoint(originalLine.start(), 0, 0);
        assertPoint(originalLine.end(), 10, 5);

        LineSegment translatedLine = assertInstanceOf(
                LineSegment.class, translated.segments().getFirst());
        assertPoint(translatedLine.start(), 25, 40);
        assertPoint(translatedLine.end(), 35, 45);
    }

    @Test
    void translatesArcEndpointsAndCenterWhilePreservingDirection() {
        ArcSegment originalArc = new ArcSegment(
                new Point2(0, 10),
                new Point2(20, 10),
                new Point2(10, 10),
                ArcDirection.COUNTERCLOCKWISE);
        ToolPath original = new ToolPath(List.of(originalArc));

        ToolPath translated = original.translated(5, -2);

        ArcSegment translatedArc = assertInstanceOf(
                ArcSegment.class, translated.segments().getFirst());
        assertPoint(translatedArc.start(), 5, 8);
        assertPoint(translatedArc.end(), 25, 8);
        assertPoint(translatedArc.center(), 15, 8);
        assertEquals(ArcDirection.COUNTERCLOCKWISE, translatedArc.direction());

        assertPoint(originalArc.start(), 0, 10);
        assertPoint(originalArc.end(), 20, 10);
        assertPoint(originalArc.center(), 10, 10);
    }

    @Test
    void protectsGeometryValueObjectInvariants() {
        assertThrows(IllegalArgumentException.class,
                () -> new Point2(Double.NaN, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ArcSegment(
                        new Point2(1, 0),
                        new Point2(1, 0),
                        new Point2(0, 0),
                        ArcDirection.CLOCKWISE));
        assertThrows(IllegalArgumentException.class,
                () -> new ToolPath(List.of()));
    }

    private void assertPoint(Point2 point, double expectedX, double expectedY) {
        assertEquals(expectedX, point.x(), TOLERANCE);
        assertEquals(expectedY, point.y(), TOLERANCE);
    }
}
