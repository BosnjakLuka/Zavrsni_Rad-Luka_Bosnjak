package hr.lukabosnjak.persistence.repository;

import hr.lukabosnjak.domain.entities.Role;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface RoleRepository {
    Role save(Role role) throws SQLException;

    Optional<Role> findByName(String name) throws SQLException;

    List<Role> findAll() throws SQLException;

    boolean deleteIfUnused(String name) throws SQLException;
}
