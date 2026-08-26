package hr.lukabosnjak.service;

import hr.lukabosnjak.domain.entities.MachiningParameters;

import java.util.Objects;

/**
 * Small immutable source of default machining values for the operator workflow.
 * Values are explicitly marked when they are only software reference values.
 */
public record MachiningParametersPreset(
        String name,
        boolean referenceOnly,
        double spindleSpeed,
        double feedRate,
        double plungeRate,
        double cutDepth,
        double stepDown,
        double safeZ
) {
    public MachiningParametersPreset {
        name = Objects.requireNonNull(name, "name");
    }

    public static MachiningParametersPreset referenceDefaults() {
        return new MachiningParametersPreset(
                "Referentne testne postavke",
                true,
                18000.0,
                500.0,
                150.0,
                1.0,
                1.0,
                5.0);
    }

    public MachiningParameters toParameters() {
        return new MachiningParameters(
                null, spindleSpeed, feedRate, plungeRate, cutDepth, stepDown, safeZ);
    }
}
