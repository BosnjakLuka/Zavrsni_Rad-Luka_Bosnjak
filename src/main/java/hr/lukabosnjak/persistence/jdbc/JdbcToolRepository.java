package hr.lukabosnjak.persistence.jdbc;

import hr.lukabosnjak.domain.entities.CncMachine;
import hr.lukabosnjak.domain.entities.Tool;
import hr.lukabosnjak.persistence.repository.ToolRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcToolRepository extends JdbcRepositorySupport implements ToolRepository {
    private static final String SELECT_WITH_MACHINE = """
            SELECT t.tool_id, t.tool_number, t.name AS tool_name, t.type, t.diameter,
                   t.cutting_length, t.flute_count, t.active,
                   t.created_at AS tool_created_at, t.updated_at AS tool_updated_at,
                   m.cnc_machine_id, m.name AS machine_name, m.manufacturer, m.model, m.controller,
                   m.work_area_x, m.work_area_y, m.work_area_z, m.max_feed_rate,
                   m.min_spindle_speed, m.max_spindle_speed,
                   m.created_at AS machine_created_at, m.updated_at AS machine_updated_at
            FROM TOOL t
            JOIN CNC_MACHINE m ON m.cnc_machine_id = t.cnc_machine_id
            """;

    public JdbcToolRepository() {
    }

    public JdbcToolRepository(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    @Override
    public Tool save(Tool tool) throws SQLException {
        requireNew(tool.getToolId(), "Tool");
        if (tool.getCncMachine() == null || tool.getCncMachine().getCncMachineId() == null) {
            throw new IllegalArgumentException("Tool requires a persisted CNC machine");
        }

        long id = executeInsert("""
                INSERT INTO TOOL (cnc_machine_id, tool_number, name, type, diameter, cutting_length,
                    flute_count, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, statement -> {
            statement.setLong(1, tool.getCncMachine().getCncMachineId());
            statement.setInt(2, tool.getToolNumber());
            statement.setString(3, tool.getName());
            statement.setString(4, tool.getType());
            statement.setDouble(5, tool.getDiameter());
            statement.setDouble(6, tool.getCuttingLength());
            statement.setInt(7, tool.getFluteCount());
            statement.setBoolean(8, tool.isActive());
        });
        return findById(id).orElseThrow(() -> new SQLException("Saved tool was not found"));
    }

    @Override
    public Tool update(Tool tool) throws SQLException {
        if (tool.getToolId() == null || tool.getCncMachine() == null
                || tool.getCncMachine().getCncMachineId() == null) {
            throw new IllegalArgumentException("Tool requires persisted identifiers");
        }
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE TOOL SET cnc_machine_id = ?, tool_number = ?, name = ?, type = ?,
                       diameter = ?, cutting_length = ?, flute_count = ?, active = ?,
                       updated_at = CURRENT_TIMESTAMP
                     WHERE tool_id = ?
                     """)) {
            statement.setLong(1, tool.getCncMachine().getCncMachineId());
            statement.setInt(2, tool.getToolNumber());
            statement.setString(3, tool.getName());
            statement.setString(4, tool.getType());
            statement.setDouble(5, tool.getDiameter());
            statement.setDouble(6, tool.getCuttingLength());
            statement.setInt(7, tool.getFluteCount());
            statement.setBoolean(8, tool.isActive());
            statement.setLong(9, tool.getToolId());
            if (statement.executeUpdate() != 1) throw new SQLException("Tool was not found");
        }
        return findById(tool.getToolId()).orElseThrow(() -> new SQLException("Updated tool was not found"));
    }

    @Override
    public Optional<Tool> findById(long toolId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_WITH_MACHINE + " WHERE t.tool_id = ?")) {
            statement.setLong(1, toolId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Tool> findAllByMachineId(long cncMachineId) throws SQLException {
        List<Tool> tools = new ArrayList<>();
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     SELECT_WITH_MACHINE + " WHERE t.cnc_machine_id = ? ORDER BY t.tool_number")) {
            statement.setLong(1, cncMachineId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    tools.add(map(resultSet));
                }
            }
        }
        return tools;
    }

    @Override
    public Optional<Tool> findByMachineIdAndToolNumber(long cncMachineId, int toolNumber) throws SQLException {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     SELECT_WITH_MACHINE + " WHERE t.cnc_machine_id = ? AND t.tool_number = ?")) {
            statement.setLong(1, cncMachineId);
            statement.setInt(2, toolNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    private static Tool map(ResultSet resultSet) throws SQLException {
        CncMachine machine = new CncMachine(
                resultSet.getLong("cnc_machine_id"), resultSet.getString("machine_name"),
                resultSet.getString("manufacturer"), resultSet.getString("model"),
                resultSet.getString("controller"), resultSet.getDouble("work_area_x"),
                resultSet.getDouble("work_area_y"), resultSet.getObject("work_area_z", Double.class),
                resultSet.getObject("max_feed_rate", Double.class),
                resultSet.getObject("min_spindle_speed", Double.class),
                resultSet.getObject("max_spindle_speed", Double.class),
                resultSet.getTimestamp("machine_created_at").toLocalDateTime(),
                resultSet.getTimestamp("machine_updated_at").toLocalDateTime()
        );
        return new Tool(
                resultSet.getLong("tool_id"), machine, resultSet.getInt("tool_number"),
                resultSet.getString("tool_name"), resultSet.getString("type"),
                resultSet.getDouble("diameter"), resultSet.getDouble("cutting_length"),
                resultSet.getInt("flute_count"), resultSet.getBoolean("active"),
                resultSet.getTimestamp("tool_created_at").toLocalDateTime(),
                resultSet.getTimestamp("tool_updated_at").toLocalDateTime()
        );
    }
}
