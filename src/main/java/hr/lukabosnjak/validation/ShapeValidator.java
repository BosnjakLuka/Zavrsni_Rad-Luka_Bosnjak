package hr.lukabosnjak.validation;

import hr.lukabosnjak.domain.entities.Shape;
import hr.lukabosnjak.domain.enums.ShapeSubtype;

public final class ShapeValidator {
    public void validate(Shape shape) {
        if (shape == null) {
            throw new ValidationException("Oblik mora biti zadan.");
        }
        if (shape.getShapeType() == null) {
            throw new ValidationException("Vrsta oblika mora biti odabrana.");
        }

        switch (shape.getShapeType()) {
            case SQUARE -> requirePositive(
                    shape.getDimensionA(), "Duljina stranice mora biti veća od 0 mm.");
            case RECTANGLE -> {
                requirePositive(
                        shape.getDimensionA(), "Širina pravokutnika mora biti veća od 0 mm.");
                requirePositive(
                        shape.getDimensionB(), "Visina pravokutnika mora biti veća od 0 mm.");
            }
            case CIRCLE -> requirePositive(
                    shape.getDimensionA(), "Promjer kruga mora biti veći od 0 mm.");
            case TRIANGLE -> {
                if (shape.getShapeSubtype() != ShapeSubtype.EQUILATERAL) {
                    throw new ValidationException("Za trokut je podržan samo podtip EQUILATERAL.");
                }
                requirePositive(
                        shape.getDimensionA(), "Duljina stranice trokuta mora biti veća od 0 mm.");
            }
        }
    }

    private void requirePositive(Double value, String message) {
        if (value == null || !Double.isFinite(value) || value <= 0) {
            throw new ValidationException(message);
        }
    }
}
