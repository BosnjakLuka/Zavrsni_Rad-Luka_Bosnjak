package hr.lukabosnjak.gcode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public final class NcExportService {
    public void export(GCodeProgram program, Path destination) throws IOException {
        Objects.requireNonNull(program, "program");
        Objects.requireNonNull(destination, "destination");
        requireNcExtension(destination);

        byte[] content = encodeAscii(program.text());
        Files.write(
                destination,
                content,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
    }

    private void requireNcExtension(Path destination) {
        Path fileNamePath = destination.getFileName();
        if (fileNamePath == null) {
            throw new IllegalArgumentException("Destination must have an .nc file name");
        }

        String fileName = fileNamePath.toString();
        int extensionStart = fileName.length() - 3;
        if (extensionStart < 0 || !fileName.regionMatches(true, extensionStart, ".nc", 0, 3)) {
            throw new IllegalArgumentException("Destination file must use the .nc extension");
        }
    }

    private byte[] encodeAscii(String text) {
        try {
            ByteBuffer encoded = StandardCharsets.US_ASCII.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(text));
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            return bytes;
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("G-code program must contain only US-ASCII characters", exception);
        }
    }
}
