package hr.lukabosnjak.geometry;

import hr.lukabosnjak.domain.entities.Shape;
import hr.lukabosnjak.validation.ShapeValidator;

import java.util.List;
import java.util.Objects;

public final class ToolPathService {
    private final ShapeValidator shapeValidator;

    public ToolPathService(ShapeValidator shapeValidator) {
        this.shapeValidator = Objects.requireNonNull(shapeValidator);
    }

    public ToolPath generate(Shape shape) {
        shapeValidator.validate(shape);

        return switch (shape.getShapeType()) {
            case SQUARE -> generateSquare(shape.getDimensionA());
            case RECTANGLE -> generateRectangle(shape.getDimensionA(), shape.getDimensionB());
            case CIRCLE -> generateCircle(shape.getDimensionA());
            case TRIANGLE -> generateEquilateralTriangle(shape.getDimensionA());
        };
    }

    private ToolPath generateSquare(double side) {
        return generateRectangle(side, side);
    }

    private ToolPath generateRectangle(double width, double height) {
        Point2 bottomLeft = new Point2(0, 0);
        Point2 bottomRight = new Point2(width, 0);
        Point2 topRight = new Point2(width, height);
        Point2 topLeft = new Point2(0, height);

        return new ToolPath(List.of(
                new LineSegment(bottomLeft, bottomRight),
                new LineSegment(bottomRight, topRight),
                new LineSegment(topRight, topLeft),
                new LineSegment(topLeft, bottomLeft)));
    }

    private ToolPath generateEquilateralTriangle(double side) {
        Point2 pointA = new Point2(0, 0);
        Point2 pointB = new Point2(side, 0);
        Point2 pointC = new Point2(side / 2, Math.sqrt(3) / 2 * side);

        return new ToolPath(List.of(
                new LineSegment(pointA, pointB),
                new LineSegment(pointB, pointC),
                new LineSegment(pointC, pointA)));
    }

    private ToolPath generateCircle(double diameter) {
        double radius = diameter / 2;
        Point2 center = new Point2(radius, radius);
        Point2 left = new Point2(0, radius);
        Point2 right = new Point2(diameter, radius);

        return new ToolPath(List.of(
                new ArcSegment(left, right, center, ArcDirection.COUNTERCLOCKWISE),
                new ArcSegment(right, left, center, ArcDirection.COUNTERCLOCKWISE)));
    }
}
