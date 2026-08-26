package hr.lukabosnjak.service;

import hr.lukabosnjak.config.DatabaseInitializer;
import hr.lukabosnjak.domain.entities.CncMachine;
import hr.lukabosnjak.persistence.jdbc.ConnectionProvider;
import hr.lukabosnjak.persistence.jdbc.JdbcCncMachineRepository;
import hr.lukabosnjak.persistence.jdbc.JdbcMaterialTypeRepository;
import hr.lukabosnjak.persistence.jdbc.JdbcRoleRepository;
import hr.lukabosnjak.persistence.jdbc.JdbcToolRepository;
import hr.lukabosnjak.persistence.jdbc.JdbcUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class V1BootstrapServiceIntegrationTest {
    private ConnectionProvider connectionProvider;
    private JdbcRoleRepository roles;
    private JdbcMaterialTypeRepository materialTypes;
    private JdbcCncMachineRepository machines;
    private JdbcToolRepository tools;
    private JdbcUserRepository users;
    private V1BootstrapService bootstrapService;

    @BeforeEach
    void initializeDatabase() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:bootstrap-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        connectionProvider = () -> DriverManager.getConnection(jdbcUrl, "sa", "");
        try (Connection connection = connectionProvider.getConnection()) {
            DatabaseInitializer.initialize(connection);
        }
        roles = new JdbcRoleRepository(connectionProvider);
        materialTypes = new JdbcMaterialTypeRepository(connectionProvider);
        machines = new JdbcCncMachineRepository(connectionProvider);
        tools = new JdbcToolRepository(connectionProvider);
        users = new JdbcUserRepository(connectionProvider);
        bootstrapService = new V1BootstrapService(roles, materialTypes, machines, users);
    }

    @Test
    void initializesConfirmedReferenceDataIdempotently() throws Exception {
        bootstrapService.initialize();
        bootstrapService.initialize();

        assertEquals(V1BootstrapService.ROLE_NAMES,
                roles.findAll().stream().map(role -> role.getName()).sorted().toList());
        assertEquals(V1BootstrapService.MATERIAL_TYPE_NAMES,
                materialTypes.findAll().stream().map(material -> material.getName()).sorted().toList());
        assertEquals(1, machines.findAll().size());
        CncMachine machine = machines.findAll().getFirst();
        assertEquals("ZK-1325", machine.getName());
        assertEquals("ZK-1325", machine.getModel());
        assertEquals("RichAuto A11", machine.getController());
        assertEquals(1250.0, machine.getWorkAreaX());
        assertEquals(2500.0, machine.getWorkAreaY());
        assertNull(machine.getWorkAreaZ());
        assertNull(machine.getMaxFeedRate());
        assertNull(machine.getMinSpindleSpeed());
        assertNull(machine.getMaxSpindleSpeed());
        assertTrue(tools.findAllByMachineId(machine.getCncMachineId()).isEmpty());
        assertEquals(3, countRows("APP_USER"));
        assertEquals(0, countRows("MATERIAL_SHEET"));
        assertEquals(0, countRows("MACHINING_PARAMETERS"));
        assertEquals(0, countRows("SHAPE"));
        assertEquals(0, countRows("MACHINING_JOB"));
        PasswordHasher hasher = new PasswordHasher();
        assertEquals(List.of("admin", "engineer", "operator"),
                users.findAll().stream().map(user -> user.getUsername()).sorted().toList());
        assertTrue(users.findAll().stream().allMatch(user ->
                hasher.verify(user.getUsername().toCharArray(), user.getPasswordHash())));
    }

    @Test
    void removesOnlyUnusedLegacyUserRole() throws Exception {
        roles.save(new hr.lukabosnjak.domain.entities.Role(null, "USER", null));

        bootstrapService.initialize();

        assertFalse(roles.findByName("USER").isPresent());
    }

    @Test
    void smokeTestLoadsSeededRolesMaterialsAndMachineThroughRepositoriesAndServices() throws Exception {
        bootstrapService.initialize();

        assertEquals(V1BootstrapService.ROLE_NAMES,
                roles.findAll().stream().map(role -> role.getName()).sorted().toList());
        assertEquals(5, new MaterialReferenceDataService(materialTypes).loadMaterialTypes().size());
        List<CncMachine> loadedMachines = new ReferenceDataService(machines, tools).loadMachines();
        assertEquals(1, loadedMachines.size());
        assertEquals("ZK-1325", loadedMachines.getFirst().getName());
    }

    private int countRows(String tableName) throws Exception {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + tableName);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
