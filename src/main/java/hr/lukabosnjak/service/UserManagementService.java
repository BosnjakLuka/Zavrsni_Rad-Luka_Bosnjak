package hr.lukabosnjak.service;

import hr.lukabosnjak.domain.entities.Role;
import hr.lukabosnjak.domain.entities.User;
import hr.lukabosnjak.persistence.repository.RoleRepository;
import hr.lukabosnjak.persistence.repository.UserRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public final class UserManagementService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SessionContext sessionContext;
    private final AuthorizationService authorizationService;

    public UserManagementService(UserRepository userRepository, RoleRepository roleRepository,
                                 SessionContext sessionContext, AuthorizationService authorizationService) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.roleRepository = Objects.requireNonNull(roleRepository);
        this.sessionContext = Objects.requireNonNull(sessionContext);
        this.authorizationService = Objects.requireNonNull(authorizationService);
    }

    public List<User> loadUsers() {
        authorizationService.require(AuthorizationService.Permission.MANAGE_USERS);
        try {
            return userRepository.findAll();
        } catch (SQLException exception) {
            throw new UserManagementException("Dohvat korisnika nije uspio.", exception);
        }
    }

    public List<Role> loadRoles() {
        authorizationService.require(AuthorizationService.Permission.MANAGE_USERS);
        try {
            return roleRepository.findAll();
        } catch (SQLException exception) {
            throw new UserManagementException("Dohvat rola nije uspio.", exception);
        }
    }

    public User setActive(User target, boolean active) {
        authorizationService.require(AuthorizationService.Permission.MANAGE_USERS);
        User current = sessionContext.currentUser().orElseThrow(
                () -> new AuthorizationException("Za ovu akciju potrebna je prijava."));
        if (!active && Objects.equals(current.getUserId(), target.getUserId())) {
            throw new AuthorizationException("Ne možete deaktivirati vlastiti aktivni račun.");
        }
        try {
            if (!active && target.isActive() && "ADMIN".equals(target.getRole().getName())
                    && countActiveAdmins() <= 1) {
                throw new AuthorizationException("Posljednji aktivni administrator ne može biti deaktiviran.");
            }
            return userRepository.updateActive(target.getUserId(), active);
        } catch (SQLException exception) {
            throw new UserManagementException("Promjena statusa korisnika nije uspjela.", exception);
        }
    }

    public User changeRole(User target, Role role) {
        authorizationService.require(AuthorizationService.Permission.MANAGE_USERS);
        Objects.requireNonNull(role, "role");
        try {
            if (target.isActive() && "ADMIN".equals(target.getRole().getName())
                    && !"ADMIN".equals(role.getName()) && countActiveAdmins() <= 1) {
                throw new AuthorizationException("Posljednjem aktivnom administratoru ne može se promijeniti rola.");
            }
            return userRepository.updateRole(target.getUserId(), role);
        } catch (SQLException exception) {
            throw new UserManagementException("Promjena role korisnika nije uspjela.", exception);
        }
    }

    private long countActiveAdmins() throws SQLException {
        return userRepository.findAll().stream()
                .filter(User::isActive)
                .filter(user -> "ADMIN".equals(user.getRole().getName()))
                .count();
    }
}
