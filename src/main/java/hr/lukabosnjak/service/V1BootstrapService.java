package hr.lukabosnjak.service;

import hr.lukabosnjak.domain.entities.CncMachine;
import hr.lukabosnjak.domain.entities.MaterialType;
import hr.lukabosnjak.domain.entities.Role;
import hr.lukabosnjak.persistence.repository.CncMachineRepository;
import hr.lukabosnjak.persistence.repository.MaterialTypeRepository;
import hr.lukabosnjak.persistence.repository.RoleRepository;

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

    private final RoleRepository roleRepository;
    private final MaterialTypeRepository materialTypeRepository;
    private final CncMachineRepository cncMachineRepository;

    public V1BootstrapService(
            RoleRepository roleRepository,
            MaterialTypeRepository materialTypeRepository,
            CncMachineRepository cncMachineRepository
    ) {
        this.roleRepository = Objects.requireNonNull(roleRepository);
        this.materialTypeRepository = Objects.requireNonNull(materialTypeRepository);
        this.cncMachineRepository = Objects.requireNonNull(cncMachineRepository);
    }

    public void initialize() throws SQLException {
        removeUnusedLegacyUserRole();
        ensureRoles();
        ensureMaterialTypes();
        ensureZk1325Machine();
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
}
