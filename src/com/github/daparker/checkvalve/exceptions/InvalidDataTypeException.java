package com.github.daparker.checkvalve.exceptions;

public class InvalidDataTypeException extends Exception {
    private static final long serialVersionUID = 6070536769124080396L;

    public InvalidDataTypeException() {}

    public InvalidDataTypeException(String message) {
        super(message);
    }

    public InvalidDataTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
