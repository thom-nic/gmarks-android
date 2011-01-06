package org.thomnichols.android.gmarks;

//import mockit.UsingMocksAndStubs;

import org.apache.http.cookie.Cookie;
import org.junit.Before;
import org.junit.Test;
import org.thomnichols.android.gmarks.BookmarksQueryService;

import android.util.Log;

//@UsingMocksAndStubs({Log.class})
public class BookmarksQueryServiceTest {

	BookmarksQueryService gmarksSvc =  BookmarksQueryService.getInstance();
	
	@Before public void setUp() {
	}
	
//	@Test 
	public void testLogin() throws Exception {
//		gmarksSvc.login("test", "test");
		for( Cookie c : gmarksSvc.cookieStore.getCookies() ) {
			System.out.printf( "%s : %s%n", c.getName(), c.getValue() );
		}
	}
}
