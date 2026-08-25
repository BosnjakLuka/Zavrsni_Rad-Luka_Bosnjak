package hr.lukabosnjak.gcode;

import java.util.List;
import java.util.Objects;

public record GCodeProgram(List<String> lines) {
    public GCodeProgram {
        Objects.requireNonNull(lines, "lines");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("G-code program must contain at least one line");
        }
        for (String line : lines) {
            Objects.requireNonNull(line, "G-code line");
            if (line.indexOf('\r') >= 0 || line.indexOf('\n') >= 0) {
                throw new IllegalArgumentException("G-code lines must not contain line separators");
            }
        }
        lines = List.copyOf(lines);
    }

    public String text() {
        return String.join("\n", lines) + "\n";
    }
}
