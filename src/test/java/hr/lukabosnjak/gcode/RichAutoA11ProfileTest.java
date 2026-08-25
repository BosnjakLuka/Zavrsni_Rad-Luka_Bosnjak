package hr.lukabosnjak.gcode;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static hr.lukabosnjak.gcode.RichAutoA11Profile.PhysicalCapability.SPINDLE_SPEED_S;
import static hr.lukabosnjak.gcode.RichAutoA11Profile.PhysicalCapability.WORK_OFFSET_G54;
import static hr.lukabosnjak.gcode.RichAutoA11Profile.PositioningMode.ABSOLUTE;
import static hr.lukabosnjak.gcode.RichAutoA11Profile.Units.MILLIMETERS;
import static hr.lukabosnjak.gcode.RichAutoA11Profile.ZCoordinateConvention.MATERIAL_SURFACE_ZERO_NEGATIVE_CUT;
import static hr.lukabosnjak.gcode.RichAutoA11Profile.ZCoordinateConvention.MATERIAL_SURFACE_ZERO_POSITIVE_CUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RichAutoA11ProfileTest {
    @Test
    void keepsEmissionConfigurationSeparateFromPhysicalConfirmation() {
        RichAutoA11Profile profile = profile(true, false, Set.of(SPINDLE_SPEED_S));

        assertTrue(profile.emitG54());
        assertFalse(profile.isPhysicallyConfirmed(WORK_OFFSET_G54));
        assertFalse(profile.emitSpindleSpeed());
        assertTrue(profile.isPhysicallyConfirmed(SPINDLE_SPEED_S));
    }

    @Test
    void defensivelyCopiesPhysicalConfirmations() {
        EnumSet<RichAutoA11Profile.PhysicalCapability> confirmations = EnumSet.of(SPINDLE_SPEED_S);
        RichAutoA11Profile profile = profile(false, false, confirmations);

        confirmations.clear();

        assertTrue(profile.isPhysicallyConfirmed(SPINDLE_SPEED_S));
        assertThrows(UnsupportedOperationException.class,
                () -> profile.physicallyConfirmedCapabilities().add(WORK_OFFSET_G54));
    }

    @Test
    void mapsPositiveDomainValuesUsingNegativeCutConvention() {
        assertEquals(-4.5, MATERIAL_SURFACE_ZERO_NEGATIVE_CUT.toCutZ(4.5));
        assertEquals(8.0, MATERIAL_SURFACE_ZERO_NEGATIVE_CUT.toSafeZ(8.0));
    }

    @Test
    void mapsPositiveDomainValuesUsingPositiveCutConvention() {
        assertEquals(4.5, MATERIAL_SURFACE_ZERO_POSITIVE_CUT.toCutZ(4.5));
        assertEquals(-8.0, MATERIAL_SURFACE_ZERO_POSITIVE_CUT.toSafeZ(8.0));
    }

    @Test
    void rejectsValuesThatAreNotPositiveFiniteMagnitudes() {
        assertThrows(IllegalArgumentException.class,
                () -> MATERIAL_SURFACE_ZERO_NEGATIVE_CUT.toCutZ(0.0));
        assertThrows(IllegalArgumentException.class,
                () -> MATERIAL_SURFACE_ZERO_NEGATIVE_CUT.toCutZ(-1.0));
        assertThrows(IllegalArgumentException.class,
                () -> MATERIAL_SURFACE_ZERO_NEGATIVE_CUT.toSafeZ(Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> MATERIAL_SURFACE_ZERO_NEGATIVE_CUT.toSafeZ(Double.POSITIVE_INFINITY));
    }

    @Test
    void rejectsNegativeNumericPrecision() {
        assertThrows(IllegalArgumentException.class, () -> new RichAutoA11Profile(
                false, false, false, false,
                MILLIMETERS, ABSOLUTE, -1,
                MATERIAL_SURFACE_ZERO_NEGATIVE_CUT, Set.of()));
    }

    private RichAutoA11Profile profile(
            boolean emitG54,
            boolean emitSpindleSpeed,
            Set<RichAutoA11Profile.PhysicalCapability> confirmations
    ) {
        return new RichAutoA11Profile(
                false,
                emitSpindleSpeed,
                emitG54,
                false,
                MILLIMETERS,
                ABSOLUTE,
                3,
                MATERIAL_SURFACE_ZERO_NEGATIVE_CUT,
                confirmations);
    }
}
