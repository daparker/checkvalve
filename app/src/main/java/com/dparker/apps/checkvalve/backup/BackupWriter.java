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

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.dparker.apps.checkvalve.DatabaseProvider;
import com.dparker.apps.checkvalve.R;
import com.dparker.apps.checkvalve.Values;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BackupWriter implements Runnable {
    private static final String TAG = BackupWriter.class.getSimpleName();

    private Uri fileUri;
    private String filename;
    private Context context;
    private Handler handler;
    private boolean includeServers;
    private boolean includeSettings;
    private DatabaseProvider database;

    public BackupWriter(Context context, String filename, boolean includeServers, boolean includeSettings, Handler handler) {
        this.context = context;
        this.handler = handler;
        this.fileUri = null;
        this.filename = filename;
        this.includeServers = includeServers;
        this.includeSettings = includeSettings;
    }

    public BackupWriter(Context context, Uri fileUri, boolean includeServers, boolean includeSettings, Handler handler) {
        this.context = context;
        this.handler = handler;
        this.fileUri = fileUri;
        this.filename = null;
        this.includeServers = includeServers;
        this.includeSettings = includeSettings;
    }

    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        try {
            int result = saveBackupFile();
            handler.sendEmptyMessage(result);
        }
        catch( Exception e ) {
            Message msg = new Message();
            msg.what = 2;
            msg.obj = e;
            handler.sendMessage(msg);
        }
    }

    public int saveBackupFile() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.getDefault());
        String timestamp = sdf.format(new Date());
        String version = context.getString(R.string.app_version);
        File filesDir = context.getFilesDir();
        File backupFile = new File(filesDir, Values.FILE_DUMMY_TEMP);

        OutputStream os;
        BufferedWriter bw;

        File[] markerFiles = new File[]{
                new File(filesDir, Values.FILE_HIDE_CHAT_RELAY_NOTE),
                new File(filesDir, Values.FILE_HIDE_CONSOLE_RELAY_NOTE),
                new File(filesDir, Values.FILE_HIDE_ANDROID_VERSION_NOTE)};

        try {
            if( database == null )
                database = new DatabaseProvider(context);

            if( fileUri != null ) {
                os = context.getContentResolver().openOutputStream(fileUri);
                bw = new BufferedWriter(new OutputStreamWriter(os));
            }
            else {
                backupFile = new File(filename);
                bw = new BufferedWriter(new FileWriter(backupFile));
            }

            StringBuilder sb = database.getBackupData(includeServers, includeSettings);
            int versionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
            int backupVersion = 1;

            if( sb != null ) {
                // File header
                bw.write("##\r\n");
                bw.write("## CHECKVALVE DATA BACKUP - DO NOT EDIT\r\n");
                bw.write("##\r\n");
                bw.write("## Version: " + version + "\r\n");
                bw.write("## Created: " + timestamp + "\r\n");
                bw.write("##\r\n\r\n");

                // App and file version
                bw.write("[version]\r\n");
                bw.write("app=" + versionCode + "\r\n");
                bw.write("file=" + backupVersion + "\r\n");
                bw.write("\r\n");

                // Data backup
                bw.write(sb.toString());

                for( File f : markerFiles ) {
                    if( f.exists() ) {
                        bw.write("[flag]\r\n");
                        bw.write("name=" + f.getName() + "\r\n");
                        bw.write("\r\n");
                    }
                }

                // Close the output buffer
                bw.close();

                return 0;
            }
            else {
                Log.e(TAG, "saveBackupFile(): getBackupData() returned a null value!");
                bw.close();
                if( filename != null ) backupFile.delete();
                return 1;
            }
        }
        catch( Exception e ) {
            Log.e(TAG, "saveBackupFile(): Caught an exception:", e);
            if( filename != null ) backupFile.delete();
            throw e;
        }
        finally {
            database.close();
            database = null;
        }
    }
}