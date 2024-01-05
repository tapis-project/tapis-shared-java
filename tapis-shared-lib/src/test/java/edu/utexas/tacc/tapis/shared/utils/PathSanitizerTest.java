package edu.utexas.tacc.tapis.shared.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;


@Test(groups= {"unit"})
public class PathSanitizerTest {
	/* **************************************************************************** */
    /*                                    Tests                                     */
    /* **************************************************************************** */	
	/* ---------------------------------------------------------------------------- */
    /* strictDangerousCharCheckTest                                                 */
    /* ---------------------------------------------------------------------------- */
	@Test(enabled = true)
	public void hasDangerousCharsTest()
	{
        String dirPath = "/home/bud/mydir/myfile";
        boolean out = PathSanitizer.hasDangerousChars(dirPath);
        Assert.assertEquals(out, false);
        
        dirPath = "home/bud/mydir/myfile1.txt";
        out = PathSanitizer.hasDangerousChars(dirPath);
        Assert.assertEquals(out, false);
       
        dirPath = "home/bud/../mydir/myfile1.txt";
        out = PathSanitizer.hasDangerousChars(dirPath);
        Assert.assertEquals(out, false);
        
        dirPath = "home/bud/mydir/myfile1.txt;";
        out = PathSanitizer.hasDangerousChars(dirPath);
        Assert.assertEquals(out, true);
        
        dirPath = "home/bud/mydir/myfile1.txt; ls -al & ls -al";
        out = PathSanitizer.hasDangerousChars(dirPath);
        Assert.assertEquals(out, true);
        
        dirPath = "home/bud/mydir/myfile1.txt; \n \t";
        out = PathSanitizer.hasDangerousChars(dirPath);
        Assert.assertEquals(out, true);
        
        dirPath = "&><|;`";
        out = PathSanitizer.hasDangerousChars(dirPath);
        Assert.assertEquals(out, true);
        
        dirPath = "temporary; whoami > whoami.txt";
        out = PathSanitizer.hasDangerousChars(dirPath);
        Assert.assertEquals(out, true);
	}
	
	/* ---------------------------------------------------------------------------- */
    /* splitterTest                                                                 */
    /* ---------------------------------------------------------------------------- */
	@Test(enabled = true)
	public void splitterTest() {
	    String dirPath = "/home/bud/mydir/myfile";
	    String[] out = PathSanitizer.usingSplit(dirPath);
	    String[] correct = {"","home","bud","mydir","myfile"};
	    Assert.assertEquals(out,correct);
	    
	    dirPath = "/home../..bud/my..dir/myfile/..";
	    out = PathSanitizer.usingSplit(dirPath);
	    String[] correct2  = {"","home..","..bud","my..dir","myfile",".."};
	    Assert.assertEquals(out,correct2);
	}
	
	
	/* ---------------------------------------------------------------------------- */
    /* splitAndCheckForParentTest                                                   */
    /* ---------------------------------------------------------------------------- */
	@Test(enabled = true)
	public void hasParentTraversalTest() {
        String dirPath = "/home/bud/mydir/myfile";
        boolean out = PathSanitizer.hasParentTraversal(dirPath);
        Assert.assertEquals(out, false);
        
        dirPath = "/home../..bud/my..dir/myfile";
        out = PathSanitizer.hasParentTraversal(dirPath);
        Assert.assertEquals(out, false);
        
        dirPath = "/home../..bud/my..dir/myfile/..";
        out = PathSanitizer.hasParentTraversal(dirPath);
        Assert.assertEquals(out, true);      
        
        dirPath = "/home../..bud/my..dir/myfile/\u002e\u002e";
        out = PathSanitizer.hasParentTraversal(dirPath);
        Assert.assertEquals(out,true);
        
        dirPath = "/home../..bud/my..dir/myfile/.\u002e";
        out = PathSanitizer.hasParentTraversal(dirPath);
        Assert.assertEquals(out,true);
        
        dirPath = "/home../..bud/my..dir/myfile/\u002e.";
        out = PathSanitizer.hasParentTraversal(dirPath);
        Assert.assertEquals(out,true);
        
        dirPath = "/home../..bud/my..dir/myfile/\56\56";
        out = PathSanitizer.hasParentTraversal(dirPath);
        Assert.assertEquals(out,true);
        
        dirPath = "/home../..bud/my..dir/myfile/.\56";
        out = PathSanitizer.hasParentTraversal(dirPath);
        Assert.assertEquals(out,true);
        
        dirPath = "/home../..bud/my..dir/myfile/\56.";
        out = PathSanitizer.hasParentTraversal(dirPath);
        Assert.assertEquals(out,true);
	}
	
	/* ---------------------------------------------------------------------------- */
    /* strictDangerousCharCheckTest                                                 */
    /* ---------------------------------------------------------------------------- */
	@Test(enabled = true)
	public void detectControlCharsTest()
	{
		// The first 32 ASCII characters are control characters.
		// -> We expect exceptions to be thrown.
		for (int i = 0; i < 32; i++) {
			var s = Character.toString(i);
			try {PathSanitizer.detectControlChars(s);}
				catch (TapisException e) {continue;}
			Assert.assertTrue(false, "1. FAILING CHARACTER CODE: " + i);
		}
		
		// The rest of the ASCII characters are not control characters,
		// but 127 (DELETE) is not printable and has had various uses.
		// -> We expect NO exceptions to be thrown.
		for (int i = 32; i < 127; i++) {
			var s = Character.toString(i);
			try {PathSanitizer.detectControlChars(s);}
				catch (TapisException e) {Assert.assertTrue(false, "2. FAILING CHARACTER CODE: " + i);}
		}
		
		// The last ascii character (127, DEL) is not printable and will 
		// be returned as a control character along with the first extended
		// ascii characters up to 159 inclusive.
		// -> We expect exceptions to be thrown.
		for (int i = 127; i < 160; i++) {
			var s = Character.toString(i);
			try {PathSanitizer.detectControlChars(s);}
				catch (TapisException e) {continue;}
			Assert.assertTrue(false, "3. FAILING CHARACTER CODE: " + i);
		}
		
		// The rest of the extended ASCII characters are not control characters
		// and are printable.
		// -> We expect NO exceptions to be thrown.
		for (int i = 160; i < 256; i++) {
			var s = Character.toString(i);
			try {PathSanitizer.detectControlChars(s);}
				catch (TapisException e) {Assert.assertTrue(false, "4. FAILING CHARACTER CODE: " + i);}
		}
	}
	
	/* ---------------------------------------------------------------------------- */
    /* replaceControlCharsTest                                                      */
    /* ---------------------------------------------------------------------------- */
	@Test(enabled = true)
	public void replaceControlCharsTest()
	{
		// Control characters replaced.
		String s = "\ta\rb\nc\u000Bd";
		String r = PathSanitizer.replaceControlChars(s, '?');
//		System.out.println("r=" + r);
		Assert.assertEquals(r, "?a?b?c?d");
		
		s = "\u0628" + "\u064e" + "\u064a" + "\u0652" + "\u067a" + "\u064f";
		r = PathSanitizer.replaceControlChars(s, '?');
//		System.out.println("r=" + r);
		Assert.assertEquals(r, "بَيْٺُ");
		Assert.assertEquals(r, s);  // no replacement
		
		// Control character replaced.
		s = "\uc200\u0000\uc200";
		r = PathSanitizer.replaceControlChars(s, '?');
//		System.out.println("r=" + r);
		Assert.assertEquals(r, "숀?숀");
		
		s = "🥦_Salade verte";
		r = PathSanitizer.replaceControlChars(s, '?');
//		System.out.println("r=" + r);
		Assert.assertEquals(r, "🥦_Salade verte");
		Assert.assertEquals(r, s);  // no replacement
		
		s = "🥦_Salade verte".substring(0, 2);
		r = PathSanitizer.replaceControlChars(s, '?');
//		System.out.println("r=" + r);
		Assert.assertEquals(r, "🥦");
		Assert.assertEquals(r, s);  // no replacement
		
		// Not a complete, printable UTF character,
		// but it's not replaced because it's not a 
		// control character.
		s = "🥦_Salade verte".substring(0, 1);
		r = PathSanitizer.replaceControlChars(s, '?');
//		System.out.println("r=" + r);
		Assert.assertEquals(r, "\ud83e");
		Assert.assertEquals(r, s);  // no replacement
		
		// Unprintable characters that don't trip replacement.
		s = "\u5f77\u2b04\u2366\u3ca2\u2c29\u9929\udfe3\uf1f1\udc61\u12b8\u26b1\ud9d8\u8d71"; 
		r = PathSanitizer.replaceControlChars(s, '?');
//		System.out.println("r=" + r);
		Assert.assertEquals(r, s);  // no replacement
	}
	
}
