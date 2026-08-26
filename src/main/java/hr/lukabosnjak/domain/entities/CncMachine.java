package hr.lukabosnjak.domain.entities;

import java.time.LocalDateTime;

public class CncMachine {
    private Long cncMachineId;
    private String name;
    private String manufacturer;
    private String model;
    private String controller;
    private double workAreaX;
    private double workAreaY;
    private Double workAreaZ;
    private Double maxFeedRate;
    private Double minSpindleSpeed;
    private Double maxSpindleSpeed;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CncMachine(
            Long cncMachineId,
            String name,
            String manufacturer,
            String model,
            String controller,
            double workAreaX,
            double workAreaY,
            Double workAreaZ,
            Double maxFeedRate,
            Double minSpindleSpeed,
            Double maxSpindleSpeed,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this(cncMachineId, name, manufacturer, model, controller, workAreaX, workAreaY,
                workAreaZ, maxFeedRate, minSpindleSpeed, maxSpindleSpeed, true, createdAt, updatedAt);
    }

    public CncMachine(
            Long cncMachineId,
            String name,
            String manufacturer,
            String model,
            String controller,
            double workAreaX,
            double workAreaY,
            Double workAreaZ,
            Double maxFeedRate,
            Double minSpindleSpeed,
            Double maxSpindleSpeed,
            boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.cncMachineId = cncMachineId;
        this.name = name;
        this.manufacturer = manufacturer;
        this.model = model;
        this.controller = controller;
        this.workAreaX = workAreaX;
        this.workAreaY = workAreaY;
        this.workAreaZ = workAreaZ;
        this.maxFeedRate = maxFeedRate;
        this.minSpindleSpeed = minSpindleSpeed;
        this.maxSpindleSpeed = maxSpindleSpeed;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getCncMachineId() {
        return cncMachineId;
    }

    public void setCncMachineId(Long cncMachineId) {
        this.cncMachineId = cncMachineId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getController() {
        return controller;
    }

    public void setController(String controller) {
        this.controller = controller;
    }

    public double getWorkAreaX() {
        return workAreaX;
    }

    public void setWorkAreaX(double workAreaX) {
        this.workAreaX = workAreaX;
    }

    public double getWorkAreaY() {
        return workAreaY;
    }

    public void setWorkAreaY(double workAreaY) {
        this.workAreaY = workAreaY;
    }

    public Double getWorkAreaZ() {
        return workAreaZ;
    }

    public void setWorkAreaZ(Double workAreaZ) {
        this.workAreaZ = workAreaZ;
    }

    public Double getMaxFeedRate() {
        return maxFeedRate;
    }

    public void setMaxFeedRate(Double maxFeedRate) {
        this.maxFeedRate = maxFeedRate;
    }

    public Double getMinSpindleSpeed() {
        return minSpindleSpeed;
    }

    public void setMinSpindleSpeed(Double minSpindleSpeed) {
        this.minSpindleSpeed = minSpindleSpeed;
    }

    public Double getMaxSpindleSpeed() {
        return maxSpindleSpeed;
    }

    public void setMaxSpindleSpeed(Double maxSpindleSpeed) {
        this.maxSpindleSpeed = maxSpindleSpeed;
    }

    public boolean isActive() { return active; }

    public void setActive(boolean active) { this.active = active; }

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
