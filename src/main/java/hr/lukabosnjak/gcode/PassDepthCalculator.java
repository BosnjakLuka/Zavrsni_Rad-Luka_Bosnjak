package hr.lukabosnjak.gcode;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public final class PassDepthCalculator {
    private PassDepthCalculator() {
    }

    public static List<Double> calculate(double cutDepth, double stepDown) {
        requirePositiveFinite(cutDepth, "Cut depth");
        requirePositiveFinite(stepDown, "Step down");

        BigDecimal target = BigDecimal.valueOf(cutDepth);
        BigDecimal step = BigDecimal.valueOf(stepDown);
        BigDecimal[] division = target.divideAndRemainder(step);
        BigInteger passCount = division[0].toBigIntegerExact();
        if (division[1].signum() != 0) {
            passCount = passCount.add(BigInteger.ONE);
        }
        if (passCount.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
            throw new IllegalArgumentException("Requested pass count exceeds the supported list size");
        }

        int count = passCount.intValueExact();
        List<Double> depths = new ArrayList<>(count);
        for (int pass = 1; pass < count; pass++) {
            depths.add(step.multiply(BigDecimal.valueOf(pass)).doubleValue());
        }
        depths.add(cutDepth);
        return List.copyOf(depths);
    }

    private static void requirePositiveFinite(double value, String valueName) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(valueName + " must be a positive finite value");
        }
    }
}
