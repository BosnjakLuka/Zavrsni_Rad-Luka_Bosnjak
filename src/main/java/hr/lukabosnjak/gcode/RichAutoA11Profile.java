package hr.lukabosnjak.gcode;

import java.util.Objects;
import java.util.Set;

public record RichAutoA11Profile(
        boolean emitFeedRate,
        boolean emitSpindleSpeed,
        boolean emitG54,
        boolean emitSpindleCommands,
        Units units,
        PositioningMode positioningMode,
        int numericPrecision,
        ZCoordinateConvention zCoordinateConvention,
        ArcCenterMode arcCenterMode,
        Set<PhysicalCapability> physicallyConfirmedCapabilities
) {
    public RichAutoA11Profile {
        Objects.requireNonNull(units, "units");
        Objects.requireNonNull(positioningMode, "positioningMode");
        Objects.requireNonNull(zCoordinateConvention, "zCoordinateConvention");
        Objects.requireNonNull(arcCenterMode, "arcCenterMode");
        Objects.requireNonNull(physicallyConfirmedCapabilities, "physicallyConfirmedCapabilities");
        if (numericPrecision < 0) {
            throw new IllegalArgumentException("Numeric precision must not be negative");
        }
        physicallyConfirmedCapabilities = Set.copyOf(physicallyConfirmedCapabilities);
    }

    public boolean isPhysicallyConfirmed(PhysicalCapability capability) {
        return physicallyConfirmedCapabilities.contains(Objects.requireNonNull(capability, "capability"));
    }

    /**
     * Output settings matching the supplied reference program.
     * Emission flags do not claim that the controller has been physically tested.
     */
    public static RichAutoA11Profile referenceProgramProfile() {
        return new RichAutoA11Profile(
                true,
                false,
                true,
                true,
                Units.MILLIMETERS,
                PositioningMode.ABSOLUTE,
                3,
                ZCoordinateConvention.MATERIAL_SURFACE_ZERO_NEGATIVE_CUT,
                ArcCenterMode.RELATIVE_TO_ARC_START,
                Set.of());
    }

    public enum Units {
        MILLIMETERS
    }

    public enum PositioningMode {
        ABSOLUTE
    }

    public enum ArcCenterMode {
        RELATIVE_TO_ARC_START
    }

    public enum ZCoordinateConvention {
        MATERIAL_SURFACE_ZERO_NEGATIVE_CUT(-1.0, 1.0),
        MATERIAL_SURFACE_ZERO_POSITIVE_CUT(1.0, -1.0);

        private final double cutSign;
        private final double safeSign;

        ZCoordinateConvention(double cutSign, double safeSign) {
            this.cutSign = cutSign;
            this.safeSign = safeSign;
        }

        public double toCutZ(double positiveDepth) {
            return signedCoordinate(positiveDepth, cutSign, "Cut depth");
        }

        public double toSafeZ(double positiveSafeZ) {
            return signedCoordinate(positiveSafeZ, safeSign, "Safe Z");
        }

        private static double signedCoordinate(double positiveMagnitude, double sign, String valueName) {
            if (!Double.isFinite(positiveMagnitude) || positiveMagnitude <= 0.0) {
                throw new IllegalArgumentException(valueName + " must be a positive finite value");
            }
            return sign * positiveMagnitude;
        }
    }

    public enum PhysicalCapability {
        FEED_RATE_F,
        SPINDLE_SPEED_S,
        WORK_OFFSET_G54,
        SPINDLE_COMMANDS,
        METRIC_UNITS_G21,
        XY_PLANE_G17,
        ABSOLUTE_POSITIONING_G90,
        PROGRAM_END_M30,
        Z_DIRECTION_AND_WORK_ZERO,
        ARC_MOVES_G02_G03,
        RELATIVE_ARC_CENTER_IJ
    }
}
