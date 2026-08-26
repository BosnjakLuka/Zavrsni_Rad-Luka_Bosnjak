package hr.lukabosnjak.service;

import hr.lukabosnjak.domain.entities.CncMachine;
import hr.lukabosnjak.domain.entities.Tool;
import hr.lukabosnjak.persistence.repository.CncMachineRepository;
import hr.lukabosnjak.persistence.repository.ToolRepository;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReferenceDataServiceTest {

    @Test
    void returnsMachinesAndToolsForSelectedMachine() {
        CncMachine machine = machine(4L);
        Tool tool = new Tool(7L, machine, 3, "Alat", "", 3, 10, 2, true, null, null);
        ReferenceDataService service = new ReferenceDataService(new MachineRepositoryStub(List.of(machine)), new ToolRepositoryStub(List.of(tool)));

        assertEquals(List.of(machine), service.loadMachines());
        assertEquals(List.of(tool), service.loadTools(machine));
        assertEquals(List.of(), service.loadTools(null));
    }

    @Test
    void wrapsSqlAccessError() {
        ReferenceDataService service = new ReferenceDataService(new MachineRepositoryStub(new SQLException("database unavailable")), new ToolRepositoryStub(List.of()));

        assertThrows(ReferenceDataAccessException.class, service::loadMachines);
    }

    private CncMachine machine(Long id) {
        return new CncMachine(id, "Stroj", null, null, "", 100, 100, 50, 1000, 1000, 24000, null, null);
    }

    private static final class MachineRepositoryStub implements CncMachineRepository {
        private final List<CncMachine> machines;
        private final SQLException exception;

        private MachineRepositoryStub(List<CncMachine> machines) { this.machines = machines; this.exception = null; }
        private MachineRepositoryStub(SQLException exception) { this.machines = List.of(); this.exception = exception; }
        @Override public CncMachine save(CncMachine machine) { throw new UnsupportedOperationException(); }
        @Override public Optional<CncMachine> findById(long id) { throw new UnsupportedOperationException(); }
        @Override public List<CncMachine> findAll() throws SQLException { if (exception != null) throw exception; return machines; }
    }

    private static final class ToolRepositoryStub implements ToolRepository {
        private final List<Tool> tools;
        private ToolRepositoryStub(List<Tool> tools) { this.tools = tools; }
        @Override public Tool save(Tool tool) { throw new UnsupportedOperationException(); }
        @Override public Optional<Tool> findById(long id) { throw new UnsupportedOperationException(); }
        @Override public List<Tool> findAllByMachineId(long id) { return tools; }
        @Override public Optional<Tool> findByMachineIdAndToolNumber(long machineId, int toolNumber) { throw new UnsupportedOperationException(); }
    }
}
