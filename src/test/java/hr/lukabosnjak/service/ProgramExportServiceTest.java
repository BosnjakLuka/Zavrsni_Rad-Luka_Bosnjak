package hr.lukabosnjak.service;

import hr.lukabosnjak.gcode.GCodeProgram;
import hr.lukabosnjak.gcode.NcExportService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgramExportServiceTest {

    @Test
    void delegatesNcExportToExistingService() throws Exception {
        Path directory = Files.createTempDirectory("cnc-export-service-test");
        Path destination = directory.resolve("preview.nc");
        ProgramExportService service = new ProgramExportService(new NcExportService());

        service.export(new GCodeProgram(List.of("G21", "M30")), destination);

        assertEquals("G21\nM30\n", Files.readString(destination));
    }
}
