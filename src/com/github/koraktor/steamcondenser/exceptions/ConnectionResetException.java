/**
 * This code is free software; you can redistribute it and/or modify it under
 * the terms of the new BSD License.
 *
 * Copyright (c) 2013, Sebastian Staudt
 */

package com.github.koraktor.steamcondenser.exceptions;

/**
 * Indicates that a connection has been reset by the peer
 *
 * @author Sebastian Staudt
 */
public class ConnectionResetException extends SteamCondenserException {

    /**
     * UID added by David A. Parker for CheckValve
     */
    private static final long serialVersionUID = 2970197351939282021L;

    public ConnectionResetException() {
        super("Connection reset by peer");
    }

}
