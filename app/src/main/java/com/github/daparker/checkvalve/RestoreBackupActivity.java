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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.View.OnClickListener;
import com.github.daparker.checkvalve.R;
import com.github.daparker.checkvalve.backup.BackupParser;
import com.github.daparker.checkvalve.exceptions.InvalidBackupFileException;

public class RestoreBackupActivity extends Activity {
    private static final String TAG = RestoreBackupActivity.class.getSimpleName();

    private EditText field_backup_file;
    private TextView help_text;
    private TableLayout file_details_table;
    private File storageDir;
    private ProgressDialog p;

    private DatabaseProvider database;

    private boolean canReadExternal;
    
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

                alertDialogBuilder = new AlertDialog.Builder(RestoreBackupActivity.this, AlertDialog.THEME_HOLO_DARK);
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

        if( android.os.Build.VERSION.SDK_INT >= 14 ) {
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
        this.findViewById(R.id.restorebackup_cancel_button).setOnClickListener(cancelButtonListener);
        this.findViewById(R.id.restorebackup_restore_button).setOnClickListener(restoreButtonListener);
        this.findViewById(R.id.restorebackup_chooser_button).setOnClickListener(chooserButtonListener);
        
        field_backup_file = (EditText)findViewById(R.id.restorebackup_field_backup_file);

        help_text = (TextView)findViewById(R.id.restorebackup_help_text);
        help_text.setText(String.format(this.getString(R.string.help_text_restore_backup), storageDir.getName()));
        
        file_details_table = (TableLayout)findViewById(R.id.restorebackup_file_details_table);

        canReadExternal = true;

        if( Build.VERSION.SDK_INT >= 23 ) {
            if( ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                canReadExternal = false;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if( database == null )
            database = new DatabaseProvider(RestoreBackupActivity.this);

        if( Build.VERSION.SDK_INT >= 23 ) {
            if( ! canReadExternal ) {
                String[] EXTERNAL_PERM = {
                        Manifest.permission.READ_EXTERNAL_STORAGE
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

    public void onActivityResult( int request, int result, Intent data ) {
        switch( request ) {
            case Values.ACTIVITY_FILE_CHOOSER:
                if( result == Activity.RESULT_OK ) {
                    File selectedFile = (File)data.getSerializableExtra(Values.EXTRA_BACKUP_FILE);
                    String filename = selectedFile.getName();
                    field_backup_file.setText(filename);
                    
                    try {
                        if( ! showFileDetails(selectedFile) ) {
                            this.findViewById(R.id.restorebackup_file_details_layout).setVisibility(View.GONE);
                            UserVisibleMessage.showMessage(RestoreBackupActivity.this, R.string.msg_general_error);
                        }
                    }
                    catch( InvalidBackupFileException ibfe ) {
                        UserVisibleMessage.showMessage(RestoreBackupActivity.this, R.string.msg_backup_file_invalid);
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if( requestCode == Values.PERMISSIONS_REQUEST ) {
            if( grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
                canReadExternal = true;
            }
            else {
                UserVisibleMessage.showMessage(this, R.string.msg_external_read_denied);
                setResult(1);
                finish();
            }
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
    
    public boolean showFileDetails(File file) throws InvalidBackupFileException {
        String appVersion = new String();
        String fileDate = new String();
        String line = new String();
        boolean valid = false;

        try {
            file_details_table.removeAllViews();
            
            BufferedReader r = new BufferedReader(new FileReader(file));
            Log.d(TAG, "Reading file " + file.getAbsolutePath());
            int lineNum = 0;
            
            while( (line = r.readLine()) != null ) {
                lineNum++;
                
                if( line.startsWith("## Version: ") ) {
                    Log.d(TAG, "Found version header on line " + lineNum);
                    appVersion = line.replaceFirst("## Version: ", "").trim();
                    Log.i(TAG, "Backup file is from app version " + appVersion);
                    continue;
                }
                
                if( line.startsWith("## Created: ") ) {
                    Log.d(TAG, "Found date header on line " + lineNum);
                    fileDate = line.replaceFirst("## Created: ", "").trim();
                    Log.i(TAG, "Backup file was created " + fileDate);
                    continue;
                }
                
                if( appVersion.length() > 0 && fileDate.length() > 0 ) {
                    valid = true;
                    break;
                }
            }
            
            r.close();
            
            if( ! valid ) {
                Log.e(TAG, "Backup file is not valid!");
                throw new InvalidBackupFileException();
            }
            
            long sizeInKB = file.length() / 1024;
            
            TextView versionLabel = new TextView(RestoreBackupActivity.this);
            versionLabel.setText(R.string.label_backup_app_version);
            versionLabel.setTextColor(Color.WHITE);
            versionLabel.setTextSize(14);
            versionLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            versionLabel.setPadding(0, 0, 10, 0);
            
            TextView versionText = new TextView(RestoreBackupActivity.this);
            versionText.setText(appVersion);
            versionText.setTextColor(Color.WHITE);
            versionText.setTextSize(14);
            
            TextView dateLabel = new TextView(RestoreBackupActivity.this);
            dateLabel.setText(R.string.label_backup_date);
            dateLabel.setTextColor(Color.WHITE);
            dateLabel.setTextSize(14);
            dateLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            dateLabel.setPadding(0, 0, 10, 0);
            
            TextView dateText = new TextView(RestoreBackupActivity.this);
            dateText.setText(fileDate);
            dateText.setTextColor(Color.WHITE);
            dateText.setTextSize(14);
            
            TextView sizeLabel = new TextView(RestoreBackupActivity.this);
            sizeLabel.setText(R.string.label_backup_size);
            sizeLabel.setTextColor(Color.WHITE);
            sizeLabel.setTextSize(14);
            sizeLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            sizeLabel.setPadding(0, 0, 10, 0);
            
            TextView sizeText = new TextView(RestoreBackupActivity.this);
            sizeText.setText(Long.valueOf(sizeInKB).toString() + " KB");
            sizeText.setTextColor(Color.WHITE);
            sizeText.setTextSize(14);
            
            TableRow versionRow = new TableRow(RestoreBackupActivity.this);
            versionRow.addView(versionLabel);
            versionRow.addView(versionText);
            
            TableRow dateRow = new TableRow(RestoreBackupActivity.this);
            dateRow.addView(dateLabel);
            dateRow.addView(dateText);
            
            TableRow sizeRow = new TableRow(RestoreBackupActivity.this);
            sizeRow.addView(sizeLabel);
            sizeRow.addView(sizeText);

            file_details_table.addView(versionRow);
            file_details_table.addView(dateRow);
            file_details_table.addView(sizeRow);
            
            this.findViewById(R.id.restorebackup_file_details_layout).setVisibility(View.VISIBLE);
            
            return true;
        }
        catch( InvalidBackupFileException ibfe ) {
            throw ibfe;
        }
        catch( Exception e ) {
            return false;
        }
    }
}