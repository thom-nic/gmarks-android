package org.thomnichols.android.gmarks;

import org.thomnichols.android.gmarks.R;

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
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FilterQueryProvider;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class LabelsListActivity extends ListActivity implements OnClickListener {
	static final String TAG = "GMARKS LABELS";
	
    private static final String[] PROJECTION = new String[] {
        Label.Columns._ID, // 0
        Label.Columns.TITLE, // 1
        Label.Columns.COUNT, // 2
    };
    
    protected static final int COLUMN_INDEX_TITLE = 1;
    static final int SORT_ALPHA= 1;
    static final int SORT_COUNT= 2;
    static final String KEY_LABELS_SORT_PREF = "labels_sort_by";
    
    protected int currentSort = SORT_ALPHA;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        setContentView(R.layout.labels_list_view);
        ((LinearLayout)findViewById(R.id.allListItems)).setOnClickListener(this);
        setTitle(R.string.labels_activity);

        // If no data was given in the intent (because we were started
        // as a MAIN activity), then use our default content provider.
        Intent intent = getIntent();
        if (intent.getData() == null) intent.setData(Label.CONTENT_URI);
        
        if (Intent.ACTION_MAIN.equals(intent.getAction())) {
        	// start up the background service if necessary.
        	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        	if ( prefs.getBoolean(SettingsActivity.KEY_BACKGROUND_SYNC_ENABLED, false) ) {
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
        this.currentSort = PreferenceManager.getDefaultSharedPreferences(this)
        		.getInt(KEY_LABELS_SORT_PREF, SORT_ALPHA);
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
        
        ((Button)findViewById(R.id.syncBtn)).setOnClickListener(this);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if ( requestCode != resultCode ) {
    		Log.w(TAG, "Did not get expected login result code: " + resultCode );
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
        // if picker, don't show UI options!
        if ( Intent.ACTION_PICK.equals(getIntent().getAction()) ) return true;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.label_list, menu);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	super.onPrepareOptionsMenu(menu);
		menu.findItem(R.id.menu_sort_alpha).setVisible(
				this.currentSort != SORT_ALPHA );
		menu.findItem(R.id.menu_sort_count).setVisible(
				this.currentSort != SORT_COUNT );
		
		Configuration hwConfig = getResources().getConfiguration();
		if (hwConfig.keyboard == Configuration.KEYBOARD_QWERTY &&
				hwConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO)
			// if HW keyboard visible, don't need to show the 'go to label' menu
			menu.findItem(R.id.menu_go_to).setVisible(false);
    	// TODO decide whether or not to make login/logout options visible
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
        	mgr.showSoftInput(getListView(), InputMethodManager.SHOW_FORCED);
        	break;
        case R.id.menu_sync:
        	Log.d(TAG, "Starting sync...");
        	Toast.makeText(this, "Starting sync...", Toast.LENGTH_SHORT).show();
        	new RemoteSyncTask(this).execute();
        	break;
        case R.id.menu_settings:
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        case R.id.menu_sort_alpha:
            this.currentSort = SORT_ALPHA; 
            PreferenceManager.getDefaultSharedPreferences(this)
            	.edit().putInt(KEY_LABELS_SORT_PREF, SORT_ALPHA).commit();
        	((SimpleCursorAdapter)getListAdapter()).changeCursor(
        			managedQuery( getIntent().getData(), PROJECTION, 
    						null, null, Label.Columns.SORT_ALPHA) );
        	break;
        case R.id.menu_sort_count:
            this.currentSort = SORT_COUNT;
            PreferenceManager.getDefaultSharedPreferences(this)
            	.edit().putInt(KEY_LABELS_SORT_PREF, SORT_COUNT).commit();
        	((SimpleCursorAdapter)getListAdapter()).changeCursor(
        			managedQuery( getIntent().getData(), PROJECTION, 
    						null, null, Label.Columns.SORT_COUNT) );
        	break;
	    case R.id.menu_logout:
	    	Log.d(TAG, "Logging out...");
	    	BookmarksQueryService.getInstance().clearAuthCookies();
	    	Toast.makeText(this, "You are now logged out.", Toast.LENGTH_SHORT).show();
	    	break;
		case R.id.menu_login:
			Log.d(TAG, "Logging in...");
			Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show();
			break;
		}
        return super.onOptionsItemSelected(item);
    }

	public void onClick(View v) {
		if ( v.getId() == R.id.syncBtn ) {
			Log.d(TAG, "Starting sync...");
        	Toast.makeText(this, "Starting sync...", Toast.LENGTH_SHORT).show();
        	new RemoteSyncTask(this).execute();
		}

		else if ( v.getId() == R.id.allListItems ) {
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
