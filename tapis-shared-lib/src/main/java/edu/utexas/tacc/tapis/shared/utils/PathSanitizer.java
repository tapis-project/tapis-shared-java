package edu.utexas.tacc.tapis.shared.utils;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class PathSanitizer{
	//STATIC VARIABLES
	private final static String  dots = "..";
	private final static Pattern slashPattern = Pattern.compile("/");
	
	//Regex pattern that returns true if the string being checked DOES NOT
    //contain any of the chars: &, >, <, |, ;, `
	private final static Pattern  prohibitedPattern = Pattern.compile("[&><|;`]+");
	
	
	//Regex function to check user supplied strings for dangerous characters. This
	//function is intended to be used to prevent users from inserting commands and 
	//escapes to any string that makes its way to the command line. Checks for presence of 
	//patterns: "..", "&&", "||", "\n" and additionally the presence of chars: &, >, <, |, ;, ` 
	//This method returns true if there are NO dangerous characters or patterns.
	/** This method returns true if there are NO dangerous characters or patterns.
	 * 
	 * @param cmdChars string to appear on command line
	 * @return true if no command line dangerous characters, false otherwise 
	 */
	public static boolean hasDangerousChars(String cmdChars) {
	    // Check for control characters, including \t, \n, \x0B, \f, \r.
	    for (int i = 0; i < cmdChars.length(); i++) {
	        if (Character.getNumericValue(cmdChars.charAt(i)) < 32) return true;
	    }
	    
	    // Check for other characters that we want to prohibit
	    // from appearing on the command line.
	    Matcher m = prohibitedPattern.matcher(cmdChars);
	    if (m.matches()) return true;
	    
        // No prohibited characters or substrings.
        return false;
	}  
	
	
	//Method to check user supplied strings for presence of ".." path traversal pattern.
	//This function is intended to be used to cleanse any string that makes its 
	//way to the command line directly and should not allow for unwanted path traversal.
	//Returns true if special case ".." is detected and string should be rejected.
	/** Returns true if special case ".." is detected and string should be rejected.
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
			}
		}
		return containsChar;
	}
	
	//Method to parse paths by regex pattern of "/". This returns each element of a path 
    //delimited by "/" in a regular string array.
    /**
     * 
     * @param path
     * @return
     */
    static String[] usingSplit(String path) {
        return slashPattern.split(path);
    }
}