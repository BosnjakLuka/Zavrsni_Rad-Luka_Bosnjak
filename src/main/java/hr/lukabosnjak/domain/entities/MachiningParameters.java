package hr.lukabosnjak.domain.entities;

public class MachiningParameters {
    private Long machiningParametersId;
    private double spindleSpeed;
    private double feedRate;
    private double plungeRate;
    private double cutDepth;
    private double stepDown;
    private double safeZ;

    public MachiningParameters(
            Long machiningParametersId,
            double spindleSpeed,
            double feedRate,
            double plungeRate,
            double cutDepth,
            double stepDown,
            double safeZ
    ) {
        this.machiningParametersId = machiningParametersId;
        this.spindleSpeed = spindleSpeed;
        this.feedRate = feedRate;
        this.plungeRate = plungeRate;
        this.cutDepth = cutDepth;
        this.stepDown = stepDown;
        this.safeZ = safeZ;
    }

    public Long getMachiningParametersId() {
        return machiningParametersId;
    }

    public void setMachiningParametersId(Long machiningParametersId) {
        this.machiningParametersId = machiningParametersId;
    }

    public double getSpindleSpeed() {
        return spindleSpeed;
    }

    public void setSpindleSpeed(double spindleSpeed) {
        this.spindleSpeed = spindleSpeed;
    }

    public double getFeedRate() {
        return feedRate;
    }

    public void setFeedRate(double feedRate) {
        this.feedRate = feedRate;
    }

    public double getPlungeRate() {
        return plungeRate;
    }

    public void setPlungeRate(double plungeRate) {
        this.plungeRate = plungeRate;
    }

    public double getCutDepth() {
        return cutDepth;
    }

    public void setCutDepth(double cutDepth) {
        this.cutDepth = cutDepth;
    }

    public double getStepDown() {
        return stepDown;
    }

    public void setStepDown(double stepDown) {
        this.stepDown = stepDown;
    }

    public double getSafeZ() {
        return safeZ;
    }

    public void setSafeZ(double safeZ) {
        this.safeZ = safeZ;
    }
}
