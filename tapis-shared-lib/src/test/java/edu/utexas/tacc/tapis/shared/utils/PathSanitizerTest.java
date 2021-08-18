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
	
}
