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
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.widget.EditText;
import android.widget.TextView;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.View.OnClickListener;
import com.github.daparker.checkvalve.R;
import com.github.daparker.checkvalve.backup.BackupParser;

public class RestoreBackupActivity extends Activity {
    private static final String TAG = RestoreBackupActivity.class.getSimpleName();

    private EditText field_backup_file;
    private TextView help_text;
    private File storageDir;
    private ProgressDialog p;

    private DatabaseProvider database;
    
    private OnClickListener restoreButtonListener = new OnClickListener() {
        @SuppressLint({ "InlinedApi", "NewApi" })
        public void onClick( View v ) {
            /*
             * "Restore" button was clicked
             */

            if( field_backup_file.getText().toString().length() == 0 ) {
                UserVisibleMessage.showMessage(RestoreBackupActivity.this, R.string.msg_empty_fields);
            }
            else {
                AlertDialog.Builder alertDialogBuilder;

                if( android.os.Build.VERSION.SDK_INT >= 11 )
                    alertDialogBuilder = new AlertDialog.Builder(RestoreBackupActivity.this, AlertDialog.THEME_HOLO_DARK);
                else
                    alertDialogBuilder = new AlertDialog.Builder(RestoreBackupActivity.this);

                alertDialogBuilder.setTitle(R.string.title_confirm_restore);
                alertDialogBuilder.setMessage(R.string.msg_restore_backup);
                alertDialogBuilder.setCancelable(false);

                alertDialogBuilder.setPositiveButton(R.string.button_restore, new DialogInterface.OnClickListener() {
                    public void onClick( DialogInterface dialog, int id ) {
                        /*
                         *  "Restore" button was clicked
                         */
                        restoreBackupFile();
                    }
                });

                alertDialogBuilder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                    public void onClick( DialogInterface dialog, int id ) {
                        /*
                         * "Cancel" button was clicked
                         */
                        dialog.cancel();
                    }
                });

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        }
    };

    private OnClickListener cancelButtonListener = new OnClickListener() {
        public void onClick( View v ) {
            /*
             * "Cancel" button was clicked
             */
            
            setResult(1);
            finish();
        }
    };

    private OnClickListener chooserButtonListener = new OnClickListener() {
        public void onClick( View v ) {
            /*
             * "Cancel" button was clicked
             */
            
            runFileChooser();
        }
    };
    
    @SuppressLint("HandlerLeak")
    private Handler backupParserHandler = new Handler() {
        public void handleMessage(Message msg) {
            if( p.isShowing() )
                p.dismiss();
            
            switch( msg.what ) {
                case 0:
                    // Data was restored successfully
                    UserVisibleMessage.showMessage(RestoreBackupActivity.this, R.string.msg_backup_file_restored);
                    finish();
                    break;
                case 1:
                    // Error during file parsing
                    UserVisibleMessage.showMessage(RestoreBackupActivity.this, R.string.msg_general_error);
                    break;
                case 2:
                    // Invalid backup file
                    UserVisibleMessage.showMessage(RestoreBackupActivity.this, R.string.msg_backup_file_invalid);
                    break;
                case 3:
                    // Some other error
                    UserVisibleMessage.showMessage(RestoreBackupActivity.this, R.string.msg_general_error);
                    break;
                default:
                    // This should never happen
                    Log.e(TAG, "backupWriterHandler: handleMessage(): Invalid Message what value " + msg.what);
                    UserVisibleMessage.showMessage(RestoreBackupActivity.this, R.string.msg_general_error);
                    break;
            }
        }
    };
    
    @SuppressLint("NewApi")
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        if( android.os.Build.VERSION.SDK_INT < 11 ) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        else if( android.os.Build.VERSION.SDK_INT >= 14 ) {
            if( ViewConfiguration.get(this).hasPermanentMenuKey() )
                requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        
        this.setContentView(R.layout.restore_backup);
        this.setResult(1);
        
        if( database == null )
            database = new DatabaseProvider(RestoreBackupActivity.this);

        // Default backup file location
        storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        
        // Set button listeners
        this.findViewById(R.id.cancelButton).setOnClickListener(cancelButtonListener);
        this.findViewById(R.id.restoreButton).setOnClickListener(restoreButtonListener);
        this.findViewById(R.id.chooserButton).setOnClickListener(chooserButtonListener);
        
        field_backup_file = (EditText)findViewById(R.id.field_backup_file);

        help_text = (TextView)findViewById(R.id.help_text);
        help_text.setText(String.format(this.getString(R.string.help_text_restore_backup), storageDir.getName()));
    }

    @Override
    protected void onResume() {
        super.onResume();

        if( database == null )
            database = new DatabaseProvider(RestoreBackupActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if( database != null ) {
            database.close();
            database = null;
        }
    }

    @Override
    public void onConfigurationChanged( Configuration newConfig ) {
        super.onConfigurationChanged(newConfig);
        return;
    }

    public void onActivityResult( int request, int result, Intent data ) {
        switch( request ) {
            case Values.ACTIVITY_FILE_CHOOSER:
                if( result == Activity.RESULT_OK )
                    field_backup_file.setText(data.getStringExtra(Values.EXTRA_FILE_NAME));
                break;
            default:
                break;
        }
    }

    public void restoreBackupFile() {
        String filename = field_backup_file.getText().toString().trim();
        File backupFile = new File(storageDir, filename);
        String filepath = backupFile.getAbsolutePath();
        
        try {
            if( Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ) {
                if( !backupFile.exists() ) {
                    UserVisibleMessage.showMessage(RestoreBackupActivity.this, R.string.msg_backup_file_does_not_exist);
                }
                else {
                    // Show the progress dialog
                    p = ProgressDialog.show(RestoreBackupActivity.this, "", getText(R.string.status_restoring_backup), true, false);
                    
                    BackupParser b = new BackupParser(RestoreBackupActivity.this, filepath, backupParserHandler);
                    Thread t = new Thread(b);
                    t.start();
                }
            }
            else {
                UserVisibleMessage.showMessage(RestoreBackupActivity.this, R.string.msg_backup_storage_not_mounted);
            }
        }
        catch( Exception e ) {
            Log.e(TAG, "restoreBackupFile(): Caught an exception:", e);
            UserVisibleMessage.showMessage(RestoreBackupActivity.this, R.string.msg_general_error);
            backupFile.delete();
        }
    }
    
    public void runFileChooser() {
        if( Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ) {
            Intent settingsIntent = new Intent();
            settingsIntent.setClassName("com.github.daparker.checkvalve", "com.github.daparker.checkvalve.backup.FileChooserActivity");
            startActivityForResult(settingsIntent, Values.ACTIVITY_FILE_CHOOSER);
        }
        else {
            UserVisibleMessage.showMessage(RestoreBackupActivity.this, R.string.msg_backup_storage_not_mounted);
        }
    }
}