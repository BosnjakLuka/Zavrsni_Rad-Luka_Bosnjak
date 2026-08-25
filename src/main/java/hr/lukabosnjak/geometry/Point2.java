package hr.lukabosnjak.geometry;

public record Point2(double x, double y) {
    public Point2 {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("Point coordinates must be finite");
        }
    }

    public Point2 translated(double offsetX, double offsetY) {
        return new Point2(x + offsetX, y + offsetY);
    }
}
