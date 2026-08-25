package hr.lukabosnjak.validation;

import hr.lukabosnjak.domain.entities.MaterialSheet;

public final class MaterialSheetValidator {
    public void validate(MaterialSheet materialSheet) {
        if (materialSheet == null) {
            throw new ValidationException("Ploča materijala mora biti zadana.");
        }

        requirePositive(materialSheet.getWidth(), "Širina ploče mora biti veća od 0 mm.");
        requirePositive(materialSheet.getHeight(), "Visina ploče mora biti veća od 0 mm.");
        requirePositive(materialSheet.getThickness(), "Debljina ploče mora biti veća od 0 mm.");
    }

    private void requirePositive(double value, String message) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new ValidationException(message);
        }
    }
}
