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

package com.github.daparker.checkvalve;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.View.OnClickListener;
import com.github.daparker.checkvalve.R;
import com.github.daparker.checkvalve.backup.BackupWriter;

public class CreateBackupActivity extends Activity {
    private static final String TAG = CreateBackupActivity.class.getSimpleName();

    private boolean includeServers;
    private boolean includeSettings;
    private boolean canWriteExternal;
    private Button saveButton;
    private Button cancelButton;
    private EditText field_save_as;
    private TextView help_text;
    private File storageDir;

    private DatabaseProvider database;
    
    private OnClickListener saveButtonListener = new OnClickListener() {
        public void onClick( View v ) {
            /*
             * "Save" button was clicked
             */

            if( field_save_as.getText().toString().length() == 0 ) {
                UserVisibleMessage.showMessage(CreateBackupActivity.this, R.string.msg_empty_fields);
            }
            else {
                saveBackupFile();
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

    @SuppressLint("HandlerLeak")
    private Handler backupWriterHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch( msg.what ) {
                case 0:
                    // File was written successfully
                    UserVisibleMessage.showMessage(CreateBackupActivity.this, R.string.msg_backup_file_created);
                    finish();
                    break;
                case 1:
                    // Database error
                    UserVisibleMessage.showMessage(CreateBackupActivity.this, R.string.msg_db_failure);
                    break;
                case 2:
                    // Exception
                    Exception e = (Exception)msg.obj;
                    Log.d(TAG, "Caught an exception:", e);
                    UserVisibleMessage.showMessage(CreateBackupActivity.this, R.string.msg_general_error);
                    break;
                default:
                    // Something else went wrong
                    Log.e(TAG, "backupWriterHandler: handleMessage(): Invalid Message what value " + msg.what);
                    UserVisibleMessage.showMessage(CreateBackupActivity.this, R.string.msg_general_error);
                    break;
            }
        }
    };
    
    @SuppressLint("NewApi")
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        if( android.os.Build.VERSION.SDK_INT >= 14 ) {
            if( ViewConfiguration.get(this).hasPermanentMenuKey() )
                requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        
        this.setContentView(R.layout.create_backup);
        this.setResult(1);
        
        if( database == null )
            database = new DatabaseProvider(CreateBackupActivity.this);
        
        // Default backup file name
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String timestamp = sdf.format(new Date());
        String defaultFilename = "checkvalve_backup_" + timestamp + ".bkp";
        storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        
        cancelButton = (Button)findViewById(R.id.createbackup_cancel_button);
        cancelButton.setOnClickListener(cancelButtonListener);
        
        saveButton = (Button)findViewById(R.id.createbackup_save_button);
        saveButton.setOnClickListener(saveButtonListener);
        
        field_save_as = (EditText)findViewById(R.id.createbackup_field_save_as);
        field_save_as.setText(defaultFilename);

        help_text = (TextView)findViewById(R.id.createbackup_help_text);
        help_text.setText(String.format(this.getString(R.string.help_text_create_backup), storageDir.getName()));
        
        includeServers = true;
        includeSettings = true;
        canWriteExternal = true;

        if( Build.VERSION.SDK_INT >= 23 ) {
            if( ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                canWriteExternal = false;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if( database == null )
            database = new DatabaseProvider(CreateBackupActivity.this);

        if( Build.VERSION.SDK_INT >= 23 ) {
            if( ! canWriteExternal ) {
                String[] EXTERNAL_PERM = {
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                };

                ActivityCompat.requestPermissions(this, EXTERNAL_PERM, Values.PERMISSIONS_REQUEST);
            }
        }
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if( requestCode == Values.PERMISSIONS_REQUEST ) {
            if( grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
                canWriteExternal = true;
            }
            else {
                UserVisibleMessage.showMessage(this, R.string.msg_external_write_denied);
                setResult(1);
                finish();
            }
        }
    }

    public void checkboxHandler( View view ) {
        boolean checkState = ((CheckBox)view).isChecked();

        Log.i(TAG, "checkboxHandler(): View name=" + view.toString() + "; id=" + view.getId() + "; checked=" + checkState);

        if( view.getId() == R.id.createbackup_checkbox_include_servers )
            includeServers = checkState;
        else
            includeSettings = checkState;
    }

    public void saveBackupFile() {
        String filename = field_save_as.getText().toString().trim();
        File backupFile = new File(storageDir, filename);
        String filepath = backupFile.getAbsolutePath();
        
        try {
            if( Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ) {
                if( backupFile.exists() ) {
                    UserVisibleMessage.showMessage(CreateBackupActivity.this, R.string.msg_backup_file_exists);
                }
                else {
                    if( backupFile.createNewFile() ) {
                        BackupWriter b = new BackupWriter(CreateBackupActivity.this, filepath, includeServers, includeSettings, backupWriterHandler);
                        Thread t = new Thread(b);
                        t.start();
                    }
                    else {
                        UserVisibleMessage.showMessage(CreateBackupActivity.this, R.string.msg_backup_file_create_failed);
                    }
                }
            }
            else {
                UserVisibleMessage.showMessage(CreateBackupActivity.this, R.string.msg_backup_storage_not_mounted);
            }
        }
        catch( Exception e ) {
            Log.e(TAG, "saveBackupFile(): Caught an exception:", e);
            UserVisibleMessage.showMessage(CreateBackupActivity.this, R.string.msg_general_error);
            backupFile.delete();
        }
    }
}