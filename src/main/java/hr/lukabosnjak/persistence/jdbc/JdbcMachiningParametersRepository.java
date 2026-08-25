package hr.lukabosnjak.persistence.jdbc;

import hr.lukabosnjak.domain.entities.MachiningParameters;
import hr.lukabosnjak.persistence.repository.MachiningParametersRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class JdbcMachiningParametersRepository extends JdbcRepositorySupport
        implements MachiningParametersRepository {
    public JdbcMachiningParametersRepository() {
    }

    public JdbcMachiningParametersRepository(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    @Override
    public MachiningParameters save(MachiningParameters parameters) throws SQLException {
        requireNew(parameters.getMachiningParametersId(), "Machining parameters");
        long id;
        try (Connection connection = connectionProvider.getConnection()) {
            id = insertOnOpenConnection(connection, parameters);
        }
        return findById(id).orElseThrow(() -> new SQLException("Saved machining parameters were not found"));
    }

    long insertOnOpenConnection(Connection connection, MachiningParameters parameters) throws SQLException {
        requireNew(parameters.getMachiningParametersId(), "Machining parameters");
        return executeInsert(connection, """
                INSERT INTO MACHINING_PARAMETERS
                    (spindle_speed, feed_rate, plunge_rate, cut_depth, step_down, safe_z)
                VALUES (?, ?, ?, ?, ?, ?)
                """, statement -> {
            statement.setDouble(1, parameters.getSpindleSpeed());
            statement.setDouble(2, parameters.getFeedRate());
            statement.setDouble(3, parameters.getPlungeRate());
            statement.setDouble(4, parameters.getCutDepth());
            statement.setDouble(5, parameters.getStepDown());
            statement.setDouble(6, parameters.getSafeZ());
        });
    }

    @Override
    public Optional<MachiningParameters> findById(long machiningParametersId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM MACHINING_PARAMETERS WHERE machining_parameters_id = ?")) {
            statement.setLong(1, machiningParametersId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    private static MachiningParameters map(ResultSet resultSet) throws SQLException {
        return new MachiningParameters(
                resultSet.getLong("machining_parameters_id"), resultSet.getDouble("spindle_speed"),
                resultSet.getDouble("feed_rate"), resultSet.getDouble("plunge_rate"),
                resultSet.getDouble("cut_depth"), resultSet.getDouble("step_down"),
                resultSet.getDouble("safe_z")
        );
    }
}
