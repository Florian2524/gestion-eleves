package fr.afpa.backend.exception;

public class InvalidPhotoException extends RuntimeException {
    public InvalidPhotoException(String message) { super(message); }
}
