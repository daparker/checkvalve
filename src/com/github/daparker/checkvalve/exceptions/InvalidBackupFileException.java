/*
 * Copyright 2010-2017 by David A. Parker <parker.david.a@gmail.com>
 * 
 * This file is part of CheckValve, an HLDS/SRCDS query app for Android.
 * 
 * CheckValve is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 * 
 * CheckValve is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with the CheckValve source code.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package com.github.daparker.checkvalve.exceptions;

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
