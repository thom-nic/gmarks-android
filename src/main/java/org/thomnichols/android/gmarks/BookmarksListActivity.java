	package org.thomnichols.android.gmarks;

import org.thomnichols.android.gmarks.R;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.ContentUris;
import android.content.Intent;
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
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemLongClickListener;

public class BookmarksListActivity extends ListActivity {

    private static final String TAG = "BOOKMARKS LIST";

    // Menu item ids
    public static final int CONTEXT_MENU_ITEM_DELETE = Menu.FIRST;
    public static final int CONTEXT_MENU_ITEM_EDIT = Menu.FIRST + 1;
    
    static final String KEY_BOOKMARKS_SORT_PREF = "bookmarks_sort_by";
    static final int SORT_MODIFIED = 0;
    static final int SORT_TITLE = 1;
    protected int currentSort = SORT_MODIFIED;

    /**
     * The columns we are interested in from the database
     */
    private static final String[] PROJECTION = new String[] {
            Bookmark.Columns._ID, // 0
            Bookmark.Columns.TITLE, // 1
            Bookmark.Columns.URL, // 2
            Bookmark.Columns.HOST, // 3
    };

    // the cursor index of the url column
    private static final int COLUMN_INDEX_URL = 2;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        // bind an action for long-press (not a context menu)
        getListView().setOnItemLongClickListener(this.longClickListener);
        
        this.currentSort = PreferenceManager.getDefaultSharedPreferences(this)
				.getInt(KEY_BOOKMARKS_SORT_PREF, SORT_MODIFIED);

        Intent intent = getIntent();
        Uri uri = intent.getData();
        
        if ( Intent.ACTION_PICK.equals(intent.getAction()) ) {
        	setTitle(R.string.choose_bookmark);
        }
        else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            uri = uri.buildUpon().appendPath("search")
            	.appendQueryParameter("q", query).build();
            intent.setData( uri );
            this.setTitle("GMarks search results for '" + query + "'");
        }
        else {
            String labelName = uri.getQueryParameter("label");
            if ( labelName != null ) this.setTitle("Bookmarks for label '" + labelName + "'");
        }
        
        Cursor cursor = getCursorFromIntent(intent);
        // Used to map notes entries from the database to views
        SimpleCursorAdapter adapter = new SimpleCursorAdapter( this, 
        		R.layout.bookmarkslist_item, cursor,
                new String[] { Bookmark.Columns.TITLE, Bookmark.Columns.HOST }, 
                new int[] { R.id.title, R.id.host } );
        setListAdapter(adapter);
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
    	super.onNewIntent(intent);
    	((SimpleCursorAdapter)this.getListAdapter()).changeCursor(
    			getCursorFromIntent(intent) );
    }
    
    protected Cursor getCursorFromIntent(Intent intent) {
        if (intent.getData() == null) intent.setData(Bookmark.CONTENT_URI);
        
        String sort = currentSort == SORT_MODIFIED ? 
        		Bookmark.Columns.SORT_MODIFIED : Bookmark.Columns.SORT_TITLE;

		return managedQuery( getIntent().getData(), PROJECTION, null, null, sort );
    }
        
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.bookmark_list, menu);

        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        final boolean haveItems = getListAdapter().getCount() > 0;

        if ( haveItems ) {
//    		menu.findItem(R.id.menu_delete).setVisible( true );
        }
        else menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
        
		menu.findItem(R.id.menu_sort_title).setVisible(
				this.currentSort != SORT_TITLE );
		menu.findItem(R.id.menu_sort_date).setVisible(
				this.currentSort != SORT_MODIFIED );

        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch ( item.getItemId() ) {
        case R.id.menu_add:
        	Intent intent = new Intent(Intent.ACTION_INSERT);
        	intent.setType(Bookmark.CONTENT_ITEM_TYPE);
        	String label = getIntent().getData().getQueryParameter("label");
        	if ( label != null ) intent.putExtra("label", label);
            // Launch activity to insert a new item
            startActivity(intent);
            return true;
        case R.id.menu_sort_title:
            this.currentSort = SORT_TITLE;
            PreferenceManager.getDefaultSharedPreferences(this)
            	.edit().putInt(KEY_BOOKMARKS_SORT_PREF, SORT_TITLE).commit();
        	((SimpleCursorAdapter)getListAdapter()).changeCursor(
        			getCursorFromIntent(getIntent()) );
        	break;
        case R.id.menu_sort_date:
            this.currentSort = SORT_MODIFIED;
            PreferenceManager.getDefaultSharedPreferences(this)
            	.edit().putInt(KEY_BOOKMARKS_SORT_PREF, SORT_MODIFIED).commit();
        	((SimpleCursorAdapter)getListAdapter()).changeCursor(
        			getCursorFromIntent(getIntent()) );
        	break;
        case R.id.menu_delete:
        	Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());
        	startActivity( new Intent(Intent.ACTION_DELETE, uri) );
        	break;
        case R.id.menu_sync:
        	Log.d(TAG, "Starting sync...");
        	Toast.makeText(this, "Starting sync...", Toast.LENGTH_SHORT).show();
        	// TODO only sync bookmarks for this label?
        	new RemoteSyncTask(this).execute();
        }
        return super.onOptionsItemSelected(item);
    }
        
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);
    	String action = getIntent().getAction();
        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {
            // The caller is waiting for us to return a note selected by
            // the user.  The have clicked on one, so return it now.
            setResult(RESULT_OK, new Intent().setData(uri));
            finish();
        } else {
        	String bookmarkURL = ((CursorWrapper)l.getItemAtPosition(position))
        		.getString(COLUMN_INDEX_URL);
        	startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(bookmarkURL)));
        }
    }
    
    protected OnItemLongClickListener longClickListener = new OnItemLongClickListener() {
		public boolean onItemLongClick(AdapterView<?> adapter, View v, int position, long id) {
			Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);	        
            startActivity(new Intent(Intent.ACTION_EDIT, uri));
			return false;
		}    	
        
        //TODO after starting the activity, if the item was updated or deleted, this 
        // view will need to be refreshed!
	};
}
