package hr.lukabosnjak.service;

import hr.lukabosnjak.config.DatabaseInitializer;
import hr.lukabosnjak.domain.entities.Role;
import hr.lukabosnjak.domain.entities.User;
import hr.lukabosnjak.persistence.jdbc.ConnectionProvider;
import hr.lukabosnjak.persistence.jdbc.JdbcRoleRepository;
import hr.lukabosnjak.persistence.jdbc.JdbcUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceIntegrationTest {
    private JdbcRoleRepository roles;
    private JdbcUserRepository users;
    private PasswordHasher hasher;
    private SessionContext session;
    private AuthService authService;

    @BeforeEach
    void initializeDatabase() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:auth-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        ConnectionProvider connectionProvider = () -> DriverManager.getConnection(jdbcUrl, "sa", "");
        try (Connection connection = connectionProvider.getConnection()) {
            DatabaseInitializer.initialize(connection);
        }
        roles = new JdbcRoleRepository(connectionProvider);
        users = new JdbcUserRepository(connectionProvider);
        roles.save(new Role(null, "OPERATOR", null));
        hasher = new PasswordHasher();
        session = new SessionContext();
        authService = new AuthService(users, roles, hasher, session);
    }

    @Test
    void registersNormalizedActiveOperatorAndLogsInAndOut() {
        char[] password = "SoftwareTest1".toCharArray();

        User registered = authService.register("  NewOperator  ", password, "  Test ", " User  ");
        User loggedIn = authService.login("NEWOPERATOR", password);

        assertEquals("newoperator", registered.getUsername());
        assertEquals("Test", registered.getFirstName());
        assertEquals("User", registered.getLastName());
        assertEquals("OPERATOR", registered.getRole().getName());
        assertTrue(registered.isActive());
        assertTrue(hasher.verify(password, registered.getPasswordHash()));
        assertEquals(registered.getUserId(), loggedIn.getUserId());
        assertTrue(session.isAuthenticated());
        authService.logout();
        assertFalse(session.isAuthenticated());
    }

    @Test
    void rejectsDuplicateUsernameAndInvalidRegistrationData() {
        authService.register("duplicate", "SoftwareTest1".toCharArray(), "Test", "User");

        assertThrows(RegistrationException.class, () -> authService.register(
                " DUPLICATE ", "SoftwareTest2".toCharArray(), "Test", "User"));
        assertThrows(RegistrationException.class, () -> authService.register(
                "short-password", "short".toCharArray(), "Test", "User"));
        assertThrows(RegistrationException.class, () -> authService.register(
                "missing-name", "SoftwareTest1".toCharArray(), " ", "User"));
    }

    @Test
    void rejectsMissingWrongPasswordAndInactiveUsersWithoutStartingSession() throws Exception {
        Role operator = roles.findByName("OPERATOR").orElseThrow();
        users.save(new User(
                null, operator, "inactive", hasher.hash("SoftwareTest1".toCharArray()),
                "Inactive", "User", false, null, null));
        authService.register("active", "SoftwareTest1".toCharArray(), "Active", "User");

        assertThrows(AuthenticationException.class,
                () -> authService.login("missing", "SoftwareTest1".toCharArray()));
        assertThrows(AuthenticationException.class,
                () -> authService.login("active", "WrongPassword1".toCharArray()));
        assertThrows(AuthenticationException.class,
                () -> authService.login("inactive", "SoftwareTest1".toCharArray()));
        assertFalse(session.isAuthenticated());
    }
}
