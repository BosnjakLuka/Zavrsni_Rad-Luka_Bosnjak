package hr.lukabosnjak.service;

import hr.lukabosnjak.domain.entities.MachiningJob;
import hr.lukabosnjak.gcode.GCodeProgram;
import hr.lukabosnjak.persistence.repository.MachiningJobRepository;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SavedJobServiceTest {

    @Test
    void loadsJobAndConvertsStoredGCodeToProgram() {
        MachiningJob job = job(5L, "G21\nM30\n");
        SavedJobService service = new SavedJobService(new RepositoryStub(job));

        assertEquals(List.of(job), service.loadAll());
        assertEquals(job, service.loadById(5L));
        assertEquals("G21\nM30\n", service.gCodeProgramOf(job).text());
    }

    @Test
    void rejectsMissingOrEmptySavedProgram() {
        SavedJobService service = new SavedJobService(new RepositoryStub(null));

        assertThrows(IllegalArgumentException.class, () -> service.loadById(7L));
        assertThrows(IllegalArgumentException.class, () -> service.gCodeProgramOf(job(1L, " ")));
    }

    private MachiningJob job(Long id, String gCode) {
        return new MachiningJob(id, null, null, null, null, null, null, "Program", 1, gCode, null, null);
    }

    private static final class RepositoryStub implements MachiningJobRepository {
        private final MachiningJob job;
        private RepositoryStub(MachiningJob job) { this.job = job; }
        @Override public MachiningJob save(MachiningJob job) { throw new UnsupportedOperationException(); }
        @Override public Optional<MachiningJob> findById(long id) { return job == null ? Optional.empty() : Optional.of(job); }
        @Override public List<MachiningJob> findAll() throws SQLException { return job == null ? List.of() : List.of(job); }
    }
}
