package edu.utexas.tacc.tapis.shared.utils;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class PathSanitizer{
	//STATIC VARIABLES
	private final static String  dots = "..";
	private final static Pattern slashPattern = Pattern.compile("/");
	
	//Regex pattern that returns false if the string being checked DOES
    //contain any of the chars: &, >, <, |, ;, `
	private final static Pattern  prohibitedPattern = Pattern.compile("[^&><|;`]+");
	
	
	/** This method returns true if there are dangerous characters or patterns.
	 * 
	 * <p>
	 * This function is intended to be used to prevent users from inserting commands and 
	 * escapes to any string that makes its way to the command line. Checks for presence of 
     * control characters (\n,\t,etc.) as well as the presence of chars: &, >, <, |, ;, ` 
	 *
	 * @param cmdChars string to appear on command line
	 * @return true if presence of command line dangerous characters, false otherwise 
	 */
	public static boolean hasDangerousChars(String cmdChars) {
	    // Check for control characters, including \t, \n, \x0B, \f, \r.
	    for (int i = 0; i < cmdChars.length(); i++) {
	        if (Character.isISOControl(cmdChars.charAt(i))) return true;
	    }
	    
	    // Check for other characters that we want to prohibit
	    // from appearing on the command line.
	    Matcher m = prohibitedPattern.matcher(cmdChars);
	    if (!m.matches()) return true;
	    
        // No prohibited characters or substrings.
        return false;
	}  
	
	
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