package hr.lukabosnjak.persistence.repository;

import hr.lukabosnjak.domain.entities.User;

import java.sql.SQLException;
import java.util.Optional;

public interface UserRepository {
    User save(User user) throws SQLException;

    Optional<User> findByUsername(String username) throws SQLException;
}
