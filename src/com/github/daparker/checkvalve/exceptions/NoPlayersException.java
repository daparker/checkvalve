package com.github.daparker.checkvalve.exceptions;

public class NoPlayersException extends Exception {
    private static final long serialVersionUID = 1046009911246248211L;

    public NoPlayersException() {}

    public NoPlayersException(String message) {
        super(message);
    }

    public NoPlayersException(String message, Throwable cause) {
        super(message, cause);
    }
}
