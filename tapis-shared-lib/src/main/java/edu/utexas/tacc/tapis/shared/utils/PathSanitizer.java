package edu.utexas.tacc.tapis.shared.utils;

import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;

public class PathSanitizer 
{
	// Static variables
	private final static String  dots = "..";
	private final static Pattern slashPattern = Pattern.compile("/");
	
	// Regex pattern that returns true if the string being checked DOES NOT
    // contain any of the chars: &, >, <, |, ;, `
	private final static Pattern allowedPattern = Pattern.compile("[^&><|;`]+");
	
	// ** See TapisUtils.conditionalQuote() for similar processing.
	
	/* ---------------------------------------------------------------------- */
	/* detectControlChars:                                                    */
	/* ---------------------------------------------------------------------- */
	/** Throw an exception when encountering a control character in the input
	 * string. Control characters include tab, new line and carriage return.
	 * Exceptions are also thrown when an invalid codepoint is encountered.
	 * 
	 * @param s the input string to be validated
	 * @exception when a control character or invalid character is encountered
	 */
	public static void detectControlChars(String s) throws TapisException
	{
		// Maybe there's nothing to do.
		if (s == null || s.isEmpty()) return;
		
		// Index is visible in catch blocks.
		int i = 0;
		try {
			// Check for control characters, including \t, \n, \x0B, \f, \r.
			for (; i < s.length(); i++) {
				var codepoint = Character.codePointAt(s, i);  // possible exception here
				if (Character.isISOControl(codepoint)) {
					var cpName = Character.getName(codepoint);
					var cpHex = HexFormat.of().toHexDigits(codepoint);
					var msg = MsgUtils.getMsg("TAPIS_INVALID_INPUT_CHARACTER", cpHex, cpName, i, s);
					throw new TapisException(msg);
				}
			}
		} 
		catch (TapisException e) {throw e;}
		catch (Exception e) {
			var msg = MsgUtils.getMsg("TAPIS_INVALID_INPUT_CHARACTER", "????", "unknown", i, s);
			throw new TapisException(msg);
		}
	}

	/* ---------------------------------------------------------------------- */
	/* replaceControlChars:                                                   */
	/* ---------------------------------------------------------------------- */
	/** Replace each control character with the replacement character.  If the
	 * character at any position isn't a valid codepoint, then that position 
	 * will also be replaced.  To handle invalid codepoints and the exceptions
	 * they cause, we make a recursive call to this method for the segment of
	 * the input string starting one character after the offending index.
	 * 
	 * @param s the input string to be validated
	 * @param replacement the character to replace control characters
	 */
	public static String replaceControlChars(String s, char replacement)
	{
		// Maybe there's nothing to do.
		if (s == null || s.isEmpty()) return s;
		
		// Usually we won't need this string.
		StringBuilder builder = null;
		
		// Index is visible in catch blocks.
		int i = 0;
		try {
			// Check for control characters, including \t, \n, \x0B, \f, \r.
			for (; i < s.length(); i++) {
				var codepoint = Character.codePointAt(s, i);  // possible exception here
				if (Character.isISOControl(codepoint)) {
					if (builder == null) builder = new StringBuilder(s);
					builder.setCharAt(i, replacement);
				}
			}
		} 
		catch (Exception e) {
			// Work with the latest updates if any have occurred.
			String scur;
			if (builder != null) scur = builder.toString();
			  else scur = s; 
			
			// Get the string up to the current character that caused the exception
			// and append the replacement character, which will be in the ith position.
			String s1;
			if (i > 0) s1 = scur.substring(0, i);
			  else s1 = "";
			
			// Replace the character at the position that caused the exception.
			s1 += replacement;
			
			// Make the recursive call to process the remaining part of the string.
			// We increment the index to skip over the character that caused the 
			// current exception.  If we are already at the last index of the string,
			// there's nothing more to check.
			//
			// Since we advance the index on each recursive call and the string is of 
			// finite length, the recursion is guaranteed to terminate.
			String s2;
			if (i < s.length() - 1) s2 = replaceControlChars(s.substring(i+1), replacement);
			  else s2 = "";
			
			// Put the two pieces together.
			return s1 + s2;
		}
		
		// Return either the unchanged original string or a new modified string.
		if (builder == null) return s;
		  else return builder.toString();
	}

	/* ---------------------------------------------------------------------- */
	/* hasDangerousChars:                                                     */
	/* ---------------------------------------------------------------------- */
	/** This method returns true if there are dangerous characters or patterns
	 * in the input string.  Control and invalid characters are treated as dangerous.
	 *
	 * This function is intended to be used to prevent users from inserting commands and 
	 * escapes into any string that makes its way to the command line. Checks for presence of 
     * control characters (\n,\t,etc.) as well as the presence of chars: &, >, <, |, ;, ` 
     * 
     * NOTE: Spaces are not treated as dangerous.
     * 
     * NOTE: If needed improvements to this method would include (1) returning the 
     *       offending character and its position, and (2) allowing an option to 
     *       also treat spaces as dangerous. 
	 *
	 * @param cmdChars string to appear on command line
	 * @return true if presence of command line dangerous characters, false otherwise 
	 */
	public static boolean hasDangerousChars(String cmdChars) 
	{
		// Don't blow up.
		if (StringUtils.isBlank(cmdChars)) return false;
		
	    // Check for control characters, including \t, \n, \x0B, \f, \r.
		try {
			for (int i = 0; i < cmdChars.length(); i++) {
				var codepoint = Character.codePointAt(cmdChars, i); // possible exception here
				if (Character.isISOControl(codepoint)) return true;
			}
		} catch (Exception e) {return true;}
	    
	    // Check for other characters that we want to prohibit
	    // from appearing on the command line.
	    Matcher m = allowedPattern.matcher(cmdChars);
	    
	    // Return false if only allowed characters were found.
	    return !m.matches();
	}  

	/* ---------------------------------------------------------------------- */
	/* hasParentTraversal:                                                    */
	/* ---------------------------------------------------------------------- */
	/** Returns true if special case ".." is detected and string should be rejected.
	 * 
	 * <p>
	 * Method to check user supplied strings for presence of ".." path traversal pattern.
	 * This method is intended to be used to cleanse any string that makes its way to the 
	 * command line directly and should not allow for unwanted path traversal.
	 * 
	 * @param pathIn path to be tested
	 * @return true if has parent traversal, false otherwise
	 */
	public static boolean hasParentTraversal(String pathIn) {
		String[] contents = PathSanitizer.usingSplit(pathIn);
	    boolean containsChar = false;
		for(int i=0; i<contents.length; i++) {
			if(contents[i].equals(dots)) {
			    containsChar = true;
			    break;
			}
		}
		return containsChar;
	}
	
	/* ---------------------------------------------------------------------- */
	/* usingSplit:                                                            */
	/* ---------------------------------------------------------------------- */
    /** Method to parse paths by regex pattern of "/".
     * 
     * <p>
     * This returns each element of a path delimited by "/" in a regular string array.
     * 
     * @param path
     * @return String array representing the contents of a path delimited by "/"
     */
    static String[] usingSplit(String path) {
        return slashPattern.split(path);
    }
}