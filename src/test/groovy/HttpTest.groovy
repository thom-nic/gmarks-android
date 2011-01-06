import groovyx.net.http.HTTPBuilder;
import static groovyx.net.http.Method.*
import static groovyx.net.http.ContentType.*

import org.apache.http.impl.client.BasicCookieStore;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.5.1' )
public class HttpTest {
	
	def http = null
	
	def user = System.getProperty("user");
	def password = System.getProperty("password");
	
//	@Before 
	public void setUp() {
		http = new HTTPBuilder('https://www.google.com/')
		http.headers."User-Agent" = 'Mozilla/5.0 (Linux; U; Android 2.1; en-us) AppleWebKit/530.17 (KHTML, like Gecko) Version/4.0 Mobile Safari/530.17'
		http.headers.Accept = '*/*'
	}
	
//	@Test 
	public void testGet() {
		http.client.cookieStore = new BasicCookieStore()
		def resp = http.request(GET,TEXT) {
		 uri.path = '/bookmarks/l'
		 uri.query = ['fa':'1']
		 
		 response.success = { resp, data ->
		    println resp.context['http.target_host'].toURI()
		    println resp.context['http.request'].URI
			println '--------------------------'
			System.out << data
		 }
		}
		
		println "Cookies: ------------------ "
		http.client.cookieStore.cookies.each {
		    println "${it.name} : ${it.value}"
		}
	}
	
//	@Test 
	public void testLogin() {
		http.request( GET, TEXT ) {
			uri.path = '/accounts/ServiceLogin'
			uri.query = [ service:'bookmarks',
				passive:'true', nui:'1',
				'continue':'https://www.google.com/bookmarks/l',
				followup:'https://www.google.com/bookmarks/l' ]
			
			response.success = { resp, data ->
				println "ServiceLogin RESPONSE: ${resp.status}"
			 }
		}
		
		def galx = http.client.cookieStore.cookies.find { it.name == 'GALX' }.value
		Assert.assertNotNull( galx )
		
		def location = http.request( POST, TEXT ) {
			uri.path = '/accounts/ServiceLoginAuth'
			send URLENC, ['Email': user,
				'Passwd' : password,
				"PersistentCookie": "yes",
				"continue": "https://www.google.com/bookmarks/l",
				GALX: galx ]

			response.'302' = { resp, data ->
				println "ServiceLoginAuth RESPONSE: ${resp.status}"
			    println "Location: ${resp.headers.Location}"
				return resp.headers.Location
			 }
		}
		
		def finalLocation = http.request( location, GET, TEXT ) {
			response.'302' = { resp, data ->
				println "CheckCookie final location: ${resp.headers.Location}"
				return resp.headers.Location	
			}
		}
		
		http.request( GET, TEXT ) {
			uri.path = '/bookmarks/api/threadsearch'
			uri.query = [q: '', start: '', fo: '', g:'']
			headers.Accept = "text/javascript"
			response.success = { resp, data ->
				println "Threadsearch response! -------------------------"
				System.out << data	
				println()
			}
		}
		
		
		println "Cookies: ------------------ "
		http.client.cookieStore.cookies.each {
		    println "${it.name} : ${it.value}"
		}
	}
}