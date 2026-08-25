package hr.lukabosnjak.geometry;

import java.util.Objects;

public final class ToolPathBoundsCalculator {
    private static final double FULL_TURN = Math.PI * 2;
    private static final double ANGLE_TOLERANCE = 1e-12;
    private static final double[] CARDINAL_ANGLES = {
            0,
            Math.PI / 2,
            Math.PI,
            Math.PI * 3 / 2
    };

    public Bounds2 calculate(ToolPath toolPath) {
        Objects.requireNonNull(toolPath);

        BoundsAccumulator bounds = new BoundsAccumulator();
        for (PathSegment segment : toolPath.segments()) {
            bounds.include(segment.start());
            bounds.include(segment.end());
            if (segment instanceof ArcSegment arc) {
                includeArcExtrema(bounds, arc);
            }
        }
        return bounds.toBounds();
    }

    private void includeArcExtrema(BoundsAccumulator bounds, ArcSegment arc) {
        double radius = Math.hypot(
                arc.start().x() - arc.center().x(),
                arc.start().y() - arc.center().y());
        double startAngle = angleFromCenter(arc.start(), arc.center());
        double endAngle = angleFromCenter(arc.end(), arc.center());

        for (double angle : CARDINAL_ANGLES) {
            if (isOnSweep(angle, startAngle, endAngle, arc.direction())) {
                bounds.include(new Point2(
                        arc.center().x() + radius * Math.cos(angle),
                        arc.center().y() + radius * Math.sin(angle)));
            }
        }
    }

    private double angleFromCenter(Point2 point, Point2 center) {
        return normalizeAngle(Math.atan2(point.y() - center.y(), point.x() - center.x()));
    }

    private boolean isOnSweep(
            double candidate,
            double start,
            double end,
            ArcDirection direction
    ) {
        double sweep;
        double candidateOffset;
        if (direction == ArcDirection.COUNTERCLOCKWISE) {
            sweep = normalizeAngle(end - start);
            candidateOffset = normalizeAngle(candidate - start);
        } else {
            sweep = normalizeAngle(start - end);
            candidateOffset = normalizeAngle(start - candidate);
        }
        return candidateOffset <= sweep + ANGLE_TOLERANCE;
    }

    private double normalizeAngle(double angle) {
        double normalized = angle % FULL_TURN;
        return normalized < 0 ? normalized + FULL_TURN : normalized;
    }

    private static final class BoundsAccumulator {
        private double minX = Double.POSITIVE_INFINITY;
        private double minY = Double.POSITIVE_INFINITY;
        private double maxX = Double.NEGATIVE_INFINITY;
        private double maxY = Double.NEGATIVE_INFINITY;

        private void include(Point2 point) {
            minX = Math.min(minX, point.x());
            minY = Math.min(minY, point.y());
            maxX = Math.max(maxX, point.x());
            maxY = Math.max(maxY, point.y());
        }

        private Bounds2 toBounds() {
            return new Bounds2(minX, minY, maxX, maxY);
        }
    }
}
