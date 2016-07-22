/*
 * Copyright 2010-2015 by David A. Parker <parker.david.a@gmail.com>
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

import java.io.File;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/**
 * Custom wrapper around the built-in Toast class for displaying messages to the user.
 */
public class UserVisibleMessage {
    private static final String TAG = UserVisibleMessage.class.getSimpleName();

    /**
     * Displays the specified CharSequence centered on the screen for the duration Toast.LENGTH_SHORT.
     * 
     * @param context The context to use
     * @param text The text to be displayed in the message
     */
    public static Toast showMessage( Context context, CharSequence text ) {
        Log.d(TAG, "Showing message [context=" + context.toString() + "][text=" + text + "]");

        // Create a new Toast message
        Toast t = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        t.show();
        return t;
    }

    /**
     * Displays the specified string resource centered on the screen for the duration Toast.LENGTH_SHORT.
     * 
     * @param context The context to use
     * @param resId The ID of the string resource to be displayed in the message
     */
    public static Toast showMessage( Context context, int resId ) {
        Log.d(TAG, "Showing message [context=" + context.toString() + "][resId=" + resId + "]");

        // Create a new Toast message
        Toast t = Toast.makeText(context, resId, Toast.LENGTH_SHORT);
        t.show();
        return t;
    }
    
    /**
     * Displays a note to the user and includes the option to not show this note again.
     * 
     * @param context The context to use
     * @param file The filename of the marker file to create or delete
     * @param resId The resource ID of the message to display in the note
     */
    public static void showNote( Context context, String file, int resId ) {
        File f = new File(context.getFilesDir(), file);
        
        if( f.exists() ) {
            Log.d(TAG, "Marker file " + file + " exists; note will not be displayed.");
            return;
        }
        
        Log.d(TAG, "Marker file " + file + " does not exist; showing note.");
        
        
        Intent showNoteIntent = new Intent();
        showNoteIntent.setClassName("com.github.daparker.checkvalve", "com.github.daparker.checkvalve.ShowNoteActivity");
        showNoteIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        showNoteIntent.putExtra(Values.EXTRA_NOTE_ID, resId);
        showNoteIntent.putExtra(Values.EXTRA_FILE_NAME, file);
        context.startActivity(showNoteIntent);
    }
}