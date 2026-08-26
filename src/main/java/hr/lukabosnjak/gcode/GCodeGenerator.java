package hr.lukabosnjak.gcode;

import hr.lukabosnjak.domain.entities.MachiningParameters;
import hr.lukabosnjak.geometry.ToolPath;

public interface GCodeGenerator {
    GCodeProgram generate(ToolPath toolPath, MachiningParameters parameters);
}
