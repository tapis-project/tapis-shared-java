package edu.utexas.tacc.tapis.shared.utils;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;

import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.MultilineRecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.utexas.tacc.tapis.security.client.SKClient;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.exceptions.recoverable.TapisRecoverableException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.security.ServiceClients;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadLocal;

public class TapisUtils
{
  /* **************************************************************************** */
  /*                                  Constants                                   */
  /* **************************************************************************** */
  // Local logger.
  private static final Logger _log = LoggerFactory.getLogger(TapisUtils.class);
  
  // Set the display text for null references.
  public static final String NULL_STRING = "[null]";
  public static final String UNKNOWN_VALUE = "unknown";
  
  // Create the recursive dump styles.  The multiline style is readable by humans,
  // the comparable style is good for comparing the nested values of two objects
  // for equality.
  public static final MultilineRecursiveToStringStyle multiRecursiveStyle = new MultilineRecursiveToStringStyle();
  public static final ComparableRecursiveToStringStyle recursiveStyle = new ComparableRecursiveToStringStyle();
  
  // The tapis version resource path name. Maven 
  // fills in the version number at build time.
  public static final String TAPIS_VERSION_FILE = "/tapis.version";

  // Full version includes git info
  public static final String TAPIS_FULLVERSION_FILE = "/tapis.fullversion";
  
  // Build time and git info file names.
  public static final String BUILD_TIME_FILE = "/build.time";
  public static final String GIT_INFO_FILE   = "/git.info";

  // Used to generate 3 bytes of randomness that fit into 2^24 - 1.
  private static final int CEILING = 0x1000000;
  
  // Regex pattern that returns true if the string being checked DOES NOT
  // contain any of the chars: &, >, <, |, ;, `, <space>
  private final static Pattern safePathPattern = Pattern.compile("[^ &><|;`]+");
	
  // Split on unicode whitespace.
  // From https://stackoverflow.com/questions/225337/how-to-split-a-string-with-any-whitespace-chars-as-delimiters  
  //
  // The (?U) inline embedded flag option is the equivalent 
  // of using Pattern.UNICODE_CHARACTER_CLASS that enables \s shorthand 
  // character class to match any characters from the whitespace Unicode category.
  private static final Pattern _spaceSplitter = Pattern.compile("(?U)\\s+");
  
  // This regex provides some basic validation of a uri acceptable to Tapis.  
  // The string must start with an alphabetic character followed by zero or more
  // alphanumeric or {+ . -} characters.  This forms the uri scheme and is always
  // followed by "://".  The uri must end in one or more non-control characters.  
  private static final Pattern _weakUriValidator = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*://[^\\p{Cntrl}]+");
  
  // Reusable empty array.
  private static final String[] EMPTY_STRING_ARRAY = new String[0];
  
  // Formatter for converting an Instant into a string for SQL
  private static final DateTimeFormatter UTC_OUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.nnnnnn");
  // Formatters for converting string to Instant for patterns:
  //   yyyy-MM-ddTHH:mm:ss.nnnnnn+HH:MM, e.g. 2200-04-29T14:15:00.123456+01:00
  //   yyyy-MM-ddTHH:mm+HH:MM, e.g. 2200-04-29T14:15+01:00
  //   yyyy-MM-ddTHH+HH:MM, e.g. 2200-04-29T14+01:00
  private static final DateTimeFormatter[] UTC_IN_FORMATTERS = {
    new DateTimeFormatterBuilder().parseCaseInsensitive().append(ISO_LOCAL_DATE)
            .appendLiteral('T').appendPattern("HH").appendLiteral(":").appendPattern("mm")
            .appendLiteral(":").appendPattern("ss").appendLiteral(".").appendPattern("nnnnnn")
            .optionalStart().appendOffset("+HH:MM", "Z").toFormatter(),
    new DateTimeFormatterBuilder().parseCaseInsensitive().append(ISO_LOCAL_DATE)
          .appendLiteral('T').appendPattern("HH").appendLiteral(":").appendPattern("mm")
          .optionalStart().appendOffset("+HH:MM", "Z").toFormatter(),
    new DateTimeFormatterBuilder().parseCaseInsensitive().append(ISO_LOCAL_DATE)
          .appendLiteral('T').appendPattern("HH")
          .optionalStart().appendOffset("+HH:MM", "Z").toFormatter()
    // NOTE: Tried a builder and ofPattern to handle requirement to support timezone when no hour:minute
    // specified but could not find something that worked. So explicit check is done in getUTCTimeFromString
    //    DateTimeFormatter.ofPattern("yyyy-MM-ddZZZZ"),
    //    new DateTimeFormatterBuilder().parseCaseInsensitive().append(ISO_LOCAL_DATE)
    //          .appendOffset("+HH:MM", "+00:00").toFormatter(),
  };

  /* **************************************************************************** */
  /*                                    Fields                                    */
  /* **************************************************************************** */
  // The version string read in from the tapis version resource file.
  private static String _tapisVersion;
  private static String _tapisFullVersion;
  
  // The build timestamp and git information.
  private static String _buildTime;
  private static String _gitBranch;
  private static String _gitCommit;

  /* **************************************************************************** */
  /*                                Public Methods                                */
  /* **************************************************************************** */
  /* ---------------------------------------------------------------------------- */
  /* toString:                                                                    */
  /* ---------------------------------------------------------------------------- */
  /** Use reflection to construct a string out of the contents of an object.  The 
   * fields of an object and its superclasses are printed to a string along with
   * the class name and its virtual address.  The address information allows one to 
   * distinguish between different instances of a class.
   * 
   * Be careful not to use this method to inadvertently expose passwords.
   * 
   * Here's an example of a serialized Person object:
   * 
   * <pre>
   * Person@182f0db[
   *   name=John Doe
   *   age=33
   *   smoker=false
   * ]
   * </pre>
   * 
   * @param obj the object whose content will be serialized.  Null is allowed.
   * @return the non-static content of the object represented in a string.
   */
  public static String toString(Object obj) 
  {
   // We always return a string.
   if (obj == null) return NULL_STRING;
   String s;
   
   // Sometimes this fails if there are circular references or other conditions
   // that confuse the reflective code. We log problems and still return a string.
   try {s = ReflectionToStringBuilder.toString(obj, multiRecursiveStyle);}
       catch (Exception e) {
           _log.warn(MsgUtils.getMsg("TAPIS_INTROSPECTION_ERROR", obj.getClass().getName(), e.getMessage()), e);
           return obj.toString();
       }
   return s; // successful introspection
  }

  /* ---------------------------------------------------------------------------- */
  /* toComparableString:                                                          */
  /* ---------------------------------------------------------------------------- */
  /** Use reflection to construct a string out of the contents of an object.  The 
   * fields of an object and its superclasses are printed to a string along with
   * the class name.  The format is compact and it does not include any information
   * that could distinguish two objects of the same type with the same field values.
   * This latter property allows one to use simple string comparison to determine
   * if two objects contain the same values.
   * 
   * Be careful not to use this method to inadvertently expose passwords.
   *
   * Here's an example of a serialized Person object:
   * 
   * <pre>
   * Person[name=John Doe,age=33,smoker=false]
   * </pre>
   *
   * @param obj the object whose content will be serialized.  Null is allowed.
   * @return the non-static content of the object represented in a string.
   */
  public static String toComparableString(Object obj) 
  {
   if (obj == null) return NULL_STRING;
   return ReflectionToStringBuilder.toString(obj, recursiveStyle); //ToStringStyle.SHORT_PREFIX_STYLE);
  }
  
  /* ---------------------------------------------------------------------------- */
  /* toSqlString:                                                                 */
  /* ---------------------------------------------------------------------------- */
  /** Extract the sql text from the prepared statement.  This method may need to
   * be enhanced to handle different connection pool implementations.
   * 
   * @param pstmt a non-null prepared statement
   * @return the current sql text that the statement represents
   * @throws SQLException on error
   */
  public static String toSqlString(PreparedStatement pstmt) 
    throws SQLException
  {
    return pstmt.unwrap(PreparedStatement.class).toString();
  }
  
  /* ---------------------------------------------------------------------------- */
  /* getUTCTimeNow:                                                          */
  /* ---------------------------------------------------------------------------- */
  /** Get the current instant's timestamp in UTC.
   *
   * @return a sql UTC timestamp object ready for persisting in the database.
   */
  public static LocalDateTime getUTCTimeNow()
  {
    // Return the current UTC timestamp for database operations.
    // Maybe there's a simpler way to do this, but just getting the current time
    // in milliseconds causes jvm local time to be saved to the database.
    return LocalDateTime.now(ZoneId.of(ZoneOffset.UTC.getId()));
  }

  /* ---------------------------------------------------------------------------- */
  /* getInstantFromSqlTimestamp:                                                  */
  /* ---------------------------------------------------------------------------- */
  /** Get the instant representation of a UTC timestamp retrieved the database.
   * 
   * @param ts an SQL UTC timestamp 
   * @return the instant also in UTC
   */
  public static Instant getInstantFromSqlTimestamp(Timestamp ts)
  {
    return ts.toLocalDateTime().toInstant(ZoneOffset.UTC);
  }

  /* ---------------------------------------------------------------------------- */
  /* getUTCTimeFromString:                                                        */
  /* ---------------------------------------------------------------------------- */
  /** Convert a string into a LocalDateTime, a timestamp in UTC
   *  NOTE: LocalDateTime is used for compatibility with DB/SQL, it is not local but UTC.
   * Rules:
   *  - If no timezone info is present then it defaults to UTC
   *  - Missing information defaults to the earliest time,
   *    e.g. 2020 represents 2020-01-01T00:00:00.000000Z
   * Examples of valid Tapis timestamp formats:
   * 2020-04-29T20:15:52:123456-06:00
   * 2020-04-29T20:15:52:12Z
   * 2020-04-29T20:15:52-06:00
   * 2020-04-29T20:15-06:00
   * 2020-04-29T20-06:00
   * 2020-04-29-06:00
   * 2020-04-29
   * 2020-04
   * 2020
   * @param timeStr String representing a valid Tapis timestamp
   * @return the time represented by the string using the above rules
   * @throws DateTimeParseException if string is not a valid timestamp
   */
  public static LocalDateTime getUTCTimeFromString(String timeStr) throws DateTimeParseException
  {
    if (StringUtils.isBlank(timeStr)) throw new DateTimeParseException("Empty timestamp string", "", 0);
    // If there is a trailing Z strip
    // it off. In most cases the formatter does not handle it.
    if (timeStr.endsWith("Z")) timeStr = timeStr.substring(0, timeStr.length()-1);
    if (timeStr.length() < 4) throw new DateTimeParseException("Less than 4 characters in string", timeStr, 0);
    // If 4, 7 or 10 characters convert to earliest time.
    if (timeStr.length() == 4)
    {
      // Must be yyyy
      timeStr += "-01-01T00:00:00";
    }
    else if (timeStr.length() == 7)
    {
      // Must be yyyy-MM
      timeStr += "-01T00:00:00";
    }
    else if (timeStr.length() == 10)
    {
      // Must be yyyy-MM-dd or yyyy+HH:MM
      if (timeStr.charAt(7) != ':') timeStr += "T00:00:00";
      else
      {
        var tmpStr = timeStr.substring(0,4) + "-01-01T00:00:00" + timeStr.substring(4,10);
        timeStr = tmpStr;
      }
    }
    // Cannot get DateTimeFormatter working for a date (no HH:mm) when there is a timezone offset,
    //   so do explicit checks here.
    else if (timeStr.length() == 13)
    {
      // Check for yyyy-MM+HH:MM
      if (timeStr.charAt(10) == ':' && (timeStr.charAt(7) == '+' || timeStr.charAt(7) == '-'))
      {
        var tmpStr = timeStr.substring(0, 7) + "-01T00:00:00" + timeStr.substring(7, 13);
        timeStr = tmpStr;
      }
    }
    else if (timeStr.length() == 16)
    {
      // Check for yyyy-MM-dd+HH:MM
      if (timeStr.charAt(13) == ':' && (timeStr.charAt(10) == '+' || timeStr.charAt(10) == '-'))
      {
        var tmpStr = timeStr.substring(0, 10) + "T00:00:00" + timeStr.substring(10, 16);
        timeStr = tmpStr;
      }
    }

    // Attempt to convert the string into a timestamp
    // First try to parse as an Instant
    try {
      return LocalDateTime.ofInstant(Instant.parse(timeStr),ZoneOffset.UTC);
    }
    catch (DateTimeParseException e) { }
    // Now try general LocalDateTime
    try {
      return LocalDateTime.parse(timeStr);
    }
    catch (DateTimeParseException e) { }
    // Try various formats using a DateTimeFormatter with LocalDateTime
    for (DateTimeFormatter formatter : UTC_IN_FORMATTERS)
    {
      try
      {
        return LocalDateTime.parse(timeStr, formatter);
      }
      catch (DateTimeParseException e) { }
    }
    throw new DateTimeParseException("Not a valid Tapis Timestamp", timeStr, 0);
  }

  /* ---------------------------------------------------------------------------- */
  /* getSQLStringFromInstant:                                                     */
  /* ---------------------------------------------------------------------------- */
  /** Convert a LocalDateTime to a string representing a timestamp in UTC with
   *    microsecond precision for use in SQL
   *  NOTE: LocalDateTime is used for compatibility with DB/SQL, it is not local but UTC.
   *
   * @param timestamp Timestamp to convert
   * @return a string representation of a UTC timestamp with microsecond precision
   */
  public static String getSQLStringFromUTCTime(LocalDateTime timestamp)
  {
    return UTC_OUT_FORMATTER.format(timestamp);
  }

  /* ---------------------------------------------------------------------------- */
  /* getTapisVersion:                                                             */
  /* ---------------------------------------------------------------------------- */
  /** Read the Tapis version from the version file.  The version file contains only
   * the version of this software as written by Maven during the build.  The value
   * is the value hardcoded into the POM file.
   * 
   * @return the current software's version
   */
  public static String getTapisVersion()
  {
    // Assign the version string only on the first time through.
    if (_tapisVersion == null) 
    {
      try (InputStream ins = TapisUtils.class.getResourceAsStream(TAPIS_VERSION_FILE)) {
        _tapisVersion = IOUtils.toString(ins, StandardCharsets.UTF_8);
        if (StringUtils.isBlank(_tapisVersion) || _tapisVersion.startsWith("${")) 
        	_tapisVersion = UNKNOWN_VALUE;
      }
      catch (Exception e) {
        _log.error(MsgUtils.getMsg("TAPIS_VERSION_FILE_ERROR", TAPIS_VERSION_FILE));
        _tapisVersion = UNKNOWN_VALUE;
      }
    }
    return _tapisVersion;
  }

  /* ---------------------------------------------------------------------------- */
  /* getTapisFullVersion:                                                         */
  /* ---------------------------------------------------------------------------- */
  /** Read the Tapis full version from a file.  The file contains version and git
   *  info as written by Maven during the build. Fall back to basic version
   *  if resource file not found or on error.
   *
   * @return the current software's full version including git info or basic version if full version not found
   */
  public static String getTapisFullVersion()
  {
    // Assign the version string only on the first time through.
    if (_tapisFullVersion == null)
    {
      if (TapisUtils.class.getResource(TAPIS_FULLVERSION_FILE) != null)
      {
        try (InputStream ins = TapisUtils.class.getResourceAsStream(TAPIS_FULLVERSION_FILE)) {
          _tapisFullVersion = IOUtils.toString(ins, StandardCharsets.UTF_8).trim();
        }
        catch (Exception e) {
          // Error. Fall back to basic version
          _log.warn(MsgUtils.getMsg("TAPIS_VERSION_FILE_ERROR", TAPIS_FULLVERSION_FILE));
          _tapisFullVersion = getTapisVersion();
        }
      }
      else
      {
        // Resource not found. Fall back to basic version
        _tapisFullVersion = getTapisVersion();
      }
    }
    return _tapisFullVersion;
  }

  /* ---------------------------------------------------------------------------- */
  /* getBuildTime:                                                                */
  /* ---------------------------------------------------------------------------- */
  /** Read the Tapis build time file.  
   * 
   * @return the current software's build time.
   */
  public static String getBuildTime()
  {
    // Assign the build time string only on the first time through.
    if (_buildTime == null) 
    {
      try (InputStream ins = TapisUtils.class.getResourceAsStream(BUILD_TIME_FILE)) {
        _buildTime = IOUtils.toString(ins, StandardCharsets.UTF_8);
        if (StringUtils.isBlank(_buildTime) || _buildTime.startsWith("${")) 
        	_buildTime = UNKNOWN_VALUE;
      }
      catch (Exception e) {
        _log.error(MsgUtils.getMsg("TAPIS_VERSION_FILE_ERROR", BUILD_TIME_FILE));
        _buildTime = UNKNOWN_VALUE;
      }
    }
    return _buildTime;
  }

  /* ---------------------------------------------------------------------------- */
  /* getGitBranch:                                                                */
  /* ---------------------------------------------------------------------------- */
  /** Read the git info file and return the branch.  
   * 
   * @return the git branch used to build the current software.
   */
  public static String getGitBranch()
  {
    // Assign the git info only on the first time through.
    if (_gitBranch == null) getGitInfo();
    return _gitBranch;
  }

  /* ---------------------------------------------------------------------------- */
  /* getGitCommit:                                                                */
  /* ---------------------------------------------------------------------------- */
  /** Read the git info file and return the commit number.  
   * 
   * @return the git commit used to build the current software.
   */
  public static String getGitCommit()
  {
    // Assign the git info only on the first time through.
    if (_gitCommit == null) getGitInfo();
    return _gitCommit;
  }

  /* ---------------------------------------------------------------------------- */
  /* getGitInfo:                                                                */
  /* ---------------------------------------------------------------------------- */
  /** Read the git info file and assign the two git static fields.  
   */
  private static synchronized void getGitInfo()
  {
    // Assign the version string only on the first time through.
    if (_gitBranch == null) 
    {
      try (InputStream ins = TapisUtils.class.getResourceAsStream(GIT_INFO_FILE)) {
          // git.info file should contain "${scmBranch}  ${buildNumber}", e.g. "develop  b8cb6330"
          // Split it into string array using one or more spaces as delimiter
          String gitStr = IOUtils.toString(ins, StandardCharsets.UTF_8);
          String[] gitInfo = gitStr.split("\\s+");
          if (gitInfo.length < 2)
          {
        	  _log.error(MsgUtils.getMsg("TAPIS_VERSION_FILE_ERROR", GIT_INFO_FILE));
              _gitBranch = UNKNOWN_VALUE;
              _gitCommit = UNKNOWN_VALUE;
          }	
          else
          {
        	  // Assign the valid, non-null values.
        	  _gitBranch = StringUtils.isBlank(gitInfo[0]) ? UNKNOWN_VALUE : gitInfo[0];
        	  _gitCommit = StringUtils.isBlank(gitInfo[1]) ? UNKNOWN_VALUE : gitInfo[1];
        	  if (_gitBranch.startsWith("${")) _gitBranch = UNKNOWN_VALUE;
        	  if (_gitCommit.startsWith("${")) _gitCommit = UNKNOWN_VALUE;
          }
      }
      catch (Exception e) {
        _log.error(MsgUtils.getMsg("TAPIS_VERSION_FILE_ERROR", GIT_INFO_FILE));
        _gitBranch = UNKNOWN_VALUE;
        _gitCommit = UNKNOWN_VALUE;
      }
    }
  }

  /* ---------------------------------------------------------------------------- */
  /* getPasswordFromConsole:                                                      */
  /* ---------------------------------------------------------------------------- */
  /** Get the specified user's password masking the input if possible.  Note there
   * may be a newline character at the end of the returned string.
   * 
   * @param userid the user account whose credentials are requested.
   * @return the password string.
   */
  public static String getPasswordFromConsole(String userid)
  {
    // Construct prompt.
    String prompt = "Enter the password for userid " + userid + ": ";
    
    // Get the console.
    Console console = System.console();
    
    // Normal command line execution.
    if (console != null) 
    {
      // Use console facilities to hide password.
      console.printf("%s", prompt);
      char[] pwd = console.readPassword();
      if (pwd != null) return new String(pwd);
        else return null;
    }
    
    // When no console is available (like in Eclipse),
    // try using stdin and stdout.
    System.out.print(prompt);
    byte[] bytes = new byte[4096];
    try {   
        // Read the input bytes which are not masked.
        int bytesread = System.in.read(bytes);
        return new String(bytes, 0, bytesread);
      }
      catch (IOException e){}
    
    // We failed to get a password.
    return null;
  }
  
  /* ---------------------------------------------------------------------------- */
  /* getInputFromConsole:                                                         */
  /* ---------------------------------------------------------------------------- */
  /** Get the user input if possible.  Note there may be a newline character at the 
   * end of the returned string.
   * 
   * @param prompt the text to display to get a response from the user
   * @return user input or null if no input was captured
   */
  public static String getInputFromConsole(String prompt)
  {
    // Get the console.
    Console console = System.console();
    
    // Normal command line execution.
    if (console != null) 
    {
      // Use console facilities to hide password.
      console.printf("%s", prompt);
      String input = console.readLine();
      return input;
    }
    
    // When no console is available (like in Eclipse),
    // try using stdin and stdout.
    System.out.print(prompt);
    byte[] bytes = new byte[4096];
    try {   
        // Read the input bytes which are not masked.
        int bytesread = System.in.read(bytes);
        return new String(bytes, 0, bytesread);
      }
      catch (IOException e){}
    
    // We failed to get a password.
    return null;
  }
  
  /* ---------------------------------------------------------------------------- */
  /* getLocalHostname:                                                            */
  /* ---------------------------------------------------------------------------- */
  /** Returns the local hostname by resolving the HOSTNAME environment variable.
   * If that variable is not available, it makes a call the linux hostname program.
   * If that fails, "localhost" is returned by default.
   *  
   * @return hostname of current machine
   */
  public static String getLocalHostname()
  {
      // First try the environment variable.
      String hostname = System.getenv("HOSTNAME");
      
      // Call the hostname program to get the configured hostname.
      // This may differ from what's returned by this statement from
      // the original Agave code:
      //
      //    InetAddress.getLocalHost().getHostName();
      //
      // The above code may call out to DNS, which comes with its own
      // set of problems and unpredictability.  For more discussion, see: 
      //  
      // https://stackoverflow.com/questions/7348711/recommended-way-to-get-hostname-in-java
      if (StringUtils.isBlank(hostname)) 
      {
          // Eclipse seems to think the scanner and its inputstream don't get automatically closed.
          // Debugging into the code shows that this is not true, no resources are left open.
          try (Scanner s = new Scanner(Runtime.getRuntime().exec("hostname").getInputStream()).useDelimiter("\\A")) {
              hostname = s.hasNext() ? s.next() : "localhost";
          } catch (Exception e) {
              _log.error(MsgUtils.getMsg("TAPIS_LOCAL_HOSTNAME_ERROR", "localhost"));
              hostname = "localhost"; // return something
          }
      }
      
      return hostname;
  }
  
  /* ---------------------------------------------------------------------------- */
  /* getIpAddressesFromNetInterface:                                              */
  /* ---------------------------------------------------------------------------- */
  /** Parses local network interface and returns a list of host addresses.
   * 
   * @return list of ip addresses for the host machine
   * @throws Exception on error
   */
  public static List<String> getIpAddressesFromNetInterface() 
   throws Exception 
  {
      List<String> ipAddresses = new ArrayList<String>();
      try 
      {
          Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
          while (networkInterfaces.hasMoreElements()) 
          {
              NetworkInterface ni = networkInterfaces.nextElement();
              if (ni.isUp() && !ni.isLoopback() && !ni.isPointToPoint() && !ni.isVirtual()) 
              {
                  Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
                  while (inetAddresses.hasMoreElements()) 
                  {
                      InetAddress add = inetAddresses.nextElement();
                      String ip = add.getHostAddress();
                      if (IPAddressValidator.validate(ip) 
                              && !StringUtils.startsWithAny(ip, new String[]{"127", ":", "0"})) 
                      {
                          ipAddresses.add(ip);
                      }
                  }
              }
          }
      } catch (Exception e) {
          String msg = MsgUtils.getMsg("TAPIS_GET_LOCAL_IP_ADDR");
          _log.error(msg, e);
          throw e;
      }
      
      return ipAddresses;
  }
  
  /* ---------------------------------------------------------------------------- */
  /* getMD5LongHash:                                                              */
  /* ---------------------------------------------------------------------------- */
  /** This function generates a MD5 hash from a string, takes the first 8 bytes out 
   * of it and generates a signed integer value out of the byte array assuming the 
   * array to be in BigEndian formatted.  This function is suitable for lightweight
   * hashing duty where even the full md5 is more than what's needed. 
   * 
   * @param str the string to hash.
   * @return the hash value prefix expressed as a long
   */
  public static long getMD5LongHash(String str) throws NoSuchAlgorithmException 
  {     
          MessageDigest md = MessageDigest.getInstance("MD5");
          md.update(str.getBytes());
          return ByteBuffer.wrap(Arrays.copyOf(md.digest(), 8)).asLongBuffer().get();
  }
  
  /* ---------------------------------------------------------------------------- */
  /* tapisify:                                                                    */
  /* ---------------------------------------------------------------------------- */
  /** Wrap non-tapis exceptions in an TapisException keeping the same error message
   * in the wrapped exception. 
   * 
   * @param e any throwable that we might wrap in an tapis exception
   * @return a TapisException
   */
  public static TapisException tapisify(Exception e){return tapisify(e, null);}
  
  /* ---------------------------------------------------------------------------- */
  /* tapisify:                                                                    */
  /* ---------------------------------------------------------------------------- */
  /** Wrap non-tapis exceptions in an TapisException.  If the msg parameter is non-null
   * then force wrapping even for Tapis exceptions and insert the msg.  If the msg
   * parameter is null then use the original exception's message in the wrapped
   * exception.  
   * 
   * @param e any throwable that we might wrap in an tapis exception
   * @param msg the new message or null
   * @return a TapisException
   */
  public static TapisException tapisify(Throwable e, String msg)
  {
      // Protect ourselves.
      if (e == null) return new TapisException(msg);
      
      // The result exception.
      TapisException tapisException = null;
      
      // -------- Null Message for TapisException
      if ((msg == null) && (e instanceof TapisException)) {
          // Use the exception as-is unless there's a new message.
          tapisException = (TapisException) e;
      }
      // -------- Wrapper for TapisRecoverableException
      else if (e instanceof TapisRecoverableException) {
          // Wrap the recoverable exception in a generic tapis exception.
          // Recoverable exceptions are discovered by searching the cause chain
          // using findInChain(), so there's no loss when burying them inside
          // another exception.
          tapisException = new TapisException(msg, e);
      }
      // -------- Wrapper for TapisException
      else if (e instanceof TapisException) 
      {
          // Create a new instance of the same tapis exception type.
          Class<?> cls = e.getClass();
                
          // Get the two argument (msg, cause) constructor that all 
          // TapisException subtypes implement EXCEPT TapisRecoverableExceptions.
          Class<?>[] parameterTypes = {String.class, Throwable.class};
          Constructor<?> cons = null;
          try {cons = cls.getConstructor(parameterTypes);}
               catch (Exception e2) {
                  String msg2 = MsgUtils.getMsg("TAPIS_REFLECTION_ERROR", cls.getName(), 
                                                "getConstructor", e.getMessage());
                  _log.error(msg2, e2);
               }
                
          // Use the constructor to assign the result variable.
          if (cons != null) 
              try {tapisException = (TapisException) cons.newInstance(msg, e);}
                  catch (Exception e2) {
                      String msg2 = MsgUtils.getMsg("TAPIS_REFLECTION_ERROR", cls.getName(), 
                                                    "newInstance", e.getMessage());
                      _log.error(msg2, e2);
                  }
                
          // If nothing worked create a generic tapis exception wrapper.
          if (tapisException == null) tapisException = new TapisException(msg, e);
      } 
      // -------- Wrapper for Non-TapisException
      else {
          // Wrap all non-TapisExceptions whether or not there's a new message. 
          tapisException = new TapisException(msg == null ? e.getMessage() : msg, e);
      }
      
      return tapisException;
  }

  /* ---------------------------------------------------------------------------- */
  /* findInChain:                                                                 */
  /* ---------------------------------------------------------------------------- */
  /** Given an exception with a chain of zero or more causal exceptions, determine
   * if the exception or any of its causal predecessors are of one of the specified
   * exception class types.  The idea is to determine if an exception or any of its
   * causal predecessors are instances of one of the exception types specified in 
   * the classes parameter. 
   * 
   * If the top level exception matches or if a match is found in the exception's 
   * causal chain to one of the target classes, the matching exception object is
   * returned.  Otherwise, null is returned.
   * 
   * @param ex some exception with a chain of zero or more causal exceptions
   * @param classes the target exception type or types we are searching for
   * @return the first matching type in the exception chain or 
   *         null if no match is found 
   */
  public static Exception findInChain(Exception ex, Class<?>... classes)
  {
      // Is there anything to do?
      if (ex == null || classes == null || classes.length == 0) return null;
      
      // Test whether the exception is assignable to any of the classes.
      for (Class<?> cls : classes) {
          // If the root assignable to the current class, we're done.
          if (cls.isInstance(ex)) return ex;
          
          // See if any of the causal exceptions buried 
          // in the exception chain are assignable.
          Throwable cause = ex.getCause();
          while (cause != null) {
              if (cls.isInstance(cause) && (cause instanceof Exception)) 
                  return (Exception)cause;
              cause = cause.getCause();
          }
      }
      
      // No match in exception chain.
      return null;
  }
  
  /* ---------------------------------------------------------------------------- */
  /* findFirstMatchingException:                                                  */
  /* ---------------------------------------------------------------------------- */
  /** Traverse the exception chain starting with the given Throwable and looking for
   * an exception whose class name begins with the prefix string.  The traversal 
   * ends at the last exception in the chain, the exception that does not have a
   * cause exception.
   * 
   * @param e a throwable
   * @param classNamePrefix the prefix class name to be matched
   * @return the first exception from the top for which there is a match or null for no match
   */
  public static Throwable findFirstMatchingException(Throwable e, String classNamePrefix)
  {
      // Look through the exception chain for the first one whose
      // class name starts with the prefix.
      while (e != null) {
          if (e.getClass().getName().startsWith(classNamePrefix)) return e;
          e = e.getCause();
      }
      
      // The prefix string does not match any class 
      // name in the exception chain. 
      return null;
  }
  
  /* ---------------------------------------------------------------------------- */
  /* getRandomString:                                                             */
  /* ---------------------------------------------------------------------------- */
  /** Generate a pseudo-random base64url string that can be used to identify a 
   * request serviced by a thread.  
   * 
   * @return the 4 character randomized string
   */
  public static String getRandomString() 
  {
      // Get a pseudo-random int value that has its low-order 
      // 24 bits randomized, which is enough to generate a 
      // 4 character base64 string.
      int n = ThreadLocalRandom.current().nextInt(CEILING);
      byte[] b = new byte[3];
      b[2] = (byte) (n);
      n >>>= 8;
      b[1] = (byte) (n);
      n >>>= 8;
      b[0] = (byte) (n);
      
      // Encode the 3 bytes into 4 characters 
      // and avoid any padding.
      return Base64.getUrlEncoder().encodeToString(b);
  }
  
  /* ---------------------------------------------------------------------------- */
  /* isAdmin:                                                                     */
  /* ---------------------------------------------------------------------------- */
  /** Convenience method that retrieves oboUser and oboTenant from threadlocal and
   * passes them to the actual isAdmin method.  This method is expected to be called
   * from service code in which threadlocal has already been validated.
   * 
   * @param user the non-null user that may be an admin
   * @param tenant the user's non-null tenant
   * @return true if the user is a tenant admin, false otherwise
   * @throws TapisException
   */
  public static boolean isAdmin(String user, String tenant) 
   throws TapisException
  {
      var threadContext = TapisThreadLocal.tapisThreadContext.get();
      return isAdmin(threadContext.getOboUser(), threadContext.getOboTenantId(), user, tenant);
  }
  
  /* ---------------------------------------------------------------------------- */
  /* isAdmin:                                                                     */
  /* ---------------------------------------------------------------------------- */
  /** Check if the user has been assigned the admin role in their tenant.  Null 
   * checking on inputs is not performed for performance reasons.
   * 
   * @param oboUser the non-null obo user used on the REST call
   * @param oboTenant the non-null obo tenant used on the REST call
   * @param user the non-null user being tested
   * @param tenant the user's non-null tenant
   * @return true if the user is a tenant admin, false otherwise
   * @throws TapisException
   */
  public static boolean isAdmin(String oboUser, String oboTenant, String user, String tenant) 
   throws TapisException
  {
      // Disallow cross tenant queries.
      if (!tenant.equals(oboTenant)) {
          String msg = MsgUtils.getMsg("JOBS_MISMATCHED_TENANT", oboTenant, tenant);
          throw new TapisException(msg);
      }
      
      // Get the sk client for the oboUser@oboTenant.
      SKClient skClient;
      try {
          skClient = ServiceClients.getInstance().getClient(oboUser, oboTenant, SKClient.class);
      }
      catch (Exception e) {
          String msg = MsgUtils.getMsg("TAPIS_CLIENT_NOT_FOUND", "SK", oboTenant, oboUser);
          throw new TapisException(msg, e);
      }
      
      // Issue the sk call to see if the user has been assigned the 
      // tenant admin role.
      boolean hasRole;
      try {hasRole = skClient.hasRole(tenant, user, SkConstants.ADMIN_ROLE_NAME);} 
      catch (Exception e) {
          String msg = MsgUtils.getMsg("SK_ROLE_GET_ERROR", tenant, user, tenant, 
                                       SkConstants.ADMIN_ROLE_NAME);
          throw new TapisException(msg, e);
      }
      
      return hasRole;
  }
  
  /* ---------------------------------------------------------------------------- */
  /* extractFilename:                                                             */
  /* ---------------------------------------------------------------------------- */
  /** Extract the last path segment from a tapis or non-tapis url string.  The string
   * may contain unresolved macros and other characters that are invalid in URLs or
   * URIs, so we have to do the parsing ourselves without relying on standard classes.
   * 
   * This method always returns a string of length zero or more.  The last path 
   * segment typically represents a simple file name, which is the reason why this
   * method is usually called.  If the url ends with a slash, as is the case when
   * it represents a directory, the empty string will be returned.
   * 
   * @param url a tapis or non-tapis url string
   * @return the last path segment or an empty string if none could be extracted
   */
  public static String extractFilename(String url)
  {
      // Get the easy cases out of the way.
      if (url == null || url.isEmpty()) return "";
      if (url.endsWith("/")) return "";
      
      // The path segment starts at the 3rd slash for all urls.
      final int pathStartIndex = 3;
      
      // Walk the url looking for the path start index.
      String path = null;
      int slashCount = 0;
      for (int i = 0; i < url.length(); i++) {
          // Look for slashes.
          char c = url.charAt(i);
          if (c != '/') continue;
          slashCount++;
          
          // Are we ready to assign the path?
          if (slashCount == pathStartIndex) {
              path = url.substring(i); // leading slash included
              break;
          }
      }
      if (path == null) return "";
      
      // Extract the last segment from the path.
      // We know the path starts with a slash.
      int lastSlashIndex = path.lastIndexOf("/");
      if (lastSlashIndex + 1 >= path.length()) return "";
      return path.substring(lastSlashIndex + 1);
  }

  /* ---------------------------------------------------------------------------- */
  /* getLastLine:                                                                 */
  /* ---------------------------------------------------------------------------- */
  /**
   * Get all characters after the last newline character is a string.  The string
   * must be non-null and must already be trimmed of leading and trailing whitespace.
   * This method is useful in stripping the banner information from the output of
   * remote commands.
   *
   * @param s the remote result string
   * @return the last line of the string
   */
  public static String getLastLine(String s)
  {
    // The input is a non-null, trimmed string so a non-negative index must be at least one
    // character from the end of the string.
    int index = s.lastIndexOf('\n');
    if (index < 0) return s;
    return s.substring(index + 1);
  }

  /* ---------------------------------------------------------------------------- */
  /* safelySingleQuoteString:                                                     */
  /* ---------------------------------------------------------------------------- */
  /** This method will single quote a string and convert all embedded single quotes
   * into '\''.  For example, file'name would be converted to 'file'\''name'.  This 
   * works because the escaped single quote \' is technically between two single-quoted 
   * arguments in a script.  
   * 
   * This escaping is to prevent unix command injection in shell scripts.  Something 
   * like embedding ;rm -rf / in a command that gets executed in a bash shell.
   * 
   * If the input string is null, it is returned as is.
   * 
   * @param unquotedString non-null string
   * @return the quoted string or null
   */
  public static String safelySingleQuoteString(String unquotedString) 
  {
	  if (unquotedString == null) return unquotedString;
	  StringBuilder sb = new StringBuilder();
	  sb.append("'");
	  sb.append(unquotedString.replace("'", "'\\''"));
	  sb.append("'");
	  return sb.toString();
  }

  /* ---------------------------------------------------------------------------- */
  /* safelyDoubleQuoteString:                                                     */
  /* ---------------------------------------------------------------------------- */
  /** This method will double quote a string and convert all embedded double quotes
   * into \\\".  For example, file"name would be converted to "file\\\"name".  
   * 
   * If the input string is null, it is returned as is.
   * 
   * @param unquotedString non-null string
   * @return the quoted string or null
   */
  public static String safelyDoubleQuoteString(String unquotedString) 
  {
	  if (unquotedString == null) return unquotedString;
	  StringBuilder sb = new StringBuilder();
	  sb.append("\"");
	  sb.append(unquotedString.replace("\"", "\\\""));
	  sb.append("\"");
	  return sb.toString();
  }

  /* ---------------------------------------------------------------------- */
  /* conditionalQuote:                                                      */
  /* ---------------------------------------------------------------------- */
	/** Conditionally double quote the input string for safe use on the command
	 * line.  This method will conditionally double quote the string if it does
	 * not match the safePathPattern.  No leading, trailing or internal whitespace
	 * is removed or changed.  
	 * 
	 * IT IS ASSUMED THAT THE STRING CONTAINS NO CONTROL CHARACTERS (see 
	 * PathSanitizer.detectControlChars() and PathSanitizer.replaceControlChars()).
	 *   
	 * This method checks for the presence of these characters only: 
	 * 
	 * 					&, >, <, |, ;, `, <space>
	 *
	 * If the string is already double quoted it will not be changed.  If the
	 * string is not already double quoted and it contains unsafe command line
	 * characters, it will be double quoted.  If it contains no unsafe characters
	 * the input string will be returned unchanged.
	 * 
	 * @param s an input string to appear on the command line 
	 * @return a command line safe version of the string  
	 */
  public static String conditionalQuote(String s)
  {
	  // Maybe there's nothing to do.
	  if (StringUtils.isBlank(s)) return s;
		
	  // Don't double quote a string that's already double quoted.
	  if (s.startsWith("\"") && s.endsWith("\"")) return s;
		
	  // Check for characters that we want to prohibit
	  // from appearing on the command line unquoted.
	  Matcher m = safePathPattern.matcher(s);
	  if (!m.matches()) s = TapisUtils.safelyDoubleQuoteString(s);
		
	  return s;
  }

	/* ---------------------------------------------------------------------------- */
	/* splitIntoKeyValue:                                                           */
	/* ---------------------------------------------------------------------------- */
	/** Split a string into a key (the first word) and value (the remainder). It's
	 * assumed all control characters have been removed from the string.  Leading and
	 * trailing whitespace is stripped off before splitting.
	 * 
	 * @param s the string to be split
	 * @return a string array of length 0, 1 or 2
	 */
	public static String[] splitIntoKeyValue(String s)
	{
		// Don't blow up.
		if (StringUtils.isBlank(s)) return EMPTY_STRING_ARRAY;
		
		// The array returned will have 1 element if it contains no embedded whitespace
		// or 2 elements if there is at least one whitespace character within it.
		return _spaceSplitter.split(s.strip(), 2);
	}
	
	/* ---------------------------------------------------------------------------- */
	/* splitIntoWords:                                                              */
	/* ---------------------------------------------------------------------------- */
	/** Split a string into whitespace delimited words. It's assumed all control 
	 * characters have been removed from the string.  Leading and trailing whitespace 
	 * is stripped off before splitting.
	 * 
	 * @param s the string to be split
	 * @return a string array of length 0 or more
	 */
	public static String[] splitIntoWords(String s)
	{
		// Don't blow up.
		if (StringUtils.isBlank(s)) return EMPTY_STRING_ARRAY;
		
		// The array returned will have 1 element if it contains no embedded whitespace
		// or more if there are multiple sequences of whitespace.
		return _spaceSplitter.split(s.strip(), 0);
	}
	
	/* ---------------------------------------------------------------------------- */
	/* weaklyValidateUri:                                                           */
	/* ---------------------------------------------------------------------------- */
	/** This method validates the basic structure of a uri string to be: 
	 * 
	 * 		scheme://{something}
	 * 
	 * where the {something} will not contain control characters.
	 * 
	 * @param s the uri string
	 * @return true if valid, false otherwise
	 */
	public static boolean weaklyValidateUri(String s)
	{
		// Garbage in.
		if (s == null) return false;
		
		// Return whether the string qualifies as a weakly validated uri.
		return _weakUriValidator.matcher(s).matches();
	}
}
