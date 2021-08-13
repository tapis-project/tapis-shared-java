package edu.utexas.tacc.tapis.shared.utils;

import java.io.IOException;
import java.net.URI;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;


public class PathSanitizer{
	public static void main(String[] args) {
	    String path1 = "home/bud/mydir/myfile1";
	    String path2 = "home/bud/mydir/myfile1.txt";
	    String path3 = "home/bud/../mydir/myfile1.txt";
	    String path4 = "home/bud/../mydir/myfile1.txt; ls -al";
	    String path5 = "home/bud/../mydir/myfile1.txt; ls -al && ls";
		
	    //usingJavaRegexForPattern("/home/bud/../mydir/myfile","..");      
	    
	    System.out.println(detectParentDir(path1)); //true, is alphanum and contains accepted char "/"
	    System.out.println(detectParentDir(path2)); //true, is alphanum and contains accepted char "/","."
	    System.out.println(detectParentDir(path3)); //false, contains unwanted pattern ".."  
	    System.out.println(detectParentDir(path4)); //false, contains unwanted pattern ".."
	    System.out.println(detectParentDir(path5) + "\n"); //false, contains unwanted pattern ".."
	   
	    
	    System.out.println(strictDangerousCharCheck(path1)); //true, doesn't contain specified dangerous chars
	    System.out.println(strictDangerousCharCheck(path2)); //true, doesn't contain specified dangerous chars
	    System.out.println(strictDangerousCharCheck(path3)); //true, doesn't contain specified dangerous chars
	    System.out.println(strictDangerousCharCheck(path4)); //false, contains dangerous char ";"
	    System.out.println(strictDangerousCharCheck(path5) + "\n"); // false, contains dangerous chars ";" and "&"
	    
	    URI x;
	    
	    
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
	
	//Regex function to check user supplied strings for presence of non alphanumeric chars.
	//This function is intended to be used to cleanse any string that makes its 
	//way to the command line directly and should not allow for unwanted path
	//traversal (ex: host_eval($HOME). When this function returns true it is
	//signaling that the string being checked is comprised of only alphanumeric chars.
	
	public static boolean detectParentDir(String in) {
	    final String pattern = "..";
	    if(in.indexOf(pattern) != -1) return false;  
	    else return true;
	}
	
	
	//Regex function to check user supplied strings for dangerous characters. This
	//function is intended to be used to prevent users from inserting commands and 
	//escapes to any string that makes its way to the command line. Checks for presence of 
	//patterns: "..", "&&", "||", "\n" and additionally the presence of
	//chars $, &, >, |, ;, `
	public static boolean strictDangerousCharCheck(String in) {
        String[] patterns = {"..","&&","||","\n",};
        for(int i=0; i<patterns.length; i++) {
        	if(in.indexOf(patterns[i]) != -1) {
        		return false;
        	}
        }
		boolean exitFlag = Pattern.matches("[^$&>|;`]+", in);
        return exitFlag;
	}
	
	public static void usingJavaRegexCharClass() {
		
		//Note: alphanumeric only version of regex does not allow for any whitespace
		
		System.out.println("\nREGEX TEST SET 1");
		//match "[abc]" a,b,or c
		System.out.println(Pattern.matches("[abc]", "a"));      				//true (not a or b or c)
		System.out.println(Pattern.matches("[abc]", "z"));      				//false (not a or b or c)
		System.out.println(Pattern.matches("[abc]", "abc"));    				//false (a, b, and c occur more than once)
		System.out.println(Pattern.matches("[abc]{1,}", "abc"));				//true (a or b or c occur one or more times)
		
		System.out.println("\nREGEX TEST SET 2");
		//not match "[^abc]" any char but a,b, or c
		System.out.println(Pattern.matches("[^abc]", "a"));     				//false (among a or b or c)
		System.out.println(Pattern.matches("[^abc]", "z"));     				//true (not among a or b or c)
		System.out.println(Pattern.matches("[^abc]", "abc"));   				//false (a, b, and c occur more than once)
		System.out.println(Pattern.matches("[abc]{1,}", "abc"));				//true (a or b or c occur one or more times)
		
		System.out.println("\nREGEX TEST SET 3");
		//range match "[a-cf-h]" a through c or f through h
		System.out.println(Pattern.matches("[a-cf-h]", "a"));   				//true (among a through c)
		System.out.println(Pattern.matches("[a-cf-h]", "d"));   				//false (not among a through c or f through h)
		System.out.println(Pattern.matches("[a-cf-h]", "abc")); 				//false (a, b, and c occur more than once
		
		System.out.println("\nREGEX TEST SET 4");
		//alphanumeric only match
		System.out.println(Pattern.matches("[a-zA-Z0-9]{1,}", "a"));   			//true (a is an alphanumeric char)
		System.out.println(Pattern.matches("[a-zA-Z0-9]{1,}", "abc")); 			//true (a, b, and c are alphanumeric chars)
		System.out.println(Pattern.matches("[a-zA-Z0-9]{1,}", "abc;"));			//false (; is not an alphanumeric char)
		
		System.out.println("\nREGEX TEST SET 5");
		//alphanumeric only with additional character cases like "/"
		System.out.println(Pattern.matches("[a-zA-Z0-9/]{1,}", "a")); 			 //true (a is an alphanumeric char)
		System.out.println(Pattern.matches("[a-zA-Z0-9/]{1,}", "a1")); 			//true (a and 1 are both alphanumeric chars)
		System.out.println(Pattern.matches("[a-zA-Z0-9/]{1,}", "a/")); 			//true (a is alphanumeric and / is an accepted special char)
		System.out.println(Pattern.matches("[a-zA-Z0-9/]{1,}", "a/;"));			//false (; is not an alphanumeric char or an accepted special char)
		
		System.out.println("\nREGEX TEST SET 6");
		//negation of specified dangerous characters
		System.out.println(Pattern.matches("[^!@#$%^&*()>|;]{1,}", "a"));     	//true (a is not among the specified chars)
		System.out.println(Pattern.matches("[^!@#$%^&*()>|;]{1,}", "abc123"));	//true (abc123 are all not among the specified chars)
		System.out.println(Pattern.matches("[^!@#$%^&*()>|;]{1,}", "a/"));    	//true (a/ are both not among the specified chars)
		System.out.println(Pattern.matches("[^!@#$%^&*()>|;]{1,}", "a/;"));   	//false (; is among the specified chars)
		System.out.println(Pattern.matches("[^!@#$%^&*()>|;]{1,}", "a/;;"));  	//false (; is among the specified chars, more a test for duplicate ;;)
		
	}
	   
}