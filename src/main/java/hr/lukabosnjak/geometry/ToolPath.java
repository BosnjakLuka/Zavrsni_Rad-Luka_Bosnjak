package hr.lukabosnjak.geometry;

import java.util.List;
import java.util.Objects;

public record ToolPath(List<PathSegment> segments) {
    public ToolPath {
        Objects.requireNonNull(segments);
        segments = List.copyOf(segments);
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("ToolPath must contain at least one segment");
        }
    }

    public Point2 startPoint() {
        return segments.getFirst().start();
    }

    public ToolPath translated(double offsetX, double offsetY) {
        return new ToolPath(segments.stream()
                .map(segment -> segment.translated(offsetX, offsetY))
                .toList());
    }
}
