package hr.lukabosnjak.gcode;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class GCodeFormatter {
    private final int decimalPlaces;

    public GCodeFormatter(int decimalPlaces) {
        if (decimalPlaces < 0) {
            throw new IllegalArgumentException("Decimal places must not be negative");
        }
        this.decimalPlaces = decimalPlaces;
    }

    public int decimalPlaces() {
        return decimalPlaces;
    }

    public String format(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("G-code numeric value must be finite");
        }

        BigDecimal formatted = BigDecimal.valueOf(value)
                .setScale(decimalPlaces, RoundingMode.HALF_UP);
        if (formatted.signum() == 0) {
            formatted = BigDecimal.ZERO.setScale(decimalPlaces);
        }
        return formatted.toPlainString();
    }
}
