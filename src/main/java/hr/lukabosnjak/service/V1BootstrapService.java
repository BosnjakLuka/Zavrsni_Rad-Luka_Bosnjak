package hr.lukabosnjak.service;

import hr.lukabosnjak.domain.entities.CncMachine;
import hr.lukabosnjak.domain.entities.MaterialType;
import hr.lukabosnjak.domain.entities.Role;
import hr.lukabosnjak.domain.entities.User;
import hr.lukabosnjak.persistence.repository.CncMachineRepository;
import hr.lukabosnjak.persistence.repository.MaterialTypeRepository;
import hr.lukabosnjak.persistence.repository.RoleRepository;
import hr.lukabosnjak.persistence.repository.UserRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/** Creates only the confirmed, reusable V1 reference data after database initialization. */
public final class V1BootstrapService {
    public static final List<String> ROLE_NAMES = List.of("ADMIN", "ENGINEER", "OPERATOR");
    public static final List<String> MATERIAL_TYPE_NAMES = List.of(
            "TEST_MATERIAL_1", "TEST_MATERIAL_2", "TEST_MATERIAL_3",
            "TEST_MATERIAL_4", "TEST_MATERIAL_5");

    private static final String LEGACY_USER_ROLE = "USER";
    private static final String MACHINE_NAME = "ZK-1325";
    private static final String MACHINE_CONTROLLER = "RichAuto A11";
    private static final List<DevelopmentUser> DEVELOPMENT_USERS = List.of(
            new DevelopmentUser("admin", "ADMIN", "Test", "Admin",
                    "pbkdf2-sha256$600000$KeyAeHuu3ZHtHUVgK+5yfw==$9e7KZxggA470w2o4jKg24eHCsedWM+D2h3iqcWcSrmY="),
            new DevelopmentUser("engineer", "ENGINEER", "Test", "Engineer",
                    "pbkdf2-sha256$600000$8XU91cfeNWNUpuhf2w/84w==$gq0mlzlKYDhDZn4UweocD2xmR8NT4Nb7rxdidhgdOlM="),
            new DevelopmentUser("operator", "OPERATOR", "Test", "Operator",
                    "pbkdf2-sha256$600000$XXVCO7UAMn0mQ64AEh8Zjw==$dmv2aDPPkKg+7fhxrFb2FX+DOSly/1DzfMwT5TyqcnU="));

    private final RoleRepository roleRepository;
    private final MaterialTypeRepository materialTypeRepository;
    private final CncMachineRepository cncMachineRepository;
    private final UserRepository userRepository;

    public V1BootstrapService(
            RoleRepository roleRepository,
            MaterialTypeRepository materialTypeRepository,
            CncMachineRepository cncMachineRepository,
            UserRepository userRepository
    ) {
        this.roleRepository = Objects.requireNonNull(roleRepository);
        this.materialTypeRepository = Objects.requireNonNull(materialTypeRepository);
        this.cncMachineRepository = Objects.requireNonNull(cncMachineRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    public void initialize() throws SQLException {
        removeUnusedLegacyUserRole();
        ensureRoles();
        ensureMaterialTypes();
        ensureZk1325Machine();
        ensureDevelopmentUsers();
    }

    private void removeUnusedLegacyUserRole() throws SQLException {
        if (roleRepository.findByName(LEGACY_USER_ROLE).isPresent()
                && !roleRepository.deleteIfUnused(LEGACY_USER_ROLE)) {
            throw new SQLException("Legacy role USER is assigned to APP_USER records and cannot be removed safely.");
        }
    }

    private void ensureRoles() throws SQLException {
        for (String roleName : ROLE_NAMES) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(new Role(null, roleName, null));
            }
        }
    }

    private void ensureMaterialTypes() throws SQLException {
        List<String> existingNames = materialTypeRepository.findAll().stream()
                .map(MaterialType::getName)
                .toList();
        for (String materialName : MATERIAL_TYPE_NAMES) {
            if (!existingNames.contains(materialName)) {
                materialTypeRepository.save(new MaterialType(null, materialName, null, null, null, null));
            }
        }
    }

    private void ensureZk1325Machine() throws SQLException {
        boolean exists = cncMachineRepository.findAll().stream().anyMatch(machine ->
                MACHINE_NAME.equals(machine.getName())
                        && MACHINE_NAME.equals(machine.getModel())
                        && MACHINE_CONTROLLER.equals(machine.getController()));
        if (!exists) {
            cncMachineRepository.save(new CncMachine(
                    null, MACHINE_NAME, null, MACHINE_NAME, MACHINE_CONTROLLER,
                    1250.0, 2500.0, null, null, null, null, null, null));
        }
    }

    private void ensureDevelopmentUsers() throws SQLException {
        for (DevelopmentUser seed : DEVELOPMENT_USERS) {
            if (userRepository.findByUsername(seed.username()).isEmpty()) {
                Role role = roleRepository.findByName(seed.roleName())
                        .orElseThrow(() -> new SQLException("Bootstrap role is missing: " + seed.roleName()));
                userRepository.save(new User(
                        null, role, seed.username(), seed.passwordHash(),
                        seed.firstName(), seed.lastName(), true, null, null));
            }
        }
    }

    private record DevelopmentUser(
            String username,
            String roleName,
            String firstName,
            String lastName,
            String passwordHash
    ) {
    }
}
