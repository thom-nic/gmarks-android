package org.thomnichols.android.gmarks;

import org.thomnichols.android.gmarks.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class BookmarkViewActivity extends Activity {
	private static String TAG = "BookmarkView";
	
    private static final String[] PROJECTION = new String[] {
	        Bookmark.Columns._ID, // 0
	        Bookmark.Columns.TITLE, // 1
	        Bookmark.Columns.URL, // 2 
	        Bookmark.Columns.DESCRIPTION  // 3
	};
    private static final int COLUMN_INDEX_TITLE = 1;
    private static final int COLUMN_INDEX_URL = 2;
    private static final int COLUMN_INDEX_DESCRIPTION = 3;
    
	private Uri mUri;
	private Cursor mCursor;
	private EditText titleField;
	private EditText urlField;
	private EditText descriptionField;
	
	ProgressDialog waitDialog;
	 
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();

        final String action = intent.getAction();
        if (Intent.ACTION_EDIT.equals(action)) {
            // Requested to edit: set that state, and the data being edited.
            mUri = intent.getData();
        }
        else if (Intent.ACTION_VIEW.equals(action)) {
        	// if viewing, immediately open the HTTP URL associated with this item.
            mUri = intent.getData();
        	this.mCursor = managedQuery(mUri, PROJECTION, null, null, null);
        	if ( ! this.mCursor.moveToFirst() ) {
        		Log.w(TAG, "Can't find bookmark for ID: " + mUri);
        		finish();
        		return;
        	}
        	String bookmarkURL = this.mCursor.getString(COLUMN_INDEX_URL);
        	startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(bookmarkURL)));
        	finish();
        }
        else {
            // Whoops, unknown action!  Bail.
            Log.e(TAG, "Unknown action, exiting");
            finish();
            return;
        }

        // Set the layout for this activity.  You can find it in res/layout/note_editor.xml
        setContentView(R.layout.bookmark_view);
        
        this.titleField = (EditText) findViewById(R.id.title);
        this.urlField = (EditText) findViewById(R.id.url);
        this.descriptionField = (EditText) findViewById(R.id.description);
        
        // Get the note!
        this.mCursor = managedQuery(mUri, PROJECTION, null, null, null);
        
		Button loginButton = (Button)findViewById(R.id.saveBtn);
		loginButton.setOnClickListener( this.saveClicked );
    }
	
    @Override
    protected void onResume() {
        super.onResume();

        // If we didn't have any trouble retrieving the data, it is now
        // time to get at the stuff.
        if (mCursor != null) {
            // Make sure we are at the one and only row in the cursor.
            mCursor.moveToFirst();
            
            setTitle("Edit Bookmark");

            // This is a little tricky: we may be resumed after previously being
            // paused/stopped.  We want to put the new text in the text view,
            // but leave the user where they were (retain the cursor position
            // etc).  This version of setText does that for us.
            this.titleField.setTextKeepState( mCursor.getString(COLUMN_INDEX_TITLE) );
            this.urlField.setTextKeepState( mCursor.getString(COLUMN_INDEX_URL) );
            this.descriptionField.setTextKeepState( mCursor.getString(COLUMN_INDEX_DESCRIPTION) );   
        }
        else {
            setTitle("Error");
            this.titleField.setText("Cursor was null!");
        }
    }
    
    class UpdateBookmarkTask extends AsyncTask<Void,Void,Boolean> {
		@Override protected Boolean doInBackground(Void... arg0) {
			return true;
		}

		@Override
		protected void onPostExecute(Boolean updateSuccess ) {
			BookmarkViewActivity.this.waitDialog.dismiss();
			
			if ( updateSuccess ) {
				// update the database
				
				BookmarkViewActivity.this.finish();
			}
			else Toast.makeText(BookmarkViewActivity.this, "Save failed", Toast.LENGTH_LONG);
		}
    }
    
    private View.OnClickListener saveClicked = new View.OnClickListener() {
		public void onClick(View v) {
			Log.d(TAG, "Saving bookmark ID: " + mUri + " ...");
			
	        BookmarkViewActivity.this.waitDialog = ProgressDialog.show(
	        		BookmarkViewActivity.this, "", 
	                "Saving ...", true );
	        
	        new UpdateBookmarkTask().execute();
		}
	};
}
