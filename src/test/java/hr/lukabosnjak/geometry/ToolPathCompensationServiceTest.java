package hr.lukabosnjak.geometry;

import hr.lukabosnjak.domain.entities.Tool;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToolPathCompensationServiceTest {
    private final ToolPathCompensationService service = new ToolPathCompensationService();

    @Test
    void offsetsOutsideRectangleByThreeMillimetresForDiameterSix() {
        ToolPath compensated = service.compensate(
                rectangle(100.0, 200.0),
                tool(6.0),
                CutSide.OUTSIDE);

        assertRectangle(compensated, -3.0, -3.0, 106.0, 206.0);
    }

    @Test
    void offsetsInsideRectangleByFourMillimetresForDiameterEight() {
        ToolPath compensated = service.compensate(
                rectangle(100.0, 200.0),
                tool(8.0),
                CutSide.INSIDE);

        assertRectangle(compensated, 4.0, 4.0, 92.0, 192.0);
    }

    private ToolPath rectangle(double width, double height) {
        return new ToolPath(List.of(
                new LineSegment(new Point2(0.0, 0.0), new Point2(width, 0.0)),
                new LineSegment(new Point2(width, 0.0), new Point2(width, height)),
                new LineSegment(new Point2(width, height), new Point2(0.0, height)),
                new LineSegment(new Point2(0.0, height), new Point2(0.0, 0.0))));
    }

    private Tool tool(double diameter) {
        return new Tool(1L, null, 1, "Software test tool", null, diameter,
                null, null, true, null, null);
    }

    private void assertRectangle(
            ToolPath path,
            double minX,
            double minY,
            double width,
            double height
    ) {
        List<PathSegment> segments = path.segments();
        assertEquals(4, segments.size());
        assertPoint((LineSegment) segments.get(0), minX, minY, minX + width, minY);
        assertPoint((LineSegment) segments.get(1), minX + width, minY, minX + width, minY + height);
        assertPoint((LineSegment) segments.get(2), minX + width, minY + height, minX, minY + height);
        assertPoint((LineSegment) segments.get(3), minX, minY + height, minX, minY);
    }

    private void assertPoint(
            LineSegment segment,
            double startX,
            double startY,
            double endX,
            double endY
    ) {
        assertEquals(startX, segment.start().x(), 1e-9);
        assertEquals(startY, segment.start().y(), 1e-9);
        assertEquals(endX, segment.end().x(), 1e-9);
        assertEquals(endY, segment.end().y(), 1e-9);
    }
}
