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
        
        // If no data was given in the intent (because we were started
        // as a MAIN activity), then use our default content provider.
        Intent intent = getIntent();
        Cursor cursor = getCursorFromIntent(intent);
        Uri uri = intent.getData();
        
        String labelName = uri.getQueryParameter("label");
        if ( labelName != null ) this.setTitle("Bookmarks for label '" + labelName + "'");
        
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
        if (intent.getData() == null) {
            intent.setData(Bookmark.CONTENT_URI);
        }
        
        Uri uri = getIntent().getData();

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            uri = uri.buildUpon().appendPath("search")
            	.appendQueryParameter("q", query).build();
            intent.setData( uri );
            this.setTitle("GMarks search results for '" + query + "'");
        }

        // Perform a managed query. The Activity will handle closing and requerying the cursor
        // when needed.
        Log.i(TAG,"Intent URI: " + uri );
        return managedQuery( uri, PROJECTION, null, null,
                Bookmark.Columns.DEFAULT_SORT_ORDER );
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

        if (haveItems) {
            // This is the selected item.
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());

            // Build menu...  always starts with the EDIT action...
            Intent[] specifics = new Intent[1];
            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);
            MenuItem[] items = new MenuItem[1];

            // ... is followed by whatever other actions are available...
            Intent intent = new Intent(null, uri);
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
            menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0, null, specifics, intent, 0,
                    items);

            // Give a shortcut to the edit action.
            if (items[0] != null) {
                items[0].setShortcut('1', 'e');
            }
        } else {
            menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
        }

        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.addItem:
            // Launch activity to insert a new item
            startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
            return true;
        case R.id.sync:
        	Log.d(TAG, "Starting sync...");
        	Toast.makeText(this, "Starting sync...", Toast.LENGTH_SHORT);
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
	};
}
