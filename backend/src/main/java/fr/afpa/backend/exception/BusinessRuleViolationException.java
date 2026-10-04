package fr.afpa.backend.exception;

public class BusinessRuleViolationException
        extends RuntimeException {

    public BusinessRuleViolationException(
            String message
    ) {
        super(message);
    }
}
