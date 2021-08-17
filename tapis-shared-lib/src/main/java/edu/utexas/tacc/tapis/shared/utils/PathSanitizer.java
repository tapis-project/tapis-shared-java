package edu.utexas.tacc.tapis.shared.utils;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class PathSanitizer{
	//STATIC VARIABLES
	final static String dotPattern = "..";
	final static Pattern dotPatt = Pattern.compile(dotPattern);
	final static String slashPattern = "/";
	final static Pattern slashPatt = Pattern.compile(slashPattern);
	//Regex pattern that returns true if the string being checked DOES NOT
    //contain any of the chars: &, >, <, |, ;, `
	final static String regPattern = "[^&><|;`]+";
	final static Pattern regPatt = Pattern.compile(regPattern);
	final static String[] dangPatterns = {"..","&&","||","\n",};
	
	//Method to parse paths by regex pattern of "/". This returns each element of a path 
	//delimited by "/" in a regular string array.
	public static String[] usingSplit(String path) {
		return slashPatt.split(path);
	}
	
	
	//Regex function to check user supplied strings for dangerous characters. This
	//function is intended to be used to prevent users from inserting commands and 
	//escapes to any string that makes its way to the command line. Checks for presence of 
	//patterns: "..", "&&", "||", "\n" and additionally the presence of chars: &, >, <, |, ;, ` 
	//This method returns true if there are NO dangerous characters or patterns.
	public static boolean strictDangerousCharCheck(String pathIn) {
        for(int i=0; i<dangPatterns.length; i++) {
        	if(pathIn.indexOf(dangPatterns[i]) != -1) {
        		return false;
        	}
        }
        Matcher m = regPatt.matcher(pathIn);
        //Regex pattern that returns true if the string being checked DOES NOT
        //contain any of the chars: &, >, <, |, ;, `
		boolean exitFlag = m.matches();
        return exitFlag;
	}  
	
	
	//Method to check user supplied strings for presence of ".." path traversal pattern.
	//This function is intended to be used to cleanse any string that makes its 
	//way to the command line directly and should not allow for unwanted path traversal.
	//Returns true if special case ".." is detected and string should be rejected.
	public static boolean splitAndCheckForParent(String pathIn) {
		String[] contents = PathSanitizer.usingSplit(pathIn);
	    boolean containsChar = false;
		for(int i=0; i<contents.length; i++) {
			if(contents[i].matches(dotPattern)) {
			    containsChar = true;
			}
		}
		return containsChar;
	}
}