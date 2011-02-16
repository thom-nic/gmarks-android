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

import static org.thomnichols.android.gmarks.GmarksProvider.BOOKMARKS_TABLE_NAME;

import java.io.IOException;

import org.thomnichols.android.gmarks.BookmarksQueryService.AuthException;
import org.thomnichols.android.gmarks.GmarksProvider.DBException;
import org.thomnichols.android.gmarks.GmarksProvider.DatabaseHelper;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Synchronize bookmark lists
 */
public class ListsSync {
	static final String TAG = "BOOKMARK LIST SYNC";
	long lastSync = 0;
	long lastSyncAttempt = 0;
	SharedPreferences prefs;
	
	public ListsSync(Context ctx) {
		this.prefs = Prefs.get(ctx);
		this.lastSync = prefs.getLong(Prefs.KEY_LAST_LIST_SYNC, 0);
		this.lastSyncAttempt = prefs.getLong(Prefs.KEY_LAST_LIST_SYNC_ATTEMPT, 0);
	}
	
	public void synchronizeMyLists( DatabaseHelper dbHelper ) throws DBException, 
			AuthException, IOException {
		
		BookmarksQueryService svc = BookmarksQueryService.getInstance();
		
		Long thisSync = System.currentTimeMillis();
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		try {
			Iterable<BookmarkList> iter = svc.getMyBookmarks();
			db.beginTransaction();
			ContentValues vals = new ContentValues();
			for ( BookmarkList bl : iter ) {
				if ( bl.getModifiedDate() < this.lastSync ) break;
				
				vals.clear();
				vals.put(BookmarkList.Columns.TITLE, bl.getTitle());
				vals.put(BookmarkList.Columns.DESCRIPTION, bl.getDescription());
				vals.put(BookmarkList.Columns.CREATED_DATE, bl.getCreatedDate());
				vals.put(BookmarkList.Columns.MODIFIED_DATE, bl.getModifiedDate());
				vals.put(BookmarkList.Columns.OWNED, bl.isOwnedByUser()?1:0);
				vals.put(BookmarkList.Columns.SHARED, bl.isShared()?1:0);
				vals.put(BookmarkList.Columns.PUBLISHED, bl.isPublished()?1:0);
				// See if this list already exists:
				
				Long rowID = null;
				Cursor cursor = db.query( BookmarkList.TABLE_NAME, 
						new String[] { BookmarkList.Columns._ID }, 
						BookmarkList.Columns.THREAD_ID+ "=?", 
						new String[] { bl.getThreadId() }, 
						null, null, null );
    			
				try {
					if ( ! cursor.moveToFirst() ) { // insert the new list
						vals.put(Bookmark.Columns.GOOGLEID, bl.getThreadId());
						rowID = db.insert( BookmarkList.TABLE_NAME, "", vals );
					}
					else { // update current bookmark:
						rowID = cursor.getLong(0);
						db.update( BookmarkList.TABLE_NAME, 
								vals, Bookmark.Columns._ID+ "=?", 
								new String[] { rowID.toString() } );
					}
					if ( rowID == null ) {
						Log.w(TAG, "No row ID while creating bookmark list: " + bl.getTitle() );
						throw new DBException("Couldn't create list: " + bl.getTitle());
					}
				}
				finally { cursor.close(); }

				// TODO create comments:
				
				// TODO create bookmarks for this list:
			}
			
			prefs.edit().putLong(Prefs.KEY_LAST_LIST_SYNC, thisSync).commit();
			db.setTransactionSuccessful();
		}
		finally {
			if ( db.inTransaction() ) db.endTransaction();
			db.close();
		}
		prefs.edit().putLong(Prefs.KEY_LAST_LIST_SYNC_ATTEMPT, thisSync).commit();
	}
}