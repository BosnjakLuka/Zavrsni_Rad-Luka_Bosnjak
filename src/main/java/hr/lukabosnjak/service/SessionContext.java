package hr.lukabosnjak.service;

import hr.lukabosnjak.domain.entities.User;

import java.util.Optional;

/** In-memory session for the currently running desktop application process. */
public final class SessionContext {
    private User currentUser;

    public Optional<User> currentUser() {
        return Optional.ofNullable(currentUser);
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }

    void login(User user) {
        currentUser = user;
    }

    public void logout() {
        currentUser = null;
    }
}
