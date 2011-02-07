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
        
        this.currentSort = Prefs.get(this).getInt(Prefs.KEY_BOOKMARKS_SORT_PREF, SORT_MODIFIED);

        setTitle(R.string.bookmarks_activity);
        
        final Intent intent = getIntent();
        if (intent.getData() == null) intent.setData(Bookmark.CONTENT_URI);
        Uri uri = intent.getData();
        final String action = intent.getAction();
        
        if ( Intent.ACTION_PICK.equals(action) ) {
        	setTitle(R.string.choose_bookmark);
        }
        else if ( Intent.ACTION_VIEW.equals(action) && uri.getScheme().startsWith("http") ) {
        	// this was the result of a search where an item was chosen; 
        	// just start the view activity for that URL.
        	startActivity(new Intent(Intent.ACTION_VIEW, uri));
        	finish();
        	return;
        }
        else if (Intent.ACTION_SEARCH.equals(action)) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            uri = uri.buildUpon().appendPath("search")
            	.appendQueryParameter("q", query).build();
            intent.setData( uri );
            this.setTitle( getString(R.string.search_results_title, query) );
        }
        else {
            String labelName = uri.getQueryParameter("label");
            if ( labelName != null )
            	this.setTitle( getString(R.string.label_results_title, labelName) );
        }
        
        Cursor cursor = getCursorFromIntent(intent);
        // Used to map notes entries from the database to views
        SimpleCursorAdapter adapter = new SimpleCursorAdapter( this, 
        		R.layout.bookmarkslist_item, cursor,
                new String[] { Bookmark.Columns.TITLE, Bookmark.Columns.HOST }, 
                new int[] { R.id.title, R.id.host } );
        setListAdapter(adapter);
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

		if ( Hardware.hasSearchButton() )
			menu.findItem(R.id.menu_search).setVisible(false);
		
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
        case R.id.menu_search:
        	this.onSearchRequested();
        	break;
        case R.id.menu_sort_title:
            this.currentSort = SORT_TITLE;
            Prefs.edit(this).putInt(Prefs.KEY_BOOKMARKS_SORT_PREF, SORT_TITLE).commit();
        	((SimpleCursorAdapter)getListAdapter()).changeCursor(
        			getCursorFromIntent(getIntent()) );
        	break;
        case R.id.menu_sort_date:
            this.currentSort = SORT_MODIFIED;
            Prefs.edit(this).putInt(Prefs.KEY_BOOKMARKS_SORT_PREF, SORT_MODIFIED).commit();
        	((SimpleCursorAdapter)getListAdapter()).changeCursor(
        			getCursorFromIntent(getIntent()) );
        	break;
        case R.id.menu_delete:
        	Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());
        	startActivity( new Intent(Intent.ACTION_DELETE, uri) );
        	break;
        case R.id.menu_sync:
        	Log.d(TAG, "Starting sync...");
        	Toast.makeText(this, R.string.sync_begin_msg, Toast.LENGTH_SHORT).show();
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
