package hr.lukabosnjak.service;

public final class AuthorizationException extends RuntimeException {
    public AuthorizationException(String message) {
        super(message);
    }
}
