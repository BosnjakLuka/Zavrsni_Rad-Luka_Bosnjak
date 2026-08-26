package hr.lukabosnjak.domain.entities;

import java.time.LocalDateTime;

public class Tool {
    private Long toolId;
    private CncMachine cncMachine;
    private int toolNumber;
    private String name;
    private String type;
    private double diameter;
    private Double cuttingLength;
    private Integer fluteCount;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Tool(
            Long toolId,
            CncMachine cncMachine,
            int toolNumber,
            String name,
            String type,
            double diameter,
            Number cuttingLength,
            Number fluteCount,
            boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.toolId = toolId;
        this.cncMachine = cncMachine;
        this.toolNumber = toolNumber;
        this.name = name;
        this.type = type;
        this.diameter = diameter;
        this.cuttingLength = cuttingLength == null ? null : cuttingLength.doubleValue();
        this.fluteCount = fluteCount == null ? null : fluteCount.intValue();
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getToolId() {
        return toolId;
    }

    public void setToolId(Long toolId) {
        this.toolId = toolId;
    }

    public CncMachine getCncMachine() {
        return cncMachine;
    }

    public void setCncMachine(CncMachine cncMachine) {
        this.cncMachine = cncMachine;
    }

    public int getToolNumber() {
        return toolNumber;
    }

    public void setToolNumber(int toolNumber) {
        this.toolNumber = toolNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getDiameter() {
        return diameter;
    }

    public void setDiameter(double diameter) {
        this.diameter = diameter;
    }

    public Double getCuttingLength() {
        return cuttingLength;
    }

    public void setCuttingLength(Double cuttingLength) {
        this.cuttingLength = cuttingLength;
    }

    public Integer getFluteCount() {
        return fluteCount;
    }

    public void setFluteCount(Integer fluteCount) {
        this.fluteCount = fluteCount;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
}
