package hr.lukabosnjak.validation;

import hr.lukabosnjak.domain.entities.MachiningParameters;

public final class MachiningParametersValidator {
    public void validate(MachiningParameters parameters) {
        if (parameters == null) {
            throw new ValidationException("Parametri obrade moraju biti zadani.");
        }

        requirePositive(parameters.getSpindleSpeed(), "Brzina vretena mora biti veća od 0.");
        requirePositive(parameters.getFeedRate(), "Brzina posmaka mora biti veća od 0.");
        requirePositive(parameters.getPlungeRate(), "Brzina poniranja mora biti veća od 0.");
        requirePositive(parameters.getCutDepth(), "Dubina rezanja mora biti veća od 0 mm.");
        requirePositive(parameters.getStepDown(), "Dubina jednog prolaza mora biti veća od 0 mm.");
        requirePositive(parameters.getSafeZ(), "Sigurna Z udaljenost mora biti veća od 0 mm.");
    }

    private void requirePositive(double value, String message) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new ValidationException(message);
        }
    }
}
