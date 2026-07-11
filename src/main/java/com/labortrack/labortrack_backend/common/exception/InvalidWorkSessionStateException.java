package com.labortrack.labortrack_backend.common.exception;

public class InvalidWorkSessionStateException extends RuntimeException {
    public InvalidWorkSessionStateException(String message) {
        super(message);
    }
}
