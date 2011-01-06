/**
 * 
 */
package org.thomnichols.android.gmarks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
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
	
	private static BookmarksQueryService instance = null;
	
//	protected DefaultHttpClient http;
	protected AndroidHttpClient http;
	protected HttpContext ctx;
//	protected String USER_AGENT = "";
	protected CookieStore cookieStore;
	protected String TAG = "BOOKMARKS QUERY";
	protected boolean authInitialized = false;
	
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
		String createURL = "https://www.google.com/bookmarks/api/thread?op=Star";

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		return createOrUpdate( createURL, new UrlEncodedFormEntity(params) );
	}
	
	public Bookmark update( Bookmark b ) throws IOException {
		String updateURL = "https://www.google.com/bookmarks/api/thread?op=UpdateThreadElement";
		
		JSONObject requestObj = new JSONObject();
		try {
			JSONObject bookmarkObj = new JSONObject();
			// TODO this is part of a bookmark but I've been ignoring it...
//			bookmarkObj.append("threadId", "");
			bookmarkObj.append( "elementId", b.getGoogleId());
			bookmarkObj.append( "title", b.getTitle() );
			bookmarkObj.append( "url", b.getUrl() );
			bookmarkObj.append( "snippet", b.getDescription() );
			JSONArray labels = new JSONArray();
			for (String label : b.getLabels() ) labels.put(label);
			bookmarkObj.append( "labels", labels );
			
// these are in the request but empty... maybe we can ignore them???
//			"authorId":0,"timestamp":0,"formattedTimestamp":0,"signedUrl":"",
//			"previewUrl":"","threadComments":[],"parentId":"",
			bookmarkObj.append( "authorId", 0 );
			bookmarkObj.append( "timestamp", 0 );
			bookmarkObj.append( "formattedTimestamp", 0 );
			bookmarkObj.append( "signedUrl", "" );
			bookmarkObj.append( "previewUrl", "" );
			bookmarkObj.append( "threadComments", new JSONArray() );
			bookmarkObj.append( "parentId", "" );

			requestObj.append("threadResults", bookmarkObj);
			
			// other unneeded params that are part of an update request:
			JSONArray emptyArray = new JSONArray();
			requestObj.append("threads", emptyArray);
			requestObj.append("threadQueries", emptyArray);
			requestObj.append("threadComments", emptyArray);
			
		}
		catch ( JSONException ex ) {
			throw new IOException( "Error creating request", ex );
		}
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add( new BasicNameValuePair("xt", requestObj.toString()) );
		return createOrUpdate( updateURL, new UrlEncodedFormEntity(params) );
	}
	
	public Bookmark createOrUpdate( String url, HttpEntity postBody ) throws IOException {
		HttpPost post = new HttpPost( url );
		
		post.setEntity( postBody );
		HttpResponse resp = http.execute( post, this.ctx );
		
		int respCode = resp.getStatusLine().getStatusCode(); 
		if ( respCode > 299 ) 
			throw new IOException( "Unexpected response code: " + respCode );

		try { // always assume a single item is created or updated.
			JSONObject respObj = parseJSON(resp);
			respObj = respObj.getJSONArray("results").getJSONObject(0).getJSONObject("threadResult");
			Bookmark b = new Bookmark( respObj.getString("elementId"),
					respObj.getString("title"),
					respObj.getString("url"),
					respObj.getString("host"),
					respObj.getString("snippet"),
					respObj.getLong("timestamp"),
					respObj.getLong("timestamp") );
			
			JSONArray labelJSON = respObj.getJSONArray("labels");
			
			for ( int i=0; i< labelJSON.length(); i++ )
				b.getLabels().add(labelJSON.getString(i));
			
			return b;
		} 
		catch ( JSONException ex ) {
			throw new IOException( "Response parse error", ex );
		}
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
