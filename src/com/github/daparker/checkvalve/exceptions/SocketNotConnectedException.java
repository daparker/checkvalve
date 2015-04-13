package com.github.daparker.checkvalve.exceptions;

public class SocketNotConnectedException extends Exception {
    private static final long serialVersionUID = -6681226504207709439L;

    public SocketNotConnectedException() {}

    public SocketNotConnectedException(String message) {
        super(message);
    }

    public SocketNotConnectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
