package hr.lukabosnjak.domain.entities;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MaterialSheetTest {
    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2026, 8, 25, 12, 0);

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY})
    void constructorRejectsInvalidDimensions(double invalidDimension) {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> createSheet(invalidDimension, 20.0, 3.0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> createSheet(10.0, invalidDimension, 3.0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> createSheet(10.0, 20.0, invalidDimension))
        );
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY})
    void settersRejectInvalidDimensionsWithoutChangingValidState(double invalidDimension) {
        MaterialSheet sheet = createSheet(10.0, 20.0, 3.0);

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> sheet.setWidth(invalidDimension)),
                () -> assertThrows(IllegalArgumentException.class, () -> sheet.setHeight(invalidDimension)),
                () -> assertThrows(IllegalArgumentException.class, () -> sheet.setThickness(invalidDimension))
        );
        assertAll(
                () -> assertEquals(10.0, sheet.getWidth()),
                () -> assertEquals(20.0, sheet.getHeight()),
                () -> assertEquals(3.0, sheet.getThickness())
        );
    }

    private static MaterialSheet createSheet(double width, double height, double thickness) {
        MaterialType materialType = new MaterialType(1L, "Plywood", null, TIMESTAMP, TIMESTAMP, null);
        return new MaterialSheet(1L, materialType, width, height, thickness, TIMESTAMP, TIMESTAMP);
    }
}
