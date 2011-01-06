package org.thomnichols.android.gmarks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.thomnichols.android.gmarks.BookmarksQueryService.AllBookmarksIterator;
import org.thomnichols.android.gmarks.GmarksProvider.DatabaseHelper;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.Browser;
import android.provider.Browser.BookmarkColumns;

public class BrowserSync {
	
	Context ctx;
	
	public BrowserSync(Context ctx) {
		this.ctx = ctx;
	}

	// TODO this needs to be async
	public void syncBrowserBookmarks( String label, long updatedSince, ContentResolver cr ) throws IOException {
		// first, attempt to sync all browser bookmarks to our local cache:
		BookmarksQueryService bookmarksSvc = BookmarksQueryService.getInstance();
		DatabaseHelper bookmarksDB = new DatabaseHelper(this.ctx);
		SQLiteDatabase db = bookmarksDB.getWritableDatabase();
		
		Iterable<Bookmark> allBookmarks = bookmarksSvc.getAllBookmarksForLabel(label);
		
		try {
			List<Bookmark> inserts = new ArrayList<Bookmark>();
			List<Bookmark> updates = new ArrayList<Bookmark>();

			// TODO use created/ modified time versus last update time
			Cursor cursor = Browser.getAllBookmarks(cr);
			try {
				while( cursor.moveToNext() ) {
					String url = cursor.getString(
							cursor.getColumnIndex(BookmarkColumns.URL));
					String title = cursor.getString(
							cursor.getColumnIndex(BookmarkColumns.TITLE));
					Long created = cursor.getLong(
							cursor.getColumnIndex(BookmarkColumns.CREATED));
					
					if ( created < updatedSince ) continue;	// nothing to see here
					
					Bookmark b = bookmarksDB.findByURL(url,db);
					
					if ( b == null ) {
						b = bookmarksSvc.create(b);
						inserts.add(b);
					}
					else { // update the remote & local cache
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
							b = bookmarksSvc.update(b);
							updates.add(b);
						}
					}
				}
			}
			finally { cursor.close(); }
			
			// update the local cache:
			for (Bookmark b : inserts ) {
				bookmarksDB.insert(b, db);
			}
			
			for ( Bookmark b : updates ) {
				bookmarksDB.update(b, db);
			}
		}
		finally { 
			db.close();
			bookmarksDB.close();
		}
	}
	
	public void syncToBrowser( List<Bookmark> bookmarks ) {
		// browser bookmarks are limited to name and URL.  Use URL as the key.
		 
	}
}
