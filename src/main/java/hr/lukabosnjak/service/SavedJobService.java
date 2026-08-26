package hr.lukabosnjak.service;

import hr.lukabosnjak.domain.entities.MachiningJob;
import hr.lukabosnjak.gcode.GCodeProgram;
import hr.lukabosnjak.persistence.repository.MachiningJobRepository;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Provides read-only access to persisted jobs for the JavaFX quick-access workflow. */
public final class SavedJobService {
    private final MachiningJobRepository machiningJobRepository;

    public SavedJobService(MachiningJobRepository machiningJobRepository) {
        this.machiningJobRepository = Objects.requireNonNull(machiningJobRepository);
    }

    public List<MachiningJob> loadAll() {
        try {
            return machiningJobRepository.findAll();
        } catch (SQLException exception) {
            throw new SavedJobAccessException("Učitavanje spremljenih programa nije uspjelo.", exception);
        }
    }

    public MachiningJob loadById(long jobId) {
        try {
            return machiningJobRepository.findById(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Spremljeni program više ne postoji."));
        } catch (SQLException exception) {
            throw new SavedJobAccessException("Otvaranje spremljenog programa nije uspjelo.", exception);
        }
    }

    public GCodeProgram gCodeProgramOf(MachiningJob job) {
        Objects.requireNonNull(job, "job");
        String gCode = job.getGCode();
        if (gCode == null || gCode.isBlank()) {
            throw new IllegalArgumentException("Spremljeni program nema G-code sadržaj.");
        }
        return new GCodeProgram(Arrays.stream(gCode.split("\\R"))
                .filter(line -> !line.isEmpty())
                .toList());
    }
}
