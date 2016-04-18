package com.dparker.apps.checkvalve.exceptions;

public class InvalidBackupFileException extends Exception {
    private static final long serialVersionUID = 7076230360276860079L;

    public InvalidBackupFileException() {}

    public InvalidBackupFileException(String message) {
        super(message);
    }

    public InvalidBackupFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
