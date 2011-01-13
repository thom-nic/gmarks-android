package org.thomnichols.android.gmarks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.thomnichols.android.gmarks.GmarksProvider.DBException;
import org.thomnichols.android.gmarks.GmarksProvider.DatabaseHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.Browser;
import android.provider.Browser.BookmarkColumns;
import android.util.Log;

public class BrowserSync {
	private static final String TAG = "GMARKS BROWSER SYNC";
	
	Context ctx;
	
	static final String[] browserColumnsProjection = {
		BookmarkColumns.URL,
		BookmarkColumns.TITLE,
		BookmarkColumns.CREATED
	};
	static final int COL_URL = 0;
	static final int COL_TITLE = 1;
	static final int COL_CREATED = 2;

	
	public BrowserSync(Context ctx) {
		this.ctx = ctx;
	}

	// TODO this needs to be async
	public void syncBrowserBookmarks( String label, long updatedSince ) throws IOException, DBException {
		// first, attempt to sync all browser bookmarks to our local cache:
		BookmarksQueryService bookmarksSvc = BookmarksQueryService.getInstance();
		DatabaseHelper bookmarksDB = new DatabaseHelper(this.ctx);
		SQLiteDatabase db = bookmarksDB.getWritableDatabase();
 		db.beginTransaction();

		Log.d(TAG,"Syncing browser bookmarks for label: " + label);
//		List<Bookmark> labelBookmarksToSync = bookmarksDB.findByLabel(label, db);
		Iterable<Bookmark> labelBookmarksToSync = bookmarksSvc.getAllBookmarksForLabel(label);
		List<Bookmark> labelBookmarks = new ArrayList<Bookmark>();
		for ( Bookmark b : labelBookmarksToSync )
			labelBookmarks.add(b);
		
		try {
			List<Bookmark> inserts = new ArrayList<Bookmark>();
			List<Bookmark> updates = new ArrayList<Bookmark>();

			// this will be used to collect the browser's current bookmarks
			Set<String> browserURLs = new HashSet<String>();
			// TODO use created/ modified time in where clause versus last update time
			Cursor cursor = ctx.getContentResolver().query(Browser.BOOKMARKS_URI, 
					browserColumnsProjection, "bookmark=1", null, null);
			try {
				while( cursor.moveToNext() ) {
					String url = cursor.getString(COL_URL);
					browserURLs.add(url);
					String title = cursor.getString(COL_TITLE);
					Long created = cursor.getLong(COL_CREATED);
					
					if ( created < updatedSince ) continue;	// nothing to see here
					
					Bookmark b = bookmarksDB.findByURL(url,db);
					
					if ( b == null ) { // create new GMark
						Log.d(TAG,"Creating bookmark from browser: " + title);
						Bookmark newBookmark = new Bookmark(null,null,
								title, url, null, "", created, 0);
						newBookmark.getLabels().add(label);
						b = bookmarksSvc.create(newBookmark);
						inserts.add(b);
					}
					else { // update the remote & local GMark
						boolean needUpdate = false;
						if ( ! title.equals(b.getTitle()) ) {
							b.setTitle(title);
							needUpdate = true;
						}
						if ( ! b.getLabels().contains(label) ) {
							b.getLabels().add(label);
							needUpdate = true;
						}
						if ( needUpdate ) {
							Log.d(TAG,"Updating bookmark from browser: " + title);
							b = bookmarksSvc.update(b);
							Log.d(TAG,"Bookmark ID: " + b.getGoogleId());
							updates.add(b);
						}
					}
				}
			}
			finally { cursor.close(); }
			
			// update the local cache:
			for ( Bookmark b : inserts ) bookmarksDB.insert(b, db);			
			for ( Bookmark b : updates ) bookmarksDB.update(b, db);
			
			// now find any new remote bookmarks that should be added to the browser:
			for ( Bookmark b : labelBookmarksToSync ) {
				if ( b.getModifiedDate() < updatedSince ) continue;
				if ( browserURLs.contains(b.getUrl()) ) continue;

//				Browser.saveBookmark(this.ctx, b.getTitle(), b.getUrl());
				ContentValues vals = new ContentValues();
				vals.put(BookmarkColumns.TITLE, b.getTitle());
				vals.put(BookmarkColumns.URL, b.getUrl());
				vals.put(BookmarkColumns.CREATED, b.getCreatedDate());
				vals.put(BookmarkColumns.BOOKMARK, 1);
//				vals.put(BookmarkColumns., value) TODO favicon
				ctx.getContentResolver().insert(Browser.BOOKMARKS_URI, vals);
				Log.d(TAG,"Created browser bookmark: " + b.getTitle());
			}
			
			db.setTransactionSuccessful();
		}
		finally { 
			db.endTransaction();
			db.close();
			bookmarksDB.close();
		}
	}
}
