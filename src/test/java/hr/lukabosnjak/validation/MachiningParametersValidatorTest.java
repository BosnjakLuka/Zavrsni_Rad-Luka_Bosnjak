package hr.lukabosnjak.validation;

import hr.lukabosnjak.domain.entities.MachiningParameters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MachiningParametersValidatorTest {
    private final MachiningParametersValidator validator = new MachiningParametersValidator();

    @Test
    void acceptsPositiveFiniteParameters() {
        assertDoesNotThrow(() -> validator.validate(parameters()));
    }

    @Test
    void rejectsMissingParameters() {
        assertMessage("Parametri obrade moraju biti zadani.", null);
    }

    @Test
    void rejectsNonPositiveSpindleSpeed() {
        MachiningParameters parameters = parameters();
        parameters.setSpindleSpeed(0.0);
        assertMessage("Brzina vretena mora biti veća od 0.", parameters);
    }

    @Test
    void rejectsNonPositiveFeedRate() {
        MachiningParameters parameters = parameters();
        parameters.setFeedRate(-1.0);
        assertMessage("Brzina posmaka mora biti veća od 0.", parameters);
    }

    @Test
    void rejectsNonPositivePlungeRate() {
        MachiningParameters parameters = parameters();
        parameters.setPlungeRate(0.0);
        assertMessage("Brzina poniranja mora biti veća od 0.", parameters);
    }

    @Test
    void rejectsNonPositiveCutDepth() {
        MachiningParameters parameters = parameters();
        parameters.setCutDepth(0.0);
        assertMessage("Dubina rezanja mora biti veća od 0 mm.", parameters);
    }

    @Test
    void rejectsNonPositiveStepDown() {
        MachiningParameters parameters = parameters();
        parameters.setStepDown(-1.0);
        assertMessage("Dubina jednog prolaza mora biti veća od 0 mm.", parameters);
    }

    @Test
    void rejectsNonPositiveSafeZWithoutInferringCoordinateSign() {
        MachiningParameters parameters = parameters();
        parameters.setSafeZ(0.0);
        assertMessage("Sigurna Z udaljenost mora biti veća od 0 mm.", parameters);
    }

    @Test
    void rejectsNonFiniteParameters() {
        MachiningParameters nanParameters = parameters();
        nanParameters.setFeedRate(Double.NaN);
        assertMessage("Brzina posmaka mora biti veća od 0.", nanParameters);

        MachiningParameters infiniteParameters = parameters();
        infiniteParameters.setSafeZ(Double.POSITIVE_INFINITY);
        assertMessage("Sigurna Z udaljenost mora biti veća od 0 mm.", infiniteParameters);
    }

    private void assertMessage(String expectedMessage, MachiningParameters parameters) {
        ValidationException exception = assertThrows(
                ValidationException.class, () -> validator.validate(parameters));
        assertEquals(expectedMessage, exception.getMessage());
    }

    private MachiningParameters parameters() {
        return new MachiningParameters(null, 18000.0, 2400.0, 600.0, 6.0, 3.0, 10.0);
    }
}
