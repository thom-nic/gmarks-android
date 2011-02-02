/*
 * This script is based on 'StringsResourceTranslator.java' created by 
 * the ZXing team/ Sean Owen
 * http://zxing.googlecode.com/svn-history/r1442/trunk/javase/src/com/google/zxing/StringsResourceTranslator.java
 *
 * Re-written to Groovy by Thom Nichols
 */
import java.util.regex.Matcher;
import java.util.regex.Pattern;

ENTRY_PATTERN = Pattern.compile('<string name="([^"]+)">([^<]+)</string>');
STRINGS_FILE_NAME_PATTERN = Pattern.compile("values-(.+)");
TRANSLATE_RESPONSE_PATTERN = Pattern.compile( /\{"translatedText":"([^"]+)"\}/ );

LANGUAGE_CODE_MASSAGINGS = [ 
		"ja-rJP": "ja",
		"zh-rCN": "zh-cn",
		"zh-rTW": "zh-tw"]

void translate(File englishFile, File translatedFile, Collection<String> forceRetranslation) {

	SortedMap<String,String> english = readLines(englishFile)
	if ( ! translatedFile.exists() ) translatedFile.createNewFile()
	SortedMap<String,String> translated = readLines(translatedFile)
	String parentName = translatedFile.parentFile.name

	Matcher stringsFileNameMatcher = STRINGS_FILE_NAME_PATTERN.matcher(parentName)
	stringsFileNameMatcher.find()
	String language = stringsFileNameMatcher.group(1)
	String massagedLanguage = LANGUAGE_CODE_MASSAGINGS.get(language)
	if (massagedLanguage) language = massagedLanguage

	println "Translating " + language

	File resultTempFile = File.createTempFile(parentName, ".xml")
	resultTempFile.deleteOnExit()

	boolean anyChange = false
	resultTempFile.withWriter('UTF-8') { out ->
		out << '<?xml version="1.0" encoding="UTF-8"?>\n'
		out << "<resources>\n"

		english.entrySet().each { englishEntry ->
			String key = englishEntry.key
			String englishString = englishEntry.value

			String translatedString = translated[key]
			if (translatedString == null || forceRetranslation.contains(key)) {
				anyChange = true
				translatedString = translateString(englishString, language)
			}
			if ( englishString == translatedString ) return // skip identical strings
			out << """  <string name="$key">"""
			out << translatedString
			out << "</string>\n"
		}
		out << "</resources>\n"
	}

	if (anyChange) {
		println "  Writing translations for $language"
		resultTempFile.withInputStream { _in -> 
			translatedFile.withOutputStream { out -> out << _in }
		}
	}
}

String translateString(String english, String language) {
	println "  Need translation for " + english
	URL translateURL = new URL(
			"http://ajax.googleapis.com/ajax/services/language/translate?v=1.0&q=" +
			URLEncoder.encode(english, "UTF-8") +
			"&langpair=en%7C" + language)

	Matcher m = TRANSLATE_RESPONSE_PATTERN.matcher(translateURL.text)
	if ( ! m.find() ) throw new IOException("No translate result")
	String translation = m.group(1)
	println "  Got translation " + translation
	return translation
}

SortedMap<String,String> readLines(File file) {
	SortedMap<String,String> entries = new TreeMap<String,String>()
	file.eachLine('UTF-8') { line ->
		Matcher m = ENTRY_PATTERN.matcher(line)
		if ( !m.find() ) return
		entries.put m.group(1), m.group(2)
	}
	return entries
}


// main():
File resDir = new File(args[0])
File valueDir = new File(resDir, "values")
File stringsFile = new File(valueDir, "strings.xml")
Collection<String> forceRetranslation = args.length > 1 ? args[1..-1] : []

File[] translatedValuesDirs = resDir.listFiles(new FileFilter() {
	public boolean accept(File file) {
		return file.isDirectory() && file.name.startsWith("values-")
	}
})

translatedValuesDirs.each { translatedValuesDir ->
	File translatedStringsFile = new File(translatedValuesDir, "strings.xml")
	translate stringsFile, translatedStringsFile, forceRetranslation
}

