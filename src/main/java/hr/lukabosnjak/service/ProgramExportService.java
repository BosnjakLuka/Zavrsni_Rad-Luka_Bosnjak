package hr.lukabosnjak.service;

import hr.lukabosnjak.gcode.GCodeProgram;
import hr.lukabosnjak.gcode.NcExportService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Service boundary for exporting an already generated program. */
public final class ProgramExportService {
    private final NcExportService ncExportService;

    public ProgramExportService(NcExportService ncExportService) {
        this.ncExportService = Objects.requireNonNull(ncExportService);
    }

    public void export(GCodeProgram program, Path destination) throws IOException {
        ncExportService.export(program, destination);
    }
}
