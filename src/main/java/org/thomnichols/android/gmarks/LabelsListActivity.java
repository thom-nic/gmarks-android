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

import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class LabelsListActivity extends ListActivity implements OnClickListener {
	static final String TAG = "GMARKS LABELS";
	
    private static final String[] PROJECTION = new String[] {
        Label.Columns._ID, // 0
        Label.Columns.TITLE, // 1
        Label.Columns.COUNT, // 2
    };
    
    static final int LOGIN_ACTIVITY_RESULT = 0x01;
    
    protected static final int COLUMN_INDEX_TITLE = 1;
    static final int SORT_ALPHA= 1;
    static final int SORT_COUNT= 2;
    
    protected int currentSort = SORT_ALPHA;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        setContentView(R.layout.labels_list_view);
        setTitle(R.string.labels_activity);
        
    	SharedPreferences prefs = Prefs.get(this);

    	// If no data was given in the intent (because we were started
        // as a MAIN activity), then use our default content provider.
        Intent intent = getIntent();
        if (intent.getData() == null) intent.setData(Label.CONTENT_URI);
        
        if (Intent.ACTION_MAIN.equals(intent.getAction())) {
        	// start up the background service if necessary.
        	if ( prefs.getBoolean(Prefs.KEY_BACKGROUND_SYNC_ENABLED, false) ) {
        		Log.d(TAG,"Starting background sync service...");
        		Intent service = new Intent(this, BackgroundService.class);
    			service.setAction( Intent.ACTION_RUN );
        		startService(service);
        	}
        }
        
    	if (Intent.ACTION_PICK.equals(intent.getAction()) ) {
    		// don't show 'all bookmarks' option:
    		if ( ! intent.getBooleanExtra("allBookmarks", false) )
    			findViewById(R.id.allListItems).setVisibility(View.GONE);
    		setTitle(R.string.choose_label);
    	}
        
        Log.i(TAG,"Intent URI: " + getIntent().getData() );
        this.currentSort = prefs.getInt(Prefs.KEY_LABELS_SORT_PREF, SORT_ALPHA);
        String sort = currentSort == SORT_ALPHA ? 
        		Label.Columns.SORT_ALPHA : Label.Columns.SORT_COUNT;
        Cursor cursor = managedQuery( getIntent().getData(), PROJECTION, null, null, sort );
        
        // Used to map labels from the database to views
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
        		this, R.layout.labels_list_item, cursor,
                new String[] { Label.Columns.TITLE, "count(label_id)" }, 
                new int[] { R.id.title, R.id.count });
        adapter.setStringConversionColumn(1); // used for text filtering
        adapter.setFilterQueryProvider(new FilterQueryProvider() {
			public Cursor runQuery(CharSequence constraint) {
				String label = constraint.toString();
				label.replaceAll("'", "");
		        String sort = currentSort == SORT_ALPHA ? 
		        		Label.Columns.SORT_ALPHA : Label.Columns.SORT_COUNT;
				return managedQuery( getIntent().getData(), PROJECTION, 
						"label like '"+label+"%'", null, sort);
			}
		});
        setListAdapter(adapter);
        
        findViewById(R.id.allListItems).setOnClickListener(this);

        ((Button)findViewById(R.id.syncBtn)).setOnClickListener(this);
        ((Button)findViewById(R.id.loginBtn)).setOnClickListener(this);
        ((TextView)findViewById(R.id.welcome_msg)).setText(
        		Html.fromHtml(getString(R.string.welcome_msg)) );
    }
    
    @Override
    protected void onResume() {
        if ( BookmarksQueryService.getInstance().isAuthInitialized() ) { 
        	findViewById(R.id.syncBtn).setVisibility(View.VISIBLE);
        	findViewById(R.id.loginBtn).setVisibility(View.GONE);
        }
        else {
        	findViewById(R.id.loginBtn).setVisibility(View.VISIBLE);
        	findViewById(R.id.syncBtn).setVisibility(View.GONE);
        }
        
        findViewById(R.id.allListItems).setVisibility(
        		getListView().getChildCount() < 1 ? View.GONE : View.VISIBLE );

    	super.onResume();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if ( requestCode == LOGIN_ACTIVITY_RESULT && resultCode == RESULT_OK ) {
    		// Login successful.
    		// nothing to do here since onResume should toggle the correct UI state.
    	}
    };
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	super.onListItemClick(l, v, position, id);
    	String action = getIntent().getAction();
    	String labelText = ((CursorWrapper)l.getItemAtPosition(position))
    		.getString(COLUMN_INDEX_TITLE);
    	
    	if (Intent.ACTION_PICK.equals(action) ) {
            // The caller is waiting for us to return a label selected
        	Intent result = new Intent();
    		result.setData( ContentUris.withAppendedId(getIntent().getData(), id) );
    		result.putExtra("label", labelText); // most often, they want the label text
    		Log.d(TAG, "Selected label: "+ labelText);
    		Log.d(TAG, "Setting result" + result);
            setResult(RESULT_OK, result);
            finish();
        } 
    	else {
        	// user has selected a label for which to show all bookmarks
        	Uri queryUri = Bookmark.CONTENT_URI.buildUpon()
        		.appendQueryParameter("label_id", ""+id)
        		.appendQueryParameter("label", labelText)
        		.build();
        	startActivity(new Intent(Intent.ACTION_VIEW, queryUri));
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.label_list, menu);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	super.onPrepareOptionsMenu(menu);
    	
        // if not logged in, don't show UI options!
        if ( ! BookmarksQueryService.getInstance().isAuthInitialized() ) return false;
    	
		menu.findItem(R.id.menu_sort_alpha).setVisible(
				this.currentSort != SORT_ALPHA );
		menu.findItem(R.id.menu_sort_count).setVisible(
				this.currentSort != SORT_COUNT );
		
		Configuration hwConfig = getResources().getConfiguration();
		if (hwConfig.keyboard == Configuration.KEYBOARD_QWERTY &&
				hwConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO)
			// if HW keyboard visible, don't need to show the 'go to label' menu
			menu.findItem(R.id.menu_go_to).setVisible(false);
		
		if ( Hardware.hasSearchButton() )
			menu.findItem(R.id.menu_search).setVisible(false);
		
        if ( Intent.ACTION_PICK.equals(getIntent().getAction()) ) {
        	// hide menu actions that divert away from this picker:
        	menu.findItem(R.id.menu_add).setVisible(false);
        	menu.findItem(R.id.menu_search).setVisible(false);
        	menu.findItem(R.id.menu_settings).setVisible(false);
        	menu.findItem(R.id.menu_sync).setVisible(false);
        }
		
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_add:
        	startActivity(new Intent(Intent.ACTION_INSERT).setType(Bookmark.CONTENT_ITEM_TYPE));
        	break;
        case R.id.menu_go_to:
//        	getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        	InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        	mgr.showSoftInput(getListView(), 0);
        	break;
        case R.id.menu_sync:
        	Log.d(TAG, "Starting sync...");
        	Toast.makeText(this, R.string.sync_begin_msg, Toast.LENGTH_SHORT).show();
        	new RemoteSyncTask(this).execute();
        	break;
        case R.id.menu_settings:
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        case R.id.menu_search:
        	this.onSearchRequested();
        	break;
        case R.id.menu_sort_alpha:
            this.currentSort = SORT_ALPHA; 
            Prefs.edit(this).putInt(Prefs.KEY_LABELS_SORT_PREF, SORT_ALPHA).commit();
        	((SimpleCursorAdapter)getListAdapter()).changeCursor(
        			managedQuery( getIntent().getData(), PROJECTION, 
    						null, null, Label.Columns.SORT_ALPHA) );
        	break;
        case R.id.menu_sort_count:
            this.currentSort = SORT_COUNT;
            Prefs.edit(this).putInt(Prefs.KEY_LABELS_SORT_PREF, SORT_COUNT).commit();
        	((SimpleCursorAdapter)getListAdapter()).changeCursor(
        			managedQuery( getIntent().getData(), PROJECTION, 
    						null, null, Label.Columns.SORT_COUNT) );
        	break;
	    case R.id.menu_logout:
	    	Log.d(TAG, "Logging out...");
	    	BookmarksQueryService.getInstance().clearAuthCookies();
	    	Toast.makeText(this, R.string.logged_out_msg, Toast.LENGTH_SHORT).show();
	    	break;
		case R.id.menu_login:
			Log.d(TAG, "Logging in...");
			Toast.makeText(this, R.string.logging_in_msg, Toast.LENGTH_SHORT).show();
			startActivity( new Intent(this,WebViewLoginActivity.class) );
			break;
		}
        return super.onOptionsItemSelected(item);
    }

	public void onClick(View v) {
		final int viewID = v.getId();
		if ( viewID == R.id.syncBtn ) {
			Log.d(TAG, "Starting sync...");
        	Toast.makeText(this, R.string.sync_begin_msg, Toast.LENGTH_SHORT).show();
        	new RemoteSyncTask(this).execute();
		}
		
		else if ( viewID == R.id.loginBtn ) {
			startActivityForResult(
					new Intent("org.thomnichols.gmarks.action.LOGIN"), 
					LOGIN_ACTIVITY_RESULT );
		}

		else if ( viewID == R.id.allListItems ) {
			Intent intent = getIntent();
			String action = intent.getAction();
			Log.d(TAG, "All items: " + action );
			if (Intent.ACTION_PICK.equals(action) ) {
				// The caller is waiting for us to return a label selected,
				// in this case, 'all bookmarks'
				Intent result = new Intent();
				result.setData( Bookmark.CONTENT_URI );
				Log.d(TAG, "Setting result" + result);
				setResult(RESULT_OK, result);
				finish();
	        }
			else {
				startActivity(new Intent(Intent.ACTION_VIEW).setType(Bookmark.CONTENT_TYPE));
			}
		}
	}
}
