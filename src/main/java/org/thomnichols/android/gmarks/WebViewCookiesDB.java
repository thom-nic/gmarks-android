/* This file is part of GMarks. Copyright 2010, 2011 Thom Nichols
 *
 * GMarks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GMarks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GMarks.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thomnichols.android.gmarks;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

/**
 * Better way to access WebView cookies; Browser.getAllCookies() returns a string
 * that you have to parse manually, plus you don't get path/expiry/secure params.
 * @author tnichols
 */
public class WebViewCookiesDB {
	static final String TAG = "WEBVIEW DB QUERY";
	static final int DB_VERSION = 10;
	static final String DATABASE = "webview.db";
	static final String TABLE_NAME = "cookies";
	
	static final String[] COLUMNS = { "_id", "name", "value", "domain", "path", "expires", "secure" };
	static final int COL_NAME = 1;
	static final int COL_VALUE = 2;
	static final int COL_DOMAIN = 3;
	static final int COL_PATH = 4;
	static final int COL_EXPIRES = 5;
	static final int COL_SECURE = 6;
	
	Context ctx;
	
	public WebViewCookiesDB( Context ctx ) {
		this.ctx = ctx;
	}
	
	List<Cookie> getCookies() {
		SQLiteDatabase db = SQLiteDatabase.openDatabase(
				ctx.getDatabasePath(DATABASE).getPath(), 
				null, SQLiteDatabase.OPEN_READONLY);
		while ( db.isDbLockedByOtherThreads() ) {
			Log.w(TAG, "Waiting for other thread to flush DB");
			try { Thread.sleep(500); } catch ( InterruptedException ex ) {} 
		}
		
		try {
			db.execSQL("PRAGMA read_uncommitted = true;");
			List<Cookie> cookies = new ArrayList<Cookie>();
			Cursor cursor = db.query(TABLE_NAME, COLUMNS, null, null, null, null, null );
			
			while ( cursor.moveToNext() ) {
				BasicClientCookie c = new BasicClientCookie( cursor.getString(COL_NAME), cursor.getString(COL_VALUE) );
				c.setDomain( cursor.getString(COL_DOMAIN) );
				c.setPath( cursor.getString(COL_PATH));
				Long expiry = cursor.getLong(COL_EXPIRES);
				if ( expiry != null ) c.setExpiryDate( new Date( expiry ) );
				c.setSecure( cursor.getShort(COL_SECURE) == 1 );
				Log.d(TAG, "Got cookie: " + c);
				cookies.add(c);
			}
			cursor.close();
			
//			cursor = db.query(TABLE_NAME, new String[] {"count(name)"}, null, null, null, null, null);
//			cursor.moveToFirst();
//			Log.d("WEBVIEW DB QUERY", "COunt: " + cursor.getLong(0) );
//			 cursor.close();
			return cookies;
		}
		finally { db.close(); }
	}

	public void deleteCookie(String key) {
		SQLiteDatabase db = SQLiteDatabase.openDatabase(
				ctx.getDatabasePath(DATABASE).getPath(), 
				null, SQLiteDatabase.OPEN_READWRITE);
		while ( db.isDbLockedByOtherThreads() ) {
			Log.w(TAG, "Waiting for other thread to flush DB");
			try { Thread.sleep(500); } catch ( InterruptedException ex ) {} 
		}
		
		try {
			db.delete(TABLE_NAME, COLUMNS[COL_NAME] + "=?", new String[] {key});
		}
		catch ( SQLiteException ex ) {
			Log.w(TAG,"Error deleting cookie: " + key, ex);
		}
		finally { db.close(); }
	}
}
