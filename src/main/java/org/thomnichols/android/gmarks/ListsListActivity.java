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
 */package org.thomnichols.android.gmarks;

import org.thomnichols.android.gmarks.BookmarksQueryService.AuthException;
import org.thomnichols.android.gmarks.GmarksProvider.DatabaseHelper;

import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CursorAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class ListsListActivity extends ListActivity {
	static final String TAG = "BOOKMARKS LISTS ACT";

	String category;
	
    private static final String[] PROJECTION = new String[] {
        BookmarkList.Columns._ID, // 0
        BookmarkList.Columns.TITLE, // 1
    };
	static final String[] LIST_COLUMNS = {BookmarkList.Columns.TITLE};
	static final int[] LIST_VIEW_IDS = {R.id.title};
	
	static final int SYNC_RESULT_OK = 0;
	static final int SYNC_RESULT_ERROR = 1;
	static final int SYNC_RESULT_AUTH_ERR = 2;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	
		Intent intent = getIntent();
		if ( intent.getAction() == null ) intent.setAction(Intent.ACTION_VIEW);

		Uri uri = intent.getData();
		if ( uri == null ) {
			uri = BookmarkList.CONTENT_URI;
			this.category = intent.getStringExtra(BookmarkList.PARAM_CATEGORY);
			if ( category != null ) uri = uri.buildUpon().appendQueryParameter(
					BookmarkList.PARAM_CATEGORY, category ).build();
			intent.setData(uri);
		}
		else this.category = uri.getQueryParameter(BookmarkList.PARAM_CATEGORY);
		
		setListAdapter ( new SimpleCursorAdapter( this, 
				R.layout.lists_list_item, 
				// TODO get list sort order from prefs
				managedQuery(uri, PROJECTION, null, null, null), 
				LIST_COLUMNS, LIST_VIEW_IDS) ); 
	}
	
	protected void doListSync() {
		Toast.makeText(this, R.string.sync_begin_msg, Toast.LENGTH_SHORT).show();
		final ListActivity ctx = this;
		new AsyncTask<Void, Integer, Integer>() {
			@Override
			protected Integer doInBackground(Void... params) {
				Log.d(TAG,"Starting list sync...");
				ListsSync sync = new ListsSync(ctx);
				DatabaseHelper db = new DatabaseHelper(ctx);
				try {
					sync.synchronizeMyLists(db);
					((CursorAdapter)ctx.getListAdapter()).getCursor().requery();
				}
				catch ( AuthException ex ) {
					Log.w(TAG,"Auth failure",ex);
					return SYNC_RESULT_AUTH_ERR;
				}
				catch ( Exception ex ) {
					Log.w(TAG, "Lists sync error", ex);
					return SYNC_RESULT_ERROR;
				}
				return SYNC_RESULT_OK;
			}
			protected void onPostExecute(Integer result) {
				int msgID = R.string.sync_done_msg;
				if ( result == SYNC_RESULT_AUTH_ERR ) {
					msgID = R.string.sync_notify_auth_error;
					startActivity( new Intent(ctx,WebViewLoginActivity.class) );
				}
				else if ( result == SYNC_RESULT_ERROR )
					msgID = R.string.sync_notify_error;
				Toast.makeText(ctx, msgID, Toast.LENGTH_LONG);
			};
		}.execute();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "List selected for category " + this.category);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.lists_list, menu);
        return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch ( item.getItemId() ) {
		case R.id.menu_sync:
			doListSync();
			return true;
		}
		return false;
	}
}
