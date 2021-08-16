package edu.utexas.tacc.tapis.shared.utils;

import java.nio.charset.Charset;

import org.apache.commons.lang3.CharSet;
import org.apache.commons.lang3.StringEscapeUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import edu.utexas.tacc.tapis.shared.utils.PathSanitizer;


@Test(groups= {"unit"})
public class PathSanitizerTest {
	/* **************************************************************************** */
    /*                                    Tests                                     */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* detectParentDirTest                                                          */
    /* ---------------------------------------------------------------------------- */
	@Test(enabled=true)
	public void detectParentDirTest()
	{
		String dirPath = "/home/bud/mydir/myfile";
		Boolean isParent = PathSanitizer.detectParentDir(dirPath);
	    Boolean falseHolder = false;
	    Boolean trueHolder = true;
		Assert.assertEquals(isParent, trueHolder);
		
		dirPath = "/home/bud/mydir/myfile.txt";
		isParent = PathSanitizer.detectParentDir(dirPath);
		Assert.assertEquals(isParent, trueHolder);
		
		dirPath = "/home/bud/../mydir/myfile.txt";
		isParent = PathSanitizer.detectParentDir(dirPath);
        Assert.assertEquals(isParent, falseHolder);
        
        String test = "\56\u002e";
        System.out.println(test);
	}
	
	/* ---------------------------------------------------------------------------- */
    /* strictDangerousCharCheckTest                                                 */
    /* ---------------------------------------------------------------------------- */
	@Test(enabled = true)
	public void strictDangerousCharCheckTest()
	{
        String dirPath = "/home/bud/mydir/myfile";
        Boolean testResult = PathSanitizer.strictDangerousCharCheck(dirPath);
        Boolean falseHolder = false;
        Boolean trueHolder = true;
        Assert.assertEquals(testResult, trueHolder);
       
        dirPath = "home/bud/mydir/myfile1.txt";
        testResult = PathSanitizer.strictDangerousCharCheck(dirPath);
        Assert.assertEquals(testResult, trueHolder);
       
        dirPath = "home/bud/../mydir/myfile1.txt";
        testResult = PathSanitizer.strictDangerousCharCheck(dirPath);
        Assert.assertEquals(testResult, falseHolder);
       
        dirPath = "home/bud/mydir/myfile1.txt; ls -al";
        testResult = PathSanitizer.strictDangerousCharCheck(dirPath);
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
        boolean out = PathSanitizer.splitAndCheckForParent(dirPath);
        boolean expectedOut = false;
        Assert.assertEquals(out, expectedOut);
        
        dirPath = "/home../..bud/my..dir/myfile";
        out = PathSanitizer.splitAndCheckForParent(dirPath);
        expectedOut = false;
        Assert.assertEquals(out, expectedOut);
        
        dirPath = "/home../..bud/my..dir/myfile/..";
        out = PathSanitizer.splitAndCheckForParent(dirPath);
        expectedOut = true;
        Assert.assertEquals(out, expectedOut);      
        
        dirPath = "/home../..bud/my..dir/myfile/../\u002e\u002e";
        System.out.println(dirPath);
        
        dirPath = "/home../..bud/my..dir/myfile/../%2e%2e";
        System.out.println(dirPath);
	}
	
}
