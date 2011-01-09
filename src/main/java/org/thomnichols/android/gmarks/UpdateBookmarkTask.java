package org.thomnichols.android.gmarks;

import java.io.IOException;

import org.thomnichols.android.gmarks.BookmarksQueryService.AuthException;
import org.thomnichols.android.gmarks.BookmarksQueryService.NotFoundException;
import org.thomnichols.android.gmarks.GmarksProvider.DBException;
import org.thomnichols.android.gmarks.GmarksProvider.DatabaseHelper;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class UpdateBookmarkTask extends AsyncTask<Void,Void,Integer> {
	static final String TAG = "GMARK UPDATE TASK";
	
    static final int ACTION_DELETE = 0;
    static final int ACTION_UPDATE = 1;
    static final int ACTION_NEW = 2;

    static final int RESULT_OK = 0;
    static final int RESULT_AUTH_FAILED = 1;
    static final int RESULT_NOT_FOUND = 2;
    static final int RESULT_ERROR_UNKNOWN = 500;
    
    final int action;
    Bookmark bookmark;
    final Context ctx;
    final boolean showProgress;
    ProgressDialog waitDialog; 
    
    public UpdateBookmarkTask(int action, Bookmark b, Context ctx, boolean showProgress) {
		this.action = action;
		this.bookmark = b;
		this.ctx = ctx;
		this.showProgress = showProgress;
	}
    
    @Override
    protected void onPreExecute() {
    	super.onPreExecute();
		BookmarksQueryService remoteSvc = BookmarksQueryService.getInstance();
		
		if ( ! remoteSvc.isAuthInitialized() ) {
			DatabaseHelper dbHelper = new DatabaseHelper(this.ctx);
			remoteSvc.setAuthCookies( dbHelper.restoreCookies() );
		}
		
    	if ( showProgress ) {
    		String progressText = "Saving...";
    		if ( this.action == ACTION_DELETE ) progressText = "Deleting bookmark...";
	    	this.waitDialog = ProgressDialog.show(
	        		this.ctx, "", 
	                progressText, true );
    	}
    }
	
	@Override protected Integer doInBackground(Void... arg0) {
		BookmarksQueryService remoteSvc = BookmarksQueryService.getInstance();
		
		try {
			switch ( this.action ) {
			case ACTION_NEW:
				this.bookmark = remoteSvc.create(this.bookmark);
				break;
			case ACTION_UPDATE:
				remoteSvc.update(this.bookmark);
				break;
			case ACTION_DELETE:
				remoteSvc.delete(this.bookmark.getGoogleId());
				break;
			}
			Log.d(TAG,"Success!");
			return RESULT_OK;
		}
		catch ( AuthException ex ) {
			return RESULT_AUTH_FAILED;
		}
		catch ( NotFoundException ex ) {
			return RESULT_NOT_FOUND;
		}
		catch ( IOException ex ) {
			Log.w(TAG, "Update error", ex );
			return RESULT_ERROR_UNKNOWN;
		}
	}
	
	/**
	 * Override this method to handle an activity start when auth fails.
	 */
	protected void onRemoteAuthFailure() {}

	@Override
	protected void onPostExecute(Integer resultCode ) {
		
		if ( this.showProgress ) this.waitDialog.dismiss();
		
		if ( resultCode == RESULT_AUTH_FAILED ) {
			this.onRemoteAuthFailure();
			Toast.makeText(this.ctx, "Error: Auth failure", Toast.LENGTH_LONG).show();
			return;
		}
		else if ( resultCode == RESULT_NOT_FOUND ) {
			// if deleting, that's OK, we're just deleting a stale local bookmark.
			if ( this.action != ACTION_DELETE ) {
				Toast.makeText(this.ctx, "Error: Bookmark not found", Toast.LENGTH_LONG).show();				
				return;
			}
		}
		else if ( resultCode !=  RESULT_OK ) {
			Log.w(TAG,"Update remote failed: " );
			String text = this.action == ACTION_DELETE ? "Delete failed" : "Save failed";
			Toast.makeText(this.ctx, text, Toast.LENGTH_LONG).show();
			return;
		}
		
		// update the database
		DatabaseHelper dbHelper = new DatabaseHelper(this.ctx);
		
		try {
			switch (this.action) {
			case ACTION_NEW:
				dbHelper.insert(this.bookmark, null);
				break;
			case ACTION_UPDATE:
				dbHelper.update(this.bookmark, null);
				break;
			case ACTION_DELETE:
				dbHelper.deleteBookmark(this.bookmark.get_id(), null);
				break;
			}
		}
		catch ( DBException ex ) {
			Log.w(TAG,"Database update failure", ex);
		}
		finally { dbHelper.close(); }
		
		// TODO update labels count

		String notifyText = this.action == ACTION_NEW ? "Bookmark created."
				: this.action == ACTION_UPDATE ? "Bookmark updated." :
					"Bookmark deleted.";
		Toast.makeText(this.ctx, notifyText, Toast.LENGTH_SHORT).show();
	}
}
