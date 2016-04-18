/*
 * Copyright 2010-2016 by David A. Parker <parker.david.a@gmail.com>
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

import java.io.File;
import java.io.FileFilter;

public class BackupFileFilter implements FileFilter {
    private String[] extensions = { ".bkp" };

    public boolean accept( File pathname ) {
        if( pathname.isDirectory() && !pathname.isHidden() ) {
            return true;
        }

        for( String x : extensions ) {
            if( pathname.getName().endsWith(x) ) {
                return true;
            }
        }

        return false;
    }

}