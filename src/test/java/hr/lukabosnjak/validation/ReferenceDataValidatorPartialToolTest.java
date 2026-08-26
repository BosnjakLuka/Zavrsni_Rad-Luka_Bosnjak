package hr.lukabosnjak.validation;

import hr.lukabosnjak.domain.entities.CncMachine;
import hr.lukabosnjak.domain.entities.Tool;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ReferenceDataValidatorPartialToolTest {
    @Test
    void acceptsKnownToolNumberNameAndDiameterWhenOtherAttributesAreUnknown() {
        CncMachine machine = new CncMachine(
                1L, "ZK-1325", null, null, "RichAuto A11",
                1250.0, 2500.0, null, null, null, null, null, null);

        assertDoesNotThrow(() -> new ReferenceDataValidator().validate(
                new Tool(null, machine, 1, "Glodalo", null, 6.0, null, null,
                        true, null, null)));
    }
}
