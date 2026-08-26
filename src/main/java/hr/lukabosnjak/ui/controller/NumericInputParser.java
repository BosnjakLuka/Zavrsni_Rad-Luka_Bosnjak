package hr.lukabosnjak.ui.controller;

/**
 * Parses numeric values entered in JavaFX text fields without coupling the UI to domain validation.
 */
public final class NumericInputParser {

    private NumericInputParser() {
    }

    public static double parseRequiredFinite(String value, String fieldLabel) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Polje '" + fieldLabel + "' je obavezno.");
        }

        try {
            double parsedValue = Double.parseDouble(value.trim().replace(',', '.'));
            if (!Double.isFinite(parsedValue)) {
                throw new IllegalArgumentException("Polje '" + fieldLabel + "' mora sadržavati konačan broj.");
            }
            return parsedValue;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Polje '" + fieldLabel + "' mora sadržavati broj.");
        }
    }

    public static int parseRequiredInteger(String value, String fieldLabel) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Polje '" + fieldLabel + "' je obavezno.");
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Polje '" + fieldLabel + "' mora sadržavati cijeli broj.");
        }
    }
}
