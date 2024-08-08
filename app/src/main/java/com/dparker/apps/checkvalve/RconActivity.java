/*
 * Copyright 2010-2024 by David A. Parker <parker.david.a@gmail.com>
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

package com.dparker.apps.checkvalve;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.koraktor.steamcondenser.exceptions.RCONBanException;
import com.github.koraktor.steamcondenser.exceptions.RCONNoAuthException;
import com.github.koraktor.steamcondenser.servers.GameServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

public class RconActivity extends Activity {
    private static final String TAG = RconActivity.class.getSimpleName();

    private static Bundle settings;

    private static Animation fade_in;
    private static Animation fade_out;

    private static ProgressDialog p;
    private static Context context;

    private static TextView rcon_console;
    private static TextView sending;
    private static AutoCompleteTextView field_command;
    private static Toast toastMessage;

    private static String password;
    private static String command;
    private static String[] unsafeCommands;
    private static String server;
    private static int port;
    private static int timeout;
    private static int last;
    private static int pos;
    private static boolean rconIsAuthenticated;
    private static boolean enableHistory;
    private static boolean volumeButtons;
    private static float scaledDensity;

    private static GameServer srv;
    private Thread receiverThread;
    private NetworkEventReceiver receiverRunnable;
    private ArrayList<String> commandHistory;

    private final OnClickListener sendButtonListener = new OnClickListener() {
        public void onClick(View v) {
            /*
             * "Send" button was clicked
             */

            command = field_command.getText().toString().trim();

            if( command.isEmpty() )
                UserVisibleMessage.showMessage(RconActivity.this, R.string.msg_empty_rcon_command);
            else
                sendCommand(false);
        }
    };

    private final OnKeyListener keyListener = new OnKeyListener() {
        public boolean onKey(View v, int k, KeyEvent e) {
            /*
             * "Enter" or "Done" key was pressed
             */
            if( e.getKeyCode() == KeyEvent.KEYCODE_ENTER && e.getAction() == KeyEvent.ACTION_UP ) {
                command = field_command.getText().toString().trim();

                if( command.isEmpty() )
                    UserVisibleMessage.showMessage(RconActivity.this, R.string.msg_empty_rcon_command);
                else
                    sendCommand(false);

                return true;
            }

            if( enableHistory ) {
                /*
                 * D-Pad up arrow was pressed
                 */
                if( e.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP && e.getAction() == KeyEvent.ACTION_DOWN ) {
                    // Put the previous command in the text field
                    if( pos > 0 ) field_command.setText(commandHistory.get(--pos));
                    return true;
                }

                /*
                 * D-pad down arrow was pressed
                 */
                if( e.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN && e.getAction() == KeyEvent.ACTION_DOWN ) {
                    // Put the next command in the text field
                    if( pos < last ) field_command.setText(commandHistory.get(++pos));
                    return true;
                }
            }

            if( volumeButtons ) {
                if( e.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP ) {
                    float size = rcon_console.getTextSize();
                    size /= scaledDensity;

                    if( e.getAction() == KeyEvent.ACTION_DOWN ) {
                        if( size < 18.0 ) rcon_console.setTextSize(++size);
                        return true;
                    }

                    if( e.getAction() == KeyEvent.ACTION_UP ) {
                        if( toastMessage != null ) toastMessage.cancel();
                        String fontSize = Float.valueOf(size).toString();
                        String message = getString(R.string.msg_font_size) + " " + fontSize;
                        toastMessage = UserVisibleMessage.showMessage(RconActivity.this, message);

                        return true;
                    }
                }

                if( e.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN ) {
                    float size = rcon_console.getTextSize();
                    size /= scaledDensity;

                    if( e.getAction() == KeyEvent.ACTION_DOWN ) {
                        if( size > 6.0 ) rcon_console.setTextSize(--size);
                        return true;
                    }

                    if( e.getAction() == KeyEvent.ACTION_UP ) {
                        if( toastMessage != null ) toastMessage.cancel();
                        String fontSize = Float.valueOf(size).toString();
                        String message = getString(R.string.msg_font_size) + " " + fontSize;
                        toastMessage = UserVisibleMessage.showMessage(RconActivity.this, message);

                        return true;
                    }
                }
            }

            return false;
        }
    };

    // Handler for the server query thread
    private static class staticProgressHandler extends Handler {
        public void handleMessage(Message msg) {
            p.dismiss();

            if( msg.obj != null ) {
                srv = (GameServer) msg.obj;

                if( !rconIsAuthenticated ) {
                    if( password.isEmpty() )
                        getPassword(context);
                    else
                        rconAuthenticate();
                }
            }
            else {
                Log.w(TAG, "EngineQuery returned a null object.");
                UserVisibleMessage.showMessage(context, R.string.msg_rcon_general_error);
            }
        }
    }

    private static final staticProgressHandler progressHandler = new staticProgressHandler();

    // Handler for the "Sending" pop-up thread
    private static class staticPopUpHandler extends Handler {
        public void handleMessage(Message msg) {
            runFadeOutAnimation(sending);

            switch( msg.what ) {
                case 0:
                    String response = (String) msg.obj;
                    rcon_console.append("> " + command + "\n\n");
                    rcon_console.append(response + "\n\n");
                    scrollToBottomOfText();
                    break;
                case 1:
                    if( msg.obj.getClass() == RCONNoAuthException.class ) {
                        showBadPasswordMessage();
                    }
                    else if( msg.obj.getClass() == RCONBanException.class ) {
                        UserVisibleMessage.showMessage(context, R.string.msg_rcon_ban_exception);
                    }
                    else if( msg.obj.getClass() == TimeoutException.class ) {
                        UserVisibleMessage.showMessage(context, R.string.msg_rcon_disconnected);
                    }
                    else {
                        UserVisibleMessage.showMessage(context, R.string.msg_rcon_general_error);
                    }
                    break;
            }

            command = "";
        }
    }
    private static final staticPopUpHandler popUpHandler = new staticPopUpHandler();


    // Handler for the RCON authentication thread
    private static class staticRconAuthHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            runFadeOutAnimation(sending);

            switch( msg.what ) {
                case -1:
                    // An error occurred getting the engine type (most likely a socket timeout)
                    break;
                case 0:
                    rconIsAuthenticated = true;
                    srv = (GameServer) msg.obj;
                    break;
                case 1:
                    Log.d(TAG, "rconAuthHandler [" + msg + "]");

                    try {
                        Log.d(TAG, "Message object string = " + msg.obj.toString());
                        Log.d(TAG, "Message object class = " + msg.obj.getClass());

                        if( msg.obj.getClass() == RCONNoAuthException.class ) {
                            // Failed authentication
                            UserVisibleMessage.showMessage(context, R.string.msg_no_rcon_auth);
                            getPassword(context);
                        }
                        else if( msg.obj.getClass() == RCONBanException.class ) {
                            // RCONNoAuthException
                            UserVisibleMessage.showMessage(context, R.string.msg_rcon_ban_exception);
                        }
                        else if( msg.obj.getClass() == TimeoutException.class ) {
                            // TimeoutException (happens if RCON password was already sent)
                            UserVisibleMessage.showMessage(context, R.string.msg_rcon_timeout_exception);
                        }
                        else {
                            // Any other exception
                            UserVisibleMessage.showMessage(context, R.string.msg_rcon_general_error);
                        }
                    }
                    catch( Exception e ) {
                        Log.e(TAG, "rconAuthHandler caught an exception:", e);
                        UserVisibleMessage.showMessage(context, R.string.msg_rcon_general_error);
                    }

                    break;
            }

            p.dismiss();
        }
    }
    private static final staticRconAuthHandler rconAuthHandler = new staticRconAuthHandler();


    // Handler for the network event receiver thread
    private static class staticNetworkEventHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            /*
             * Message object "what" codes:
             * -2  =  Fatal exception in the NetworkEventReceiver thread
             * -1  =  No network connectivity
             *  0  =  Initial event from broadcast receiver (should be ignored)
             *  1  =  Network connection change
             */

            Log.d(TAG, "Received " + msg.what + " from NetworkEventReceiver");

            switch( msg.what ) {
                case -2:
                    Activity a = (Activity) context;
                    Log.e(TAG, "The network event receiver has aborted");
                    UserVisibleMessage.showMessage(context, R.string.msg_general_error);
                    a.finish();
                    break;

                case -1:
                    UserVisibleMessage.showMessage(context, R.string.msg_connection_lost);
                    closeRconConnection();
                    rconIsAuthenticated = false;
                    break;

                case 1:
                    UserVisibleMessage.showMessage(context, R.string.msg_network_change);
                    closeRconConnection();
                    rconIsAuthenticated = false;
                    getServerType();

                    break;

                default:
                    break;
            }
        }
    }
    private static final staticNetworkEventHandler networkEventHandler = new staticNetworkEventHandler();

    @SuppressLint("NewApi")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if( ViewConfiguration.get(this).hasPermanentMenuKey() )
            requestWindowFeature(Window.FEATURE_NO_TITLE);

        context = RconActivity.this;
        settings = Values.getSettings(RconActivity.this);

        // Get RCON settings
        float defaultFontSize = (float) settings.getInt(Values.SETTING_RCON_DEFAULT_FONT_SIZE);
        volumeButtons = settings.getBoolean(Values.SETTING_RCON_VOLUME_BUTTONS);
        enableHistory = settings.getBoolean(Values.SETTING_RCON_ENABLE_HISTORY);

        this.setContentView(R.layout.rcon);
        this.setResult(1);

        Intent thisIntent = getIntent();

        server = thisIntent.getStringExtra(Values.EXTRA_SERVER);
        port = thisIntent.getIntExtra(Values.EXTRA_PORT, 27015);
        timeout = thisIntent.getIntExtra(Values.EXTRA_TIMEOUT, 2);
        password = thisIntent.getStringExtra(Values.EXTRA_PASSWORD);
        rconIsAuthenticated = false;
        scaledDensity = this.getResources().getDisplayMetrics().scaledDensity;

        fade_in = AnimationUtils.loadAnimation(RconActivity.this, R.anim.fade_in);
        fade_out = AnimationUtils.loadAnimation(RconActivity.this, R.anim.fade_out);

        Button send_button = findViewById(R.id.rcon_send_button);

        rcon_console = findViewById(R.id.rcon_console);
        sending = findViewById(R.id.rcon_sending);

        send_button.setOnClickListener(sendButtonListener);
        rcon_console.setHorizontallyScrolling(true);
        rcon_console.setTextSize(defaultFontSize);

        sending.setVisibility(View.INVISIBLE);

        String[] commandList = getCommandList();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.autocomplete_textview_custom, commandList);

        field_command = findViewById(R.id.rcon_field_command);
        field_command.setAdapter(adapter);
        field_command.setOnKeyListener(keyListener);

        // Hack to disable auto-complete if desired by the user
        if( settings.getBoolean(Values.SETTING_RCON_SHOW_SUGGESTIONS) )
            field_command.setThreshold(1);
        else
            field_command.setThreshold(1000);

        if( settings.getBoolean(Values.SETTING_RCON_WARN_UNSAFE_COMMAND) )
            unsafeCommands = this.getResources().getStringArray(R.array.unsafe_commands);
        else
            unsafeCommands = null;

        if( enableHistory ) {
            commandHistory = new ArrayList<>();
            commandHistory.add(0, "");
            last = 0;
            pos = 0;
        }

        receiverRunnable = new NetworkEventReceiver(this, networkEventHandler);
        receiverThread = new Thread(receiverRunnable);
        receiverThread.start();

        getServerType();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        shutdownNetworkEventReceiver();
        if( srv != null ) srv.disconnect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.rcon_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if( item.getItemId() == R.id.back ) {
            finish();
            return true;
        }
        else if( item.getItemId() == R.id.clear_console ) {
            rcon_console.setText("");
            rcon_console.scrollTo(0, 0);
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void onActivityResult(int request, int result, Intent data) {
        if( request == Values.ACTIVITY_RCON_PASSWORD_DIALOG ) {
            if( result == 0 ) finish();

            if( result == 1 ) {
                password = data.getStringExtra(Values.EXTRA_PASSWORD);
                rconAuthenticate();
            }
        }

        if( request == Values.ACTIVITY_CONFIRM_UNSAFE_COMMAND && result == 1 ) sendCommand(true);
    }

    public String[] getCommandList() {
        Resources res = getResources();
        String[] result;

        if( ! settings.getBoolean(Values.SETTING_RCON_INCLUDE_SM) ) {
            // Get the list of standard commands
            result = res.getStringArray(R.array.all_commands);
        }
        else {
            // Get the lists of both standard and SourceMod commands 
            String[] allCommands = res.getStringArray(R.array.all_commands);
            String[] smCommands = res.getStringArray(R.array.sm_commands);

            // Determine the length of each list
            int len1 = allCommands.length;
            int len2 = smCommands.length;

            // Create a new array to hold the combined contents of both lists
            result = new String[len1 + len2];

            // Concatenate the arrays so the command list will include both
            // standard and SourceMod commands
            System.arraycopy(allCommands, 0, result, 0, len1);
            System.arraycopy(smCommands, 0, result, len1, len2);
        }

        return result;
    }

    public void sendCommand(boolean force) {
        if( enableHistory ) {
            commandHistory.add(last, command);
            pos = ++last;
        }

        if( unsafeCommands != null ) {
            if( !force ) {
                // Get the bare command without any arguments
                String bareCommand = (command.contains(" ")) ? command.substring(0, command.indexOf(" ")) : command;

                if( Arrays.asList(unsafeCommands).contains(bareCommand) ) {
                    // Show a warning and force user acknowledgment
                    confirmUnsafeCommand();
                    return;
                }
            }
        }

        field_command.setText("");

        runFadeInAnimation(sending);

        new Thread(new RconQuery(command, srv, popUpHandler)).start();
    }

    public static void getServerType() {
        // Show the progress dialog
        p = ProgressDialog.show(context, "", context.getText(R.string.status_connecting), true, false);

        // Run the server queries in a new thread
        new Thread(new EngineQuery(server, port, timeout, progressHandler)).start();
    }

    public static void scrollToBottomOfText() {
        /*
         * This is based on code I found at:
         * http://groups.google.com/group/android-developers/browse_thread/thread/8752156cca1e3742
         */
        int lineCount = rcon_console.getLineCount();
        int lineHeight = rcon_console.getLineHeight();
        int viewHeight = rcon_console.getHeight();
        int difference = (lineCount * lineHeight) - viewHeight;

        if( difference < 1 )
            return;

        rcon_console.scrollTo(0, difference);
    }

    public static void rconAuthenticate() {
        p = ProgressDialog.show(context, "", context.getText(R.string.status_rcon_verifying_password), true, false);
        new Thread(new RconAuth(password, srv, rconAuthHandler)).start();
    }

    public static void runFadeInAnimation(View v) {
        v.startAnimation(fade_in);
    }

    public static void runFadeOutAnimation(View v) {
        v.startAnimation(fade_out);
    }

    public static void getPassword(Context c) {
        if( c instanceof Activity ) {
            Activity a = (Activity) c;
            Intent rconPasswordIntent = new Intent();
            rconPasswordIntent.setClassName("com.dparker.apps.checkvalve", "com.dparker.apps.checkvalve.RconPasswordActivity");
            a.startActivityForResult(rconPasswordIntent, Values.ACTIVITY_RCON_PASSWORD_DIALOG);
        }
    }

    public static void showBadPasswordMessage() {
        UserVisibleMessage.showMessage(context, R.string.msg_no_rcon_auth);
        getPassword(context);
    }

    @SuppressLint({"InlinedApi", "NewApi"})
    public void confirmUnsafeCommand() {
        AlertDialog.Builder alertDialogBuilder;

        alertDialogBuilder = new AlertDialog.Builder(RconActivity.this, AlertDialog.THEME_HOLO_DARK);
        alertDialogBuilder.setTitle(R.string.title_confirm_unsafe_command);
        alertDialogBuilder.setMessage(R.string.msg_send_unsafe_command);
        alertDialogBuilder.setCancelable(false);

        alertDialogBuilder.setPositiveButton(R.string.button_send, (dialog, id) -> {
            /*
             *  "Send" button was clicked
             */
            sendCommand(true);
        });

        alertDialogBuilder.setNegativeButton(R.string.button_cancel, (dialog, id) -> {
            /*
             * "Cancel" button was clicked
             */
            dialog.cancel();
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void shutdownNetworkEventReceiver() {
        if( receiverRunnable != null ) {
            Log.d(TAG, "shutdownNetworkEventReceiver(): NetworkEventReceiver object is not null, calling unregisterReceiver()");
            receiverRunnable.unregisterReceiver();
            receiverRunnable.shutDown();
        }
        else {
            Log.d(TAG, "shutdownNetworkEventReceiver(): NetworkEventReceiver object is null");
        }

        if( receiverThread != null ) {
            Log.d(TAG, "shutdownNetworkEventReceiver(): NetworkEventReceiver thread is not null");

            if( receiverThread.isAlive() ) {
                Log.d(TAG, "shutdownNetworkEventReceiver(): NetworkEventReceiver thread is alive, calling interrupt() on thread " + receiverThread.getId());
                receiverThread.interrupt();
            }
            else {
                Log.d(TAG, "shutdownNetworkEventReceiver(): NetworkEventReceiver thread is not alive, not interrupting");
            }
        }
        else {
            Log.d(TAG, "shutdownNetworkEventReceiver(): NetworkEventReceiver thread is null");
        }
    }

    public static void closeRconConnection() {
        try {
            if( srv != null ) srv.disconnect();
        }
        catch( Exception e ) {
            Log.w(TAG, "closeRconConnection(): Caught an exception:", e);
        }
    }
}
