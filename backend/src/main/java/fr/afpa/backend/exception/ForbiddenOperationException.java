package fr.afpa.backend.exception;

public class ForbiddenOperationException
        extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
