package edu.utexas.tacc.tapis.search;

import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Types;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SearchUtils
{
  // Private constructor to make it non-instantiable
  private SearchUtils() { throw new AssertionError(); }

  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */
  // Local logger.
  private static final Logger _log = LoggerFactory.getLogger(SearchUtils.class);

  // Regex for parsing (<attr1>.<op>.<val1>)~(<attr2>.<op>.<val2>) ... See validateAndExtractSearchList
  private static final String SEARCH_REGEX = "(?:\\\\.|[^~\\\\]++)+";

  // Our special characters. These must be escaped when appearing in a value
  private static final List<Character> SEARCH_VAL_SPECIAL_CHARS = Arrays.asList('~', ',', '(', ')');

  // ************************************************************************
  // *********************** Enums ******************************************
  // ************************************************************************

  // Supported operators for search
  public enum SearchOperator {EQ, NEQ, GT, GTE, LT, LTE, IN, NIN, LIKE, NLIKE, BETWEEN, NBETWEEN}
  // All search operator strings as a set
  public static final Set<String> SEARCH_OP_SET = Stream.of(SearchOperator.values()).map(Enum::name).collect(Collectors.toSet());

  // All search operators as a set
  public static final EnumSet<SearchOperator> ALL_OPSET = EnumSet.allOf(SearchOperator.class);
  // Operators allowed for search when column is a string type
  public static final EnumSet<SearchOperator> STRING_OPSET =
        EnumSet.of(SearchOperator.EQ, SearchOperator.NEQ, SearchOperator.LT, SearchOperator.LTE,
                   SearchOperator.GT, SearchOperator.GTE,SearchOperator.LIKE, SearchOperator.NLIKE,
                   SearchOperator.BETWEEN, SearchOperator.NBETWEEN,
                   SearchOperator.IN, SearchOperator.NIN);
  // Operators allowed for search when column is a numeric type
  public static final EnumSet<SearchOperator> NUMERIC_OPSET =
        EnumSet.of(SearchOperator.EQ, SearchOperator.NEQ, SearchOperator.LT, SearchOperator.LTE,
                   SearchOperator.GT, SearchOperator.GTE, SearchOperator.BETWEEN, SearchOperator.NBETWEEN,
                   SearchOperator.IN, SearchOperator.NIN);
  // Operators allowed for search when column is a timestamp type
  public static final EnumSet<SearchOperator> TIMESTAMP_OPSET =
        EnumSet.of(SearchOperator.EQ, SearchOperator.NEQ, SearchOperator.LT, SearchOperator.LTE,
                   SearchOperator.GT, SearchOperator.GTE, SearchOperator.BETWEEN, SearchOperator.NBETWEEN);
  // Operators allowed for search when column is a boolean type
  public static final EnumSet<SearchOperator> BOOLEAN_OPSET =
        EnumSet.of(SearchOperator.EQ, SearchOperator.NEQ);

  // Operators for which the value may be a list
  public static final EnumSet<SearchOperator> listOpSet =
        EnumSet.of(SearchOperator.IN, SearchOperator.NIN, SearchOperator.BETWEEN, SearchOperator.NBETWEEN);

  // Map of java sql type to list of allowed search operators
  public static final Map<Integer, EnumSet<SearchOperator>> ALLOWED_OPS_BY_TYPE =
          Map.ofEntries(Map.entry(Types.CHAR, STRING_OPSET),
                        Map.entry(Types.VARCHAR, STRING_OPSET),
                        Map.entry(Types.BIGINT, NUMERIC_OPSET),
                        Map.entry(Types.DECIMAL, NUMERIC_OPSET),
                        Map.entry(Types.DOUBLE, NUMERIC_OPSET),
                        Map.entry(Types.FLOAT, NUMERIC_OPSET),
                        Map.entry(Types.INTEGER, NUMERIC_OPSET),
                        Map.entry(Types.NUMERIC, NUMERIC_OPSET),
                        Map.entry(Types.REAL, NUMERIC_OPSET),
                        Map.entry(Types.SMALLINT, NUMERIC_OPSET),
                        Map.entry(Types.TINYINT, NUMERIC_OPSET),
                        Map.entry(Types.BOOLEAN, BOOLEAN_OPSET),
                        Map.entry(Types.DATE, TIMESTAMP_OPSET),
                        Map.entry(Types.TIMESTAMP, TIMESTAMP_OPSET));

  /**
   * Convert a string into a SearchOperator
   * @param opStr String containing all search conditions
   * @return the corresponding SearchOperator or null if not found
   */
  public static SearchOperator getSearchOperator(String opStr)
  {
    SearchOperator op = null;
    if (SEARCH_OP_SET.contains(opStr)) op = SearchOperator.valueOf(opStr);
    return op;
  }

  /**
   * Validate a list of search conditions and extract the conditions
   * Search list must have the form  (<cond>)~(<cond>)~ ...
   *    where <cond> = <attr>.<op>.<value>
   * If there is only one condition the surrounding parentheses are optional
   * @param searchListStr - String containing all search conditions
   * @return the list of extracted search conditions
   * @throws IllegalArgumentException if error encountered while parsing.
   */
  public static List<String> validateAndExtractSearchList(String searchListStr) throws IllegalArgumentException
  {
    var searchList = new ArrayList<String>();
    if (StringUtils.isBlank(searchListStr)) return searchList;
    _log.trace("Parsing SearchList: " + searchListStr);
    // Parse search string into a list of conditions using a regex pattern to split
    // Set delimiter as ~ and escape as \
    //    SEARCH_REGEX = "(?:\\\\.|[^~\\\\]++)+"
    //        "(" +          // start a match group
    //        "?:" +         // match either of
    //        escape + "." + // any escaped character
    //        "|" +          // or
    //        "[^" + delimiter + escape + "]++" + // match any char except delim or escape, possessive match
    //        ")" +          // end a match group
    //        "+";           // repeat any number of times, ignoring empty results. Use * instead of + to include empty results
    Pattern regexPattern = Pattern.compile(SEARCH_REGEX);
    Matcher regexMatcher = regexPattern.matcher(searchListStr);
    while (regexMatcher.find()) { searchList.add(regexMatcher.group()); }
    // If we found only one match the searchList string may be a single condition that may or may not
    // be surrounded by parentheses. So handle that case.
    if (searchList.size() == 1)
    {
      String cond = searchList.get(0);
      // Add parentheses if not present, check start and end
      // Check for unbalanced parentheses in validateAndExtractSearchCondition
      if (!cond.startsWith("(") && !cond.endsWith(")")) cond = "(" + cond + ")";
      searchList.set(0, cond);
    }

    var retList = new ArrayList<String>();
    // Validate and process each condition. IllegalArg thrown if invalid
    for (String cond : searchList)
    {
      String bareCond = validateAndProcessSearchCondition(cond);
      retList.add(bareCond);
    }
    // Remove any empty matches, e.g. () might have been included one or more times
    retList.removeIf(item -> StringUtils.isBlank(item));
    return retList;
  }

  /**
   * Validate and extract a search condition that must have the form (<attr>.<op>.<value>)
   * For operators that take a list (IN, NIN, BETWEEN, NBETWEEN) the value is processed as a CSV list
   * For the value(s):
   *   Check for any of our special chars that are unescaped
   *   Check that for BETWEEN/NBETWEEN it is a 2 element list
   *   For LIKE/NLIKE translate special characters * -> % and ! -> _
   *   Remove escapes from our special characters as needed
   * @param cond the condition to process
   * @return the validated and processed condition without surrounding parentheses
   * @throws IllegalArgumentException if condition is invalid
   */
  public static String validateAndProcessSearchCondition(String cond) throws IllegalArgumentException
  {
    if (StringUtils.isBlank(cond) || !cond.startsWith("(") || !cond.endsWith(")"))
    {
      String errMsg = MsgUtils.getMsg("SEARCH_COND_UNBALANCED", cond);
      throw new IllegalArgumentException(errMsg);
    }
    _log.trace("Validate and process search condition: " + cond);

    // Validate/process everything inside ()
    // At this point the condition must have surrounding parentheses. Strip them off.
    String retCond = cond.substring(1, cond.length() - 1);

    // A blank string is OK at this point and means we are done
    if (StringUtils.isBlank(retCond)) return retCond;

    // Validate that condition is of the form <attr>.<op>.<value> where
    //       <attr> and <op> may contain only certain characters.
    // Validate and extract <attr>, <op> and <value>
    // <value> is everything passed the second . and <value> may be a CSV list
    int dot1 = retCond.indexOf('.');
    if (dot1 < 0)
    {
      String errMsg = MsgUtils.getMsg("SEARCH_COND_INVALID", cond);
      throw new IllegalArgumentException(errMsg);
    }
    int dot2 = retCond.indexOf('.', dot1 + 1);
    if (dot2 < 0)
    {
      String errMsg = MsgUtils.getMsg("SEARCH_COND_INVALID", cond);
      throw new IllegalArgumentException(errMsg);
    }
    String attr = retCond.substring(0, dot1);
    String op = retCond.substring(dot1 + 1, dot2);
    String fullValueStr = retCond.substring(dot2 + 1);
    // <attr>, <op> and <val> must not be empty
    if (StringUtils.isBlank(attr) || StringUtils.isBlank(op) || StringUtils.isBlank(fullValueStr))
    {
      String errMsg = MsgUtils.getMsg("SEARCH_COND_INVALID", cond);
      throw new IllegalArgumentException(errMsg);
    }

    // Validate <attr>
    // <attr> must start with [a-zA-Z] and contain only [a-zA-Z0-9_]
    Matcher m = (Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*$")).matcher(attr);
    if (!m.find())
    {
      String errMsg = MsgUtils.getMsg("SEARCH_COND_INVALID_ATTR", cond);
      throw new IllegalArgumentException(errMsg);
    }
    // Validate <op>
    // Verify <op> is supported.
    if (!SEARCH_OP_SET.contains(op.toUpperCase()))
    {
      String errMsg = MsgUtils.getMsg("SEARCH_COND_INVALID_OP", op, cond);
      throw new IllegalArgumentException(errMsg);
    }
    SearchOperator operator = SearchOperator.valueOf(op.toUpperCase());

    // Validate and process <value>
    // <value> may be a list so always build a list to simplify the logic below
    // If the operator takes a list set a flag since it will require special processing
    List<String> valList = Collections.emptyList();
    boolean isListOperator = listOpSet.contains(operator);
    if (isListOperator) valList = getValueList(fullValueStr); else valList = Collections.singletonList(fullValueStr);

    // Make sure each value does not have any of our special characters that are unescaped.
    for (String val : valList)
    {
      if (!validateValueSpecialChars(val))
      {
        String errMsg = MsgUtils.getMsg("SEARCH_COND_INVALID_VAL", cond);
        throw new IllegalArgumentException(errMsg);
      }
    }

    // For BETWEEN/NBETWEEN the value must be a 2 element list
    if ((operator.equals(SearchOperator.BETWEEN) || operator.equals(SearchOperator.NBETWEEN))
        && valList.size() != 2)
    {
      String errMsg = MsgUtils.getMsg("SEARCH_COND_INVALID_OP2", operator.name(), cond);
      throw new IllegalArgumentException(errMsg);
    }

    // For all operators except LIKE/NLIKE we can now unescape our special characters
    // LIKE/NLIKE deal with escaped characters (as opposed to EQ for example) but require special
    //   processing because of *! and %_. So leaving in escapes for LIKE/NLIKE simplifies processing.
    if (!operator.equals(SearchOperator.LIKE) && !operator.equals(SearchOperator.NLIKE))
    {
      var valListTmp = new ArrayList<String>();
      for (String val : valList)
      {
        // Unescape our special chars. For list operators commas are left escaped
        valListTmp.add(unescapeSpecialChars(val, isListOperator));
      }
      valList = valListTmp;
    }

    // For LIKE/NLIKE translate special characters as needed
    // Not a list operator so only one value in valList
    // * -> %
    // ! -> _
    if (operator.equals(SearchOperator.LIKE) || operator.equals(SearchOperator.NLIKE))
    {
      // Existing % and _ must be escaped before going to SQL
      //  so escape any unescaped %, _ characters
      fullValueStr = fullValueStr.replaceAll("(?<!\\\\)%", "\\\\%"); // match % but not \%
      fullValueStr = fullValueStr.replaceAll("(?<!\\\\)_", "\\\\_"); // match _ but not \_
      // Now do translations
      // Use regex to skip escaped characters
      fullValueStr = fullValueStr.replaceAll("(?<!\\\\)\\*", "%"); // match * but not \*
      fullValueStr = fullValueStr.replaceAll("(?<!\\\\)!", "_"); // match ! but not \!
      valList = Collections.singletonList(fullValueStr);
    }

    // Rebuild the list
    // NOTE that at this point each individual value in the list should not have any unescaped commas,
    //   because if it had then they would create separate items in the list when processed above.
    StringJoiner sj = new StringJoiner(",");
    for (String v : valList) { sj.add(v); }
    fullValueStr = sj.toString();
    retCond = attr + "." + op + "." + fullValueStr;
    return retCond;
  }

  /**
   * Break up a string containing comma separated values
   * @param valStr string containing comma separated list of values
   * @return Resulting list of strings
   */
  public static List<String> getValueList(String valStr)
  {
    List<String> retList = Arrays.asList(valStr.split("(?<!\\\\),")); // match , but not \,
    return retList;
  }

  /**
   * Check that value and sqlType are compatible for a value or list of values.
   * sqlTypeName, tableName and colName used only for logging
   * @param sqlType sql type to check against
   * @param op search operator, needed to determine if it might be a list of values
   * @param valStr string containing a single value or CSV list for a list operator
   * @param sqlTypeName name for sql type - logging only
   * @param tableName name of table - logging only
   * @param colName column name - logging only
   * @return true if valid, else false
   */
  public static boolean validateTypeAndValueList(int sqlType, SearchOperator op, String valStr,
                                                 String sqlTypeName, String tableName, String colName)
  {
    if (StringUtils.isBlank(valStr)) return true;
    List<String> valList = Collections.emptyList();
    // Build list of values to check
    if (listOpSet.contains(op))
      valList = getValueList(valStr);
    else
      valList = new ArrayList<>(Collections.singletonList(valStr));
    // Check each value
    for (String val : valList)
    {
      if (!validateTypeAndValue(sqlType, val, sqlTypeName))
      {
        String msg = MsgUtils.getMsg("SEARCH_DB_INVALID_SEARCH_VALUE", op.name(), sqlTypeName, val, tableName, colName);
        _log.error(msg);
        return false;
      }
    }
    return true;
  }

  // ************************************************************************
  // **************************  Private Methods  ***************************
  // ************************************************************************

  private static boolean isInteger(String valStr)
  {
    try { Integer.parseInt(valStr); } catch(NumberFormatException e) { return false; }
    return true;
  }

  private static boolean isLong(String valStr)
  {
    try { Long.parseLong(valStr); } catch(NumberFormatException e) { return false; }
    return true;
  }

  private static boolean isShort(String valStr)
  {
    try { Short.parseShort(valStr); } catch(NumberFormatException e) { return false; }
    return true;
  }

  private static boolean isDouble(String valStr)
  {
    try { Double.parseDouble(valStr); } catch(NumberFormatException e) { return false; }
    return true;
  }

  private static boolean isFloat(String valStr)
  {
    try { Float.parseFloat(valStr); } catch(NumberFormatException e) { return false; }
    return true;
  }

  /**
   * Check that value given as a string is a valid Tapis numeric
   * @param valStr value to check
   * @return true if valid, else false
   */
  private static boolean isNumeric(String valStr)
  {
    if (NumberUtils.isCreatable(valStr)) return false;
    return true;
  }

  /**
   * Check that value given as a string is a valid Tapis Timestamp
   * Valid strings are True, true, False, false
   * @param valStr value to check
   * @return true if valid, else false
   */
  private static boolean isTimestamp(String valStr)
  {
    try { Instant.parse(valStr); } catch(DateTimeParseException e) { return false; }
    return true;
  }

  /**
   * Check that value given as a string is a valid Tapis boolean
   * Do a case insensitive match against "true" and "false"
   * @param valStr value to check
   * @return true if valid, else false
   */
  private static boolean isBoolean(String valStr)
  {
    if (StringUtils.isBlank(valStr)) return false;
    if (valStr.equalsIgnoreCase("true") || valStr.equalsIgnoreCase("false"))
      return true;
    else
      return false;
  }

  /**
   * Check that value and sqlType are compatible.
   * sqlTypeName is only used for logging.
   * Mappings based on recommendations from:
   *   https://www.cis.upenn.edu/~bcpierce/courses/629/jdkdocs/guide/jdbc/getstart/mapping.doc.html
   * @param valStr value to check
   * @param sqlType sql type to check against
   * @param sqlTypeName name for sql type - logging only
   * @return true if valid, else false
   */
  private static boolean validateTypeAndValue(int sqlType, String valStr, String sqlTypeName)
  {
    if (StringUtils.isBlank(valStr)) return false;
    switch (sqlType)
    {
      case Types.CHAR:
      case Types.VARCHAR:
        if (StringUtils.isNotBlank(valStr)) return true;
        break;
      case Types.INTEGER:
        if (isInteger(valStr)) return true;
        break;
      case Types.BIGINT:
        if (isLong(valStr)) return true;
        break;
      case Types.SMALLINT:
        if (isShort(valStr)) return true;
        break;
      case Types.TINYINT:
        if (isShort(valStr)) return true;
        break;
      case Types.FLOAT:
        if (isDouble(valStr)) return true;
        break;
      case Types.REAL:
        if (isFloat(valStr)) return true;
        break;
      case Types.DOUBLE:
        if (isDouble(valStr)) return true;
        break;
      case Types.BOOLEAN:
        if (isBoolean(valStr)) return true;
        break;
      case Types.NUMERIC:
        if (isNumeric(valStr)) return true;
        break;
      case Types.DECIMAL:
        if (isNumeric(valStr)) return true;
        break;
      case Types.DATE:
        if (SearchUtils.isTimestamp(valStr)) return true;
        break;
      case Types.TIMESTAMP:
        if (SearchUtils.isTimestamp(valStr)) return true;
        break;
      default:
        // Sql Type not supported, log a warning and return false
        String msg = MsgUtils.getMsg("SEARCH_DB_UNSUPPORTED_SQLTYPE", sqlType, sqlTypeName);
        _log.warn(msg);
        break;
    }
    return false;
  }

  /**
   * Check for unescaped special characters in a value
   * @param valStr value to check
   * @return true if value contains no unescaped characters otherwise false
   */
  private static boolean validateValueSpecialChars(String valStr)
  {
    if (StringUtils.isBlank(valStr)) return true;

    // regex=(?<!\\)<char> Match <char> but not \<char>
    String regex = "(?<!" + Pattern.quote("\\") + ")";
    for (Character c : SEARCH_VAL_SPECIAL_CHARS)
    {
      Pattern p = Pattern.compile(regex + Pattern.quote(c.toString()));
      if (p.matcher(valStr).find()) return false;
    }
    return true;
  }

  /**
   * Unescape our special characters in a value
   * Some operators take a list and for those commas must remain escaped
   * @param valStr value to process
   * @param isListOp indicates operator takes a list
   * @return value with escape characters removed as appropriate
   */
  private static String unescapeSpecialChars(String valStr, boolean isListOp)
  {
    String retVal = valStr;
    if (StringUtils.isBlank(valStr)) return valStr;
    for (Character c : SEARCH_VAL_SPECIAL_CHARS)
    {
      if (isListOp &&  c.equals(',')) continue; // Commas remain escaped for list operators
      String regex = "\\\\" + Pattern.quote(c.toString());
      retVal = retVal.replaceAll(regex, c.toString()); // Replace \<char> with <char>
    }
    return retVal;
  }
}
