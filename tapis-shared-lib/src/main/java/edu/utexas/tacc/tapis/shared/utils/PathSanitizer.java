package edu.utexas.tacc.tapis.shared.utils;

import java.io.IOException;
import java.net.URI;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.commons.io.FilenameUtils;

public class PathSanitizer{
	//STATIC VARIABLES
	final static String dotPattern = "..";
	final static Pattern dotPatt = Pattern.compile(dotPattern);
	final static String slashPattern = "/";
	final static Pattern slashPatt = Pattern.compile(slashPattern);
	//Regex pattern that returns true if the string being checked DOES NOT
    //contain any of the chars: $, &, >, |, ;, `
	final static String regPattern = "[^$&>|;`]+";
	final static Pattern regPatt = Pattern.compile(regPattern);
	final static String[] dangPatterns = {"..","&&","||","\n",};
	
	public static String[] usingSplit(String path) {
		return slashPatt.split(path);
	}
	
	
	//Not very useful for path sanitizing
	//Checks for ".." and "." and recognizes them, but it then returns
	//the path with ".." and "." executed meaning /home/bud/../mydir/myfile
	//returns /home/mydir/myfile
	//
    //FilenameUtils class also not very helpful for recognizing 
    //dangerous characters like ";","|",">x"
	
	public static void usingFilenameUtils() throws IOException {
	    String path = "/home/bud/../mydir/myfile.txt";
	    System.out.println("Full Path: " +FilenameUtils.getFullPath(path));
	    System.out.println("Relative Path: " +FilenameUtils.getPath(path));
	    System.out.println("Prefix: " +FilenameUtils.getPrefix(path));
	    System.out.println("Extension: " + FilenameUtils.getExtension(path));
	    System.out.println("Base: " + FilenameUtils.getBaseName(path));
	    System.out.println("Name: " + FilenameUtils.getName(path));

	    String filename = "/home/bud/../mydir/./myfile";
	    System.out.println("Normalized Path: " + FilenameUtils.normalize(filename));
	}
	
	//Method to check user supplied strings for presence of ".." path traversal pattern.
	//This function is intended to be used to cleanse any string that makes its 
	//way to the command line directly and should not allow for unwanted path traversal. 
	//When this function returns true it is signaling that the path being checked DOES NOT
	//contain "..". This function can be extended to include a list of unwanted pattern to check for.
	public static boolean detectParentDir(String in) {
	    if(in.indexOf(dotPattern) != -1) return false;  
	    else return true;
	}
	
	
	//Regex function to check user supplied strings for dangerous characters. This
	//function is intended to be used to prevent users from inserting commands and 
	//escapes to any string that makes its way to the command line. Checks for presence of 
	//patterns: "..", "&&", "||", "\n" and additionally the presence of chars: $, &, >, |, ;, ` 
	//This method returns true if there are NO dangerous characters or patterns.
	public static boolean strictDangerousCharCheck(String pathIn) {
        for(int i=0; i<dangPatterns.length; i++) {
        	if(pathIn.indexOf(dangPatterns[i]) != -1) {
        		return false;
        	}
        }
        Matcher m = regPatt.matcher(pathIn);
        //Regex pattern that returns true if the string being checked DOES NOT
        //contain any of the chars: $, &, >, |, ;, `
		boolean exitFlag = m.matches();
        return exitFlag;
	}  
	
	//returns true if special case is detected and string should be rejected
	//TODO: ADD FUNCTION TO PROCESS UNICODE BLOCKS EX: \u002e
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