package hr.lukabosnjak.gcode;

import hr.lukabosnjak.domain.entities.MachiningParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RichAutoA11ProgramEnvelope {
    private final RichAutoA11Profile profile;
    private final GCodeFormatter formatter;

    public RichAutoA11ProgramEnvelope(RichAutoA11Profile profile) {
        this.profile = Objects.requireNonNull(profile, "profile");
        this.formatter = new GCodeFormatter(profile.numericPrecision());
    }

    public List<String> headerLines(MachiningParameters parameters) {
        Objects.requireNonNull(parameters, "parameters");

        List<String> lines = new ArrayList<>();
        lines.add(unitsCommand(profile.units()));
        lines.add("G17");
        lines.add(positioningCommand(profile.positioningMode()));

        if (profile.emitG54()) {
            lines.add("G54");
        }
        if (profile.emitSpindleSpeed()) {
            lines.add("S" + formatter.format(parameters.getSpindleSpeed()));
        }
        if (profile.emitSpindleCommands()) {
            lines.add("M03");
        }

        return List.copyOf(lines);
    }

    public List<String> footerLines() {
        if (profile.emitSpindleCommands()) {
            return List.of("M05", "M30");
        }
        return List.of("M30");
    }

    private String unitsCommand(RichAutoA11Profile.Units units) {
        return switch (units) {
            case MILLIMETERS -> "G21";
        };
    }

    private String positioningCommand(RichAutoA11Profile.PositioningMode positioningMode) {
        return switch (positioningMode) {
            case ABSOLUTE -> "G90";
        };
    }
}
