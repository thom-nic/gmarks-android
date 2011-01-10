 package org.thomnichols.android.gmarks;

import static org.thomnichols.android.gmarks.GmarksProvider.BOOKMARKS_TABLE_NAME;
import static org.thomnichols.android.gmarks.GmarksProvider.BOOKMARK_LABELS_TABLE_NAME;
import static org.thomnichols.android.gmarks.GmarksProvider.LABELS_TABLE_NAME;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.thomnichols.android.gmarks.BookmarksQueryService.AuthException;
import org.thomnichols.android.gmarks.R;

import android.app.Activity;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.widget.CursorAdapter;
import android.widget.Toast;

class RemoteSyncTask extends AsyncTask<Void, Integer, Integer> {
	static final String TAG = "GMARKS SYNC";
	
	static final String SHARED_PREFS_NAME = "sync_prefs";
	static final String PREF_LAST_SYNC = "last_sync";
	
	static final int RESULT_SUCCESS = 0;
	static final int RESULT_FAILURE_AUTH = 1;
	static final int RESULT_FAILURE_UNKNOWN = 500;
	
	NotificationManager notificationManager;
	Notification notification;
	Activity ctx;
	long lastSyncTime = 0;
	final SharedPreferences syncPrefs;
	long thisSyncTime = 0;
	
	RemoteSyncTask(Activity ctx) {
		this.ctx = ctx;
		notificationManager = (NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
		notification = new Notification( R.drawable.ic_sync, 
				"Gmarks sync in progress...", 
				System.currentTimeMillis() );
		
		this.syncPrefs = ctx.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
	}
	
	@Override protected void onPreExecute() {
		super.onPreExecute();
		this.lastSyncTime = syncPrefs.getLong(PREF_LAST_SYNC, 0);
		Log.d(TAG,"Syncing bookmarks modified since: " + lastSyncTime);
		this.thisSyncTime = System.currentTimeMillis();
	}
	
	@Override protected Integer doInBackground(Void... arg0) {
    	GmarksProvider.DatabaseHelper dbHelper = 
    		new GmarksProvider.DatabaseHelper(this.ctx);

    	BookmarksQueryService remoteSvc = BookmarksQueryService.getInstance();
    	if ( ! remoteSvc.authInitialized ) 
			remoteSvc.setAuthCookies( dbHelper.restoreCookies() );

		SQLiteDatabase db = dbHelper.getWritableDatabase();
    	
		try {
    		db.beginTransaction();
    		ContentValues vals = new ContentValues();
			
    		// sync label list
			List<Label> labels = remoteSvc.getLabels();
    		Map<String,Long> labelIDs = new HashMap<String, Long>(labels.size());
			
        	for ( Label l : labels ) {
        		if ( this.isCancelled() ) break;

        		Long rowID = null;
        		vals.put(Label.Columns.COUNT, l.getCount());
        		
    			// update counts if label already exists.
    			Cursor cursor = db.query( LABELS_TABLE_NAME, 
    					new String[] { "_id" }, 
    					Label.Columns.TITLE+ "=?", 
    					new String[] { l.getTitle() }, 
    					null, null, null );
    			try {
	    			if ( cursor.moveToFirst() ) {
	    				rowID = cursor.getLong(0);
	    				// TODO check result
	    				db.updateWithOnConflict( LABELS_TABLE_NAME, vals, 
	    						Label.Columns.TITLE+"=?", 
	    						new String[] { l.getTitle() },
	    						SQLiteDatabase.CONFLICT_ABORT );
	    			}
	    			else {
	    				vals.put(Label.Columns.TITLE, l.getTitle());
	    				rowID = db.insertWithOnConflict( LABELS_TABLE_NAME, 
	        				"", vals, SQLiteDatabase.CONFLICT_ABORT );
	    			}
	    			if ( rowID >=0 ) labelIDs.put( l.getTitle(), rowID );
	    			else Log.w(TAG, "Invalid row ID: " + rowID );
    			}
    			finally { cursor.close(); }
        	}
			
			// sync bookmarks:
	    	Iterable<Bookmark> allBookmarks = remoteSvc.getAllBookmarks();
    		int count = 0;
        	for ( Bookmark b : allBookmarks ) {
        		if ( this.isCancelled() ) break;
        		
        		/* Bookmarks are returned in chrono order starting with the 
        		 * most recently modified.  Keep iterating until we've reached 
        		 * a bookmark whose 'modified' datetime is before the last sync time. */
        		if ( b.getModifiedDate() < this.lastSyncTime ) break;
        		
//        		ContentValues vals = new ContentValues();
        		vals.clear();
        		vals.put(Bookmark.Columns.THREAD_ID, b.getThreadId());
        		vals.put(Bookmark.Columns.TITLE, b.getTitle());
        		vals.put(Bookmark.Columns.HOST, b.getHost());
        		vals.put(Bookmark.Columns.URL, b.getUrl());
        		vals.put(Bookmark.Columns.DESCRIPTION, b.getDescription());
        		vals.put(Bookmark.Columns.CREATED_DATE, b.getCreatedDate());
        		vals.put(Bookmark.Columns.MODIFIED_DATE, b.getModifiedDate());
        		vals.put(Bookmark.Columns.LABELS, b.getAllLabels());

    			Long bookmarkRowID = null;
    			Cursor cursor = db.query( BOOKMARKS_TABLE_NAME, 
    					new String[] { Bookmark.Columns._ID }, 
    					Bookmark.Columns.GOOGLEID+ "=?", 
    					new String[] { b.getGoogleId() }, 
    					null, null, null );
    			
    			// Insert or update the next bookmark.
    			boolean bookmarkInserted = false;
    			try {
        			if ( ! cursor.moveToFirst() ) { // insert a new bookmark row
        				Log.d(TAG, "Inserting bookmark: " + b.getTitle() );
                		vals.put(Bookmark.Columns.GOOGLEID, b.getGoogleId());
		        		bookmarkRowID = db.insertWithOnConflict(
		        				BOOKMARKS_TABLE_NAME, "", vals,
		        				SQLiteDatabase.CONFLICT_ABORT );
		        		
		        		bookmarkInserted = true;
        			}
        			else { // update current bookmark:
        				bookmarkRowID = cursor.getLong(0);
	        			Log.d( TAG, "Updating bookmark: " + b.getTitle() );
	        			db.updateWithOnConflict( BOOKMARKS_TABLE_NAME, 
	        					vals, Bookmark.Columns._ID+ "=?", 
	        					new String[] { bookmarkRowID.toString() },
	        					SQLiteDatabase.CONFLICT_ABORT );
	        		}
        			if ( bookmarkRowID == null ) {
        				Log.w(TAG, "No row ID while attempting to insert bookmark: " + b.getTitle() );
        				return RESULT_FAILURE_UNKNOWN;
        			}
	        		count++;
    			}
    			finally { cursor.close(); }
	        		
        		// add label relations for bookmark
    			b.set_id(bookmarkRowID);
    			dbHelper.updateLabels(db, b);
/*        		for ( String label : b.getLabels() ) {
        			Long labelID = labelIDs.get( label );
        			if ( labelID == null ) {
        				Log.w(TAG, "No _id for label " + label );
        				continue;
        			}
        			vals.clear();
        			vals.put("bookmark_id", bookmarkRowID);
        			vals.put("label_id", labelID );
        			db.insertWithOnConflict( BOOKMARK_LABELS_TABLE_NAME, 
        					null, vals, SQLiteDatabase.CONFLICT_IGNORE );
        		}
        		if ( b.getLabels().size() < 1 ) {
        			Long labelID = labelIDs.get( "^none" );
        			if ( labelID != null ) {
        				vals.clear();
	        			vals.put("bookmark_id", bookmarkRowID);
	        			vals.put("label_id", labelID );
	        			db.insertWithOnConflict( BOOKMARK_LABELS_TABLE_NAME, 
	        					null, vals, SQLiteDatabase.CONFLICT_IGNORE );
        			}
        		}
*/
        		
        		// update full-text search:
        		vals.clear();
        		vals.put("docid", bookmarkRowID);
        		vals.put(Bookmark.Columns.TITLE+"_fts", b.getTitle());
        		vals.put(Bookmark.Columns.HOST+"_fts", b.getHost());
        		vals.put(Bookmark.Columns.DESCRIPTION+"_fts", b.getDescription());
        		vals.put("labels_fts", TextUtils.join(" ", b.getLabels()));
        		long result = -1;
        		try {
        			if ( bookmarkInserted )
        				result = db.insertWithOnConflict(BOOKMARKS_TABLE_NAME+"_FTS", "",
	        				vals, SQLiteDatabase.CONFLICT_IGNORE );
        		}
        		catch ( SQLiteConstraintException ex ) {
        			// this keeps throwing an exception even though I am using 
        			// a conflict strategy!??!!!
        			Log.w(TAG, "FTS Update Error for ID: " + bookmarkRowID, ex);
        		}
        		if ( result < 0 ) { // update, not insert:
        			vals.remove("docid");
        			result = db.updateWithOnConflict(BOOKMARKS_TABLE_NAME+"_FTS", vals, 
        					"docid=?", new String[] { bookmarkRowID.toString() }, 
        					SQLiteDatabase.CONFLICT_FAIL );
        			if ( result < 0 ) {
        				Log.w(TAG, "Error updating FTS table for bookmark: " + b.getTitle() );
        			}
        		}
        		
        		// TODO different message if there were no new bookmarks.
        		if ( count % 10 == 0 ) this.publishProgress(count);
        	}
        	
        	if ( ! this.isCancelled() ) db.setTransactionSuccessful();
			this.publishProgress(count);
        	return RESULT_SUCCESS;
		}
		catch ( AuthException ex ) {
			Log.d(TAG, "Auth error" );
			return RESULT_FAILURE_AUTH;
		}
		catch ( Exception ex ) {
			Log.e(TAG, "Error syncing bookmarks", ex);
		}
		finally {
			db.endTransaction();
			db.close();
			dbHelper.close();
		}
		return RESULT_FAILURE_UNKNOWN;
	}
	
	@Override protected void onPostExecute( Integer result ) {
		if ( result == RESULT_SUCCESS ) {
			Toast.makeText(this.ctx, "GMarks Sync complete", Toast.LENGTH_LONG).show();
			if ( this.ctx instanceof ListActivity ) {
				Log.d(TAG,"Refreshing listview...");
				((CursorAdapter)((ListActivity)this.ctx).getListAdapter()).getCursor().requery();
//				((CursorAdapter)((ListActivity)this.ctx).getListAdapter()).notifyDataSetChanged();
			}
			// update shared 'last sync' state
			this.syncPrefs.edit().putLong(PREF_LAST_SYNC, this.thisSyncTime).commit();
		}
		else if ( result == RESULT_FAILURE_AUTH ) {
			this.ctx.startActivityForResult(new Intent("org.thomnichols.gmarks.action.LOGIN"), 
					Activity.RESULT_OK );
		}
		else Toast.makeText(this.ctx, "GMarks sync error", Toast.LENGTH_LONG).show();
	}
	
	@Override protected void onCancelled() {
		Log.d( TAG, "Sync cancelled by user" );
	}
	
	@Override protected void onProgressUpdate(Integer... values) {
		int count = values[0];
		Intent intent = new Intent( this.ctx, this.ctx.getClass() );
		notification.setLatestEventInfo( this.ctx, 
				"GMarks Sync", "Synchronized " + count + " bookmarks", 
				PendingIntent.getActivity(this.ctx, 0, intent, 0) );
		this.notificationManager.notify(1, notification);
	}
}