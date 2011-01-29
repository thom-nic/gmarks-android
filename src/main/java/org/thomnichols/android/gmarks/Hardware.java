package org.thomnichols.android.gmarks;

import java.util.HashSet;
import java.util.Set;

import android.os.Build;
import android.util.Log;

public final class Hardware {
	public static final Set<String> HAS_SEARCH_KEY = new HashSet<String>();
	static {
		Log.d("GMARKS HARDWARE", "This device's Build.MODEL is " + Build.MODEL);
		/* Many of these Build.MODEL codes have come from
		 * http://en.wikipedia.org/wiki/Comparison_of_Android_devices
		 * and http://www.glbenchmark.com/result.jsp
		 */
		final Set<String> s = HAS_SEARCH_KEY;
//		s.add("google_sdk"); // Debug only
		s.add("Liquid"); // Acer
		s.add("HTC Liberty"); // AKA Aria
		s.add("HTC Desire");
		s.add("HTC+Hero"); // AKA Droid Eris
		s.add("HERO200");
		s.add("T-Mobile+G2+Touch"); // HTC Hero variant
		s.add("ERA+G2+Touch"); // HTC Hero variant
		s.add("ADR6300"); // HTC Droid Incredible
		s.add("Virtual"); // HTC Legend
		s.add("HTC+Magic");
		s.add("HTC+Sapphire"); // HTC Magic Variant
		s.add("T-Mobile+myTouch+3G"); // HTC Magic Variant
		s.add("Docomo+HT-03A"); // HTC Magic Variant
		s.add("HTC+Tattoo"); // AKA 'Click' 
		s.add("PC36100"); // HTC Evo 4G 
		s.add("Nexus+One");
		s.add("Nexus One"); // according to GLBenchmark
		s.add("HTC Ace"); // Desire HD / MyTouch 4G
		s.add("HTC Wildfire");
		s.add("LG-VS740"); // LG Ally
		// TODO LG Axis
		s.add("MB501"); // Motorola Cliq XT 
		s.add("ME502"); // Motorola Charm
		s.add("WX445"); // Motorola Citrus
		s.add("Droid"); // AKA Milestone
		s.add("DROIDX"); 
		s.add("DROID2"); 
		s.add("Nexus S");
		s.add("SCH-r880"); // Samsung Acclaim (US Cellular) 
		s.add("SGH-I897"); // Samsung Captivate 
		s.add("SAMSUNG-SGH-I897"); // Samsung Captivate 
		s.add("GT-I897"); // Samsung Captivate 
		s.add("SGH-T959D"); // Samsung Vibrant
		s.add("SGH-T959"); // Samsung Vibrant
		s.add("SCH-I500"); // Samsung Fascinate/ Showcase/ Mesmerize 
		s.add("SPH-D700"); // Samsung/ Sprint Epic 4G
		s.add("M910"); // Samsung Intercept
		s.add("SPH-M910"); // Samsung Intercept
		// the following are un-confirmed:
		s.add("MB525"); // Motorola Defy (guess) 
		s.add("DROIDPRO"); // TODO (guess) 
		s.add("SPH-M920"); // Samsung Transform (guess) 
		s.add("M920"); // Samsung Transform (guess) 
		s.add("GT-i9020"); // Nexus S (guess) 
//		s.add(""); //  
	}
	
	public static boolean hasSearchButton() {
		return HAS_SEARCH_KEY.contains(Build.MODEL);
	}
	
	private Hardware() {}
}
