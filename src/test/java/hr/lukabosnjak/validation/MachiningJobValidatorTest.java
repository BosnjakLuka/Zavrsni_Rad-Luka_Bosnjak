package hr.lukabosnjak.validation;

import hr.lukabosnjak.domain.entities.CncMachine;
import hr.lukabosnjak.domain.entities.MachiningJob;
import hr.lukabosnjak.domain.entities.MachiningParameters;
import hr.lukabosnjak.domain.entities.MaterialSheet;
import hr.lukabosnjak.domain.entities.Shape;
import hr.lukabosnjak.domain.entities.Tool;
import hr.lukabosnjak.domain.enums.ShapeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MachiningJobValidatorTest {
    private final MachiningJobValidator validator = new MachiningJobValidator(
            new ShapeValidator(), new MaterialSheetValidator(), new MachiningParametersValidator());

    @Test
    void acceptsIterationOneQuantityAndMachineRelationshipById() {
        CncMachine selectedMachine = machine(1L);
        Tool tool = tool(machine(1L), true);

        assertDoesNotThrow(() -> validator.validate(job(selectedMachine, tool)));
    }

    @Test
    void acceptsSameTransientMachineInstance() {
        CncMachine machine = machine(null);

        assertDoesNotThrow(() -> validator.validate(job(machine, tool(machine, true))));
    }

    @Test
    void rejectsMissingJob() {
        assertMessage("Posao obrade mora biti zadan.", null);
    }

    @Test
    void rejectsZeroAndNegativeQuantity() {
        MachiningJob zeroQuantity = validJob();
        zeroQuantity.setQuantity(0);
        assertMessage("Količina mora biti veća od 0.", zeroQuantity);

        MachiningJob negativeQuantity = validJob();
        negativeQuantity.setQuantity(-1);
        assertMessage("Količina mora biti veća od 0.", negativeQuantity);
    }

    @Test
    void rejectsMissingMachineAndTool() {
        MachiningJob missingMachine = validJob();
        missingMachine.setCncMachine(null);
        assertMessage("CNC stroj mora biti odabran.", missingMachine);

        MachiningJob missingTool = validJob();
        missingTool.setTool(null);
        assertMessage("Alat mora biti odabran.", missingTool);
    }

    @Test
    void rejectsToolFromAnotherMachine() {
        MachiningJob job = job(machine(1L), tool(machine(2L), true));

        assertMessage("Odabrani alat ne pripada odabranom CNC stroju.", job);
    }

    @Test
    void rejectsToolWithoutMachineRelationship() {
        MachiningJob job = job(machine(1L), tool(null, true));

        assertMessage("Odabrani alat ne pripada odabranom CNC stroju.", job);
    }

    @Test
    void rejectsInactiveTool() {
        CncMachine machine = machine(1L);
        MachiningJob job = job(machine, tool(machine, false));

        assertMessage("Odabrani alat nije aktivan.", job);
    }

    @Test
    void acceptsFeedRateAtMachineMaximumAndRejectsValueAboveIt() {
        MachiningJob atMaximum = validJob();
        atMaximum.getMachiningParameters().setFeedRate(5000.0);
        assertDoesNotThrow(() -> validator.validate(atMaximum));

        MachiningJob aboveMaximum = validJob();
        aboveMaximum.getMachiningParameters().setFeedRate(5000.1);
        assertMessage(
                "Brzina posmaka ne smije biti veća od maksimalne brzine posmaka odabranog stroja.",
                aboveMaximum);
    }

    @Test
    void acceptsInclusiveSpindleLimits() {
        MachiningJob atMinimum = validJob();
        atMinimum.getMachiningParameters().setSpindleSpeed(1000.0);
        assertDoesNotThrow(() -> validator.validate(atMinimum));

        MachiningJob atMaximum = validJob();
        atMaximum.getMachiningParameters().setSpindleSpeed(24000.0);
        assertDoesNotThrow(() -> validator.validate(atMaximum));
    }

    @Test
    void acceptsJobWhenMachineTechnicalLimitsAreUnknown() {
        CncMachine machine = machine(1L);
        machine.setWorkAreaZ(null);
        machine.setMaxFeedRate(null);
        machine.setMinSpindleSpeed(null);
        machine.setMaxSpindleSpeed(null);

        assertDoesNotThrow(() -> validator.validate(job(machine, tool(machine, true))));
    }

    @Test
    void enforcesEachKnownSpindleLimitIndependently() {
        CncMachine maximumOnly = machine(1L);
        maximumOnly.setMinSpindleSpeed(null);
        maximumOnly.setMaxSpindleSpeed(17_000.0);
        assertMessage(
                "Brzina vretena mora biti unutar raspona odabranog stroja.",
                job(maximumOnly, tool(maximumOnly, true)));

        CncMachine minimumOnly = machine(1L);
        minimumOnly.setMinSpindleSpeed(19_000.0);
        minimumOnly.setMaxSpindleSpeed(null);
        assertMessage(
                "Brzina vretena mora biti unutar raspona odabranog stroja.",
                job(minimumOnly, tool(minimumOnly, true)));
    }

    @Test
    void rejectsSpindleSpeedOutsideMachineRange() {
        MachiningJob belowMinimum = validJob();
        belowMinimum.getMachiningParameters().setSpindleSpeed(999.9);
        assertMessage("Brzina vretena mora biti unutar raspona odabranog stroja.", belowMinimum);

        MachiningJob aboveMaximum = validJob();
        aboveMaximum.getMachiningParameters().setSpindleSpeed(24000.1);
        assertMessage("Brzina vretena mora biti unutar raspona odabranog stroja.", aboveMaximum);
    }

    @Test
    void delegatesSnapshotValidationBeforeJobRules() {
        MachiningJob job = validJob();
        job.getShape().setDimensionA(0.0);
        job.setQuantity(0);

        assertMessage("Promjer kruga mora biti veći od 0 mm.", job);
    }

    private void assertMessage(String expectedMessage, MachiningJob job) {
        ValidationException exception = assertThrows(
                ValidationException.class, () -> validator.validate(job));
        assertEquals(expectedMessage, exception.getMessage());
    }

    private MachiningJob validJob() {
        CncMachine machine = machine(1L);
        return job(machine, tool(machine, true));
    }

    private MachiningJob job(CncMachine machine, Tool tool) {
        MaterialSheet sheet = new MaterialSheet(null, null, 600.0, 400.0, 18.0, null, null);
        MachiningParameters parameters = new MachiningParameters(
                null, 18000.0, 2400.0, 600.0, 6.0, 3.0, 10.0);
        Shape shape = new Shape(
                null, ShapeType.CIRCLE, null, 80.0, null, null, null, null, null);
        return new MachiningJob(
                null, null, machine, tool, sheet, parameters, shape,
                "Validation test job", 1, "", null, null);
    }

    private CncMachine machine(Long id) {
        return new CncMachine(
                id, "Test machine", null, null, "Test controller",
                1250.0, 2500.0, 150.0, 5000.0, 1000.0, 24000.0, null, null);
    }

    private Tool tool(CncMachine machine, boolean active) {
        return new Tool(
                1L, machine, 1, "Test tool", "TEST_TYPE",
                6.0, 20.0, 2, active, null, null);
    }
}
