package hr.lukabosnjak.domain.entities;

import java.time.LocalDateTime;

public class MachiningJob {
    private Long machiningJobId;
    private User createdBy;
    private CncMachine cncMachine;
    private Tool tool;
    private MaterialSheet materialSheet;
    private MachiningParameters machiningParameters;
    private Shape shape;
    private String name;
    private int quantity;
    private String gCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MachiningJob(
            Long machiningJobId,
            User createdBy,
            CncMachine cncMachine,
            Tool tool,
            MaterialSheet materialSheet,
            MachiningParameters machiningParameters,
            Shape shape,
            String name,
            int quantity,
            String gCode,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.machiningJobId = machiningJobId;
        this.createdBy = createdBy;
        this.cncMachine = cncMachine;
        this.tool = tool;
        this.materialSheet = materialSheet;
        this.machiningParameters = machiningParameters;
        this.shape = shape;
        this.name = name;
        this.quantity = quantity;
        this.gCode = gCode;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getMachiningJobId() {
        return machiningJobId;
    }

    public void setMachiningJobId(Long machiningJobId) {
        this.machiningJobId = machiningJobId;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public CncMachine getCncMachine() {
        return cncMachine;
    }

    public void setCncMachine(CncMachine cncMachine) {
        this.cncMachine = cncMachine;
    }

    public Tool getTool() {
        return tool;
    }

    public void setTool(Tool tool) {
        this.tool = tool;
    }

    public MaterialSheet getMaterialSheet() {
        return materialSheet;
    }

    public void setMaterialSheet(MaterialSheet materialSheet) {
        this.materialSheet = materialSheet;
    }

    public MachiningParameters getMachiningParameters() {
        return machiningParameters;
    }

    public void setMachiningParameters(MachiningParameters machiningParameters) {
        this.machiningParameters = machiningParameters;
    }

    public Shape getShape() {
        return shape;
    }

    public void setShape(Shape shape) {
        this.shape = shape;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getGCode() {
        return gCode;
    }

    public void setGCode(String gCode) {
        this.gCode = gCode;
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
