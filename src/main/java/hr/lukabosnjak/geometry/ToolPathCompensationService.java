package hr.lukabosnjak.geometry;

import hr.lukabosnjak.domain.entities.Tool;

import java.util.List;
import java.util.Objects;

/**
 * Converts a programmed part contour into the cutter-center path.
 */
public final class ToolPathCompensationService {
    private static final double EPSILON = 1e-9;

    public ToolPath compensate(ToolPath programmedContour, Tool tool, CutSide cutSide) {
        Objects.requireNonNull(programmedContour, "programmedContour");
        Objects.requireNonNull(tool, "tool");
        Objects.requireNonNull(cutSide, "cutSide");

        double radius = tool.getDiameter() / 2.0;
        if (!Double.isFinite(radius) || radius <= 0.0) {
            throw new IllegalArgumentException("Tool diameter must be positive and finite");
        }

        if (programmedContour.segments().stream().allMatch(LineSegment.class::isInstance)) {
            return compensateLines(programmedContour, radius, cutSide);
        }
        if (programmedContour.segments().stream().allMatch(ArcSegment.class::isInstance)) {
            return compensateArcs(programmedContour, radius, cutSide);
        }
        throw new IllegalArgumentException("Mixed line and arc contours are not supported");
    }

    private ToolPath compensateLines(ToolPath path, double radius, CutSide cutSide) {
        List<LineSegment> lines = path.segments().stream()
                .map(LineSegment.class::cast)
                .toList();
        double signedArea = signedArea(lines);
        if (Math.abs(signedArea) <= EPSILON) {
            throw new IllegalArgumentException("Contour direction cannot be determined");
        }

        boolean counterClockwise = signedArea > 0.0;
        boolean leftSide = cutSide == CutSide.INSIDE ? counterClockwise : !counterClockwise;
        List<OffsetLine> offsetLines = lines.stream()
                .map(line -> offsetLine(line, leftSide ? radius : -radius))
                .toList();

        List<LineSegment> compensated = new java.util.ArrayList<>(lines.size());
        for (int index = 0; index < lines.size(); index++) {
            OffsetLine previous = offsetLines.get((index + lines.size() - 1) % lines.size());
            OffsetLine current = offsetLines.get(index);
            OffsetLine next = offsetLines.get((index + 1) % lines.size());
            Point2 start = intersect(previous, current);
            Point2 end = intersect(current, next);
            compensated.add(new LineSegment(start, end));
        }
        return new ToolPath(compensated.stream().map(segment -> (PathSegment) segment).toList());
    }

    private ToolPath compensateArcs(ToolPath path, double radius, CutSide cutSide) {
        List<ArcSegment> arcs = path.segments().stream()
                .map(ArcSegment.class::cast)
                .toList();
        Point2 center = arcs.getFirst().center();
        ArcDirection direction = arcs.getFirst().direction();
        double originalRadius = distance(arcs.getFirst().start(), center);
        if (originalRadius <= radius + EPSILON) {
            throw new IllegalArgumentException("Inside compensation leaves no positive arc radius");
        }

        for (ArcSegment arc : arcs) {
            if (!arc.center().equals(center)
                    || arc.direction() != direction
                    || Math.abs(distance(arc.start(), center) - originalRadius) > EPSILON) {
                throw new IllegalArgumentException("Only one circular contour is supported");
            }
        }

        boolean inward = (direction == ArcDirection.COUNTERCLOCKWISE) == (cutSide == CutSide.INSIDE);
        double compensatedRadius = originalRadius + (inward ? -radius : radius);
        List<PathSegment> compensated = arcs.stream()
                .map(arc -> new ArcSegment(
                        radialPoint(arc.start(), center, compensatedRadius),
                        radialPoint(arc.end(), center, compensatedRadius),
                        center,
                        direction))
                .map(segment -> (PathSegment) segment)
                .toList();
        return new ToolPath(compensated);
    }

    private OffsetLine offsetLine(LineSegment line, double signedDistance) {
        double dx = line.end().x() - line.start().x();
        double dy = line.end().y() - line.start().y();
        double length = Math.hypot(dx, dy);
        if (length <= EPSILON) {
            throw new IllegalArgumentException("Contour cannot contain zero-length segments");
        }
        double normalX = -dy / length;
        double normalY = dx / length;
        double offsetX = normalX * signedDistance;
        double offsetY = normalY * signedDistance;
        return new OffsetLine(
                new Point2(line.start().x() + offsetX, line.start().y() + offsetY),
                new Point2(line.end().x() + offsetX, line.end().y() + offsetY));
    }

    private Point2 intersect(OffsetLine first, OffsetLine second) {
        double firstDx = first.end().x() - first.start().x();
        double firstDy = first.end().y() - first.start().y();
        double secondDx = second.end().x() - second.start().x();
        double secondDy = second.end().y() - second.start().y();
        double denominator = firstDx * secondDy - firstDy * secondDx;
        if (Math.abs(denominator) <= EPSILON) {
            throw new IllegalArgumentException("Contour contains parallel adjacent segments");
        }
        double startDx = second.start().x() - first.start().x();
        double startDy = second.start().y() - first.start().y();
        double factor = (startDx * secondDy - startDy * secondDx) / denominator;
        return new Point2(
                first.start().x() + factor * firstDx,
                first.start().y() + factor * firstDy);
    }

    private double signedArea(List<LineSegment> lines) {
        double area = 0.0;
        for (LineSegment line : lines) {
            area += line.start().x() * line.end().y() - line.end().x() * line.start().y();
        }
        return area / 2.0;
    }

    private Point2 radialPoint(Point2 point, Point2 center, double radius) {
        double dx = point.x() - center.x();
        double dy = point.y() - center.y();
        double length = Math.hypot(dx, dy);
        return new Point2(
                center.x() + dx / length * radius,
                center.y() + dy / length * radius);
    }

    private double distance(Point2 first, Point2 second) {
        return Math.hypot(first.x() - second.x(), first.y() - second.y());
    }

    private record OffsetLine(Point2 start, Point2 end) {
    }
}
