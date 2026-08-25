package hr.lukabosnjak.geometry;

public sealed interface PathSegment permits LineSegment, ArcSegment {
    Point2 start();

    Point2 end();

    PathSegment translated(double offsetX, double offsetY);
}
