package hr.lukabosnjak.geometry;

public record Bounds2(double minX, double minY, double maxX, double maxY) {
    public Bounds2 {
        if (!Double.isFinite(minX)
                || !Double.isFinite(minY)
                || !Double.isFinite(maxX)
                || !Double.isFinite(maxY)) {
            throw new IllegalArgumentException("Bounds coordinates must be finite");
        }
        if (minX > maxX || minY > maxY) {
            throw new IllegalArgumentException("Bounds minimum must not exceed maximum");
        }
    }

    public double width() {
        return maxX - minX;
    }

    public double height() {
        return maxY - minY;
    }
}
