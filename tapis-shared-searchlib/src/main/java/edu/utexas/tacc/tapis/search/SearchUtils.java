package edu.utexas.tacc.tapis.search;

import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  // ************************************************************************
  // *********************** Enums ******************************************
  // ************************************************************************

  // Supported operators for search
  public enum SearchOperator {EQ, NEQ, GT, GTE, LT, LTE, IN, NIN, LIKE, NLIKE, BETWEEN, NBETWEEN}

  public static final Set<String> SEARCH_OP_SET = Stream.of(SearchOperator.values()).map(Enum::name).collect(Collectors.toSet());

  // Operators allowed for search when column is a string type
  public static final List<SearchOperator> stringOps =
          List.of(SearchOperator.EQ, SearchOperator.NEQ,
                  SearchOperator.LT, SearchOperator.LTE,
                  SearchOperator.GT, SearchOperator.GTE,
                  SearchOperator.LIKE, SearchOperator.NLIKE,
                  SearchOperator.BETWEEN, SearchOperator.NBETWEEN);
  // Operators allowed for search when column is a number type
  public static final List<SearchOperator> numberOps =
          List.of(SearchOperator.EQ, SearchOperator.NEQ,
                  SearchOperator.LT, SearchOperator.LTE,
                  SearchOperator.GT, SearchOperator.GTE,
                  SearchOperator.BETWEEN, SearchOperator.NBETWEEN);
  // Operators allowed for search when column is a timestamp type
  public static final List<SearchOperator> timeStampOps =
          List.of(SearchOperator.EQ, SearchOperator.NEQ,
                  SearchOperator.LT, SearchOperator.LTE,
                  SearchOperator.GT, SearchOperator.GTE,
                  SearchOperator.BETWEEN, SearchOperator.NBETWEEN);
  // Operators allowed for search when column is a boolean type
  public static final List<SearchOperator> boolOps =
          List.of(SearchOperator.EQ, SearchOperator.NEQ);

// TODO  public static final Set<SearchOperator> allOps = Arrays.asList(SearchOperator.values());

  // Map of jdbc sql type to list of allowed search operators
  public static final Map<Integer, List<SearchOperator>> allowedOpsByTypeMap =
          Map.ofEntries(
                  Map.entry(Types.CHAR, stringOps),
                  Map.entry(Types.VARCHAR, stringOps),
                  Map.entry(Types.BIGINT, numberOps),
                  Map.entry(Types.DECIMAL, numberOps),
                  Map.entry(Types.DOUBLE, numberOps),
                  Map.entry(Types.FLOAT, numberOps),
                  Map.entry(Types.INTEGER, numberOps),
                  Map.entry(Types.NUMERIC, numberOps),
                  Map.entry(Types.REAL, numberOps),
                  Map.entry(Types.SMALLINT, numberOps),
                  Map.entry(Types.TINYINT, numberOps),
                  Map.entry(Types.DATE, timeStampOps),
                  Map.entry(Types.TIMESTAMP, timeStampOps),
                  Map.entry(Types.BOOLEAN, boolOps)
          );

  // TODO/TBD needed?
  public static final Map<SearchOperator, List<Integer>> allowedTypesByOpMap =
          Map.ofEntries(
                  Map.entry(SearchOperator.EQ, List.of(Types.BOOLEAN)),
                  Map.entry(SearchOperator.LT, List.of(Types.VARCHAR))
          );

  /**
   * Convert a string into a SearchOperator
   * @param opStr- String containing all search conditions
   * @return the corresponding SearchOperator
   * @throws IllegalArgumentException if string is not a SearchOperator
   */
  public static SearchOperator getSearchOperator(String opStr) throws IllegalArgumentException
  {
    if (!SEARCH_OP_SET.contains(opStr))
    {
      String msg = MsgUtils.getMsg("SEARCH_INVALID_OP", opStr);
      throw new IllegalArgumentException(msg);
    }
    return SearchOperator.valueOf(opStr);
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
    // Use a regex pattern to split the string
    // Set delimiter as ~ and escape as \
    // Pattern.quote() does escaping of any special characters that need escaping in the regex
//    String escape = Pattern.quote("\\");
//    String delimiter = Pattern.quote("~");
    // Parse search string into a list of conditions using a regex and split
//    String regexStr = "(" + // start a match group
//                      "?:" + // match either of
//                        escape + "." + // any escaped character
//                       "|" + // or
//                       "[^" + delimiter + escape + "]++" + // match any char except delim or escape, possessive match
//                        ")" + // end a match group
//                        "+"; // repeat any number of times, ignoring empty results. Use * instead of + to include empty results
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
    String retCond = cond.substring(1, cond.length()-1);

    // A blank string is OK at this point and means we are done
    if (StringUtils.isBlank(retCond)) return retCond;

    // Validate that extracted condition is of the form <attr>.<op>.<value> where
    //       <attr> and <op> may contain only certain characters.
    // Validate and extract <attr>, <op> and <value>
    int dot1 = retCond.indexOf('.');
    if (dot1 < 0)
    {
      String errMsg = MsgUtils.getMsg("SEARCH_COND_INVALID", cond);
      throw new IllegalArgumentException(errMsg);
    }
    int dot2 = retCond.indexOf('.', dot1+1);
    if (dot2 < 0)
    {
      String errMsg = MsgUtils.getMsg("SEARCH_COND_INVALID", cond);
      throw new IllegalArgumentException(errMsg);
    }
    String attr = retCond.substring(0, dot1);
    String op = retCond.substring(dot1+1, dot2);
    String val = retCond.substring(dot2+1);
    // <attr>, <op> and <val> must not be empty
    // TODO/TBD: If we support unary operators then maybe <val> can be empty
    if (StringUtils.isBlank(attr) || StringUtils.isBlank(op) || StringUtils.isBlank(val))
    {
      String errMsg = MsgUtils.getMsg("SEARCH_COND_INVALID", cond);
      throw new IllegalArgumentException(errMsg);
    }
    // <attr> must start with [a-zA-Z] and contain only [a-zA-Z0-9_]
    Matcher m = (Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*$")).matcher(attr);
    if (!m.find())
    {
      String errMsg = MsgUtils.getMsg("SEARCH_COND_INVALID_ATTR", cond);
      throw new IllegalArgumentException(errMsg);
    }
    // Verify <op> is supported.
    if (!SEARCH_OP_SET.contains(op.toUpperCase()))
    {
      String errMsg = MsgUtils.getMsg("SEARCH_COND_INVALID_OP", cond);
      throw new IllegalArgumentException(errMsg);
    }
    return retCond;
  }

  /**
   * Check that value given as a string is a valid Tapis boolean
   * Valid strings are True, true, False, false
   * @param valStr value to check
   * @return true if valid, else false
   */
  public static boolean isBoolean(String valStr)
  {
    if (StringUtils.isBlank(valStr)) return false;
    if (valStr.equals("True") || valStr.equals("true") || valStr.equals("False") || valStr.equals("false"))
      return true;
    else
      return false;
  }
}
