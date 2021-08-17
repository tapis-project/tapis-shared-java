package edu.utexas.tacc.tapis.shared.utils;

import org.testng.Assert;
import org.testng.annotations.Test;


@Test(groups= {"unit"})
public class PathSanitizerTest {
	/* **************************************************************************** */
    /*                                    Tests                                     */
    /* **************************************************************************** */	
	/* ---------------------------------------------------------------------------- */
    /* strictDangerousCharCheckTest                                                 */
    /* ---------------------------------------------------------------------------- */
	@Test(enabled = true)
	public void strictDangerousCharCheckTest()
	{
        String dirPath = "/home/bud/mydir/myfile";
        Boolean testResult = PathSanitizer.hasDangerousChars(dirPath);
        Boolean falseHolder = false;
        Boolean trueHolder = true;
        Assert.assertEquals(testResult, trueHolder);
       
        dirPath = "home/bud/mydir/myfile1.txt";
        testResult = PathSanitizer.hasDangerousChars(dirPath);
        Assert.assertEquals(testResult, trueHolder);
       
        dirPath = "home/bud/../mydir/myfile1.txt";
        testResult = PathSanitizer.hasDangerousChars(dirPath);
        Assert.assertEquals(testResult, falseHolder);
       
        dirPath = "home/bud/mydir/myfile1.txt; ls -al";
        testResult = PathSanitizer.hasDangerousChars(dirPath);
        Assert.assertEquals(testResult, falseHolder);
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
	public void splitAndCheckForParentTest() {
        String dirPath = "/home/bud/mydir/myfile";
        boolean out = PathSanitizer.hasParentTraversal(dirPath);
        boolean expectedOut = false;
        Assert.assertEquals(out, expectedOut);
        
        dirPath = "/home../..bud/my..dir/myfile";
        out = PathSanitizer.hasParentTraversal(dirPath);
        expectedOut = false;
        Assert.assertEquals(out, expectedOut);
        
        dirPath = "/home../..bud/my..dir/myfile/..";
        out = PathSanitizer.hasParentTraversal(dirPath);
        expectedOut = true;
        Assert.assertEquals(out, expectedOut);      
        
        dirPath = "/home../..bud/my..dir/myfile/\u002e\u002e";
        out = PathSanitizer.hasParentTraversal(dirPath);
        expectedOut = true;
        Assert.assertEquals(out,expectedOut);
        
        dirPath = "/home../..bud/my..dir/myfile/.\u002e";
        out = PathSanitizer.hasParentTraversal(dirPath);
        expectedOut = true;
        Assert.assertEquals(out,expectedOut);
        
        dirPath = "/home../..bud/my..dir/myfile/\u002e.";
        out = PathSanitizer.hasParentTraversal(dirPath);
        expectedOut = true;
        Assert.assertEquals(out,expectedOut);
        
        dirPath = "/home../..bud/my..dir/myfile/\56\56";
        out = PathSanitizer.hasParentTraversal(dirPath);
        expectedOut = true;
        Assert.assertEquals(out,expectedOut);
        
        dirPath = "/home../..bud/my..dir/myfile/.\56";
        out = PathSanitizer.hasParentTraversal(dirPath);
        expectedOut = true;
        Assert.assertEquals(out,expectedOut);
        
        dirPath = "/home../..bud/my..dir/myfile/\56.";
        out = PathSanitizer.hasParentTraversal(dirPath);
        expectedOut = true;
        Assert.assertEquals(out,expectedOut);
	}
	
}
