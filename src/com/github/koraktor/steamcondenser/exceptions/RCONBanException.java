/**
 * This code is free software; you can redistribute it and/or modify it under
 * the terms of the new BSD License.
 *
 * Copyright (c) 2009-2013, Sebastian Staudt
 */

package com.github.koraktor.steamcondenser.exceptions;

/**
 * This exception class indicates that the IP address your accessing the game
 * server from has been banned by the server
 * <p>
 * You or the server operator will have to unban your IP address on the server.
 *
 * @author Sebastian Staudt
 * @see com.github.koraktor.steamcondenser.servers.GameServer#rconAuth
 */
public class RCONBanException extends SteamCondenserException {

    /**
     * UID added by David A. Parker for CheckValve
     */
    private static final long serialVersionUID = -2051194164413800030L;

    /**
     * Creates a new <code>RCONBanException</code> instance
     */
    public RCONBanException() {
        super("You have been banned from this server.");
    }
}
