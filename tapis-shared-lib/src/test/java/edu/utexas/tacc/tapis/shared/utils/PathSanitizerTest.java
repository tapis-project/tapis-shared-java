package edu.utexas.tacc.tapis.shared.utils;

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
}
