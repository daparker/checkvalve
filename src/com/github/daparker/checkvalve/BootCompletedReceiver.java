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

package com.github.daparker.checkvalve;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootCompletedReceiver extends BroadcastReceiver {

    final static String TAG = BootCompletedReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent arg1) {
        DatabaseProvider db = new DatabaseProvider(context);
            
        try {
            boolean enabled = db.getBooleanSetting(DatabaseProvider.SETTINGS_ENABLE_NOTIFICATIONS);
            
            if( enabled ) {
                Log.d(TAG, "[CHECKVALVE] Starting background query service.");
                context.startService(new Intent(context, BackgroundQueryService.class));
            }
            else {
                Log.d(TAG, "[CHECKVALVE] Background service is disbaled in settings.");
            }
        }
        catch( Exception e ) {
            Log.w(TAG, "[CHECKVALVE] Caught an exception:", e);
        }
        finally {
            db.close();
        }
    }
}