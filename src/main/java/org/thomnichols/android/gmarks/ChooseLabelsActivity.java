package org.thomnichols.android.gmarks;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class ChooseLabelsActivity extends ListActivity {
	static final String TAG = "GMARKS LABELS MULTI CHOOSER";

    private static final String[] PROJECTION = new String[] {
        Label.Columns._ID, // 0
        Label.Columns.TITLE, // 1
//        Label.Columns.COUNT // 2
    };
    
    public static final String EXTRA_LABEL_IDS = "gmarks.extra.label.ids";
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setTitle("Choose Labels");
		setContentView(R.layout.labels_chooser_view);
        
		final Intent intent = getIntent();
        if (intent.getData() == null) intent.setData(Label.CONTENT_URI);
        Log.d(TAG,"Intent URI: " + intent.getData() );
		
        Cursor cursor = managedQuery( intent.getData(), PROJECTION, null, null, null );
        
        // Used to map labels from the database to views
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
        		this, android.R.layout.simple_list_item_multiple_choice, cursor,
                new String[] { Label.Columns.TITLE }, //, "count(label_id)" }, 
//                new int[] { R.id.title, R.id.count });
        		new int[] { android.R.id.text1 });
/*        adapter.setStringConversionColumn(1); // used for text filtering
        adapter.setFilterQueryProvider(new FilterQueryProvider() {
			public Cursor runQuery(CharSequence constraint) {
				String label = constraint.toString();
//		        String sort = currentSort == SORT_ALPHA ? 
//		        		Label.Columns.SORT_ALPHA : Label.Columns.SORT_COUNT;
		        String sort = Label.Columns.SORT_ALPHA;
				return managedQuery( intent.getData(), PROJECTION, 
						"label like ?", new String[]{label+"%"}, sort);
			}
		}); */
        setListAdapter(adapter);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.choose_labels, menu);
        return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final ListView list = getListView();
		switch ( item.getItemId() ) {
		case R.id.menu_select_all:
			for ( int i=0; i< getListAdapter().getCount(); i++ )
				list.setItemChecked(i, true);
			break;
		case R.id.menu_select_none:
			for ( int i=0; i< getListAdapter().getCount(); i++ )
				list.setItemChecked(i, false);
			break;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onBackPressed() {
		Intent resultData = getIntent();
		resultData.putExtra(EXTRA_LABEL_IDS, this.getListView().getCheckedItemIds());
		setResult(RESULT_OK, resultData);
		super.onBackPressed();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
//		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
	}
}
