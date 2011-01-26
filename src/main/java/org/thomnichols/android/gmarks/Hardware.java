package org.thomnichols.android.gmarks;

import java.util.HashSet;
import java.util.Set;

import android.os.Build;

public final class Hardware {
	public static final Set<String> HAS_SEARCH_KEY = new HashSet<String>();
	static {
		/* Many of these Build.MODEL codes have come from
		 * http://en.wikipedia.org/wiki/Comparison_of_Android_devices
		 */
		final Set<String> s = HAS_SEARCH_KEY;
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
		s.add("HTC Ace"); // Desire HD / MyTouch 4G
		s.add("HTC Wildfire");
		s.add("LG-VS740"); // LG Ally
		// TODO LG Axis
		s.add("MB501"); // Motorola Cliq XT 
		s.add("ME502"); // Motorola Charm
		s.add("Droid"); // AKA Milestone
		s.add("DROIDX"); 
		s.add("DROID2"); 
		s.add("SCH-r880"); // Samsung Acclaim (US Cellular) 
		s.add("SGH-I897"); // Samsung Captivate 
		s.add("M910"); // Samsung Intercept
		s.add("MB525"); // Motorola Defy (guess) 
		s.add("DROIDPRO"); // TODO (guess) 
		s.add("SGH-T959"); // Samsung Vibrant (guess) 
		s.add("SGH-t959"); // Samsung Vibrant (guess) 
		s.add("SCH-I500"); // Samsung Fascinate/ Showcase/ Mesmerize (guess) 
		s.add("SCH-i500"); // Samsung Fascinate/ Showcase/ Mesmerize (guess) 
		s.add("SPH-D700"); // Samsung/ Sprint Epic 4G (guess) 
		s.add("SPH-d700"); // Samsung/ Sprint Epic 4G (guess)
		s.add("D700"); // Samsung/ Sprint Epic 4G (guess) 
		s.add("SPH-M920"); // Samsung Transform (guess) 
		s.add("SPH-m920"); // Samsung Transform (guess) 
		s.add("M920"); // Samsung Transform (guess) 
		s.add("GT-i9020"); // Nexus S (guess) 
//		s.add(""); //  
	}
	
	public static boolean hasSearchButton() {
		return HAS_SEARCH_KEY.contains(Build.MODEL);
	}
	
	private Hardware() {}
}
