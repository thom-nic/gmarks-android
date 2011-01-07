/**
 * 
 */
package org.thomnichols.android.gmarks;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.thomnichols.android.gmarks.thirdparty.IOUtils;

import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.util.Log;

/**
 * @author tnichols
 *
 */
public class BookmarksQueryService {

	public synchronized static BookmarksQueryService getInstance() {
		if ( instance == null ) instance = new BookmarksQueryService(null);
		return instance;
	}
	
	public class AuthException extends IOException {
		private static final long serialVersionUID = 1L;
		public AuthException() { super(); }
		public AuthException( String msg ) { super(msg); }
		public AuthException( Throwable cause) { super(cause); }
		public AuthException( String msg, Throwable cause) { super(msg,cause); }
	}
	
	public class NotFoundException extends IOException {
		private static final long serialVersionUID = 1L;
		public NotFoundException() { super(); }
		public NotFoundException( String msg ) { super(msg); }
		public NotFoundException( Throwable cause) { super(cause); }
		public NotFoundException( String msg, Throwable cause) { super(msg,cause); }
	}
	
	private static BookmarksQueryService instance = null;
	
//	protected DefaultHttpClient http;
	protected AndroidHttpClient http;
	protected HttpContext ctx;
//	protected String USER_AGENT = "";
	protected CookieStore cookieStore;
	protected String TAG = "BOOKMARKS QUERY";
	protected boolean authInitialized = false;
	protected String xtParam = null;
	
	private BookmarksQueryService( String userAgent ) {
		ctx = new BasicHttpContext();
		cookieStore = new BasicCookieStore();
		ctx.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
		String defaultUA = "Mozilla/5.0 (Linux; U; Android 2.1; en-us) AppleWebKit/522+ (KHTML, like Gecko) Safari/419.3";
//		http = new DefaultHttpClient();
		http = AndroidHttpClient.newInstance( userAgent != null ? userAgent : defaultUA );
	}
	
	public void setAuthCookies( List<Cookie> cookies ) {
		this.cookieStore.clear();
		for ( Cookie c : cookies ) this.cookieStore.addCookie(c);
		// TODO if cookies are expired, don't set them & notify caller they should be refreshed.
		this.authInitialized = true;
	}
	
	public boolean isAuthInitialized() {
		return authInitialized;
	}
	
	public void clearAuthCookies() {
		this.cookieStore.clear();
		this.authInitialized = false;
	}
	
	public void login( String user, String passwd ) {
		try {
			List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
			queryParams.add( new BasicNameValuePair("service", "bookmarks") );
			queryParams.add( new BasicNameValuePair("passive", "true") );
			queryParams.add( new BasicNameValuePair("nui", "1") );
			queryParams.add( new BasicNameValuePair("continue", "https://www.google.com/bookmarks/l") );
			queryParams.add( new BasicNameValuePair("followup", "https://www.google.com/bookmarks/l") );
			HttpGet get = new HttpGet( "https://www.google.com/accounts/ServiceLogin?" + 
					URLEncodedUtils.format(queryParams, "UTF-8") );
			HttpResponse resp = http.execute(get, this.ctx);
			// this just gets the cookie but I can ignore it...
			
			if ( resp.getStatusLine().getStatusCode() != 200 )
				throw new RuntimeException( "Invalid status code for ServiceLogin " +
						resp.getStatusLine().getStatusCode() );
			resp.getEntity().consumeContent();
			
			String galx = null;
			for ( Cookie c : cookieStore.getCookies() )
				if ( c.getName().equals( "GALX" ) ) galx = c.getValue(); 
			
			if ( galx == null ) throw new RuntimeException( "GALX cookie not found!" );
			
			HttpPost loginMethod = new HttpPost("https://www.google.com/accounts/ServiceLoginAuth");
			// post parameters:
			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair("Email", user));
			nvps.add(new BasicNameValuePair("Passwd", passwd));
			nvps.add(new BasicNameValuePair("PersistentCookie", "yes"));
			nvps.add(new BasicNameValuePair("GALX", galx));			
			nvps.add(new BasicNameValuePair("continue", "https://www.google.com/bookmarks/l"));
			loginMethod.setEntity(new UrlEncodedFormEntity(nvps));
			resp = http.execute( loginMethod, this.ctx );
			
			if ( resp.getStatusLine().getStatusCode() != 302 )
				throw new RuntimeException( "Unexpected status code for ServiceLoginAuth" +
						resp.getStatusLine().getStatusCode() );
			resp.getEntity().consumeContent();
			
			Header checkCookieLocation = resp.getFirstHeader("Location");
			if ( checkCookieLocation == null ) 
				throw new RuntimeException("Missing checkCookie redirect location!");

			// CheckCookie:
			get = new HttpGet( checkCookieLocation.getValue() );
			resp = http.execute( get, this.ctx );
			
			if ( resp.getStatusLine().getStatusCode() != 302 )
				throw new RuntimeException( "Unexpected status code for CheckCookie" +
						resp.getStatusLine().getStatusCode() );
			resp.getEntity().consumeContent();
			
			this.authInitialized = true;
			Log.i(TAG, "Final redirect location: " + resp.getFirstHeader("Location").getValue() );
			Log.i(TAG, "Logged in.");
		}
		catch ( IOException ex ) {
			Log.e(TAG, "Error during login", ex );
			throw new RuntimeException("IOException during login", ex);
		}
	}
	
	public boolean testAuth() {
		HttpGet get = new HttpGet( "https://www.google.com/bookmarks/api/threadsearch?fo=Starred&g&q&start&nr=1" );
		try {
			HttpResponse resp = http.execute( get, this.ctx );
			int statusCode = resp.getStatusLine().getStatusCode();
			Log.d( "TAG", "testAuth return code: " + statusCode );
			return statusCode < 400;
		}
		catch ( IOException ex ) {
			Log.e( TAG, "Error while checking auth status", ex );
		}
		return false;
	}

	public Bookmark create( Bookmark b ) throws IOException {
		final String createURL = "https://www.google.com/bookmarks/api/thread?op=Star"
			+ "&xt=" + URLEncoder.encode( getXtParam(), "UTF-8" );
		
//td {"results":[{"threadId":"BDQAAAAAQAA","elementId":0,"authorId":0,
//                "title":"My Blog","timestamp":0,"formattedTimestamp":0,
//                "url":"http://blog.thomnichols.org","signedUrl":"",
//                "previewUrl":"","snippet":"___________","threadComments":[],
//                "parentId":"BDQAAAAAQAA","labels":["mobile"]}]}
		JSONObject requestObj = new JSONObject();
		try {
			JSONObject bookmarkObj = new JSONObject();
			// TODO this is part of a bookmark but I've been ignoring it...
//			bookmarkObj.put("threadId", "");
			bookmarkObj.put( "elementId", b.getGoogleId());
			bookmarkObj.put( "title", b.getTitle() );
			bookmarkObj.put( "url", b.getUrl() );
			bookmarkObj.put( "snippet", b.getDescription() );
			JSONArray labels = new JSONArray();
			for (String label : b.getLabels() ) labels.put(label);
			bookmarkObj.put( "labels", labels );
			
			bookmarkObj.put( "authorId", 0 );
			bookmarkObj.put( "timestamp", 0 );
			bookmarkObj.put( "formattedTimestamp", 0 );
			bookmarkObj.put( "signedUrl", "" );
			bookmarkObj.put( "previewUrl", "" );
			bookmarkObj.put( "threadComments", new JSONArray() );
			// this is the same as threadId:
			bookmarkObj.put( "parentId", "" );

			JSONArray resultArray = new JSONArray();
			resultArray.put(bookmarkObj);
			requestObj.put("results", resultArray);			
		}
		catch ( JSONException ex ) {
			throw new IOException( "Error creating request", ex );
		}

		return createOrUpdate( createURL, requestObj );
	}
	
	public Bookmark update( Bookmark b ) throws IOException {
		String updateURL = "https://www.google.com/bookmarks/api/thread?op=UpdateThreadElement" 
			+ "&xt=" + URLEncoder.encode( getXtParam(), "UTF-8" );
		
		JSONObject requestObj = new JSONObject();
		try {
			JSONObject bookmarkObj = new JSONObject();
			// TODO this is part of a bookmark but I've been ignoring it...
			bookmarkObj.put("threadId", b.getThreadId());
			bookmarkObj.put( "elementId", b.getGoogleId());
			bookmarkObj.put( "title", b.getTitle() );
			bookmarkObj.put( "url", b.getUrl() );
			bookmarkObj.put( "snippet", b.getDescription() );
			JSONArray labels = new JSONArray();
			for (String label : b.getLabels() ) labels.put(label);
			bookmarkObj.put( "labels", labels );
			
// these are in the request but empty... maybe we can ignore them???
//			"authorId":0,"timestamp":0,"formattedTimestamp":0,"signedUrl":"",
//			"previewUrl":"","threadComments":[],"parentId":"",
			bookmarkObj.put( "authorId", 0 );
			bookmarkObj.put( "timestamp", 0 );
			bookmarkObj.put( "formattedTimestamp", 0 );
			bookmarkObj.put( "signedUrl", "" );
			bookmarkObj.put( "previewUrl", "" );
			bookmarkObj.put( "threadComments", new JSONArray() );
			bookmarkObj.put( "parentId", "" );

			JSONArray results = new JSONArray();
			results.put(bookmarkObj);
			requestObj.put("threadResults", results);
			
			// other unneeded params that are part of an update request:
			JSONArray emptyArray = new JSONArray();
			requestObj.put("threads", emptyArray);
			requestObj.put("threadQueries", emptyArray);
			requestObj.put("threadComments", emptyArray);
			
		}
		catch ( JSONException ex ) {
			throw new IOException( "Error creating request", ex );
		}

		return createOrUpdate( updateURL, requestObj );
	}

	public void delete(String googleId) throws AuthException, IOException {
		Uri requestURI = Uri.parse("https://www.google.com/bookmarks/api/thread").buildUpon()
			.appendQueryParameter("xt", getXtParam() )
			.appendQueryParameter("op", "DeleteItems").build();
//		final String deleteURL = "https://www.google.com/bookmarks/api/thread?"
//			+ "xt=" + URLEncoder.encode( getXtParam(), "UTF-8" )  
//			+ "&op=DeleteItems";
		//  td	{"deleteAllBookmarks":false,"deleteAllThreads":false,"urls":[],"ids":["____"]}

		JSONObject requestObj = new JSONObject();
		try {
			requestObj.put("deleteAllBookmarks", false);
			requestObj.put("deleteAllThreads", false);
			requestObj.put("urls", new JSONArray());
			JSONArray elementIDs = new JSONArray();
			elementIDs.put( googleId );
			requestObj.put("ids", elementIDs);
		}
		catch ( JSONException ex ) {
			throw new IOException( "JSON error while creating request" );
		}
		
		String postString = "{\"deleteAllBookmarks\":false,\"deleteAllThreads\":false,\"urls\":[],\"ids\":[\""
			+ googleId + "\"]}";
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
//		params.add( new BasicNameValuePair("td", requestObj.toString()) );
		params.add( new BasicNameValuePair("td", postString) );

//		Log.d(TAG,"DELETE: " + requestObj.toString());
		Log.d(TAG,"DELETE: " + requestURI );
		Log.d(TAG,"DELETE: " + postString);

		HttpPost post = new HttpPost( requestURI.toString() );		
//		HttpPost post = new HttpPost( deleteURL );		
		post.setEntity( new UrlEncodedFormEntity(params) );
		HttpResponse resp = http.execute( post, this.ctx );
		
		int respCode = resp.getStatusLine().getStatusCode();
		if ( respCode == 401 ) throw new AuthException();
		if ( respCode > 299 ) 
			throw new IOException( "Unexpected response code: " + respCode );

		try { // always assume a single item is created or updated.
			JSONObject respObj = parseJSON(resp);
			int deletedCount = respObj.getInt("numDeletedBookmarks");
			
			if ( deletedCount < 1 )
				throw new IOException( "Bookmark could not be found" );
			if ( deletedCount > 1 )
				throw new IOException("Expected 1 deleted bookmark but got " + deletedCount );
		} 
		catch ( JSONException ex ) {
			throw new IOException( "Response parse error", ex );
		}
	}
	
	protected Bookmark createOrUpdate( String url, JSONObject requestObj ) throws AuthException, IOException {
		HttpPost post = new HttpPost( url );
		
		Log.d(TAG, "UPDATE: " + url);
		Log.d(TAG, "UPDATE: " + requestObj);
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add( new BasicNameValuePair("td", requestObj.toString()) );
		post.setEntity( new UrlEncodedFormEntity(params) );
		HttpResponse resp = http.execute( post, this.ctx );
		
		int respCode = resp.getStatusLine().getStatusCode(); 
		if ( respCode == 401 ) throw new AuthException();
		if ( respCode > 299 ) 
			throw new IOException( "Unexpected response code: " + respCode );

		try { // always assume a single item is created or updated.
			JSONObject respObj = parseJSON(resp);
			if ( respObj.has("results") ) // create response:
				respObj = respObj.getJSONArray("results").getJSONObject(0).getJSONObject("threadResult");
			else respObj = respObj.getJSONArray("threadResults").getJSONObject(0);
			Bookmark b = new Bookmark( respObj.getString("elementId"),
					respObj.getString("threadId"),
					respObj.getString("title"),
					respObj.getString("url"),
					respObj.getString("host"),
					respObj.getString("snippet"),
					-1, // no created date in response
					respObj.getLong("timestamp") );
			
			Log.d(TAG, "RESPONSE: " + respObj );
			if ( respObj.has("labels") ) {
				JSONArray labelJSON = respObj.getJSONArray("labels");
				
				for ( int i=0; i< labelJSON.length(); i++ )
					b.getLabels().add(labelJSON.getString(i));
			}
			
			return b;
		} 
		catch ( JSONException ex ) {
			Log.w(TAG, "Response parse error", ex );
			throw new IOException( "Response parse error" );
		}
	}
	
	protected String getXtParam() throws IOException {
		if ( this.xtParam != null ) return this.xtParam; // already init'd
		
		HttpGet get = new HttpGet("https://www.google.com/bookmarks/l");
		
		HttpResponse resp = http.execute(get, this.ctx);
		
		// TODO auth check
		int responseCode = resp.getStatusLine().getStatusCode(); 
		if (  responseCode != 200 ) 
			throw new IOException( "Unexpected response code: " + responseCode );
		
		// TODO encoding
		String respString = IOUtils.toString( resp.getEntity().getContent() );
		String xtSearchString = ";SL.xt = '";
		int xtStartIndex = respString.indexOf(xtSearchString);
		if ( xtStartIndex < 0 ) throw new IOException("Could not find xtSearchString");
		xtStartIndex += xtSearchString.length(); 
		this.xtParam = respString.substring( xtStartIndex, 
				respString.indexOf("'", xtStartIndex) );
		Log.d(TAG, "XT context: " + respString.substring( xtStartIndex-10, 
				respString.indexOf("'", xtStartIndex)+5 ) );
		Log.d(TAG, "GOT XT PARAM: " + xtParam );
		return this.xtParam;
	}
	
	protected JSONObject queryJSON(String uri) throws AuthException, JSONException, IOException {
		HttpGet get = new HttpGet(uri);

		HttpResponse resp = http.execute( get, this.ctx );
		int code = resp.getStatusLine().getStatusCode();
		if ( code == 401 || code == 403 ) {
			Log.d(TAG, "Auth failure from queryJSON");
			throw new AuthException(); 
		}
		if ( code != 200 ) {
			Log.e( TAG, "Unexpected response code: " + code );
			throw new IOException("Unexpected response code: " + code );
		}
		return parseJSON( resp );
	}
	
	protected JSONObject parseJSON( HttpResponse resp ) throws IOException, JSONException {
		String respData = IOUtils.toString( resp.getEntity().getContent() );
		resp.getEntity().consumeContent();
		if ( respData.startsWith(")]}'") )
			respData = respData.substring( respData.indexOf("\n") );
		
		JSONTokener parser = new JSONTokener(respData);
		return (JSONObject) parser.nextValue();
	}
	
	public List<Label> getLabels() throws IOException {
		String uri = "https://www.google.com/bookmarks/api/bookmark?op=LIST_LABELS";
		
		try {
			JSONObject labelsObj = queryJSON(uri);
			JSONArray labels = labelsObj.getJSONArray("labels");
			JSONArray counts = labelsObj.getJSONArray("counts");
			
			ArrayList<Label> list = new ArrayList<Label>();
			for ( int i=0; i< labels.length(); i++ )
				list.add( new Label(labels.getString(i), counts.getInt(i)) );
			
			return list;
		}
		catch ( JSONException ex ) {
			Log.e(TAG, "Labels query JSON parse exception", ex );
			throw new IOException("Error retrieving labels", ex );
		}
	}
	
	public Iterable<Bookmark> getAllBookmarksForLabel(String label) {
		String query = "label%3A%22" + label + "%22";
		return new AllBookmarksIterator(query);
	}
	
	/**
	 * This has the potential to be relatively large..
	 */
	public Iterable<Bookmark> getAllBookmarks() {
		// make a sequence of JSON requests until they've all been retrieved.
		// max 25 bookmarks can be requested at one time.
		return new AllBookmarksIterator();
	}
	
	class AllBookmarksIterator implements Iterator<Bookmark>, Iterable<Bookmark> {
		
		JSONObject currentBatch = null;
		int currentQueryIndex = 0;
		String uriBase = "https://www.google.com/bookmarks/api/threadsearch?g=Time&nr=25&start=";
		int totalItems = -1; 
		int currentItemIndex = 0;
		String filter = "";

		JSONArray currentSection = null;
		int sectionIndex = 0;
		
		public AllBookmarksIterator() { this(null); }
		
		public AllBookmarksIterator( String filter ) {
			if ( filter != null ) this.filter = "&q=" + filter;
			this.queryNext();
		}
		
		private boolean getNextSection() {
			if ( currentBatch == null ) return false;
			try {
				JSONArray allSections = currentBatch.getJSONArray( "threadTitles" );
				if ( this.sectionIndex >= allSections.length() )
					return false;

				this.currentSection = allSections.getJSONObject( sectionIndex++ )
					.getJSONArray("sectionContent");
				this.currentItemIndex = 0;
				this.currentQueryIndex += currentSection.length();
				return true;
			}
			catch ( JSONException ex ) {
				Log.w(TAG, "JSON iterator error", ex );
			}
			return false;
		}
		
		private boolean queryNext() {
			try {
				this.currentBatch = BookmarksQueryService.this.queryJSON(
						uriBase + currentQueryIndex + this.filter );
				this.currentItemIndex = 0;
				this.sectionIndex = 0;
				
				if ( this.totalItems < 0 ) this.totalItems = currentBatch.getInt("nr");
				
				JSONArray sectionList = currentBatch.getJSONArray("threadTitles");
				if ( sectionList.length() < 1 ) { 
					Log.w(TAG, "JSON response has 0 items!");
					return false;
				}
				
				this.currentSection = sectionList.getJSONObject(sectionIndex++).getJSONArray("sectionContent");
				this.currentQueryIndex += currentSection.length();
				return this.currentSection.length() > 0;
			}
			catch ( IOException ex ) {
				Log.w(TAG,"IO error in query all bookmarks", ex ); 
				return false;
			}
			catch ( JSONException ex ) {
				Log.w(TAG,"JSON error in query all bookmarks", ex ); 
				return false;
			}
		}
		
		public boolean hasNext() {
			return this.currentSection != null && this.currentItemIndex < this.currentSection.length()
				|| getNextSection() || queryNext();
		}

		public Bookmark next() {
			try {
				JSONObject item = this.currentSection.getJSONObject(currentItemIndex++);
				Bookmark bookmark = new Bookmark( 
						item.getString("elementId"),
						item.getString("threadId"),
						item.getString("title"), 
						item.getString("url"),
						item.getString("host"),
						item.getString("description"),
						item.getLong("timestamp"),
						item.getLong("modifiedTimestamp") );
				JSONArray labelJSON = item.getJSONArray("labels");
				
				for ( int i=0; i< labelJSON.length(); i++ )
					bookmark.getLabels().add(labelJSON.getString(i));
				
				return bookmark;
			}
			catch ( JSONException ex ) {
				Log.w(TAG, "Error parsing JSON item" + ex.getMessage());
				return null;
			}
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		public Iterator<Bookmark> iterator() {
			return this;
		}
		
	}
}
