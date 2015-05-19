package com.dparker.apps.checkvalve.exceptions;

public class NullResponseException extends Exception {
    private static final long serialVersionUID = -3177433564954371656L;

    public NullResponseException() {}

    public NullResponseException(String message) {
        super(message);
    }

    public NullResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
