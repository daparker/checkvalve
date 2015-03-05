/*
 * Copyright 2010-2011 by David A. Parker <parker.david.a@gmail.com>
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

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Custom wrapper around the built-in Toast class for displaying messages to the user.
 */
public class UserVisibleMessage
{   
    private static final String TAG = UserVisibleMessage.class.getSimpleName();

    /**
     * Displays the specified CharSequence centered on the screen for the duration Toast.LENGTH_SHORT.
     * <p>
     * 
     * @param context The context to use
     * @param text The text to be displayed in the message </p>
     */
    public static void showMessage( Context context, CharSequence text )
    {
        Log.d(TAG, "Showing message [context=" + context.toString() + "][text=" + text + "]");

        // Create a new Toast message
        Toast t = Toast.makeText(context, text, Toast.LENGTH_SHORT);

        // Show the Toast message centered on the screen
        t.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
        t.show();
    }

    /**
     * Displays the specified string resource centered on the screen for the duration Toast.LENGTH_SHORT.
     * <p>
     * 
     * @param context The context to use
     * @param resId The ID of the string resource to be displayed in the message </p>
     */
    public static void showMessage( Context context, int resId )
    {
        Log.d(TAG, "Showing message [context=" + context.toString() + "][resId=" + resId + "]");

        // Create a new Toast message
        Toast t = Toast.makeText(context, resId, Toast.LENGTH_SHORT);

        // Show the Toast message centered on the screen
        t.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
        t.show();
    }
}