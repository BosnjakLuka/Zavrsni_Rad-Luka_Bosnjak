package hr.lukabosnjak.persistence.repository;

import hr.lukabosnjak.domain.entities.User;
import hr.lukabosnjak.domain.entities.Role;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user) throws SQLException;

    Optional<User> findByUsername(String username) throws SQLException;

    List<User> findAll() throws SQLException;

    User updateActive(Long userId, boolean active) throws SQLException;

    User updateRole(Long userId, Role role) throws SQLException;
}
