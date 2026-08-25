package hr.lukabosnjak.persistence.jdbc;

import hr.lukabosnjak.domain.entities.CncMachine;
import hr.lukabosnjak.persistence.repository.CncMachineRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcCncMachineRepository extends JdbcRepositorySupport implements CncMachineRepository {
    public JdbcCncMachineRepository() {
    }

    public JdbcCncMachineRepository(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    @Override
    public CncMachine save(CncMachine machine) throws SQLException {
        requireNew(machine.getCncMachineId(), "CNC machine");
        long id = executeInsert("""
                INSERT INTO CNC_MACHINE (name, manufacturer, model, controller, work_area_x, work_area_y,
                    work_area_z, max_feed_rate, min_spindle_speed, max_spindle_speed, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, statement -> {
            statement.setString(1, machine.getName());
            statement.setString(2, machine.getManufacturer());
            statement.setString(3, machine.getModel());
            statement.setString(4, machine.getController());
            statement.setDouble(5, machine.getWorkAreaX());
            statement.setDouble(6, machine.getWorkAreaY());
            statement.setDouble(7, machine.getWorkAreaZ());
            statement.setDouble(8, machine.getMaxFeedRate());
            statement.setDouble(9, machine.getMinSpindleSpeed());
            statement.setDouble(10, machine.getMaxSpindleSpeed());
        });
        return findById(id).orElseThrow(() -> new SQLException("Saved CNC machine was not found"));
    }

    @Override
    public Optional<CncMachine> findById(long cncMachineId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM CNC_MACHINE WHERE cnc_machine_id = ?")) {
            statement.setLong(1, cncMachineId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    @Override
    public List<CncMachine> findAll() throws SQLException {
        List<CncMachine> machines = new ArrayList<>();
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM CNC_MACHINE ORDER BY cnc_machine_id");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                machines.add(map(resultSet));
            }
        }
        return machines;
    }

    static CncMachine map(ResultSet resultSet) throws SQLException {
        return new CncMachine(
                resultSet.getLong("cnc_machine_id"), resultSet.getString("name"),
                resultSet.getString("manufacturer"), resultSet.getString("model"),
                resultSet.getString("controller"), resultSet.getDouble("work_area_x"),
                resultSet.getDouble("work_area_y"), resultSet.getDouble("work_area_z"),
                resultSet.getDouble("max_feed_rate"), resultSet.getDouble("min_spindle_speed"),
                resultSet.getDouble("max_spindle_speed"),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                resultSet.getTimestamp("updated_at").toLocalDateTime()
        );
    }
}
