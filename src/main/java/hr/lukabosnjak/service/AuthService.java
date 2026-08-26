package hr.lukabosnjak.service;

import hr.lukabosnjak.domain.entities.Role;
import hr.lukabosnjak.domain.entities.User;
import hr.lukabosnjak.persistence.repository.RoleRepository;
import hr.lukabosnjak.persistence.repository.UserRepository;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;

public final class AuthService {
    private static final String DEFAULT_ROLE = "OPERATOR";
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 128;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordHasher passwordHasher;
    private final SessionContext sessionContext;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordHasher passwordHasher,
            SessionContext sessionContext
    ) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.roleRepository = Objects.requireNonNull(roleRepository);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.sessionContext = Objects.requireNonNull(sessionContext);
    }

    public User register(String username, char[] password, String firstName, String lastName) {
        String normalizedUsername = normalizeUsername(username);
        validateRegistration(normalizedUsername, password, firstName, lastName);
        try {
            if (userRepository.findByUsername(normalizedUsername).isPresent()) {
                throw new RegistrationException("Korisničko ime je već zauzeto.");
            }
            Role operatorRole = roleRepository.findByName(DEFAULT_ROLE)
                    .orElseThrow(() -> new RegistrationException("Zadana OPERATOR rola nije inicijalizirana."));
            User user = new User(
                    null, operatorRole, normalizedUsername, passwordHasher.hash(password),
                    firstName.trim(), lastName.trim(), true, null, null);
            return userRepository.save(user);
        } catch (RegistrationException exception) {
            throw exception;
        } catch (SQLException exception) {
            throw new RegistrationException("Registracija korisnika nije uspjela.", exception);
        }
    }

    public User login(String username, char[] password) {
        String normalizedUsername = normalizeUsername(username);
        try {
            User user = userRepository.findByUsername(normalizedUsername)
                    .orElseThrow(() -> invalidCredentials(null));
            if (!user.isActive()) {
                throw new AuthenticationException("Korisnički račun nije aktivan.");
            }
            if (!passwordHasher.verify(password, user.getPasswordHash())) {
                throw invalidCredentials(null);
            }
            sessionContext.login(user);
            return user;
        } catch (AuthenticationException exception) {
            throw exception;
        } catch (SQLException exception) {
            throw new AuthenticationException("Prijava trenutno nije dostupna.", exception);
        }
    }

    public void logout() {
        sessionContext.logout();
    }

    public static String normalizeUsername(String username) {
        if (username == null) {
            return "";
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private void validateRegistration(String username, char[] password, String firstName, String lastName) {
        if (username.isBlank()) {
            throw new RegistrationException("Korisničko ime je obvezno.");
        }
        if (password == null || password.length < MIN_PASSWORD_LENGTH || password.length > MAX_PASSWORD_LENGTH) {
            throw new RegistrationException("Lozinka mora imati između 8 i 128 znakova.");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new RegistrationException("Ime je obvezno.");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new RegistrationException("Prezime je obvezno.");
        }
    }

    private AuthenticationException invalidCredentials(Throwable cause) {
        return cause == null
                ? new AuthenticationException("Pogrešno korisničko ime ili lozinka.")
                : new AuthenticationException("Pogrešno korisničko ime ili lozinka.", cause);
    }
}
