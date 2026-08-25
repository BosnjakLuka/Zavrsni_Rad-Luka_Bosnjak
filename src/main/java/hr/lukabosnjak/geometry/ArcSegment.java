package hr.lukabosnjak.geometry;

import java.util.Objects;

public record ArcSegment(
        Point2 start,
        Point2 end,
        Point2 center,
        ArcDirection direction
) implements PathSegment {
    public ArcSegment {
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        Objects.requireNonNull(center);
        Objects.requireNonNull(direction);
        if (start.equals(end)) {
            throw new IllegalArgumentException("Arc start and end points must differ");
        }
    }

    @Override
    public ArcSegment translated(double offsetX, double offsetY) {
        return new ArcSegment(
                start.translated(offsetX, offsetY),
                end.translated(offsetX, offsetY),
                center.translated(offsetX, offsetY),
                direction);
    }
}
