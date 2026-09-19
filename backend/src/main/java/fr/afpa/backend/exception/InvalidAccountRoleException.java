package fr.afpa.backend.exception;

public class InvalidAccountRoleException extends RuntimeException {
    public InvalidAccountRoleException(String message) {
        super(message);
    }
}
