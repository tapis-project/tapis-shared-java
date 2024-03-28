package edu.utexas.tacc.tapis.shared.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Path;

@Test(groups={"unit"})
public class PathUtilsTest
{
  private static final Path EMPTY_PATH = Path.of("");
  private static final Path ROOT_PATH = Path.of("/");
  private static final Path REL_PATH1 = Path.of("a/b/c");
  private static final Path ABS_PATH1 = Path.of("/a/b/c");

  /* **************************************************************************** */
  /*                                    Tests                                     */
  /* **************************************************************************** */
  /* ---------------------------------------------------------------------------- */
  /* getRelativePathTest:                                                         */
  /* ---------------------------------------------------------------------------- */
  /*
   * Test that getRelativePath
   *   - strips off leading and trailing slashes
   *   - collapses multiple inner slashes to single slashes
   *   - properly resolves . and ..
   */
  @Test(enabled=true)
  public void getRelativePathTest()
  {
    // Test cases that should always return an empty path
    Assert.assertTrue(EMPTY_PATH.equals(PathUtils.getRelativePath("")));
    Assert.assertTrue(EMPTY_PATH.equals(PathUtils.getRelativePath("/")));
    Assert.assertTrue(EMPTY_PATH.equals(PathUtils.getRelativePath("//")));
    Assert.assertTrue(EMPTY_PATH.equals(PathUtils.getRelativePath("///")));
    Assert.assertTrue(EMPTY_PATH.equals(PathUtils.getRelativePath("/////////////////")));
    Assert.assertTrue(EMPTY_PATH.equals(PathUtils.getRelativePath("///")));
	Assert.assertTrue(EMPTY_PATH.equals(PathUtils.getRelativePath("/./")));
    Assert.assertTrue(EMPTY_PATH.equals(PathUtils.getRelativePath("/../")));
    Assert.assertTrue(EMPTY_PATH.equals(PathUtils.getRelativePath("../")));
    Assert.assertTrue(EMPTY_PATH.equals(PathUtils.getRelativePath("../../..")));
    Assert.assertTrue(EMPTY_PATH.equals(PathUtils.getRelativePath("./../.")));
	// Test removal of leading and trailing slashes
    Assert.assertTrue(REL_PATH1.equals(PathUtils.getRelativePath("/a/b/c")));
    Assert.assertTrue(REL_PATH1.equals(PathUtils.getRelativePath("a/b/c/")));
    Assert.assertTrue(REL_PATH1.equals(PathUtils.getRelativePath("//a/b/c")));
    Assert.assertTrue(REL_PATH1.equals(PathUtils.getRelativePath("a/b/c//")));
    Assert.assertTrue(REL_PATH1.equals(PathUtils.getRelativePath("//a/b/c//")));
    Assert.assertTrue(REL_PATH1.equals(PathUtils.getRelativePath("/////a/b/c//////")));
    Assert.assertTrue(REL_PATH1.equals(PathUtils.getRelativePath("//a//b/c")));
    // Test collapsing of inner multiple slashes
    Assert.assertTrue(REL_PATH1.equals(PathUtils.getRelativePath("/////a//////b///c///")));
	// Test resolving of . and ..
    Assert.assertTrue(REL_PATH1.equals(PathUtils.getRelativePath("../a/b/c/.")));
    Assert.assertTrue(REL_PATH1.equals(PathUtils.getRelativePath("///////a//////b/c/d/e/../..//../test//.././c/.///")));
  }

	/* ---------------------------------------------------------------------------- */
	/* getAbsolutePathTest:                                                         */
	/* ---------------------------------------------------------------------------- */
	@Test(enabled=true)
	public void getAbsolutePathTest()
	{
		// Test cases that should always return an root path
		Assert.assertTrue(ROOT_PATH.equals(PathUtils.getAbsolutePath("","")));
		Assert.assertTrue(ROOT_PATH.equals(PathUtils.getAbsolutePath("/", "")));
		Assert.assertTrue(ROOT_PATH.equals(PathUtils.getAbsolutePath("", "/")));
		Assert.assertTrue(ABS_PATH1.equals(PathUtils.getAbsolutePath("", "a/b/c/")));
		Assert.assertTrue(ABS_PATH1.equals(PathUtils.getAbsolutePath("/", "/a/b/c/")));
		Assert.assertTrue(ABS_PATH1.equals(PathUtils.getAbsolutePath("a", "b/c/")));
		Assert.assertTrue(ABS_PATH1.equals(PathUtils.getAbsolutePath("/a", "b/c/")));
		Assert.assertTrue(ABS_PATH1.equals(PathUtils.getAbsolutePath("/a", "/b///c//")));
		Assert.assertTrue(ABS_PATH1.equals(PathUtils.getAbsolutePath("/a/b/c", "/")));
		Assert.assertTrue(ABS_PATH1.equals(PathUtils.getAbsolutePath("///a", "b/c/")));
	}
}
