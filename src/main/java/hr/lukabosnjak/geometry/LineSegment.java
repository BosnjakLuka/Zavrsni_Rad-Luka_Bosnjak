package hr.lukabosnjak.geometry;

import java.util.Objects;

public record LineSegment(Point2 start, Point2 end) implements PathSegment {
    public LineSegment {
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
    }

    @Override
    public LineSegment translated(double offsetX, double offsetY) {
        return new LineSegment(
                start.translated(offsetX, offsetY),
                end.translated(offsetX, offsetY));
    }
}
