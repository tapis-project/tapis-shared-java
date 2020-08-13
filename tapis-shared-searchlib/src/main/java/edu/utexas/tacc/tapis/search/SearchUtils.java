package edu.utexas.tacc.tapis.search;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.validator.Msg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
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

  // Characters that must be escaped when appearing in a value
  private static final List<Character> SEARCH_VAL_SPECIAL_CHARS = Arrays.asList('~', ',', '(', ')');

  // ************************************************************************
  // *********************** Enums ******************************************
  // ************************************************************************

  // Supported operators for search
  public enum SearchOperator {EQ, NEQ, GT, GTE, LT, LTE, IN, NIN, LIKE, NLIKE, BETWEEN, NBETWEEN}

  // All search operator strings as a set
  public static final Set<String> SEARCH_OP_SET = Stream.of(SearchOperator.values()).map(Enum::name).collect(Collectors.toSet());

  // All search operators as a set
  public static final EnumSet<SearchOperator> allOpSet = EnumSet.allOf(SearchOperator.class);
  // Operators allowed for search when column is a string type
  public static final EnumSet<SearchOperator> stringOpSet =
        EnumSet.of(SearchOperator.EQ, SearchOperator.NEQ, SearchOperator.LT, SearchOperator.LTE,
                   SearchOperator.GT, SearchOperator.GTE,SearchOperator.LIKE, SearchOperator.NLIKE,
                   SearchOperator.BETWEEN, SearchOperator.NBETWEEN,
                   SearchOperator.IN, SearchOperator.NIN);
  // Operators allowed for search when column is a numeric type
  public static final EnumSet<SearchOperator> numericOpSet =
        EnumSet.of(SearchOperator.EQ, SearchOperator.NEQ, SearchOperator.LT, SearchOperator.LTE,
                   SearchOperator.GT, SearchOperator.GTE, SearchOperator.BETWEEN, SearchOperator.NBETWEEN,
                   SearchOperator.IN, SearchOperator.NIN);
  // Operators allowed for search when column is a timestamp type
  public static final EnumSet<SearchOperator> timestampOpSet =
        EnumSet.of(SearchOperator.EQ, SearchOperator.NEQ, SearchOperator.LT, SearchOperator.LTE,
                   SearchOperator.GT, SearchOperator.GTE, SearchOperator.BETWEEN, SearchOperator.NBETWEEN);
  // Operators allowed for search when column is a boolean type
  public static final EnumSet<SearchOperator> booleanOpSet =
        EnumSet.of(SearchOperator.EQ, SearchOperator.NEQ);

  // Operators for which the value may be a list
  public static final EnumSet<SearchOperator> listOpSet =
        EnumSet.of(SearchOperator.IN, SearchOperator.NIN, SearchOperator.BETWEEN, SearchOperator.NBETWEEN);

  // Map of java sql type to list of allowed search operators
  public static final Map<Integer, EnumSet<SearchOperator>> allowedOpsByTypeMap =
          Map.ofEntries(
                  Map.entry(Types.CHAR, stringOpSet),
                  Map.entry(Types.VARCHAR, stringOpSet),
                  Map.entry(Types.BIGINT, numericOpSet),
                  Map.entry(Types.DECIMAL, numericOpSet),
                  Map.entry(Types.DOUBLE, numericOpSet),
                  Map.entry(Types.FLOAT, numericOpSet),
                  Map.entry(Types.INTEGER, numericOpSet),
                  Map.entry(Types.NUMERIC, numericOpSet),
                  Map.entry(Types.REAL, numericOpSet),
                  Map.entry(Types.SMALLINT, numericOpSet),
                  Map.entry(Types.TINYINT, numericOpSet),
                  Map.entry(Types.BOOLEAN, booleanOpSet),
                  Map.entry(Types.DATE, timestampOpSet),
                  Map.entry(Types.TIMESTAMP, timestampOpSet)
          );

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
    // Validate that each condition has the form (<attr>.<op>.<value>)
    for (String cond : searchList)
    {
      // validate condition
      String bareCond = validateAndExtractSearchCondition(cond);
      retList.add(bareCond);
    }
    // Remove any empty matches, e.g. () might have been included one or more times
    retList.removeIf(item -> StringUtils.isBlank(item));
    return retList;
  }

  /**
   * Validate and extract a search condition that must have the form (<attr>.<op>.<value>)
   * Translate special characters for the LIKE and NLIKE operators.
   * @param cond the condition to process
   * @return the validated condition without surrounding parentheses
   * @throws IllegalArgumentException if condition is invalid
   */
  public static String validateAndExtractSearchCondition(String cond) throws IllegalArgumentException
  {
    if (StringUtils.isBlank(cond) || !cond.startsWith("(") || !cond.endsWith(")"))
    {
      String errMsg = MsgUtils.getMsg("SEARCH_COND_UNBALANCED", cond);
      throw new IllegalArgumentException(errMsg);
    }
    _log.trace("Validate and extract search condition: " + cond);

    // Validate/extract everything inside ()
    // At this point the condition must have surrounding parentheses. Strip them off.
    String retCond = cond.substring(1, cond.length() - 1);

    // A blank string is OK at this point and means we are done
    if (StringUtils.isBlank(retCond)) return retCond;

    // Validate that extracted condition is of the form <attr>.<op>.<value> where
    //       <attr> and <op> may contain only certain characters.
    // Validate and extract <attr>, <op> and <value>
    // <value> is everything passed the second .
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
    String val = retCond.substring(dot2 + 1);
    // <attr>, <op> and <val> must not be empty
    if (StringUtils.isBlank(attr) || StringUtils.isBlank(op) || StringUtils.isBlank(val))
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

    // Validate <value>
    // If the operator takes a list set a flag and extract the list
    // The value will require special processing
    List<String> valList = Collections.emptyList();
    boolean isListOperator = listOpSet.contains(operator);
    if (isListOperator) valList = getValueList(val);

    // Make sure value does not have unescaped special characters.
    // Some operators take a list in which case commas are allowed
    if (!validateValueSpecialChars(val, isListOperator))
    {
      String errMsg = MsgUtils.getMsg("SEARCH_COND_INVALID_VAL", cond);
      throw new IllegalArgumentException(errMsg);
    }

    // For BETWEEN/NBETWEEN the value must be a 2 element list
    if ((operator.equals(SearchOperator.BETWEEN) || operator.equals(SearchOperator.NBETWEEN))
        && valList.size() != 2)
    {
      String errMsg = MsgUtils.getMsg("SEARCH_COND_INVALID_OP2", operator.name(), cond);
      throw new IllegalArgumentException(errMsg);
    }

    // For LIKE/NLIKE translate special characters as needed
    // * -> %
    // ! -> _
    if (operator.equals(SearchOperator.LIKE) || operator.equals(SearchOperator.NLIKE))
    {
      // Existing % and _ must be escaped before going to SQL
      //  so escape any unescaped %, _ characters
      val = val.replaceAll("(?<!\\\\)%", "\\\\%"); // match % but not \%
      val = val.replaceAll("(?<!\\\\)_", "\\\\_"); // match _ but not \_
      // Now do translations
      // Use regex to skip escaped characters
      val = val.replaceAll("(?<!\\\\)\\*", "%"); // match * but not \*
      val = val.replaceAll("(?<!\\\\)!", "_"); // match ! but not \!
    }

    // TODO/TBD: Do we really need to unescape or just rebuild the list if necessary?
    //           because SQL will deal with them(?)
    // All special processing of escaped characters should be done by now
    //   so most characters currently escaped get replaced by themselves.
    //   The one exception is for LIKE/NLIKE escaped % and _ must remain
    // If it is an operator that takes a list we will need to re-build the
    // list so we can retain escaped commas
//    if (!isListOperator || valList.isEmpty())
//    {
//      val = unescapeValueString(val, operator);
//    }
//    else
    if (isListOperator)
    {
      // It is a list, re-build it
      // NOTE that at this point each individual value in the list should not have any unescaped commas,
      //   because if it had then they would create separate items in the list when processed above.
      StringJoiner sj = new StringJoiner(",");
      for (String v : valList)
      {
//        String v1 = unescapeValueString(v, operator);
//        v1 = v1.replaceAll(",", "\\\\,"); // Escape all commas
//        sj.add(v1);
        sj.add(v);
      }
      val = sj.toString();
    }
    retCond = attr + "." + op + "." + val;
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
   * Check that value and sqlType are compatible for a value or list of values
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
    return true; // TODO
  }

  /** TODO
   * Check that value given as a string is a valid Tapis Timestamp
   * Valid strings are True, true, False, false
   * @param valStr value to check
   * @return true if valid, else false
   */
  private static boolean isTimestamp(String valStr)
  {
    try { Instant.parse(valStr); } catch(DateTimeParseException e) { return false; }
    return true;
//    boolean cException = false;
//    try {
//      // Expecting UTC DateTime format: YYYY-MM-ddTHH:mm:ssZ
//      // Example: 2020-01-17T04:39:05Z
//      Instant v = Instant.parse(value);
//      validFormat.setValid(true);
//      validFormat.setTimeFormat(TimeFormat.UTC);
//    } catch(DateTimeParseException e) {
//      cException = true;
//      String msg = MsgUtils.getMsg("SEARCH_DATETIME_PARSE_EXCEPTION", value, TimeFormat.UTC);
//      _log.error(msg);
//    }
//    if(cException == true) {
//      try {
//        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
//                .parseCaseInsensitive()
//                .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
//                .optionalStart()
//                .appendPattern(".SSS")
//                .optionalEnd()
//                .toFormatter();
//        LocalDateTime date = LocalDateTime.parse(value,formatter);
//        validFormat.setValid(true);
//        validFormat.setTimeFormat(TimeFormat.LOCAL_DATE);
//        cException = false;
//      } catch(DateTimeParseException e) {
//        cException = true;
//        _log.debug("LocalDate parsing error ");
//        String msg = MsgUtils.getMsg("SEARCH_DATETIME_PARSE_EXCEPTION", value, TimeFormat.LOCAL_DATE);
//        _log.error(msg);
//      }
//    }
//    if(cException == true) {
//      try {
//        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
//                .parseCaseInsensitive()
//                .appendPattern("yyyy-MM-dd HH:mm:ss")
//                .optionalStart()
//                .appendPattern(".SSS")
//                .optionalEnd()
//                .toFormatter();
//
//
//        LocalDateTime date = LocalDateTime.parse(value,formatter);
//        _log.debug("---- "+ date + "--------");
//        validFormat.setValid(true);
//        validFormat.setTimeFormat(TimeFormat.LOCAL_DATE_WITH_FORMAT);
//        cException = false;
//      } catch( DateTimeParseException e) {
//        cException = true;
//        _log.debug("LocalDate_without_time parsing error ");
//        String msg = MsgUtils.getMsg("SEARCH_DATETIME_PARSE_EXCEPTION", value, TimeFormat.LOCAL_DATE_WITH_FORMAT);
//        _log.error(msg);
//      }
//    }
//    if(cException == true) {
//      try {
//
//        DateTimeFormatter formatter =  DateTimeFormatter.ISO_DATE;
//        LocalDate date = LocalDate.parse(value,formatter);
//        _log.debug("---- "+ date + "--------");
//        validFormat.setValid(true);
//        validFormat.setTimeFormat(TimeFormat.LOCAL_DATE_WITHOUT_TIME);
//        cException = false;
//      } catch(DateTimeParseException e) {
//        cException = true;
//        _log.debug("LocalDate_without_time parsing error ");
//        String msg = MsgUtils.getMsg("SEARCH_DATETIME_PARSE_EXCEPTION", value, TimeFormat.LOCAL_DATE_WITHOUT_TIME);
//        _log.error(msg);
//      }
//
//    }
//    return validFormat;
//  }
//
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
    if (StringUtils.isBlank(valStr)) return false; // TODO are blanks valid in some cases ???
    switch (sqlType)
    {
      case Types.CHAR:
      case Types.VARCHAR:
        if (StringUtils.isNotBlank(valStr)) return true;
        break;
      case Types.INTEGER:
        if (isInteger(valStr))  return true;
        break;
      case Types.BIGINT:
        if (isLong(valStr))  return true;
        break;
      case Types.SMALLINT:
        if (isShort(valStr))  return true;
        break;
      case Types.TINYINT:
        if (isShort(valStr))  return true;
        break;
      case Types.FLOAT:
        if (isDouble(valStr))  return true;
        break;
      case Types.REAL:
        if (isFloat(valStr))  return true;
        break;
      case Types.DOUBLE:
        if (isDouble(valStr))  return true;
        break;
      case Types.BOOLEAN:
        if (isBoolean(valStr)) return true;
        break;
      case Types.NUMERIC:
        if (isNumeric(valStr))  return true;
        break;
      case Types.DECIMAL:
        if (isNumeric(valStr))  return true;
        break;
      case Types.DATE:
        if (SearchUtils.isTimestamp(valStr))  return true;
        break;
      case Types.TIMESTAMP:
        if (SearchUtils.isTimestamp(valStr))  return true;
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
   * Some operators take a list and for those commas are allowed
   * @param valStr value to check
   * @param isListOp indicates operator takes a list
   * @return true if value contains no unescaped characters otherwise false
   */
  private static boolean validateValueSpecialChars(String valStr, boolean isListOp)
  {
    if (StringUtils.isBlank(valStr)) return true;

    // regex=(?<!\\)<char> Match <char> but not \<char>
    String regex = "(?<!" + Pattern.quote("\\") + ")";
    for (Character c : SEARCH_VAL_SPECIAL_CHARS)
    {
      if (isListOp &&  c.equals(',')) continue; // Commas allowed for list operators
      Pattern p = Pattern.compile(regex + Pattern.quote(c.toString()));
      if (p.matcher(valStr).find()) return false;
    }
    return true;
  }

//  /**
//   * Unescape most characters
//   * This should be called only after special processing of escaped characters has been done
//   * Most characters currently escaped get replaced by themselves.
//   *   The one exception is for LIKE/NLIKE escaped % and _ must remain
//   * @param valStr string to process
//   * @param op operator
//   * @return string with escaped characters replaced as appropriate
//   */
//  private static String unescapeValueString(String valStr, SearchOperator op)
//  {
//    // TODO/TBD: Still need to deal with escaped escape characters, i.e. \\
//    //            Consider multiple, especially odd number of escapes and single escapes at end of string
//    //   val = val.replaceAll("(?<!\\\\)\\\\", ""); // match \ but not \\
//    String retVal;
//    // Remove all \ except for \% \_
////    retVal = valStr.replaceAll("\\\\", ""); // Remove all escapes
//    retVal = valStr.replaceAll("\\\\(?![%_\\\\])", ""); // Remove \ except when followed by % or _ or \
//    return retVal;
//  }
}
