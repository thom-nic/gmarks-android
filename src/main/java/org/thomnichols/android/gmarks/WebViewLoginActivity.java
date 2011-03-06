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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.cookie.Cookie;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.WindowManager.BadTokenException;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class WebViewLoginActivity extends Activity {
	static final String TAG = "GMARKS WEBVIEW LOGIN";
	
	static final String KEY_PAUSED_FOR_TWO_FACTOR_AUTH = "gmarks.webview.paused_for_two_factor_auth";
	static final String KEY_PAUSED_AT_URL = "gmarks.webview.paused_at_current_url";
	
	static final String AUTHENTICATOR_PACKAGE = "com.google.android.apps.authenticator";
	static final Uri MARKET_URI = Uri.parse("market://details?id=" + AUTHENTICATOR_PACKAGE);
	static final Uri MARKET_WEB_URI = Uri.parse(
			"http://market.android.com/details?id=" + AUTHENTICATOR_PACKAGE);
		
	static final String loginURL = "https://www.google.com/accounts/ServiceLogin";
	static final String twoFactorAuthURL = "https://www.google.com/accounts/SmsAuth";
//	static final String checkCookieURL = "https://www.google.com/accounts/CheckCookie";
	static final String loginParams = "?service=bookmarks&passive=true"
		+ "&continue=https://www.google.com/bookmarks/l"
		+ "&followup=https://www.google.com/bookmarks/l";
	static final String targetURL = "https://www.google.com/bookmarks/";
	WebView webView = null;
	boolean loggedIn = false;
	boolean resumingTwoFactorAuth = false;
	private String resumeAtURL = loginURL + loginParams; 
	CookieSyncManager cookieSyncManager;
	ProgressDialog waitDialog;
//	CookieManager cookieManager;
	static final Set<String> requiredCookies = new HashSet<String>();
	static {
		requiredCookies.add("SID");
		requiredCookies.add("LSID");
		requiredCookies.add("HSID");
		requiredCookies.add("SSID");
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview_login);

        this.cookieSyncManager = CookieSyncManager.createInstance(this);
//        this.cookieManager = CookieManager.getInstance();
        
        this.webView = (WebView)findViewById(R.id.loginWebView);
        this.webView.setWebViewClient(this.webClient);
        this.webView.getSettings().setJavaScriptEnabled(true);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	if ( ! resumingTwoFactorAuth ) {
        	WebViewCookiesDB cookieDB = new WebViewCookiesDB(WebViewLoginActivity.this);
    		try { cookieDB.deleteCookie("SID"); } finally { cookieDB.close(); }
    	}
		final String currentURL = webView.getUrl();
		if ( resumingTwoFactorAuth && currentURL != null && 
				currentURL.startsWith(twoFactorAuthURL) ) return;
		this.webView.loadUrl( this.resumeAtURL );
    }
    
    protected void onPause() {
    	super.onPause();
    	dismissWaitDialog();
    }
    
    protected void onSaveInstanceState(Bundle outState) {
    	final String currentURL = webView.getUrl(); 
    	if ( currentURL != null && currentURL.startsWith(twoFactorAuthURL) ) {
    		outState.putBoolean(KEY_PAUSED_FOR_TWO_FACTOR_AUTH, true);
    		outState.putString(KEY_PAUSED_AT_URL, currentURL );
    	}
    	super.onSaveInstanceState(outState);
    }
    
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    	super.onRestoreInstanceState(savedInstanceState);
    	this.resumingTwoFactorAuth = savedInstanceState.getBoolean(
    			KEY_PAUSED_FOR_TWO_FACTOR_AUTH, false );
    	if ( this.resumingTwoFactorAuth && savedInstanceState.containsKey(KEY_PAUSED_AT_URL) )
    		this.resumeAtURL = savedInstanceState.getString( KEY_PAUSED_AT_URL );
    }
    
    protected void showTwoFactorAuthDialog() {
		dismissWaitDialog();

		final Intent launchAuthIntent = new Intent(Intent.ACTION_MAIN)
			.addCategory(Intent.CATEGORY_LAUNCHER)
			.setClassName( AUTHENTICATOR_PACKAGE, 
				"com.google.android.apps.authenticator.AuthenticatorActivity");
		
		ResolveInfo ri = getPackageManager().resolveActivity(launchAuthIntent, 0);
		if ( ri == null ) {
			WebViewLoginActivity.this.showAuthenticatorMissingDialog();
			return;
		}
		Log.d(TAG,"Resolve info: " + ri);
		
    	new AlertDialog.Builder(this)
    		.setTitle(R.string.two_factor_auth_dlg_title)
    		.setMessage(Html.fromHtml(getString(R.string.two_factor_auth_dlg_msg)))
    		.setCancelable(true)
    		.setNegativeButton(R.string.btn_cancel, new OnClickListener() {
    			public void onClick(DialogInterface dlg, int _) {
    				dlg.dismiss();
    			}
    		})
    		.setPositiveButton(R.string.btn_ok, new OnClickListener() {
				public void onClick(DialogInterface dlg, int _) {
//					dlg.dismiss();
					try { startActivity( launchAuthIntent ); }
					catch ( ActivityNotFoundException ex ) {
						Log.d(TAG,"Activity not found",ex);
						WebViewLoginActivity.this.showAuthenticatorMissingDialog();
					}
//					catch ( Exception ex ) {
//						Log.w(TAG,"Unexpected exception from activity launch",ex);
//					}
				}
			})
			.show();
    }
    
    protected void showAuthenticatorMissingDialog() {
    	new AlertDialog.Builder(this)
			.setTitle(R.string.two_factor_auth_dlg_title)
			.setMessage( Html.fromHtml( getString( 
				R.string.two_factor_not_installed_dlg_msg) ) )
			.setCancelable(false)
			.setPositiveButton(R.string.btn_download, new OnClickListener() {
				public void onClick(DialogInterface dialog, int _) {
					final Intent marketActivity = new Intent(Intent.ACTION_VIEW, MARKET_URI );
					if ( getPackageManager().resolveActivity(marketActivity, 0) == null )
						marketActivity.setData(MARKET_WEB_URI);
					try { startActivity( marketActivity ); }
					catch ( Exception ex ) {
						Log.w(TAG,"Couldn't open Android Market for Authenticator app!!");
					}
				}
			})
			.setNegativeButton(getString(R.string.btn_cancel), new OnClickListener() {
				public void onClick(DialogInterface dlg, int _) {
					dlg.dismiss();
				}
			})
			.show();
    }
    
    protected void dismissWaitDialog() {
    	if ( this.waitDialog == null ) return;
		try {
			waitDialog.dismiss(); // let the user log in.
		} catch ( IllegalArgumentException ex ) {}
		waitDialog = null;
    }
    
    WebViewClient webClient = new WebViewClient() {
    	public void onPageFinished(WebView view, String url) {
    		Log.d(TAG, "PAGE LOADED ======= " + url );
    		cookieSyncManager.sync();
    		
    		if ( url.startsWith(loginURL) ) {
    			dismissWaitDialog();
    			resumingTwoFactorAuth = false;
    		}
    		
    		if ( ! resumingTwoFactorAuth && url.startsWith(twoFactorAuthURL) ) {
    			resumingTwoFactorAuth = true;
    			showTwoFactorAuthDialog();
    		}
    		
    		if ( url.startsWith(targetURL) ) {
    			// This prints out all of the cookies as one long string.  Big pain in my ass.
//    			Log.d(TAG,"Cookie: " + cookieManager.getCookie("https://www.google.com") );
    			WebViewCookiesDB cookieDB = new WebViewCookiesDB(WebViewLoginActivity.this);
    			try {
    				List<Cookie> cookies = null; 
    				boolean loggedIn = false; 
    				long timeout = System.currentTimeMillis() + 10000;
    				while ( ! loggedIn ) { 
        				cookies = cookieDB.getCookies();
    					try { Thread.sleep(1500); } catch ( InterruptedException ex ) {}
	    				Log.d(TAG,"GOT cookies: " + cookies.size() );
	    				Set<String> cookieNames = new HashSet<String>();
	    				for ( Cookie c : cookies ) cookieNames.add(c.getName());
	    				
	    				if ( cookieNames.containsAll(requiredCookies) ) loggedIn = true;
	    				
	    				if ( System.currentTimeMillis() > timeout ) {
	    					Log.w(TAG,"Timeout looking for SID cookie!");
	    					break;
	    				}
    				}

    				int loginMsg = R.string.login_failed_msg;
    				if ( loggedIn ) {
	    				new GmarksProvider.DatabaseHelper(WebViewLoginActivity.this)
	    					.persistCookies( cookies );
	    				BookmarksQueryService.getInstance().setAuthCookies( cookies );	    				
		    			Log.d(TAG,"Logged in!");
		    			loginMsg = R.string.login_success_msg; 
    				}
	    			
	    			Toast.makeText(WebViewLoginActivity.this, loginMsg, Toast.LENGTH_LONG).show();
	    			WebViewLoginActivity.this.finishActivity(RESULT_OK);
	    			WebViewLoginActivity.this.finish();
    			}
    			finally { cookieDB.close(); }
    		}
    		else if ( ! url.startsWith("https://www.google.com/") ) {
    			Log.w(TAG, "Somehow we got redirected to a different domain! " + url);
    			view.loadUrl(targetURL);
    		}
    	}
    	
    	public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
    		if ( WebViewLoginActivity.this.waitDialog != null ) return; 
    		try {
	    		WebViewLoginActivity.this.waitDialog = ProgressDialog.show(
	    				WebViewLoginActivity.this, "", 
	    				getText(R.string.please_wait_msg), true );
    		}
    		// This probably happens if the activity is backgrounded 
    		catch ( BadTokenException ex ) { Log.w(TAG, "Couldn't show progress dialog...",ex); }
    	};
    	
    	public void onReceivedError(WebView view, int errorCode, 
    			String description, String failingUrl) {
    		Toast.makeText( WebViewLoginActivity.this, 
    				getString(R.string.login_failed_detail_msg, description), 
    				Toast.LENGTH_SHORT ).show();
    		
    		view.loadUrl(targetURL);
    	}
    };

    void getCookies() {
    	String dbLocation = this.webView.getSettings().getDatabasePath();
    	Log.d(TAG, "DB path: " + dbLocation );
    }
}
