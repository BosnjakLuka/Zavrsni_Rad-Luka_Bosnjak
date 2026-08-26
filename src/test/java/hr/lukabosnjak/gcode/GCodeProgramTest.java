package hr.lukabosnjak.gcode;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GCodeProgramTest {
    @Test
    void copiesLinesAndSerializesWithDeterministicLfEnding() {
        List<String> source = new ArrayList<>(List.of("G21", "M30"));

        GCodeProgram program = new GCodeProgram(source);
        source.set(0, "G20");

        assertEquals(List.of("G21", "M30"), program.lines());
        assertEquals("G21\nM30\n", program.text());
        assertThrows(UnsupportedOperationException.class, () -> program.lines().add("G54"));
    }

    @Test
    void rejectsEmptyProgramAndEmbeddedLineSeparators() {
        assertThrows(IllegalArgumentException.class, () -> new GCodeProgram(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new GCodeProgram(List.of("G21\nG90")));
        assertThrows(IllegalArgumentException.class, () -> new GCodeProgram(List.of("G21\rG90")));
    }
}
