package hr.lukabosnjak.gcode;

import hr.lukabosnjak.domain.entities.MachiningParameters;
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

        List<LineSegment> segments = requireConnectedClosedLinePath(toolPath);
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

    private List<LineSegment> requireConnectedClosedLinePath(ToolPath toolPath) {
        List<LineSegment> lineSegments = new ArrayList<>(toolPath.segments().size());
        Point2 previousEnd = null;

        for (PathSegment segment : toolPath.segments()) {
            if (!(segment instanceof LineSegment lineSegment)) {
                throw new IllegalArgumentException("Linear G-code generator supports only line segments");
            }
            if (previousEnd != null && !previousEnd.equals(lineSegment.start())) {
                throw new IllegalArgumentException("Line-segment ToolPath must be connected");
            }
            lineSegments.add(lineSegment);
            previousEnd = lineSegment.end();
        }

        if (!previousEnd.equals(lineSegments.getFirst().start())) {
            throw new IllegalArgumentException("Line-segment ToolPath must be closed");
        }
        return List.copyOf(lineSegments);
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

    private List<String> cuttingMoves(List<LineSegment> segments, double feedRate) {
        List<String> lines = new ArrayList<>(segments.size());
        for (int index = 0; index < segments.size(); index++) {
            Point2 end = segments.get(index).end();
            String command = "G01 X" + formatter.format(end.x())
                    + " Y" + formatter.format(end.y());
            if (index == 0 && profile.emitFeedRate()) {
                command += " F" + formatter.format(feedRate);
            }
            lines.add(command);
        }
        return lines;
    }
}
