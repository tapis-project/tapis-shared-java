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
	
	// Regex pattern that returns true if the string being checked DOES NOT
	// contain any of the chars: &, >, <, |, ;, `, <space>
	private final static Pattern safePathPattern = Pattern.compile("[^ &><|;`]+");
	
	/* ---------------------------------------------------------------------- */
	/* detectControlChars:                                                   */
	/* ---------------------------------------------------------------------- */
	/** Throw an exception when encountering a all control character in the 
	 * input string.  Exceptions are also thrown when an invalid codepoint is 
	 * encountered.
	 * 
	 * @param s the input string to be validated
	 * @exception when a control character or invalid character is encountered
	 */
	public static void detectControlChars(String s) throws TapisException
	{
		// Maybe there's nothing to do.
		if (s == null) return;
		
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
			var msg = MsgUtils.getMsg("TAPIS_INVALID_INPUT_CHARACTER", "xxxx", "unknown", i, s);
			throw new TapisException(msg);
		}
	}

	/* ---------------------------------------------------------------------- */
	/* sanitizeForCmdLine:                                                    */
	/* ---------------------------------------------------------------------- */
	/** Conditionally double quote the input string for safe use on the command
	 * line.  This method will conditionally double quote the string if it does
	 * not match the safePathPattern.  
	 * 
	 * It is assumed that the string contains no control characters (see 
	 * detectControlChars()).  This method checks for the presence of chars: 
	 * 
	 * 					&, >, <, |, ;, `, <space>
	 *
	 * If the string is already double quoted it will not be changed.  If the
	 * string is not already double quoted and it contains unsafe command line
	 * characters, it will be double quoted.  If it contains no unsafe characters
	 * the input string will be returned unchanged.
	 * 
	 * @param s an input string to appear on the command line 
	 * @return a command line safe version of the string  
	 */
	public static String sanitizeForCmdLine(String s)
	{
		// Maybe there's nothing to do.
		if (StringUtils.isBlank(s)) return s;
		
		// Don't double quote a string that's already double quoted.
		if (s.startsWith("\"") && s.endsWith("\"")) return s;
		
	    // Check for characters that we want to prohibit
	    // from appearing on the command line unquoted.
	    Matcher m = safePathPattern.matcher(s);
	    if (!m.matches()) s = TapisUtils.safelyDoubleQuoteString(s);
		
		return s;
	}
	
	/* ---------------------------------------------------------------------- */
	/* hasDangerousChars:                                                     */
	/* ---------------------------------------------------------------------- */
	/** This method returns true if there are dangerous characters or patterns
	 * in the input string.  Invalid characters are treated as dangerous.
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
	public static boolean hasDangerousChars(String cmdChars) {
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
	    
	    // If only allowed characters found, return false.
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