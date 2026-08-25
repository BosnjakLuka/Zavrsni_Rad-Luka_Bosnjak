package hr.lukabosnjak.persistence.repository;

import hr.lukabosnjak.domain.entities.Role;

import java.sql.SQLException;
import java.util.Optional;

public interface RoleRepository {
    Optional<Role> findByName(String name) throws SQLException;
}
