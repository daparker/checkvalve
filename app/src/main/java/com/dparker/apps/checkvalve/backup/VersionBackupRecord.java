/*
 * Copyright 2010-2019 by David A. Parker <parker.david.a@gmail.com>
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

package com.dparker.apps.checkvalve.backup;

import com.dparker.apps.checkvalve.exceptions.InvalidBackupFileException;

public class VersionBackupRecord {
    private int appVersion;
    private int fileVersion;

    public VersionBackupRecord() {
        this.appVersion = -1;
        this.fileVersion = -1;
    }

    public VersionBackupRecord(int appVersion, int fileVersion) {
        this.appVersion = appVersion;
        this.fileVersion = fileVersion;
    }

    public int getAppVersion() {
        return appVersion;
    }

    public int getFileVersion() {
        return fileVersion;
    }

    public void setAppVersion(int i) throws InvalidBackupFileException {
        if( appVersion >= 0 ) {
            throw new InvalidBackupFileException();
        }

        appVersion = i;
    }

    public void setFileVersion(int i) throws InvalidBackupFileException {
        if( fileVersion >= 0 ) {
            throw new InvalidBackupFileException();
        }

        fileVersion = i;
    }

    public boolean isValid() {
        return (appVersion > 0 && fileVersion > 0);
    }
}