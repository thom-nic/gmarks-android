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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.MultiAutoCompleteTextView;
import android.widget.SimpleCursorAdapter;

/**
 * TODO add labels list
 * @author tnichols
 */
public class BookmarkViewActivity extends Activity implements OnClickListener {
	private static String TAG = "BookmarkView";
	
    private static final String[] PROJECTION = new String[] {
	        Bookmark.Columns._ID, // 0
	        Bookmark.Columns.GOOGLEID, // 1
	        Bookmark.Columns.THREAD_ID, // 1
	        Bookmark.Columns.TITLE, // 2
	        Bookmark.Columns.URL, // 3
	        Bookmark.Columns.DESCRIPTION,  // 4
	        Bookmark.Columns.LABELS  // 4
	};
    private static final String[] LABEL_PROJECTION = new String[] {
    	Label.Columns._ID,
    	Label.Columns.TITLE };

    private static final int COLUMN_INDEX_ID = 0;
    private static final int COLUMN_INDEX_GOOGLEID = 1;
    private static final int COLUMN_INDEX_THREADID = 2;
    private static final int COLUMN_INDEX_TITLE = 3;
    private static final int COLUMN_INDEX_URL = 4;
    private static final int COLUMN_INDEX_DESCRIPTION = 5;
    private static final int COLUMN_INDEX_LABELS = 6;
    
	private Uri mUri;
	private Cursor cursor;
	
	Bookmark bookmark = null;
	
	private EditText titleField;
	private EditText urlField;
	private EditText descriptionField;
	private MultiAutoCompleteTextView labelsField;
	
	ProgressDialog waitDialog;
	 
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();

        mUri = intent.getData();
        final String action = intent.getAction();
        
        if (Intent.ACTION_VIEW.equals(action)) {
        	// if viewing, immediately open the HTTP URL associated with this item.
        	// TODO should check to verify that the intent Uri points to a single item?
        	this.cursor = managedQuery(mUri, PROJECTION, null, null, null);
        	if ( ! this.cursor.moveToFirst() ) {
        		Log.w(TAG, "Can't find bookmark for ID: " + mUri);
        		finish();
        		return;
        	}
        	String bookmarkURL = this.cursor.getString(COLUMN_INDEX_URL);
        	startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(bookmarkURL)));
        	finish();
        	return;
        }

        // don't set the view until after we've determined that we're not launching the browser.
        setContentView(R.layout.bookmark_view);
    	setTitle(R.string.new_bookmark);
        
        if (Intent.ACTION_EDIT.equals(action) || 
        		Intent.ACTION_PICK.equals(action) ) {
        	this.cursor = managedQuery(mUri, PROJECTION, null, null, null);
        	
            // Requested to edit: set that state, and the data being edited.
        	setTitle(R.string.edit_bookmark);
            ((Button)findViewById(R.id.saveBtn)).setText(R.string.btn_update);
        }
        else if (Intent.ACTION_INSERT.equals(action)) {
        	String label = intent.getStringExtra("label");
        	if ( label != null && !"^none".equals(label) )
        		((EditText)findViewById(R.id.labels)).setText(label + ", ");
        	findViewById(R.id.deleteBtn).setVisibility(View.GONE);
        }
        else if (Intent.ACTION_SEND.equals(action)) {
        	// TODO possibility that this is not a link???
        	String title = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        	String url = intent.getStringExtra(Intent.EXTRA_TEXT);
//        	Bitmap favico = intent.getParcelableExtra(Browser.EXTRA_SHARE_FAVICON);
            ((EditText) findViewById(R.id.title)).setText(title);
            ((EditText) findViewById(R.id.url)).setText(url);
        	findViewById(R.id.deleteBtn).setVisibility(View.GONE);
        }
        else {
            // Whoops, unknown action!  Bail.
            Log.e(TAG, "Unknown action, exiting");
            finish();
            return;
        }

        this.titleField = (EditText) findViewById(R.id.title);
        this.urlField = (EditText) findViewById(R.id.url);
        this.descriptionField = (EditText) findViewById(R.id.description);
        this.labelsField = (MultiAutoCompleteTextView) findViewById(R.id.labels);

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, 
        		R.layout.label_autocomplete_item,
        		managedQuery(Label.CONTENT_URI, LABEL_PROJECTION, null, null, null),
        		new String[] { Label.Columns.TITLE }, 
        		new int[] { R.id.autocomplete_item} );
        adapter.setStringConversionColumn(1); // used for text filtering
        adapter.setFilterQueryProvider(new FilterQueryProvider() {
			public Cursor runQuery(CharSequence constraint) {
				if ( constraint == null || constraint.length() < 1 ) return null;
				String label = constraint.toString();
				label.replaceAll("'", "");
				return managedQuery( Label.CONTENT_URI, LABEL_PROJECTION, 
						"label like '"+label+"%'", null, null);
			}
		});
        labelsField.setAdapter(adapter);
        labelsField.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        
        // Get the bookmark
        Log.d(TAG, "Getting data for: " + mUri);        
        
		((Button)findViewById(R.id.saveBtn)).setOnClickListener( this );
		((Button)findViewById(R.id.deleteBtn)).setOnClickListener( this );
    }
	
    @Override
    protected void onResume() {
        super.onResume();

        // If we didn't have any trouble retrieving the data, it is now
        // time to get at the stuff.
        if (cursor != null) {
            // Make sure we are at the one and only row in the cursor.
            cursor.moveToFirst();
            
            setTitle(R.string.edit_bookmark);
            
            Bookmark b = new Bookmark( cursor.getString(COLUMN_INDEX_GOOGLEID),
            		cursor.getString(COLUMN_INDEX_THREADID),
            		cursor.getString(COLUMN_INDEX_TITLE),
            		cursor.getString(COLUMN_INDEX_URL),
            		null,
            		cursor.getString(COLUMN_INDEX_DESCRIPTION),
            		0, 0 );
            b.set_id(cursor.getLong(COLUMN_INDEX_ID));
            String labels = cursor.getString(COLUMN_INDEX_LABELS);
            b.parseLabels(labels);
            if ( labels != null && labels.length() > 0 ) labels += ", ";

            // This is a little tricky: we may be resumed after previously being
            // paused/stopped.  We want to put the new text in the text view,
            // but leave the user where they were (retain the cursor position
            // etc).  This version of setText does that for us.
            this.titleField.setTextKeepState( b.getTitle() );
            this.urlField.setTextKeepState( b.getUrl() );
            this.descriptionField.setTextKeepState( b.getDescription() );
            this.labelsField.setTextKeepState( labels );
            
            this.bookmark = b;
        }
        else this.bookmark = new Bookmark();
        
        if ( Intent.ACTION_DELETE.equals(getIntent().getAction()) ) {
			Log.d(TAG, "Deleting bookmark ID: " + mUri + " ...");
		
	        new UpdateBookmarkTask(UpdateBookmarkTask.ACTION_DELETE, this.bookmark, this, true) {
	    		@Override protected void onPostExecute(Integer resultCode) {
	    			super.onPostExecute(resultCode);
	    			if ( resultCode ==  RESULT_OK ) 
	    				BookmarkViewActivity.this.finish();
	    		}
	    	}.execute();
        }
    }
        
	public void onClick(View v) {
		int action = UpdateBookmarkTask.ACTION_UPDATE;
		
		// update the bookmark object's values:
		Bookmark newBookmark = new Bookmark( 
				this.bookmark.getGoogleId(), 
				this.bookmark.getThreadId(), 
				this.titleField.getText().toString(),
				this.urlField.getText().toString(),
				null,
				this.descriptionField.getText().toString(),
				0, 0 );
		newBookmark.set_id(this.bookmark.get_id());
		newBookmark.parseLabels(this.labelsField.getText().toString());
		
		if ( v.getId() == R.id.saveBtn ) {
			Log.d(TAG, "Saving bookmark ID: " + mUri + " ...");
			if ( this.bookmark.getGoogleId() == null )
				action = UpdateBookmarkTask.ACTION_NEW;
		}
		else if ( v.getId() == R.id.deleteBtn ) {
			Log.d(TAG, "Deleting bookmark ID: " + mUri + " ...");
			action = UpdateBookmarkTask.ACTION_DELETE;
		}
		
        new UpdateBookmarkTask(action, newBookmark, this, true) {
    		@Override protected void onPostExecute(Integer resultCode) {
    			super.onPostExecute(resultCode);
    			if ( resultCode ==  RESULT_OK ) 
    				BookmarkViewActivity.this.finish();
    		}
    	}.execute();
	}	
}
