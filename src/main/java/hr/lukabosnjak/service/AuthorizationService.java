package hr.lukabosnjak.service;

import hr.lukabosnjak.domain.entities.User;

import java.util.Objects;

public final class AuthorizationService {
    public enum Permission {
        MANAGE_USERS,
        MANAGE_REFERENCE_DATA,
        GENERATE_PROGRAM
    }

    private final SessionContext sessionContext;

    public AuthorizationService(SessionContext sessionContext) {
        this.sessionContext = Objects.requireNonNull(sessionContext);
    }

    public void require(Permission permission) {
        User user = sessionContext.currentUser()
                .orElseThrow(() -> new AuthorizationException("Za ovu akciju potrebna je prijava."));
        String role = user.getRole() == null ? "" : user.getRole().getName();
        boolean allowed = switch (permission) {
            case MANAGE_USERS -> "ADMIN".equals(role);
            case MANAGE_REFERENCE_DATA -> "ADMIN".equals(role) || "ENGINEER".equals(role);
            case GENERATE_PROGRAM -> "ADMIN".equals(role) || "ENGINEER".equals(role) || "OPERATOR".equals(role);
        };
        if (!allowed) {
            throw new AuthorizationException("Nemate pravo za ovu akciju.");
        }
    }

    public boolean isAllowed(Permission permission) {
        try {
            require(permission);
            return true;
        } catch (AuthorizationException exception) {
            return false;
        }
    }
}
