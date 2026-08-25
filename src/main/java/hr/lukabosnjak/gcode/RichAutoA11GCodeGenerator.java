package hr.lukabosnjak.gcode;

import hr.lukabosnjak.domain.entities.MachiningParameters;
import hr.lukabosnjak.geometry.ArcSegment;
import hr.lukabosnjak.geometry.LineSegment;
import hr.lukabosnjak.geometry.PathSegment;
import hr.lukabosnjak.geometry.Point2;
import hr.lukabosnjak.geometry.ToolPath;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RichAutoA11GCodeGenerator implements GCodeGenerator {
    private final RichAutoA11Profile profile;
    private final GCodeFormatter formatter;
    private final RichAutoA11ProgramEnvelope programEnvelope;

    public RichAutoA11GCodeGenerator(RichAutoA11Profile profile) {
        this.profile = Objects.requireNonNull(profile, "profile");
        this.formatter = new GCodeFormatter(profile.numericPrecision());
        this.programEnvelope = new RichAutoA11ProgramEnvelope(profile);
    }

    @Override
    public GCodeProgram generate(ToolPath toolPath, MachiningParameters parameters) {
        Objects.requireNonNull(toolPath, "toolPath");
        Objects.requireNonNull(parameters, "parameters");

        List<PathSegment> segments = requireConnectedClosedPath(toolPath);
        List<Double> passDepths = PassDepthCalculator.calculate(
                parameters.getCutDepth(),
                parameters.getStepDown());
        double safeZ = profile.zCoordinateConvention().toSafeZ(parameters.getSafeZ());
        Point2 startPoint = toolPath.startPoint();

        List<String> lines = new ArrayList<>();
        lines.addAll(programEnvelope.headerLines(parameters));
        lines.add(rapidZ(safeZ));

        for (double passDepth : passDepths) {
            lines.add(rapidXy(startPoint));
            lines.add(plunge(profile.zCoordinateConvention().toCutZ(passDepth), parameters.getPlungeRate()));
            lines.addAll(cuttingMoves(segments, parameters.getFeedRate()));
            lines.add(rapidZ(safeZ));
        }

        lines.addAll(programEnvelope.footerLines());
        return new GCodeProgram(lines);
    }

    private List<PathSegment> requireConnectedClosedPath(ToolPath toolPath) {
        Point2 previousEnd = null;

        for (PathSegment segment : toolPath.segments()) {
            if (previousEnd != null && !previousEnd.equals(segment.start())) {
                throw new IllegalArgumentException("ToolPath must be connected");
            }
            previousEnd = segment.end();
        }

        if (!previousEnd.equals(toolPath.startPoint())) {
            throw new IllegalArgumentException("ToolPath must be closed");
        }
        return toolPath.segments();
    }

    private String rapidZ(double z) {
        return "G00 Z" + formatter.format(z);
    }

    private String rapidXy(Point2 point) {
        return "G00 X" + formatter.format(point.x())
                + " Y" + formatter.format(point.y());
    }

    private String plunge(double cutZ, double plungeRate) {
        String command = "G01 Z" + formatter.format(cutZ);
        if (profile.emitFeedRate()) {
            command += " F" + formatter.format(plungeRate);
        }
        return command;
    }

    private List<String> cuttingMoves(List<PathSegment> segments, double feedRate) {
        List<String> lines = new ArrayList<>(segments.size());
        for (int index = 0; index < segments.size(); index++) {
            String command = cuttingMove(segments.get(index));
            if (index == 0 && profile.emitFeedRate()) {
                command += " F" + formatter.format(feedRate);
            }
            lines.add(command);
        }
        return lines;
    }

    private String cuttingMove(PathSegment segment) {
        return switch (segment) {
            case LineSegment line -> linearMove(line);
            case ArcSegment arc -> arcMove(arc);
        };
    }

    private String linearMove(LineSegment line) {
        return "G01 X" + formatter.format(line.end().x())
                + " Y" + formatter.format(line.end().y());
    }

    private String arcMove(ArcSegment arc) {
        double centerOffsetI = switch (profile.arcCenterMode()) {
            case RELATIVE_TO_ARC_START -> arc.center().x() - arc.start().x();
        };
        double centerOffsetJ = switch (profile.arcCenterMode()) {
            case RELATIVE_TO_ARC_START -> arc.center().y() - arc.start().y();
        };

        String command = switch (arc.direction()) {
            case CLOCKWISE -> "G02";
            case COUNTERCLOCKWISE -> "G03";
        };
        return command
                + " X" + formatter.format(arc.end().x())
                + " Y" + formatter.format(arc.end().y())
                + " I" + formatter.format(centerOffsetI)
                + " J" + formatter.format(centerOffsetJ);
    }
}
