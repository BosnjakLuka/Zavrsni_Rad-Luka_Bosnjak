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
    private double workAreaZ;
    private double maxFeedRate;
    private double minSpindleSpeed;
    private double maxSpindleSpeed;
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
            double workAreaZ,
            double maxFeedRate,
            double minSpindleSpeed,
            double maxSpindleSpeed,
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

    public double getWorkAreaZ() {
        return workAreaZ;
    }

    public void setWorkAreaZ(double workAreaZ) {
        this.workAreaZ = workAreaZ;
    }

    public double getMaxFeedRate() {
        return maxFeedRate;
    }

    public void setMaxFeedRate(double maxFeedRate) {
        this.maxFeedRate = maxFeedRate;
    }

    public double getMinSpindleSpeed() {
        return minSpindleSpeed;
    }

    public void setMinSpindleSpeed(double minSpindleSpeed) {
        this.minSpindleSpeed = minSpindleSpeed;
    }

    public double getMaxSpindleSpeed() {
        return maxSpindleSpeed;
    }

    public void setMaxSpindleSpeed(double maxSpindleSpeed) {
        this.maxSpindleSpeed = maxSpindleSpeed;
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
