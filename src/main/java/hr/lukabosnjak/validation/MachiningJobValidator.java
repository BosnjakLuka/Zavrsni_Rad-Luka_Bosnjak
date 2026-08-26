package hr.lukabosnjak.validation;

import hr.lukabosnjak.domain.entities.CncMachine;
import hr.lukabosnjak.domain.entities.MachiningJob;
import hr.lukabosnjak.domain.entities.Tool;

import java.util.Objects;

public final class MachiningJobValidator {
    private final ShapeValidator shapeValidator;
    private final MaterialSheetValidator materialSheetValidator;
    private final MachiningParametersValidator machiningParametersValidator;

    public MachiningJobValidator(
            ShapeValidator shapeValidator,
            MaterialSheetValidator materialSheetValidator,
            MachiningParametersValidator machiningParametersValidator
    ) {
        this.shapeValidator = Objects.requireNonNull(shapeValidator);
        this.materialSheetValidator = Objects.requireNonNull(materialSheetValidator);
        this.machiningParametersValidator = Objects.requireNonNull(machiningParametersValidator);
    }

    public void validate(MachiningJob job) {
        if (job == null) {
            throw new ValidationException("Posao obrade mora biti zadan.");
        }

        shapeValidator.validate(job.getShape());
        materialSheetValidator.validate(job.getMaterialSheet());
        machiningParametersValidator.validate(job.getMachiningParameters());

        if (job.getQuantity() <= 0) {
            throw new ValidationException("Količina mora biti veća od 0.");
        }

        CncMachine machine = job.getCncMachine();
        if (machine == null) {
            throw new ValidationException("CNC stroj mora biti odabran.");
        }

        Tool tool = job.getTool();
        if (tool == null) {
            throw new ValidationException("Alat mora biti odabran.");
        }
        if (!belongsTo(tool, machine)) {
            throw new ValidationException("Odabrani alat ne pripada odabranom CNC stroju.");
        }
        if (!tool.isActive()) {
            throw new ValidationException("Odabrani alat nije aktivan.");
        }
        Double maxFeedRate = machine.getMaxFeedRate();
        if (maxFeedRate != null && job.getMachiningParameters().getFeedRate() > maxFeedRate) {
            throw new ValidationException(
                    "Brzina posmaka ne smije biti veća od maksimalne brzine posmaka odabranog stroja.");
        }

        double spindleSpeed = job.getMachiningParameters().getSpindleSpeed();
        Double minSpindleSpeed = machine.getMinSpindleSpeed();
        Double maxSpindleSpeed = machine.getMaxSpindleSpeed();
        if ((minSpindleSpeed != null && spindleSpeed < minSpindleSpeed)
                || (maxSpindleSpeed != null && spindleSpeed > maxSpindleSpeed)) {
            throw new ValidationException("Brzina vretena mora biti unutar raspona odabranog stroja.");
        }
    }

    private boolean belongsTo(Tool tool, CncMachine selectedMachine) {
        CncMachine toolMachine = tool.getCncMachine();
        if (toolMachine == selectedMachine) {
            return true;
        }
        return toolMachine != null
                && toolMachine.getCncMachineId() != null
                && selectedMachine.getCncMachineId() != null
                && toolMachine.getCncMachineId().equals(selectedMachine.getCncMachineId());
    }
}
