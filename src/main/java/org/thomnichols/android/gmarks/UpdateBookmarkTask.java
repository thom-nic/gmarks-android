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

import org.thomnichols.android.gmarks.BookmarksQueryService.AuthException;
import org.thomnichols.android.gmarks.BookmarksQueryService.NotFoundException;
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
    		CharSequence progressText = ctx.getText(
    				this.action == ACTION_DELETE ?
    				R.string.deleting_bookmark_msg :
    				R.string.saving_bookmark_msg);
	    	this.waitDialog = ProgressDialog.show(
	        		this.ctx, "", 
	                progressText, true );
    	}
    }
	
	@Override protected Integer doInBackground(Void... arg0) {
		BookmarksQueryService remoteSvc = BookmarksQueryService.getInstance();
		DatabaseHelper dbHelper = new DatabaseHelper(this.ctx);

		try {
			switch ( this.action ) {
			case ACTION_NEW:
				this.bookmark = remoteSvc.create(this.bookmark);
				dbHelper.insert(this.bookmark, null);
				break;
			case ACTION_UPDATE:
				this.bookmark = remoteSvc.update(this.bookmark);
				dbHelper.update(this.bookmark, null);
				break;
			case ACTION_DELETE:
				remoteSvc.delete(this.bookmark.getGoogleId());
				dbHelper.deleteBookmark(this.bookmark.get_id(), null);
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
		catch ( Exception ex ) {
			Log.w(TAG, "Update error", ex );
			return RESULT_ERROR_UNKNOWN;
		}
		finally { dbHelper.close(); }
	}
	
	/**
	 * Override this method to handle an activity start when auth fails.
	 */
	protected void onRemoteAuthFailure() {}

	@Override
	protected void onPostExecute(Integer resultCode ) {
		
		if ( this.showProgress ) {
			try {
				this.waitDialog.dismiss();
			} catch ( IllegalArgumentException ex ) {} // if 'back' was pressed 
		}
		
		if ( resultCode == RESULT_AUTH_FAILED ) {
			this.onRemoteAuthFailure();
			Toast.makeText( this.ctx, R.string.error_auth_failed_msg, 
					Toast.LENGTH_LONG ).show();
			return;
		}
		else if ( resultCode == RESULT_NOT_FOUND ) {
			// if deleting, that's OK, we're just deleting a stale local bookmark.
			Log.w(TAG,"Remote bookmark not found.");
			if ( this.action != ACTION_DELETE ) {
				Toast.makeText( this.ctx, R.string.error_not_found_msg, 
						Toast.LENGTH_LONG ).show();				
				return;
			}
		}
		else if ( resultCode !=  RESULT_OK ) {
			Log.w(TAG,"Update remote failed: " + resultCode );
			int msgID = this.action == ACTION_DELETE ? 
					R.string.error_delete_failed_msg : 
					R.string.error_save_failed_msg;
			Toast.makeText(this.ctx, msgID, Toast.LENGTH_LONG).show();
			return;
		}
		
		int notifyText = this.action == ACTION_NEW ? 
				R.string.save_ok_msg :
				this.action == ACTION_UPDATE ? 
						R.string.update_ok_msg :
						R.string.delete_ok_msg;
		Toast.makeText(this.ctx, notifyText, Toast.LENGTH_SHORT).show();
	}
}
