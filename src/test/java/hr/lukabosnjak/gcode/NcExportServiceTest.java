package hr.lukabosnjak.gcode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NcExportServiceTest {
    private final NcExportService service = new NcExportService();

    @TempDir
    Path temporaryDirectory;

    @Test
    void writesAsciiProgramToExactDestinationAndReadsItBack() throws IOException {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("hr-HR"));
            GCodeFormatter formatter = new GCodeFormatter(3);
            GCodeProgram program = new GCodeProgram(List.of(
                    "G21",
                    "G01 X" + formatter.format(1.25) + " Y" + formatter.format(2.5),
                    "M30"));
            Path destination = temporaryDirectory.resolve("software-test.nc");

            service.export(program, destination);

            assertTrue(Files.isRegularFile(destination));
            assertEquals(program.text(), Files.readString(destination, StandardCharsets.US_ASCII));
            assertArrayEquals(program.text().getBytes(StandardCharsets.US_ASCII), Files.readAllBytes(destination));
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    void acceptsUppercaseNcExtension() throws IOException {
        Path destination = temporaryDirectory.resolve("software-test.NC");

        service.export(new GCodeProgram(List.of("M30")), destination);

        assertEquals("M30\n", Files.readString(destination, StandardCharsets.US_ASCII));
    }

    @Test
    void rejectsPathsWithoutNcExtensionBeforeWriting() {
        for (String fileName : List.of("program.txt", "program.nc.txt", "program")) {
            Path destination = temporaryDirectory.resolve(fileName);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.export(new GCodeProgram(List.of("M30")), destination));

            assertEquals("Destination file must use the .nc extension", exception.getMessage());
            assertFalse(Files.exists(destination));
        }
    }

    @Test
    void rejectsNonAsciiProgramBeforeCreatingFile() {
        Path destination = temporaryDirectory.resolve("non-ascii.nc");
        GCodeProgram program = new GCodeProgram(List.of("(č)"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.export(program, destination));

        assertEquals("G-code program must contain only US-ASCII characters", exception.getMessage());
        assertFalse(Files.exists(destination));
    }

    @Test
    void doesNotOverwriteExistingDestination() throws IOException {
        Path destination = temporaryDirectory.resolve("existing.nc");
        Files.writeString(destination, "ORIGINAL\n", StandardCharsets.US_ASCII);

        assertThrows(
                FileAlreadyExistsException.class,
                () -> service.export(new GCodeProgram(List.of("M30")), destination));

        assertEquals("ORIGINAL\n", Files.readString(destination, StandardCharsets.US_ASCII));
    }
}
