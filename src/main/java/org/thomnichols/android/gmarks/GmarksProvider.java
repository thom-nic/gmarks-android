package org.thomnichols.android.gmarks;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.thomnichols.android.gmarks.thirdparty.ArrayUtils;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.LiveFolders;
import android.text.TextUtils;
import android.util.Log;

public class GmarksProvider extends ContentProvider {

	static final String TAG = "GMARKS PROVIDER";
	static String DB_NAME = "gmarks_sync.db";
	static String COOKIES_TABLE_NAME = "auth_cookies";
	static String BOOKMARKS_TABLE_NAME = "bookmarks";
	static String LABELS_TABLE_NAME = "labels";
	static String BOOKMARK_LABELS_TABLE_NAME = "bookmark_labels";
	
    private static Map<String, String> bookmarksProjectionMap;
    private static Map<String, String> labelsProjectionMap;
    private static Map<String, String> sLiveFolderProjectionMap;

    private static final int BOOKMARKS_URI = 1;
    private static final int BOOKMARK_ID_URI = 2;
    private static final int BOOKMARK_SEARCH_URI = 6;
    private static final int LABELS_URI = 3;
//    private static final int LABELS_ID_URI = 4;
    private static final int LIVE_FOLDER_BOOKMARKS_URI = 5;

    private static final UriMatcher sUriMatcher;

    private DatabaseHelper dbHelper = null;
	
    @Override
    public boolean onCreate() {
    	dbHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, 
    		String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

//        Log.d(TAG, "Managed query: " + uri);
        String groupBy = null;
        String orderBy = null;
//        String limit = null;
        switch (sUriMatcher.match(uri)) {
        case BOOKMARKS_URI:
            qb.setTables(BOOKMARKS_TABLE_NAME);
            qb.setProjectionMap(bookmarksProjectionMap);
            
            String labelID = uri.getQueryParameter("label_id");
            if ( labelID != null ) {
                qb.setTables("bookmarks join bookmark_labels on bookmarks._id = bookmark_labels.bookmark_id");
                qb.appendWhere("bookmark_labels.label_id=?");
                selectionArgs = (String[])ArrayUtils.addAll(selectionArgs, new String[]{labelID});
            }
            break;

        case BOOKMARK_SEARCH_URI:
            String query = uri.getQueryParameter("q");
            if ( query != null ) {
            	qb.setTables("bookmarks join bookmarks_FTS on bookmarks._id = bookmarks_FTS.docid");
            	qb.appendWhere("bookmarks_FTS MATCH ?");
                selectionArgs = (String[])ArrayUtils.addAll(selectionArgs, new String[]{query});
            }
            else if ( selectionArgs == null || selectionArgs.length < 1 )
            	throw new IllegalArgumentException("No search criteria given for query!");
            break;
            
        case BOOKMARK_ID_URI:
            qb.setTables(BOOKMARKS_TABLE_NAME);
            qb.setProjectionMap(bookmarksProjectionMap);
            qb.appendWhere(Bookmark.Columns._ID + "=" + uri.getPathSegments().get(1));
            break;

        case LABELS_URI:
            qb.setTables("labels join bookmark_labels on labels._id = bookmark_labels.label_id" );
            groupBy = "label";
            sortOrder = Label.Columns.DEFAULT_SORT_ORDER;
            qb.setProjectionMap(labelsProjectionMap);
            break;

        case LIVE_FOLDER_BOOKMARKS_URI:
            qb.setTables(BOOKMARKS_TABLE_NAME);
            qb.setProjectionMap(sLiveFolderProjectionMap);
            String labelId = uri.getQueryParameter("label_id");
            if ( labelId != null ) {
                qb.setTables("bookmarks join bookmark_labels on bookmarks._id = bookmark_labels.bookmark_id");
                qb.appendWhere("bookmark_labels.label_id=?");
                selectionArgs = (String[])ArrayUtils.addAll(selectionArgs, new String[]{labelId});
            }
            sortOrder = "modified DESC"; // for some reason this gets set to 'name ASC'
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // If no sort order is specified use the default
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = Bookmark.Columns.DEFAULT_SORT_ORDER;
        } else {
            orderBy = sortOrder;
        }

        // Get the database and run the query
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, groupBy, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        // Validate the requested uri
        if (sUriMatcher.match(uri) != BOOKMARKS_URI) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        Long now = Long.valueOf(System.currentTimeMillis());

        // Make sure that the fields are all set
        if (values.containsKey(Bookmark.Columns.CREATED_DATE) == false) {
            values.put(Bookmark.Columns.CREATED_DATE, now);
        }

        if (values.containsKey(Bookmark.Columns.MODIFIED_DATE) == false) {
            values.put(Bookmark.Columns.MODIFIED_DATE, now);
        }

        if (values.containsKey(Bookmark.Columns.TITLE) == false) {
            Resources r = Resources.getSystem();
            values.put(Bookmark.Columns.TITLE, r.getString(android.R.string.untitled));
        }

        if (values.containsKey(Bookmark.Columns.DESCRIPTION) == false) {
            values.put(Bookmark.Columns.DESCRIPTION, "");
        }
        
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long rowId = db.insert(BOOKMARKS_TABLE_NAME, "", values);
        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(Bookmark.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(noteUri, null);
            return noteUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case BOOKMARKS_URI:
            count = db.delete(BOOKMARKS_TABLE_NAME, where, whereArgs);
            break;

        case BOOKMARK_ID_URI:
            String noteId = uri.getPathSegments().get(1);
            count = db.delete(BOOKMARKS_TABLE_NAME, Bookmark.Columns._ID + "=" + noteId
                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;
            // TODO delete item from text search!
            
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case BOOKMARKS_URI:
            count = db.update(BOOKMARKS_TABLE_NAME, values, where, whereArgs);
            break;

        case BOOKMARK_ID_URI:
            String noteId = uri.getPathSegments().get(1);
            count = db.update(BOOKMARKS_TABLE_NAME, values, Bookmark.Columns._ID + "=" + noteId
                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
        case BOOKMARKS_URI:
        case LIVE_FOLDER_BOOKMARKS_URI:
            return Bookmark.CONTENT_TYPE;
        case BOOKMARK_ID_URI:
            return Bookmark.CONTENT_ITEM_TYPE;
        case LABELS_URI:
        	return Label.CONTENT_TYPE;
//        case LABELS_ID_URI:
//        	return Label.CONTENT_ITEM_TYPE;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(Bookmark.AUTHORITY, "bookmarks", BOOKMARKS_URI);
        sUriMatcher.addURI(Bookmark.AUTHORITY, "bookmarks/search", BOOKMARK_SEARCH_URI);
        sUriMatcher.addURI(Bookmark.AUTHORITY, "bookmarks/#", BOOKMARK_ID_URI);
        sUriMatcher.addURI(Bookmark.AUTHORITY, "labels", LABELS_URI);
//        sUriMatcher.addURI(Bookmark.AUTHORITY, "labels/#", LABELS_ID_URI);
        sUriMatcher.addURI(Bookmark.AUTHORITY, "live_folders/bookmarks", LIVE_FOLDER_BOOKMARKS_URI);

        bookmarksProjectionMap = new HashMap<String, String>();
        bookmarksProjectionMap.put(Bookmark.Columns._ID, Bookmark.Columns._ID);
        bookmarksProjectionMap.put(Bookmark.Columns.GOOGLEID, Bookmark.Columns.GOOGLEID);
        bookmarksProjectionMap.put(Bookmark.Columns.THREAD_ID, Bookmark.Columns.THREAD_ID);
        bookmarksProjectionMap.put(Bookmark.Columns.TITLE, Bookmark.Columns.TITLE);
        bookmarksProjectionMap.put(Bookmark.Columns.HOST, Bookmark.Columns.HOST);
        bookmarksProjectionMap.put(Bookmark.Columns.URL, Bookmark.Columns.URL);
        bookmarksProjectionMap.put(Bookmark.Columns.DESCRIPTION, Bookmark.Columns.DESCRIPTION);
        bookmarksProjectionMap.put(Bookmark.Columns.LABELS, Bookmark.Columns.LABELS);
        bookmarksProjectionMap.put(Bookmark.Columns.CREATED_DATE, Bookmark.Columns.CREATED_DATE);
        bookmarksProjectionMap.put(Bookmark.Columns.MODIFIED_DATE, Bookmark.Columns.MODIFIED_DATE);
        
        labelsProjectionMap = new HashMap<String, String>();
        labelsProjectionMap.put(Label.Columns._ID, Label.Columns._ID);
        labelsProjectionMap.put(Label.Columns.TITLE, Label.Columns.TITLE);
        labelsProjectionMap.put("_count", "count(label_id)");
//        labelsProjectionMap.put(Label.Columns.COUNT, Label.Columns.COUNT);
        
        // Support for Live Folders.
        sLiveFolderProjectionMap = new HashMap<String, String>();
        sLiveFolderProjectionMap.put(LiveFolders._ID, 
        		Bookmark.Columns._ID + " AS " + LiveFolders._ID);
        sLiveFolderProjectionMap.put(LiveFolders.NAME, 
        		Bookmark.Columns.TITLE + " AS " + LiveFolders.NAME);
        sLiveFolderProjectionMap.put(LiveFolders.DESCRIPTION, 
        		Bookmark.Columns.HOST + " AS " + LiveFolders.DESCRIPTION);
        // Add more columns here for more robust Live Folders.
    }
    
    public static class DBException extends Exception {
		private static final long serialVersionUID = 1L;
		public DBException() { super(); }
		public DBException(String arg0, Throwable arg1) { super(arg0, arg1); }
		public DBException(String arg0) { super(arg0); }
		public DBException(Throwable arg0) { super(arg0); }
    }
    
	public static class DatabaseHelper extends SQLiteOpenHelper {
		static final int DB_VERSION = 1;
		
		public DatabaseHelper( Context ctx ) {
			super(ctx, DB_NAME, null, 1 );
		}
		
		static final String[] cookieColumns = { 
			"name", "value", "domain", "path", "expires", "secure"
		};
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("create table " + COOKIES_TABLE_NAME + " ( "
					+ "name varchar(50) not null primary key,"
					+ "value varchar(50) not null,"
					+ "domain varchar(100),"
					+ "path varchar(100),"
					+ "expires long,"
					+ "secure tinyint default 1 )" );
			
			db.execSQL("create table " + BOOKMARKS_TABLE_NAME + " ( "
					+ "_id integer primary key,"
					+ "google_id varchar(50) not null unique,"
					+ "thread_id varchar(20) not null,"
					+ "title varchar(50) not null,"
					+ "url varchar(200) not null,"
					+ "host varchar(50) not null,"
					+ "description varchar(150) not null default ''," 
					+ "labels varchar(150) not null default '',"
					+ "created long not null,"
					+ "modified long not null )" );

			db.execSQL("create virtual table " + BOOKMARKS_TABLE_NAME + "_FTS "
					+ "USING fts3(title_fts, host_fts, description_fts, labels_fts)" );
			
			db.execSQL("create index idx_" + BOOKMARKS_TABLE_NAME + "_url on "
					+ BOOKMARKS_TABLE_NAME + "(url asc)" );

			db.execSQL( "create table " + LABELS_TABLE_NAME + " ( "
					+ "_id integer primary key,"
					+ "label varchar(30) unique not null collate nocase,"
					+ "_count int not null default 0 )" );
			
			db.execSQL( "create table " + BOOKMARK_LABELS_TABLE_NAME + " ( "
					+ "label_id integer not null"
					+ " references labels(_id) on delete cascade,"
					+ "bookmark_id integer not null" 
				    + " references bookmarks(_id) on delete cascade )" );
			
			db.execSQL( "create unique index idx_bookmarks_labels_ref on "
					+ BOOKMARK_LABELS_TABLE_NAME + " ( label_id, bookmark_id )" );
		}
	
		@Override
		public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
			// TODO Nothing to do until we're upgrading from old db.
		}
		
		@Override
		public void onOpen(SQLiteDatabase db) {
			super.onOpen(db);
			if (!db.isReadOnly()) { 
				// Enable foreign key constraints
				// NOTE versions prior to Froyo don't support this.
				db.execSQL("PRAGMA foreign_keys=ON;");
			}
		}
		
	    private static final String[] bookmarksIDColumns = new String[] {
	    	Bookmark.Columns.GOOGLEID, 
	    	Bookmark.Columns.THREAD_ID, 
	    	Bookmark.Columns.TITLE, 
	    	Bookmark.Columns._ID };
	    
	    /**
	     * Note that this does not return the full bookmark object, just a
	     * shell with the _id, elementID, title and URL filled in.
	     * @param url
	     * @param db
	     * @return
	     */
	    public Bookmark findByURL(String url, SQLiteDatabase db ) {
	    	// Get the database and run the query
	    	boolean closeDB = false;
	    	if ( db == null ) {
	    		db = getReadableDatabase();
	    		closeDB = true;
	    	}
	        try {
		        Cursor c = db.query(BOOKMARKS_TABLE_NAME, bookmarksIDColumns, 
		        		"url=?", new String[] {url}, null, null, null);

		        try { // lazy for now, only looking @ first row...
		        	if ( ! c.moveToFirst() ) return null;
		        	Bookmark b = new Bookmark(c.getString(0),c.getString(1),c.getString(2),url,null,null,-1,-1);
		        	b.set_id(c.getLong(3));
		        	return b;
		        }
		        finally { c.close(); }
	        }
	        finally { if ( closeDB ) db.close(); }
	    }
	    
	    
	    public List<Bookmark> findByLabel(String label, SQLiteDatabase db ) {
	    	// Get the database and run the query
	    	boolean closeDB = false;
	    	if ( db == null ) {
	    		db = getReadableDatabase();
	    		closeDB = true;
	    	}
	        try {
		        Cursor c = db.query("bookmarks b join bookmark_labels bl on bl.bookmark_id=b._id" 
		        		+ " join labels l on l._id=bl.label_id", 
		        		new String[] {"b._id", "b.google_id", "b.thread_id",
		        				"b.title", "b.url", "b.host", "b.description",
		        				"b.created", "b.modified"},
		        		"l.label=?", new String[] {label}, null, null, null);

		        List<Bookmark> bookmarks = new ArrayList<Bookmark>();
		        try {
		        	while ( c.moveToNext() ) {
			        	Bookmark b = new Bookmark(c.getString(1),c.getString(2),c.getString(3),
			        			c.getString(4),c.getString(5),c.getString(6),c.getLong(7),c.getLong(8));
			        	b.set_id(c.getLong(0));
			        	bookmarks.add(b);
		        	}
		        }
		        finally { c.close(); }
		        return bookmarks;
	        }
	        finally { if ( closeDB ) db.close(); }	    	
	    }
	    
	    public Bookmark insert( Bookmark b, SQLiteDatabase db ) throws DBException {
	    	boolean closeDB = false;
	    	if ( db == null ) {
	    		db = getWritableDatabase();
	    		closeDB = true;
	    		db.beginTransaction();
	    	}
	        try {
	        	ContentValues vals = new ContentValues();
	        	vals.put(Bookmark.Columns.GOOGLEID, b.getGoogleId());
	        	vals.put(Bookmark.Columns.THREAD_ID, b.getThreadId());
	        	vals.put(Bookmark.Columns.TITLE, b.getTitle());
	        	vals.put(Bookmark.Columns.URL, b.getUrl());
	        	vals.put(Bookmark.Columns.DESCRIPTION, b.getDescription());
	        	vals.put(Bookmark.Columns.HOST, b.getHost());
	        	vals.put(Bookmark.Columns.CREATED_DATE, b.getCreatedDate());
	        	vals.put(Bookmark.Columns.MODIFIED_DATE, b.getModifiedDate());
	        	vals.put(Bookmark.Columns.LABELS, b.getAllLabels());

	        	long rowID = db.insertWithOnConflict( BOOKMARKS_TABLE_NAME, "", vals, 
	        			SQLiteDatabase.CONFLICT_IGNORE );
	        	if ( rowID < 0 ) throw new DBException( "Insert conflict: " + rowID );
	        	b.set_id(rowID);

	        	this.updateLabels(db, b); // labels
	        	
	        	// update FTS table
        		vals.clear();
        		vals.put("docid", b.get_id());
        		vals.put(Bookmark.Columns.TITLE+"_fts", b.getTitle());
        		vals.put(Bookmark.Columns.HOST+"_fts", b.getHost());
        		vals.put(Bookmark.Columns.DESCRIPTION+"_fts", b.getDescription());
        		vals.put("labels_fts", b.getAllLabels());
        		try {
    				rowID = db.insertWithOnConflict(BOOKMARKS_TABLE_NAME+"_FTS", "",
        				vals, SQLiteDatabase.CONFLICT_IGNORE );
    				if ( rowID < 0 )
    					Log.w(TAG, "Row result error during FTS insert: "+ rowID);
        		}
        		catch ( SQLiteConstraintException ex ) {
        			// this keeps throwing an exception even though I am using 
        			// a conflict strategy!??!!!
        			Log.w(TAG, "FTS Update Error for ID: " + b.get_id(), ex);
        		}

	        	
	        	if ( closeDB ) {
	        		Log.d(TAG, "COmmitting changes: " + b.getTitle() );
	        		db.setTransactionSuccessful();
	        	}

	        	return b;
	        }
	        finally { 
	        	if ( closeDB ) {
	        		db.endTransaction();
	        		db.close();
	        	}
	        }
	    }

	    public void update( Bookmark b, SQLiteDatabase db ) throws DBException {
	    	boolean closeDB = false;
	    	if ( db == null ) {
	    		db = getWritableDatabase();
	    		closeDB = true;
	    		db.beginTransaction();
	    	}
	        try {
	        	ContentValues vals = new ContentValues();
	        	if ( b.getGoogleId() != null )
	        		vals.put(Bookmark.Columns.GOOGLEID, b.getGoogleId());
	        	if ( b.getThreadId() != null )
	        		vals.put(Bookmark.Columns.THREAD_ID, b.getThreadId());
	        	vals.put(Bookmark.Columns.TITLE, b.getTitle());
	        	vals.put(Bookmark.Columns.URL, b.getUrl());
	        	vals.put(Bookmark.Columns.DESCRIPTION, b.getDescription());
	        	if ( b.getHost() != null ) 
	        		vals.put(Bookmark.Columns.HOST, b.getHost());
	        	if ( b.getCreatedDate() > 0 ) 
	        		vals.put(Bookmark.Columns.CREATED_DATE, b.getCreatedDate());
	        	if ( b.getModifiedDate() > 0 ) 
	        		vals.put(Bookmark.Columns.MODIFIED_DATE, b.getModifiedDate());
	        	vals.put(Bookmark.Columns.LABELS, b.getAllLabels());
	        	
	        	String whereClause = Bookmark.Columns._ID + "=?";
	        	String[] whereArgs = new String[] { ""+b.get_id() };
	        	if ( b.get_id() == null ) {
	        		if ( b.getGoogleId() == null )
	        			throw new IllegalArgumentException("Both _id and googleID are null");
		        	whereClause = Bookmark.Columns.GOOGLEID + "=?";
	        		whereArgs = new String[] { b.getGoogleId() };
	        		Log.v(TAG,"Updating bookmark element ID: " + b.getGoogleId() );
	        	}
	        	else Log.v(TAG,"Updating bookmark _ID: " + b.get_id() );
	        	
	        	int result = db.updateWithOnConflict( BOOKMARKS_TABLE_NAME, vals,
	        			whereClause, whereArgs, SQLiteDatabase.CONFLICT_IGNORE );
	        	
	        	if ( result < 1 ) throw new DBException( "Update conflict: " + result );

	        	this.updateLabels(db, b); // labels

	        	// update FTS table
        		vals.clear();
        		vals.put(Bookmark.Columns.TITLE+"_fts", b.getTitle());
        		vals.put(Bookmark.Columns.HOST+"_fts", b.getHost());
        		vals.put(Bookmark.Columns.DESCRIPTION+"_fts", b.getDescription());
        		vals.put("labels_fts", b.getAllLabels());
        		try {
    				long rowID = db.updateWithOnConflict(BOOKMARKS_TABLE_NAME+"_FTS", vals,
    						"docid=?", new String[] { ""+b.get_id() },
    						SQLiteDatabase.CONFLICT_IGNORE );
    				if ( rowID < 0 )
    					Log.w(TAG, "Row result error during FTS update: "+ rowID);
        		}
        		catch ( SQLiteConstraintException ex ) {
        			// this keeps throwing an exception even though I am using 
        			// a conflict strategy!??!!!
        			Log.w(TAG, "FTS Update Error for ID: " + b.get_id(), ex);
        		}

	        	
	        	if ( closeDB ) {
	        		Log.d(TAG, "COmmitting changes: " + b.getTitle() );
	        		db.setTransactionSuccessful();
	        	}
	        }
	        finally {
	        	if ( closeDB ) {
	        		db.endTransaction();
	        		db.close();
	        	}
	        }
	    }
	    
	    protected void updateLabels( SQLiteDatabase db, Bookmark b ) {
        	// add label relationships
        	if ( b.getLabels().size() < 1 ) { // hack to create relation to "^none" label:
        		b.getLabels().add("^none");
        	}
        	
        	// delete label relations & rebuild; easier than finding set difference
        	db.delete(BOOKMARK_LABELS_TABLE_NAME, "bookmark_id=?", new String[]{b.get_id().toString()});

        	// create label relations
        	ContentValues vals = new ContentValues();
        	for (String label : b.getLabels() ) {
        		Cursor c = db.query(LABELS_TABLE_NAME, new String[] {"_id", "_count"}, 
        				Label.Columns.TITLE + "=?", new String[] {label.toLowerCase()}, 
        				null, null, null);
        		
        		long labelID;
        		try {
	        		if ( ! c.moveToFirst() ) { // insert a new label
	        			vals.clear();
	        			vals.put(Label.Columns.TITLE, label);
	        			labelID = db.insertWithOnConflict(LABELS_TABLE_NAME, "", vals, 
	        					SQLiteDatabase.CONFLICT_IGNORE);
	        		}
	        		else labelID = c.getLong(0); // get label ID
        		} finally { c.close(); }
        		
        		if ( labelID < 0 ) {
        			Log.w(TAG, "Couldn't get ROW ID for label " + label);
        		}
        		else { // insert label relation; ignore if one already exists.
        			vals.clear();
        			vals.put("label_id", labelID);
        			vals.put("bookmark_id", b.get_id());
        			long result = db.insertWithOnConflict(BOOKMARK_LABELS_TABLE_NAME, 
        					"", vals, SQLiteDatabase.CONFLICT_IGNORE);
        			
    				if ( result < 0 )
    					Log.w(TAG, "Couldn't update label count for label ID: " + labelID);
//    				else Log.d(TAG, "Updated count for label ID: " + labelID);
	        	}
        	}
        	// remove "^none" hack label if it's there.
        	b.getLabels().remove("^none");
	    }
	    
	    /** Delete the bookmark with the given ID */
	    public boolean deleteBookmark( long id, SQLiteDatabase db ) throws DBException {
	    	// TODO label count will be out of sync
	    	boolean closeDB = false;
	    	if ( db == null ) {
	    		db = getWritableDatabase();
	    		closeDB = true;
	    		db.beginTransaction();
	    	}
	    	try {
	    		int result = db.delete( BOOKMARKS_TABLE_NAME, 
	    				Bookmark.Columns._ID + "=?", new String[] { ""+id } );
	    		
	    		if ( result != 1 )
	    			throw new DBException("Return result " + result 
	    					+ " while deleting bookmark ID: " + id );
	    		
	    		// delete labels (SQLite in versions prior to Froyo don't
	    		// support FK constraints
	        	db.delete(BOOKMARK_LABELS_TABLE_NAME, "bookmark_id=?", new String[]{""+id});

	    		// Delete FTS row
				long count = db.delete(BOOKMARKS_TABLE_NAME+"_FTS", 
						"docid=?", new String[] { ""+id } );
				if ( count != 1 )
					Log.w(TAG, "Row result error during FTS delete: "+ count);
	    		
	        	if ( closeDB ) {
	        		Log.d(TAG, "COmmitting delete for bookmark ID: " + id );
	        		db.setTransactionSuccessful();
	        	}
	    		
	    		return true;
	    	}
	    	finally { 
	    		if ( closeDB ) {
	    			db.endTransaction();
	    			db.close();
	    		}
	    	}
	    }
		
	    public void persistCookies( List<Cookie> cookies ) {
			SQLiteDatabase db = this.getWritableDatabase();
			db.beginTransaction();
			try {
				// flush old auth cookies before persisting new ones.
				db.delete(COOKIES_TABLE_NAME, "", new String[] {} );
				
				for ( Cookie c : cookies ) {
					ContentValues row = new ContentValues();
					row.put("name", c.getName()	);
					row.put("value", c.getValue() );
					row.put("domain", c.getDomain() );
					row.put("path", c.getPath());
					Date expiry = c.getExpiryDate();
					if ( expiry != null ) row.put("expires", expiry.getTime() );
					row.put("secure", c.isSecure() ? 1 : 0 );
					
					db.insert(COOKIES_TABLE_NAME, "", row);
				}
				db.setTransactionSuccessful();
				Log.d(TAG, "Saved cookies to DB");
			}
			catch (Exception ex ) {
				Log.w(TAG, "Error persisting cookies!", ex );
			}
			finally {
				db.endTransaction();
				db.close();
			}
		}
		
		public List<Cookie> restoreCookies() {
			SQLiteDatabase db = this.getReadableDatabase();
			try {
				Cursor cursor = db.query(COOKIES_TABLE_NAME, cookieColumns, 
						null, null, null, null, null );
				List<Cookie> cookies = new ArrayList<Cookie>();
				while ( cursor.moveToNext() ) {
					BasicClientCookie c = new BasicClientCookie(cursor.getString(0), cursor.getString(1));
					c.setDomain(cursor.getString(2));
					c.setPath(cursor.getString(3));
					Long expires = cursor.getLong(4);
					if ( expires != null ) c.setExpiryDate( new Date(expires) );
					c.setSecure( 0 != cursor.getShort(5) );
					cookies.add( c );
				}
				Log.d(TAG, "Restored cookies");
				cursor.close();
				return cookies;
			}
			finally { db.close(); }
		}
		
		public void clearCookies() {
			SQLiteDatabase db = this.getWritableDatabase();
			try { db.delete(COOKIES_TABLE_NAME, "", null ); }
			finally { db.close(); }
		}
		
		

	}
}
