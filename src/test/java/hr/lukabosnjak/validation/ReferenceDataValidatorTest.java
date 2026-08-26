package hr.lukabosnjak.validation;

import hr.lukabosnjak.domain.entities.CncMachine;
import hr.lukabosnjak.domain.entities.MaterialType;
import hr.lukabosnjak.domain.entities.Tool;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReferenceDataValidatorTest {
    private final ReferenceDataValidator validator = new ReferenceDataValidator();

    @Test
    void acceptsCompleteMaterialMachineAndToolData() {
        CncMachine machine = machine(1L, 1_000.0, 24_000.0);

        assertDoesNotThrow(() -> validator.validate(
                new MaterialType(null, "MDF", null, null, null, null)));
        assertDoesNotThrow(() -> validator.validate(machine));
        assertDoesNotThrow(() -> validator.validate(
                new Tool(null, machine, 1, "Glodalo", "Ravno", 6.0, 20.0, 2,
                        true, null, null)));
    }

    @Test
    void rejectsInvalidMachineLimitsAndIncompleteToolData() {
        assertThrows(ValidationException.class, () -> validator.validate(machine(null, 25_000.0, 24_000.0)));
        assertThrows(ValidationException.class, () -> validator.validate(
                new Tool(null, machine(null, 1_000.0, 24_000.0), 0, "", "", 0.0, 0.0, 0,
                        true, null, null)));
    }

    private CncMachine machine(Long id, double minSpindleSpeed, double maxSpindleSpeed) {
        return new CncMachine(
                id, "Stroj", null, null, "Kontroler", 1250.0, 2500.0, 80.0,
                5000.0, minSpindleSpeed, maxSpindleSpeed, null, null);
    }
}
