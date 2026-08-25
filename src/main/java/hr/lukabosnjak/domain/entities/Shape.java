package hr.lukabosnjak.domain.entities;

import hr.lukabosnjak.domain.enums.ShapeSubtype;
import hr.lukabosnjak.domain.enums.ShapeType;

import java.time.LocalDateTime;

public class Shape {
    private Long shapeId;
    private ShapeType shapeType;
    private ShapeSubtype shapeSubtype;
    private Double dimensionA;
    private Double dimensionB;
    private Double dimensionC;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public Shape(
            Long shapeId,
            ShapeType shapeType,
            ShapeSubtype shapeSubtype,
            Double dimensionA,
            Double dimensionB,
            Double dimensionC,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        this.shapeId = shapeId;
        this.shapeType = shapeType;
        this.shapeSubtype = shapeSubtype;
        this.dimensionA = dimensionA;
        this.dimensionB = dimensionB;
        this.dimensionC = dimensionC;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public Long getShapeId() {
        return shapeId;
    }

    public void setShapeId(Long shapeId) {
        this.shapeId = shapeId;
    }

    public ShapeType getShapeType() {
        return shapeType;
    }

    public void setShapeType(ShapeType shapeType) {
        this.shapeType = shapeType;
    }

    public ShapeSubtype getShapeSubtype() {
        return shapeSubtype;
    }

    public void setShapeSubtype(ShapeSubtype shapeSubtype) {
        this.shapeSubtype = shapeSubtype;
    }

    public Double getDimensionA() {
        return dimensionA;
    }

    public void setDimensionA(Double dimensionA) {
        this.dimensionA = dimensionA;
    }

    public Double getDimensionB() {
        return dimensionB;
    }

    public void setDimensionB(Double dimensionB) {
        this.dimensionB = dimensionB;
    }

    public Double getDimensionC() {
        return dimensionC;
    }

    public void setDimensionC(Double dimensionC) {
        this.dimensionC = dimensionC;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
