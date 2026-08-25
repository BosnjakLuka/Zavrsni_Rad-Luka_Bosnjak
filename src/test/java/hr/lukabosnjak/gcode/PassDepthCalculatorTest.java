package hr.lukabosnjak.gcode;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PassDepthCalculatorTest {
    @Test
    void reachesTargetDepthWithoutAddingAnotherPass() {
        assertEquals(List.of(3.0, 6.0, 9.0, 10.0), PassDepthCalculator.calculate(10.0, 3.0));
    }

    @Test
    void producesOnePassWhenStepEqualsOrExceedsCutDepth() {
        assertEquals(List.of(10.0), PassDepthCalculator.calculate(10.0, 10.0));
        assertEquals(List.of(10.0), PassDepthCalculator.calculate(10.0, 12.0));
    }

    @Test
    void handlesDecimalStepsWithoutFloatingPointExtraPass() {
        assertEquals(List.of(0.1, 0.2, 0.3), PassDepthCalculator.calculate(0.3, 0.1));
    }

    @Test
    void returnsAnImmutableResult() {
        List<Double> depths = PassDepthCalculator.calculate(2.0, 1.0);

        assertThrows(UnsupportedOperationException.class, () -> depths.add(3.0));
    }

    @Test
    void rejectsValuesThatAreNotPositiveAndFinite() {
        assertThrows(IllegalArgumentException.class, () -> PassDepthCalculator.calculate(0.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> PassDepthCalculator.calculate(-1.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> PassDepthCalculator.calculate(1.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> PassDepthCalculator.calculate(1.0, -1.0));
        assertThrows(IllegalArgumentException.class, () -> PassDepthCalculator.calculate(Double.NaN, 1.0));
        assertThrows(IllegalArgumentException.class,
                () -> PassDepthCalculator.calculate(1.0, Double.POSITIVE_INFINITY));
    }
}
