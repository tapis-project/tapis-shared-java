package edu.utexas.tacc.tapis.search;

import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Types;
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

  public static final Set<String> SEARCH_OP_SET = Stream.of(SearchOperator.values()).map(Enum::name).collect(Collectors.toSet());

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

  // TODO  public static final Set<SearchOperator> allOps = Arrays.asList(SearchOperator.values());

  // Map of jdbc sql type to list of allowed search operators
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
                  Map.entry(Types.DATE, timestampOpSet),
                  Map.entry(Types.TIMESTAMP, timestampOpSet),
                  Map.entry(Types.BOOLEAN, booleanOpSet)
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
//    SEARCH_REGEX = "(?:\\\\.|[^~\\\\]++)+"
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
    // TODO/TBD: If we support unary operators then maybe <val> can be empty
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
    if (isListOperator)
    {
      valList = Arrays.asList(val.split("(?<!\\\\),")); // match , but not \,
    }

    // Make sure value does not have unescaped special characters.
    // Some operators take a list in which case commas are allowed
    if (!validateValueSpecialChars(val, isListOperator))
    {
      String errMsg = MsgUtils.getMsg("SEARCH_COND_INVALID_VAL", cond);
      throw new IllegalArgumentException(errMsg);
    }

    //    // Validate <val>
//    // If value might be a list then extract the list now.
//    // Otherwise build list with single item
//    List<String> valList;
//    if (listOpSet.contains(operator))
//    {
//      valList = Arrays.asList(val.split("(?<!\\\\),")); // match , but not \,
//    }
//    else
//    {
//     valList = Arrays.asList(val);
//    }
//    // Verify special chars are escaped
//    validateValueListSpecialChars(valList, cond);

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
      // Use regex to skip escaped characters
      // TODO What if % already present, or \%? same for _ or \_
      val = val.replaceAll("(?<!\\\\)\\*", "%"); // match * but not \*
      val = val.replaceAll("(?<!\\\\)!", "_"); // match ! but not \!
    }

    // All special processing of escaped characters should be done by now
    // so any characters currently escaped get replaced by themselves.
    // If it is an operator that takes a list we will need to re-build the
    // list so we can retain escaped commas
    if (!isListOperator || valList.isEmpty())
    {
      // TODO/TBD: Does this work correctly? what about odd number of escapes? \\\
      //           what if string ends with odd number of escapes?
      //   val = val.replaceAll("(?<!\\\\)\\\\", ""); // match \ but not \\
      val = val.replaceAll("\\\\", ""); // Remove all escapes
    }
    else
    {
      // It is a list, re-build it
      // NOTE that at this point each individual value in the list should not have any unescaped commas,
      //   because if it had then they would create separate items in the list when processed above.
      StringJoiner sj = new StringJoiner(",");
      for (String v : valList)
      {
        String v1 = v.replaceAll("\\\\", ""); // Remove all escapes TODO see above
        v1 = v1.replaceAll(",", "\\\\,"); // Escape all commas
        sj.add(v1);
      }
      val = sj.toString();
    }
    retCond = attr + "." + op + "." + val;
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

  // ************************************************************************
  // **************************  Private Methods  ***************************
  // ************************************************************************

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
      if (isListOp &&  c.equals(',')) continue;
      Pattern p = Pattern.compile(regex + Pattern.quote(c.toString()));
      if (p.matcher(valStr).find()) return false;
    }
    return true;
  }

//  /**
//   * Check for unescaped special characters in a list of value strings
//   * @param valListStr List of strings to check
//   * @param cond Original condition, used for constructing error msg
//   * @throws IllegalArgumentException if unescaped characters found
//   */
//  private static void validateValueListSpecialChars(List<String> valListStr, String cond) throws IllegalArgumentException
//  {
//    if (valListStr == null || valListStr.isEmpty()) return;
//    for (String valStr : valListStr)
//    {
//      if (StringUtils.isBlank(valStr)) continue;
//      // regex=(?<!\\)<char> Match <char> but not \<char>
//      String regex = "(?<!" + Pattern.quote("\\") + ")";
//      for (Character c : SEARCH_VAL_SPECIAL_CHARS)
//      {
//        Pattern p = Pattern.compile(regex + Pattern.quote(c.toString()));
//        if (p.matcher(valStr).find())
//        {
//          String errMsg = MsgUtils.getMsg("SEARCH_COND_INVALID_VAL", cond, valStr);
//          throw new IllegalArgumentException(errMsg);
//        }
//      }
//    }
//  }
//
//  /**
//   * Unescape characters in value
//   * @param valStr value to process
//   * @return value with escape characters removed
//   */
//  private static String unescapeValue(String valStr)
//  {
//    String retVal=null;
//    if (StringUtils.isBlank(valStr)) return valStr;
////    // TODO: Use better regex to make sure we do not replace an escaped backslash, e.g. '\\*' or '\\!'
//////    SEARCH_REGEX = "(?:\\\\.|[^~\\\\]++)+"
//////    String regexStr = "(" + // start a match group
//////                      "?:" + // match either of
//////                        escape + "." + // any escaped character
//////                       "|" + // or
//////                       "[^" + delimiter + escape + "]++" + // match any char except delim or escape, possessive match
//////                        ")" + // end a match group
//////                        "+"; // repeat any number of times, ignoring empty results. Use * instead of + to include empty results
////    val.replaceAll("(?:\\\\.&[^~\\\\]++)+", "%");
////    String regexStr = "(?:^|[^\\\\])%(([^\\\\%]|\\\\%|\\\\\\\\)*)%";
////    val = val.replaceAll("\\*", "%");
////    val = val.replaceAll("!", "_");
//    retVal = valStr.replaceAll("\\\\", "");
//    return retVal;
//  }
}
