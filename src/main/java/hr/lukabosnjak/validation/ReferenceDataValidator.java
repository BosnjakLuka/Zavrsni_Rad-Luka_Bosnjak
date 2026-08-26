package hr.lukabosnjak.validation;

import hr.lukabosnjak.domain.entities.CncMachine;
import hr.lukabosnjak.domain.entities.MaterialType;
import hr.lukabosnjak.domain.entities.Tool;

/** Validates user-entered reference data before it reaches persistence. */
public final class ReferenceDataValidator {

    public void validate(MaterialType materialType) {
        if (materialType == null) {
            throw new ValidationException("Vrsta materijala mora biti zadana.");
        }
        requireText(materialType.getName(), "Naziv vrste materijala je obavezan.");
    }

    public void validate(CncMachine machine) {
        if (machine == null) {
            throw new ValidationException("CNC stroj mora biti zadan.");
        }
        requireText(machine.getName(), "Naziv CNC stroja je obavezan.");
        requireText(machine.getController(), "Kontroler CNC stroja je obavezan.");
        requirePositive(machine.getWorkAreaX(), "Radna površina X mora biti veća od 0 mm.");
        requirePositive(machine.getWorkAreaY(), "Radna površina Y mora biti veća od 0 mm.");
        requirePositiveWhenKnown(machine.getWorkAreaZ(), "Radna površina Z mora biti veća od 0 mm.");
        requirePositiveWhenKnown(machine.getMaxFeedRate(), "Maksimalni posmak mora biti veći od 0.");
        requirePositiveWhenKnown(machine.getMinSpindleSpeed(), "Minimalna brzina vretena mora biti veća od 0.");
        requirePositiveWhenKnown(machine.getMaxSpindleSpeed(), "Maksimalna brzina vretena mora biti veća od 0.");
        if (machine.getMinSpindleSpeed() != null && machine.getMaxSpindleSpeed() != null
                && machine.getMinSpindleSpeed() > machine.getMaxSpindleSpeed()) {
            throw new ValidationException(
                    "Minimalna brzina vretena ne smije biti veća od maksimalne brzine vretena.");
        }
    }

    public void validate(Tool tool) {
        if (tool == null) {
            throw new ValidationException("Alat mora biti zadan.");
        }
        if (tool.getCncMachine() == null || tool.getCncMachine().getCncMachineId() == null) {
            throw new ValidationException("Alat mora biti povezan sa spremljenim CNC strojem.");
        }
        if (tool.getToolNumber() <= 0) {
            throw new ValidationException("Broj alata mora biti veći od 0.");
        }
        requireText(tool.getName(), "Naziv alata je obavezan.");
        requireText(tool.getType(), "Tip alata je obavezan.");
        requirePositive(tool.getDiameter(), "Promjer alata mora biti veći od 0 mm.");
        requirePositive(tool.getCuttingLength(), "Rezna duljina alata mora biti veća od 0 mm.");
        if (tool.getFluteCount() <= 0) {
            throw new ValidationException("Broj oštrica mora biti veći od 0.");
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(message);
        }
    }

    private void requirePositive(double value, String message) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new ValidationException(message);
        }
    }

    private void requirePositiveWhenKnown(Double value, String message) {
        if (value != null) {
            requirePositive(value, message);
        }
    }
}
