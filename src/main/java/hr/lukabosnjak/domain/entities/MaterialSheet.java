package hr.lukabosnjak.domain.entities;

import java.time.LocalDateTime;

public class MaterialSheet {
    private Long materialSheetId;
    private MaterialType materialType;
    private double width;
    private double height;
    private double thickness;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MaterialSheet(
            Long materialSheetId,
            MaterialType materialType,
            double width,
            double height,
            double thickness,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.materialSheetId = materialSheetId;
        this.materialType = materialType;
        setWidth(width);
        setHeight(height);
        setThickness(thickness);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getMaterialSheetId() {
        return materialSheetId;
    }

    public void setMaterialSheetId(Long materialSheetId) {
        this.materialSheetId = materialSheetId;
    }

    public MaterialType getMaterialType() {
        return materialType;
    }

    public void setMaterialType(MaterialType materialType) {
        this.materialType = materialType;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = requirePositiveDimension("width", width);
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = requirePositiveDimension("height", height);
    }

    public double getThickness() {
        return thickness;
    }

    public void setThickness(double thickness) {
        this.thickness = requirePositiveDimension("thickness", thickness);
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

    private static double requirePositiveDimension(String fieldName, double value) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be finite and greater than 0");
        }
        return value;
    }
}
