package org.thomnichols.android.gmarks;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Better way to access WebView cookies; Browser.getAllCookies() returns a string
 * that you have to parse manually, plus you don't get path/expiry/secure params.
 * @author tnichols
 */
public class WebViewCookiesDB extends SQLiteOpenHelper {
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
	
	public WebViewCookiesDB( Context ctx ) {
		// TODO version is probably different for different 
		// android versions; on 2.2 it is 10
		super(ctx, DATABASE, null, DB_VERSION );
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		// the table should already exist, created by the webview!
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int from, int to) {
		
	}
	
	List<Cookie> getCookies() {
		SQLiteDatabase db = this.getReadableDatabase();
		while ( db.isDbLockedByOtherThreads() ) {
			Log.w(TAG, "Waiting for other thread to flush DB");
			try { Thread.sleep(500); } catch ( InterruptedException ex ) {} 
		}
		
		try {
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

}
