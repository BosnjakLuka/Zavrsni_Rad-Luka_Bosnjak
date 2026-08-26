package hr.lukabosnjak.service;

import hr.lukabosnjak.domain.entities.Role;
import hr.lukabosnjak.domain.entities.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorizationServiceTest {
    @Test
    void onlyAdminMayManageUsers() {
        SessionContext session = new SessionContext();
        AuthorizationService authorization = new AuthorizationService(session);

        session.login(user("OPERATOR"));
        assertFalse(authorization.isAllowed(AuthorizationService.Permission.MANAGE_USERS));
        assertThrows(AuthorizationException.class,
                () -> authorization.require(AuthorizationService.Permission.MANAGE_USERS));

        session.logout();
        session.login(user("ADMIN"));
        assertDoesNotThrow(() -> authorization.require(AuthorizationService.Permission.MANAGE_USERS));
        assertTrue(authorization.isAllowed(AuthorizationService.Permission.MANAGE_USERS));
    }

    @Test
    void operatorMayGenerateButNotManageReferenceData() {
        SessionContext session = new SessionContext();
        session.login(user("OPERATOR"));
        AuthorizationService authorization = new AuthorizationService(session);

        assertDoesNotThrow(() -> authorization.require(AuthorizationService.Permission.GENERATE_PROGRAM));
        assertThrows(AuthorizationException.class,
                () -> authorization.require(AuthorizationService.Permission.MANAGE_REFERENCE_DATA));
    }

    private static User user(String roleName) {
        return new User(1L, new Role(1L, roleName, null), "test", "hash",
                "Test", "User", true, null, null);
    }
}
