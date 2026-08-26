package hr.lukabosnjak.gcode;

import hr.lukabosnjak.domain.entities.MachiningParameters;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static hr.lukabosnjak.gcode.RichAutoA11Profile.ArcCenterMode.RELATIVE_TO_ARC_START;
import static hr.lukabosnjak.gcode.RichAutoA11Profile.PhysicalCapability.PROGRAM_END_M30;
import static hr.lukabosnjak.gcode.RichAutoA11Profile.PhysicalCapability.SPINDLE_COMMANDS;
import static hr.lukabosnjak.gcode.RichAutoA11Profile.PositioningMode.ABSOLUTE;
import static hr.lukabosnjak.gcode.RichAutoA11Profile.Units.MILLIMETERS;
import static hr.lukabosnjak.gcode.RichAutoA11Profile.ZCoordinateConvention.MATERIAL_SURFACE_ZERO_NEGATIVE_CUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RichAutoA11ProgramEnvelopeTest {
    @Test
    void emitsOnlyRequiredHeaderAndFooterWhenOptionalCommandsAreDisabled() {
        RichAutoA11ProgramEnvelope envelope = new RichAutoA11ProgramEnvelope(
                profile(false, false, false, false, Set.of()));

        assertEquals(List.of("G21", "G17", "G90"), envelope.headerLines(parameters()));
        assertEquals(List.of("M30"), envelope.footerLines());
    }

    @Test
    void emitsAllConfiguredHeaderAndFooterCommandsInDeterministicOrder() {
        RichAutoA11ProgramEnvelope envelope = new RichAutoA11ProgramEnvelope(
                profile(true, true, true, true, Set.of()));

        assertEquals(
                List.of("G21", "G17", "G90", "G54", "S18000.00", "M03"),
                envelope.headerLines(parameters()));
        assertEquals(List.of("M05", "M30"), envelope.footerLines());
    }

    @Test
    void keepsSpindleSpeedIndependentFromSpindleCommands() {
        RichAutoA11ProgramEnvelope speedOnly = new RichAutoA11ProgramEnvelope(
                profile(false, true, false, false, Set.of()));
        RichAutoA11ProgramEnvelope commandsOnly = new RichAutoA11ProgramEnvelope(
                profile(false, false, true, false, Set.of()));

        assertEquals(List.of("G21", "G17", "G90", "S18000.00"),
                speedOnly.headerLines(parameters()));
        assertEquals(List.of("M30"), speedOnly.footerLines());
        assertEquals(List.of("G21", "G17", "G90", "M03"),
                commandsOnly.headerLines(parameters()));
        assertEquals(List.of("M05", "M30"), commandsOnly.footerLines());
    }

    @Test
    void physicalConfirmationsDoNotChangeEmittedText() {
        RichAutoA11ProgramEnvelope unconfirmed = new RichAutoA11ProgramEnvelope(
                profile(true, true, true, false, Set.of()));
        RichAutoA11ProgramEnvelope confirmed = new RichAutoA11ProgramEnvelope(
                profile(true, true, true, false, Set.of(SPINDLE_COMMANDS, PROGRAM_END_M30)));

        assertEquals(unconfirmed.headerLines(parameters()), confirmed.headerLines(parameters()));
        assertEquals(unconfirmed.footerLines(), confirmed.footerLines());
    }

    @Test
    void doesNotEmitFeedRateBeforeAFeedMotionExists() {
        RichAutoA11ProgramEnvelope envelope = new RichAutoA11ProgramEnvelope(
                profile(false, false, false, true, Set.of()));

        assertFalse(envelope.headerLines(parameters()).stream().anyMatch(line -> line.startsWith("F")));
    }

    private RichAutoA11Profile profile(
            boolean emitG54,
            boolean emitSpindleSpeed,
            boolean emitSpindleCommands,
            boolean emitFeedRate,
            Set<RichAutoA11Profile.PhysicalCapability> confirmations
    ) {
        return new RichAutoA11Profile(
                emitFeedRate,
                emitSpindleSpeed,
                emitG54,
                emitSpindleCommands,
                MILLIMETERS,
                ABSOLUTE,
                2,
                MATERIAL_SURFACE_ZERO_NEGATIVE_CUT,
                RELATIVE_TO_ARC_START,
                confirmations);
    }

    private MachiningParameters parameters() {
        return new MachiningParameters(null, 18000.0, 1200.0, 300.0, 10.0, 3.0, 5.0);
    }
}
