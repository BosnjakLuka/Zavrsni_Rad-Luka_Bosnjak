package hr.lukabosnjak.persistence.jdbc;

import hr.lukabosnjak.domain.entities.CncMachine;
import hr.lukabosnjak.domain.entities.MachiningJob;
import hr.lukabosnjak.domain.entities.MachiningParameters;
import hr.lukabosnjak.domain.entities.MaterialSheet;
import hr.lukabosnjak.domain.entities.MaterialType;
import hr.lukabosnjak.domain.entities.Role;
import hr.lukabosnjak.domain.entities.Shape;
import hr.lukabosnjak.domain.entities.Tool;
import hr.lukabosnjak.domain.entities.User;
import hr.lukabosnjak.domain.enums.ShapeSubtype;
import hr.lukabosnjak.domain.enums.ShapeType;
import hr.lukabosnjak.persistence.repository.MachiningJobRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcMachiningJobRepository extends JdbcRepositorySupport implements MachiningJobRepository {
    private static final String SELECT_COMPLETE_JOB = """
            SELECT
                j.machining_job_id, j.name AS job_name, j.quantity, j.g_code,
                j.created_at AS job_created_at, j.updated_at AS job_updated_at,
                u.user_id, u.username, u.password_hash, u.first_name, u.last_name, u.active,
                u.created_at AS user_created_at, u.updated_at AS user_updated_at,
                r.role_id, r.name AS role_name, r.description AS role_description,
                jm.cnc_machine_id AS job_machine_id, jm.name AS job_machine_name,
                jm.manufacturer AS job_machine_manufacturer, jm.model AS job_machine_model,
                jm.controller AS job_machine_controller, jm.work_area_x AS job_machine_work_area_x,
                jm.work_area_y AS job_machine_work_area_y, jm.work_area_z AS job_machine_work_area_z,
                jm.max_feed_rate AS job_machine_max_feed_rate,
                jm.min_spindle_speed AS job_machine_min_spindle_speed,
                jm.max_spindle_speed AS job_machine_max_spindle_speed,
                jm.created_at AS job_machine_created_at, jm.updated_at AS job_machine_updated_at,
                t.tool_id, t.tool_number, t.name AS tool_name, t.type AS tool_type,
                t.diameter AS tool_diameter, t.cutting_length, t.flute_count, t.active AS tool_active,
                t.created_at AS tool_created_at, t.updated_at AS tool_updated_at,
                tm.cnc_machine_id AS tool_machine_id, tm.name AS tool_machine_name,
                tm.manufacturer AS tool_machine_manufacturer, tm.model AS tool_machine_model,
                tm.controller AS tool_machine_controller, tm.work_area_x AS tool_machine_work_area_x,
                tm.work_area_y AS tool_machine_work_area_y, tm.work_area_z AS tool_machine_work_area_z,
                tm.max_feed_rate AS tool_machine_max_feed_rate,
                tm.min_spindle_speed AS tool_machine_min_spindle_speed,
                tm.max_spindle_speed AS tool_machine_max_spindle_speed,
                tm.created_at AS tool_machine_created_at, tm.updated_at AS tool_machine_updated_at,
                ms.material_sheet_id, ms.width AS sheet_width, ms.height AS sheet_height,
                ms.thickness AS sheet_thickness, ms.created_at AS sheet_created_at,
                ms.updated_at AS sheet_updated_at,
                mt.material_type_id, mt.name AS material_name, mt.description AS material_description,
                mt.created_at AS material_created_at, mt.updated_at AS material_updated_at,
                mt.deleted_at AS material_deleted_at,
                mp.machining_parameters_id, mp.spindle_speed, mp.feed_rate, mp.plunge_rate,
                mp.cut_depth, mp.step_down, mp.safe_z,
                s.shape_id, s.shape_type, s.shape_subtype, s.dimension_a, s.dimension_b, s.dimension_c,
                s.created_at AS shape_created_at, s.updated_at AS shape_updated_at,
                s.deleted_at AS shape_deleted_at
            FROM MACHINING_JOB j
            JOIN APP_USER u ON u.user_id = j.created_by_user_id
            JOIN "ROLE" r ON r.role_id = u.role_id
            JOIN CNC_MACHINE jm ON jm.cnc_machine_id = j.cnc_machine_id
            JOIN TOOL t ON t.tool_id = j.tool_id
            JOIN CNC_MACHINE tm ON tm.cnc_machine_id = t.cnc_machine_id
            JOIN MATERIAL_SHEET ms ON ms.material_sheet_id = j.material_sheet_id
            JOIN MATERIAL_TYPE mt ON mt.material_type_id = ms.material_type_id
            JOIN MACHINING_PARAMETERS mp
                ON mp.machining_parameters_id = j.machining_parameters_id
            JOIN SHAPE s ON s.shape_id = j.shape_id
            """;

    private final JdbcMaterialSheetRepository materialSheetRepository;
    private final JdbcMachiningParametersRepository machiningParametersRepository;
    private final JdbcShapeRepository shapeRepository;

    public JdbcMachiningJobRepository() {
        this(hr.lukabosnjak.config.DatabaseConfig::getConnection);
    }

    public JdbcMachiningJobRepository(ConnectionProvider connectionProvider) {
        super(connectionProvider);
        materialSheetRepository = new JdbcMaterialSheetRepository(connectionProvider);
        machiningParametersRepository = new JdbcMachiningParametersRepository(connectionProvider);
        shapeRepository = new JdbcShapeRepository(connectionProvider);
    }

    @Override
    public MachiningJob save(MachiningJob job) throws SQLException {
        validateNewJob(job);

        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long materialSheetId = materialSheetRepository.insertOnOpenConnection(
                        connection, job.getMaterialSheet());
                long machiningParametersId = machiningParametersRepository.insertOnOpenConnection(
                        connection, job.getMachiningParameters());
                long shapeId = shapeRepository.insertOnOpenConnection(connection, job.getShape());
                long jobId = insertJob(connection, job, materialSheetId, machiningParametersId, shapeId);
                MachiningJob savedJob = findById(connection, jobId)
                        .orElseThrow(() -> new SQLException("Saved machining job was not found"));
                connection.commit();
                return savedJob;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    @Override
    public Optional<MachiningJob> findById(long machiningJobId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findById(connection, machiningJobId);
        }
    }

    @Override
    public List<MachiningJob> findAll() throws SQLException {
        List<MachiningJob> jobs = new ArrayList<>();
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     SELECT_COMPLETE_JOB + " ORDER BY j.machining_job_id");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                jobs.add(map(resultSet));
            }
        }
        return jobs;
    }

    private Optional<MachiningJob> findById(Connection connection, long machiningJobId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                SELECT_COMPLETE_JOB + " WHERE j.machining_job_id = ?")) {
            statement.setLong(1, machiningJobId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    private long insertJob(
            Connection connection,
            MachiningJob job,
            long materialSheetId,
            long machiningParametersId,
            long shapeId
    ) throws SQLException {
        return executeInsert(connection, """
                INSERT INTO MACHINING_JOB
                    (created_by_user_id, cnc_machine_id, tool_id, material_sheet_id,
                     machining_parameters_id, shape_id, name, quantity, g_code, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, statement -> {
            statement.setLong(1, job.getCreatedBy().getUserId());
            statement.setLong(2, job.getCncMachine().getCncMachineId());
            statement.setLong(3, job.getTool().getToolId());
            statement.setLong(4, materialSheetId);
            statement.setLong(5, machiningParametersId);
            statement.setLong(6, shapeId);
            statement.setString(7, job.getName());
            statement.setInt(8, job.getQuantity());
            statement.setString(9, job.getGCode());
        });
    }

    private MachiningJob map(ResultSet resultSet) throws SQLException {
        Role role = new Role(resultSet.getLong("role_id"), resultSet.getString("role_name"),
                resultSet.getString("role_description"));
        User user = new User(
                resultSet.getLong("user_id"), role, resultSet.getString("username"),
                resultSet.getString("password_hash"), resultSet.getString("first_name"),
                resultSet.getString("last_name"), resultSet.getBoolean("active"),
                resultSet.getTimestamp("user_created_at").toLocalDateTime(),
                resultSet.getTimestamp("user_updated_at").toLocalDateTime());
        CncMachine jobMachine = mapMachine(resultSet, "job_machine_");
        CncMachine toolMachine = mapMachine(resultSet, "tool_machine_");
        Tool tool = new Tool(
                resultSet.getLong("tool_id"), toolMachine, resultSet.getInt("tool_number"),
                resultSet.getString("tool_name"), resultSet.getString("tool_type"),
                resultSet.getDouble("tool_diameter"), resultSet.getDouble("cutting_length"),
                resultSet.getInt("flute_count"), resultSet.getBoolean("tool_active"),
                resultSet.getTimestamp("tool_created_at").toLocalDateTime(),
                resultSet.getTimestamp("tool_updated_at").toLocalDateTime());
        Timestamp materialDeletedAt = resultSet.getTimestamp("material_deleted_at");
        MaterialType materialType = new MaterialType(
                resultSet.getLong("material_type_id"), resultSet.getString("material_name"),
                resultSet.getString("material_description"),
                resultSet.getTimestamp("material_created_at").toLocalDateTime(),
                resultSet.getTimestamp("material_updated_at").toLocalDateTime(),
                materialDeletedAt == null ? null : materialDeletedAt.toLocalDateTime());
        MaterialSheet materialSheet = new MaterialSheet(
                resultSet.getLong("material_sheet_id"), materialType,
                resultSet.getDouble("sheet_width"), resultSet.getDouble("sheet_height"),
                resultSet.getDouble("sheet_thickness"),
                resultSet.getTimestamp("sheet_created_at").toLocalDateTime(),
                resultSet.getTimestamp("sheet_updated_at").toLocalDateTime());
        MachiningParameters parameters = new MachiningParameters(
                resultSet.getLong("machining_parameters_id"), resultSet.getDouble("spindle_speed"),
                resultSet.getDouble("feed_rate"), resultSet.getDouble("plunge_rate"),
                resultSet.getDouble("cut_depth"), resultSet.getDouble("step_down"),
                resultSet.getDouble("safe_z"));
        String subtype = resultSet.getString("shape_subtype");
        Timestamp shapeDeletedAt = resultSet.getTimestamp("shape_deleted_at");
        Shape shape = new Shape(
                resultSet.getLong("shape_id"), ShapeType.valueOf(resultSet.getString("shape_type")),
                subtype == null ? null : ShapeSubtype.valueOf(subtype),
                resultSet.getDouble("dimension_a"), nullableDouble(resultSet, "dimension_b"),
                nullableDouble(resultSet, "dimension_c"),
                resultSet.getTimestamp("shape_created_at").toLocalDateTime(),
                resultSet.getTimestamp("shape_updated_at").toLocalDateTime(),
                shapeDeletedAt == null ? null : shapeDeletedAt.toLocalDateTime());

        return new MachiningJob(
                resultSet.getLong("machining_job_id"), user, jobMachine, tool, materialSheet,
                parameters, shape, resultSet.getString("job_name"), resultSet.getInt("quantity"),
                resultSet.getString("g_code"), resultSet.getTimestamp("job_created_at").toLocalDateTime(),
                resultSet.getTimestamp("job_updated_at").toLocalDateTime());
    }

    private CncMachine mapMachine(ResultSet resultSet, String prefix) throws SQLException {
        return new CncMachine(
                resultSet.getLong(prefix + "id"), resultSet.getString(prefix + "name"),
                resultSet.getString(prefix + "manufacturer"), resultSet.getString(prefix + "model"),
                resultSet.getString(prefix + "controller"), resultSet.getDouble(prefix + "work_area_x"),
                resultSet.getDouble(prefix + "work_area_y"), resultSet.getDouble(prefix + "work_area_z"),
                resultSet.getDouble(prefix + "max_feed_rate"),
                resultSet.getDouble(prefix + "min_spindle_speed"),
                resultSet.getDouble(prefix + "max_spindle_speed"),
                resultSet.getTimestamp(prefix + "created_at").toLocalDateTime(),
                resultSet.getTimestamp(prefix + "updated_at").toLocalDateTime());
    }

    private Double nullableDouble(ResultSet resultSet, String column) throws SQLException {
        double value = resultSet.getDouble(column);
        return resultSet.wasNull() ? null : value;
    }

    private void validateNewJob(MachiningJob job) {
        if (job == null) {
            throw new IllegalArgumentException("Machining job is required");
        }
        requireNew(job.getMachiningJobId(), "Machining job");
        requirePersisted(job.getCreatedBy(), job.getCreatedBy() == null ? null : job.getCreatedBy().getUserId(), "User");
        requirePersisted(job.getCncMachine(),
                job.getCncMachine() == null ? null : job.getCncMachine().getCncMachineId(), "CNC machine");
        requirePersisted(job.getTool(), job.getTool() == null ? null : job.getTool().getToolId(), "Tool");
        if (job.getMaterialSheet() == null || job.getMachiningParameters() == null || job.getShape() == null) {
            throw new IllegalArgumentException("Machining job requires all snapshot data");
        }
        requireNew(job.getMaterialSheet().getMaterialSheetId(), "Material sheet");
        requireNew(job.getMachiningParameters().getMachiningParametersId(), "Machining parameters");
        requireNew(job.getShape().getShapeId(), "Shape");
        MaterialType materialType = job.getMaterialSheet().getMaterialType();
        requirePersisted(materialType,
                materialType == null ? null : materialType.getMaterialTypeId(), "Material type");
    }

    private void requirePersisted(Object reference, Long id, String name) {
        if (reference == null || id == null) {
            throw new IllegalArgumentException(name + " must already be persisted");
        }
    }

    private void rollback(Connection connection, Exception originalException) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            originalException.addSuppressed(rollbackException);
        }
    }
}
