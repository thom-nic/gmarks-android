/*
 * This script is based on 'StringsResourceTranslator.java' created by 
 * the ZXing team/ Sean Owen
 * http://zxing.googlecode.com/svn-history/r1442/trunk/javase/src/com/google/zxing/StringsResourceTranslator.java
 *
 * Re-written to Groovy by Thom Nichols
 * IMPROVEMENTS:
 * - Using a real JSON parser, which handles JSON-encoded (\u1234) characters
 * - HTML entity un-escaping
 * - Resource string tokens (like %s) are preserved (Google translate mangles it.)
 * x Strings without a translation are ignored, not put in translated output file.
 *   (disabled, since it causes a lot of strings to be re-translated every time.)
 */
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.json.JSONObject

ENTRY_PATTERN = Pattern.compile('<string name="([^"]+)">([^<]+)</string>')
STRINGS_FILE_NAME_PATTERN = Pattern.compile("values-(.+)")
STRING_TOKEN_PATTERN = Pattern.compile( /\\?['"]?%[dsfx]\\?['"]?/ )
HTML_ENTITY_PATTERN = Pattern.compile( /&#(\d+);/ )

HTML_ENTITIES = [
		"34": '"',
		"38": '&',
		"39": "'",
		"60": '<',
		"62": '>' ]

LANGUAGE_CODE_MASSAGINGS = [ 
		"ja-rJP": "ja",
		"zh-rCN": "zh-cn",
		"zh-rTW": "zh-tw" ]

@Grab(group='org.json', module='json', version='20090211')
void translate(File englishFile, File translatedFile, Collection<String> forceRetranslation) {

	SortedMap<String,String> english = readLines(englishFile)
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
//			if ( englishString == translatedString ) return // skip identical strings
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
	println "  >> " + english

	/* Google translate mangles tokens like '%s' so we substitute them 
	 * with a single unicode character that will be left alone */ 
	Matcher tokenMatch = STRING_TOKEN_PATTERN.matcher( english )
	StringBuffer sb = new StringBuffer()
	Map tokens = [:] // used to keep track of substitutions
	// start with a character that is virtually guaranteed not to be used in text:
	int charPoint = 0x10330 
	while ( tokenMatch.find() ) {
		def replaceChar = (charPoint++) as char
		tokens[replaceChar] = tokenMatch.group(0)
		tokenMatch.appendReplacement(sb,(String)replaceChar)
	}
	english = tokenMatch.appendTail(sb).toString()
	URL translateURL = new URL(
			"http://ajax.googleapis.com/ajax/services/language/translate?v=1.0&q=" +
			URLEncoder.encode(english, "UTF-8") +
			"&langpair=en%7C" + language)

	/* Request Google translation.  Sometimes Google replies back with a
	 * 'not ok' status code, but it usually works after a couple retries. */
	int retries=5
	def resp = new JSONObject( translateURL.getText("UTF-8") )
	while ( resp.getInt('responseStatus') != 200 && --retries > 0 )
		resp = new JSONObject( translateURL.getText("UTF-8") )		
	if ( retries == 0 ) throw new IOException("No translate result")
	
	// parse as JSON to remove \u0000-encoded characters
	String translation = resp.getJSONObject('responseData').getString('translatedText')
	
	// replace HTML entities:
	Matcher entityMatch = HTML_ENTITY_PATTERN.matcher( translation )
	sb = new StringBuffer()
	while ( entityMatch.find() ) {
		def entityCode = entityMatch.group(1)
		if ( ! HTML_ENTITIES.containsKey(entityCode) )
			throw new IOException('Unknown entity code: &#$entityCode;')
		entityMatch.appendReplacement( sb, HTML_ENTITIES[entityCode] )
	} 
	translation = entityMatch.appendTail(sb).toString()
	
	// Replace each unicode character substitute with the original token:
	tokens.each { translation = translation.replace((String)it.key, it.value) }	
	println "  << " + translation
	return translation
}

SortedMap<String,String> readLines(File file) {
	SortedMap<String,String> entries = new TreeMap<String,String>()
	if ( ! file.exists() ) return entries
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

