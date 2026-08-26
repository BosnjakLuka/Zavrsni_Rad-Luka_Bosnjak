package hr.lukabosnjak.service;

import hr.lukabosnjak.domain.entities.MachiningParameters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MachiningParametersPresetTest {
    @Test
    void referenceDefaultsAreExplicitlyMarkedAndBecomeSnapshotParameters() {
        MachiningParametersPreset preset = MachiningParametersPreset.referenceDefaults();

        assertTrue(preset.referenceOnly());

        MachiningParameters parameters = preset.toParameters();

        assertEquals(18000.0, parameters.getSpindleSpeed());
        assertEquals(500.0, parameters.getFeedRate());
        assertEquals(150.0, parameters.getPlungeRate());
        assertEquals(1.0, parameters.getCutDepth());
        assertEquals(1.0, parameters.getStepDown());
        assertEquals(5.0, parameters.getSafeZ());
    }
}
