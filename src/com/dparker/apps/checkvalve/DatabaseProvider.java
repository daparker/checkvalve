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

package com.dparker.apps.checkvalve;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/*
 * Define the DatabaseProvider class
 */
public class DatabaseProvider
{
    private static final String DATABASE_NAME = "servers_db";
    private static final String DATABASE_TABLE = "servers";
    private static final String TAG				= "DatabaseProvider";    
    private static final String FIELD_ROWID		= "row_id";
    private static final String FIELD_SERVER	= "server";
    private static final String FIELD_PORT		= "port";
    private static final String FIELD_TIMEOUT	= "timeout";
    private static final String FIELD_LISTPOS	= "list_position";
    private static final String FIELD_RCON		= "rcon_password";
    
    private static final int DATABASE_VERSION = 2;

    private static final String DATABASE_CREATE =
        "CREATE TABLE servers ("
        	+ FIELD_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        	+ FIELD_SERVER + " TEXT NOT NULL, "
        	+ FIELD_PORT + " TEXT NOT NULL, " 
        	+ FIELD_TIMEOUT + " INTEGER NOT NULL,"
        	+ FIELD_LISTPOS + " INTEGER NOT NULL DEFAULT 0,"
    		+ FIELD_RCON + " TEXT NOT NULL DEFAULT '');";

    private Context context;
    private DatabaseHelper helper;
    private SQLiteDatabase db;

    public DatabaseProvider(Context ctx) 
    {
        this.context = ctx;
        helper = new DatabaseHelper(context);
    }
        
    private static class DatabaseHelper extends SQLiteOpenHelper 
    {
        DatabaseHelper(Context context) 
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) 
        {
        	db.execSQL(DATABASE_CREATE);
            return;
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
        {
        	Log.w(TAG,"Upgrading database from version " + oldVersion + " to " + newVersion);
        	
        	db.execSQL(
        			"ALTER TABLE " + DATABASE_TABLE +
        			" ADD COLUMN " + FIELD_RCON +
        			" TEXT NOT NULL DEFAULT '';"
        	);
        }
    }

    public DatabaseProvider open() throws SQLException 
    {
        db = helper.getWritableDatabase();
        return this;
    }

    public void close() 
    {
        helper.close();
    }
     
    public boolean isOpen()
    {
    	return this.db.isOpen();
    }
     
    public long insertServer(String server, int port, int timeout, String password) 
    {
    	int pos = getLastPosition() + 1;
    	
    	ContentValues values = new ContentValues();
         
    	values.put(FIELD_SERVER, server);
    	values.put(FIELD_PORT, port);
    	values.put(FIELD_TIMEOUT, timeout);
    	values.put(FIELD_LISTPOS, pos);
    	values.put(FIELD_RCON, password);

    	long result = db.insert(DATABASE_TABLE, null, values);
                  
    	return result;
    }

    public boolean deleteServer(long rowId) 
    {
    	return db.delete(DATABASE_TABLE, FIELD_ROWID + "=" + rowId, null) > 0;
    }

    public Cursor getAllServers() 
    {
    	Cursor result = db.query(
    			DATABASE_TABLE,
    			new String[] {
    					FIELD_ROWID,
    					FIELD_SERVER,
    					FIELD_PORT,
    					FIELD_TIMEOUT,
    					FIELD_LISTPOS,
    					FIELD_RCON
    			}, 
    			null,
    			null,
    			null,
    			null,
    			FIELD_LISTPOS
    	);
    	 
    	return result;
    }

    public Cursor getServer(long rowId) throws SQLException 
    {    	 
    	Cursor result = db.query(
    			DATABASE_TABLE,
    			new String[] {
    					FIELD_ROWID,
    					FIELD_SERVER,
    					FIELD_PORT,
    					FIELD_TIMEOUT,
    					FIELD_LISTPOS,
    					FIELD_RCON
    			},
    			FIELD_ROWID + "=" + rowId, 
    			null,
    			null,
    			null,
    			null
    	);
         
    	if (result != null) { result.moveToFirst(); }

    	return result;
    }

    public boolean updateServer(long rowId, String server, int port, int timeout, String password)
    {
    	ContentValues values = new ContentValues();
        
    	values.put(FIELD_SERVER, server);
    	values.put(FIELD_PORT, port);
    	values.put(FIELD_TIMEOUT, timeout);
    	values.put(FIELD_RCON, password);
                  
    	boolean result = db.update(DATABASE_TABLE, values, FIELD_ROWID + "=" + rowId, null) > 0;
                  
    	return result;
    }
    
    public int getLastPosition()
    {
    	int result = 0;
    	
    	Cursor cursor = db.query(
    			DATABASE_TABLE,
    			new String[] {
    					FIELD_LISTPOS
    			},
    			null, 
    			null,
    			null,
    			null,
    			FIELD_LISTPOS + " DESC",
    			"1"
    	);
    	
    	if( cursor.getCount() < 1 )
    		return 0;
    	
    	cursor.moveToFirst();
    	result = cursor.getInt(0);
    	cursor.close();
    	
    	return result;
    }
    
    public boolean moveServerUp(long rowId)
    {
    	Cursor cursor;
    	    	
    	cursor = db.query(
    			DATABASE_TABLE,
    			new String[] {
    					FIELD_LISTPOS
    			}, 
    			FIELD_ROWID + "=" + rowId, 
    			null, 
    			null, 
    			null, 
    			null
    	);
    	
    	cursor.moveToFirst();
    	
    	int oldPos = cursor.getInt(0);
    	int newPos = oldPos - 1;
    
    	cursor.close();
    	
    	if( oldPos == 0 ) { return true; }
    	
    	ContentValues a = new ContentValues();
    	ContentValues b = new ContentValues();
    	ContentValues c = new ContentValues();
    	
    	a.put(FIELD_LISTPOS, -1);
    	b.put(FIELD_LISTPOS, oldPos);
    	c.put(FIELD_LISTPOS, newPos);
    	
    	int r1 = db.update(DATABASE_TABLE, a, FIELD_LISTPOS + "=" + oldPos, null);
    	int r2 = db.update(DATABASE_TABLE, b, FIELD_LISTPOS + "=" + newPos, null);
    	int r3 = db.update(DATABASE_TABLE, c, FIELD_LISTPOS + "= -1", null);
    	
    	return ((r1==1) && (r2==1) && (r3==1));
    }
    
    public boolean moveServerDown(long rowId)
    {
    	Cursor cursor = db.query(
    			DATABASE_TABLE,
    			new String[] {
    					FIELD_LISTPOS
    			}, 
    			FIELD_ROWID + "=" + rowId, 
    			null, 
    			null, 
    			null, 
    			null
    	);
    	
    	cursor.moveToFirst();
    	
    	int oldPos = cursor.getInt(0);
    	int newPos = oldPos + 1;
    
    	cursor.close();
    	
    	if( oldPos == getLastPosition() ) { return true; }

    	ContentValues a = new ContentValues();
    	ContentValues b = new ContentValues();
    	ContentValues c = new ContentValues();
    	
    	a.put(FIELD_LISTPOS, -1);
    	b.put(FIELD_LISTPOS, oldPos);
    	c.put(FIELD_LISTPOS, newPos);
    	
    	int r1 = db.update(DATABASE_TABLE, a, FIELD_LISTPOS + "=" + oldPos, null);
    	int r2 = db.update(DATABASE_TABLE, b, FIELD_LISTPOS + "=" + newPos, null);
    	int r3 = db.update(DATABASE_TABLE, c, FIELD_LISTPOS + "= -1", null);
    	
    	return ((r1==1) && (r2==1) && (r3==1));
    }
}