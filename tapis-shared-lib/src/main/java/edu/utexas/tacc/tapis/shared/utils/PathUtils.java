package edu.utexas.tacc.tapis.shared.utils;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static edu.utexas.tacc.tapis.shared.uri.TapisUrl.TAPIS_PROTOCOL_PREFIX;

/*
 *
 * Utility class containing general use static methods for handling paths.
 * This class is non-instantiable
 *
 * Note that normalizing a path means:
 *  - double and single dot path steps are removed
 *  - multiple slashes are merged into a single slash
 *  - a trailing slash will be retained
 *  - if provided path is null or empty or resolving double dot steps results in no parent path
 *    then the relativePath becomes a single / resulting in the relativePath being the same as the system's rootDir.
 *
 * NOTE: Paths stored in SK for permissions and shares always relative to rootDir and always start with /
 *
 */
public class PathUtils
{
  // Private constructor to make it non-instantiable
  private PathUtils() { throw new AssertionError(); }

  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */
  // Local logger.
  public static final Logger log = LoggerFactory.getLogger(PathUtils.class);
  // Pattern used to strip off leading and trailing slashes from a string.
  // The regex says: match 0 or more slashes at start of string or 0 or more slashes at end of string.
  private static final Pattern trimSlashesPattern = Pattern.compile("^/+|/+$");

  /**
   * Construct a normalized path intended to be relative to a system's rootDir based on a path provided by a user.
   * In this case normalized means:
   *   - leading and trailing slashes are removed
   *   - redundant double and single dot path steps are resolved
   *   - multiple slashes within the path are collapsed into single slashes
   *   - path traversal escape is prevented by removing a leading "../" if present.
   *   - if provided path is null or empty or resolving double dot steps results in no parent path
   *       then the relativePath becomes the empty string resulting in the relativePath being the same as
   *       the system's rootDir.
   * @param pathStr path provided by user
   * @return Path - normalized path
   */
  public static Path getRelativePath(String pathStr)
  {
    if (StringUtils.isBlank(pathStr) || "/".equals(pathStr)) return Path.of("");
    // Remove any leading or trailing slashes
    String relativePathStr =  trimSlashesPattern.matcher(pathStr).replaceAll("");
    // Create a normalized path using the Java class Path.
    // This collapses multiple slashes to single slashes and resolves redundant elements such as . and ..
    Path normalizedPath = Path.of(relativePathStr).normalize();

    // Here we need to get rid of any leading ./ or ../.  We'll take care of all sorts of crazy stuff too
    // like ./../..././foo will become foo, etc.  We will remove the leading slash too so it will be a
    // real relative path.  There are two steps.  First remove any leading combinations of dots and slashes
    // the preceed the first slash.  Here is an explanation of the regex:
    // Regex:  ^ = starts with
    //         \\.*/ - the stuff inside parens.  match 0 or more dots followed by a slash
    //         ( ... )+ - grouping - match 1 or more of these.
    normalizedPath = Path.of(normalizedPath.toString().replaceFirst("^(\\.*/)+", ""));

    // The previous expression only rmoves things before a slash.  So, something like ".Trash" or ".." would
    // remain. We want leading dots to be retained, but not if that's all we have - .Trash is ok, but .. is not.
    // This takes care of that part.
    // RegEx - if the string contains all dots, just replace them with nothing.
    normalizedPath = Path.of(normalizedPath.toString().replaceFirst("^\\.+$", ""));
    return normalizedPath;
  }

  /**
   * Construct a normalized absolute path given a system's rootDir and path relative to rootDir.
   * @param path path relative to system's rootDir
   * @return Path - normalized absolute path
   */
  public static Path getAbsolutePath(String rootDir, String path)
  {
    // If rootDir is null or empty use "/"
    String rdir = StringUtils.isBlank(rootDir) ? "/" : rootDir;
    // Make sure rootDir starts with a /
    rdir = StringUtils.prependIfMissing(rdir, "/");
    // First get normalized relative path
    Path relativePath = getRelativePath(path);
    // Return constructed absolute path
    return Path.of(rdir, relativePath.toString());
  }

  /**
   * Construct a normalized path intended for use in Security Kernel.
   *
   *  NOTE: Paths stored in SK for permissions and shares always relative to rootDir and always start with /
   *
   * This is intended to be relative to a system's rootDir based on a path provided by a user.
   * The only difference between this and getRelativePath() is this method ensures there is a prepended /
   * @param path path provided by user
   * @return Path - normalized path with prepended /
   */
  public static Path getSKRelativePath(String path)
  {
    String pathStr = getRelativePath(path).toString();
    pathStr = StringUtils.prependIfMissing(pathStr, "/");
    return Paths.get(pathStr);
  }

  /**
   * Construct a normalized absolute S3 key given a system's rootDir and path relative to rootDir.
   * Prepends rootDir to the relative path.
   *
   * @param path path relative to system's rootDir
   * @return Path - normalized absolute key
   */
  public static String getAbsoluteKey(String rootDir, String path)
  {
    // If rootDir is null or empty use ""
    String rdir = StringUtils.isBlank(rootDir) ? "" : rootDir;
    // First get normalized relative path
    Path relativePath = getRelativePath(path);
    // Return constructed absolute path
    String absolutePathStr = Paths.get(rdir, relativePath.toString()).toString();
    return absolutePathStr;
  }

  /**
   * Construct a path to use for FileInfo given an S3 key and the system rootDir.
   * @param keyStr S3 object key
   * @return key as a path relative to rootDir
   */
  public static String getFileInfoPathFromS3Key(String keyStr, String rootDir)
  {
    if (StringUtils.isBlank(rootDir)) return keyStr;
    keyStr = StringUtils.removeStart(keyStr, rootDir);
    return StringUtils.removeStart(keyStr, "/");
  }

  /**
   * Construct a FileInfo Url to use for FileInfo given FileInfo path and system Id.
   * @param path FileInfo path
   * @param systemId system id
   * @return url using the tapis:// protocol
   */
  public static String getTapisUrlFromPath(String path, String systemId)
  {
    String url = String.format("%s/%s", systemId, path);
    url = StringUtils.replace(url, "//", "/");
    return String.format("%s%s", TAPIS_PROTOCOL_PREFIX, url);
  }

  /**
   * All paths are assumed to be relative to rootDir
   *
   * @param srcBaseStr The BASE path of the source of the transfer
   * @param srcPathStr The path to the actual file being transferred
   * @param destBaseStr The BASE path of the destination of the transfer
   * @return Path
   */
  public static Path relativizePaths(String srcBaseStr, String srcPathStr, String destBaseStr)
  {
    // Make them look like absolute paths either way
    srcPathStr = StringUtils.prependIfMissing(srcPathStr, "/");
    srcBaseStr = StringUtils.prependIfMissing(srcBaseStr, "/");
    destBaseStr = StringUtils.prependIfMissing(destBaseStr, "/");

    Path srcPath = Paths.get(srcPathStr);
    Path srcBase = Paths.get(srcBaseStr);

    // This happens if the source path is absolute, i.e. the transfer is for
    // a single file like a/b/c/file.txt
    if (srcBase.equals(srcPath) && destBaseStr.endsWith("/"))
    {
      Path p = Paths.get(destBaseStr, srcPath.getFileName().toString());
      return p;
    }
    else
    {
      Path p = Paths.get(destBaseStr, srcBase.relativize(srcPath).toString());
      return p;
    }
  }
}
