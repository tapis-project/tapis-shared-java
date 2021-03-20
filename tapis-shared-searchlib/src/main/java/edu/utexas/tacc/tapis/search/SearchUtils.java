package edu.utexas.tacc.tapis.search;

import com.google.gson.JsonSyntaxException;
import edu.utexas.tacc.tapis.search.requests.ReqMatch;
import edu.utexas.tacc.tapis.search.requests.ReqSearch;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.threadlocal.OrderBy;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MultivaluedMap;

import java.sql.Types;
import java.time.LocalDateTime;
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

import static edu.utexas.tacc.tapis.shared.threadlocal.OrderBy.DEFAULT_ORDERBY_DIRECTION;
import static edu.utexas.tacc.tapis.shared.threadlocal.OrderBy.OrderByDir;

/**
 * Utility methods for search, sort and limit related requirements.
 * For operations using dedicated endpoints and existing endpoints.
 *
 * All methods should be static. By design class cannot be instantiated.
 */
public class SearchUtils
{
  // Private constructor to make it non-instantiable
  private SearchUtils() { throw new AssertionError(); }

  // ************************************************************************
  // *********************** Constants **************************************
  // ************************************************************************

  // Local logger.
  private static final Logger _log = LoggerFactory.getLogger(SearchUtils.class);

  // Our special characters. These must be escaped when appearing in a value
  private static final List<Character> SEARCH_VAL_SPECIAL_CHARS = Arrays.asList('~', ',', '(', ')');

  // Use static compiled regex patterns for performance
  // Regex for parsing (<attr1>.<op>.<val1>)~(<attr2>.<op>.<val2>) ... See validateAndExtractSearchList
  private static final Pattern SEARCH_PATTERN = Pattern.compile("(?:\\\\.|[^~\\\\]++)+");

  // Regex for splitting a string containing a comma delimited list
  //   Match "," but not "\," (i.e. match only an unescaped comma)
  private static final Pattern COMMASPLIT_PATTERN = Pattern.compile("(?<!\\\\),");

  // Regex for valid attribute name
  // <attr> must start with [a-zA-Z] and contain only [a-zA-Z0-9_]
  private static final Pattern VALIDATTR_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*$");

  // ************************************************************************
  // *********************** Enums ******************************************
  // ************************************************************************

  // Reserved query parameters that cannot be specified when using a dedicated search endpoint
  public enum ReservedQueryParm {PRETTY, SELECT, SEARCH, ORDERBY, LIMIT, SKIP, STARTAFTER, COMPUTETOTAL}
  public static final Set<String> RESERVED_QUERY_PARMS = Stream.of(ReservedQueryParm.values()).map(Enum::name).collect(Collectors.toSet());

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

  // ************************************************************************
  // *********************** Public methods *********************************
  // ************************************************************************

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
   * Extract a list of search conditions provided as a single string
   * Search list string must have the form  (<cond>)~(<cond>)~ ...
   *    where <cond> = <attr>.<op>.<value>
   * If there is only one condition the surrounding parentheses are optional
   * This method is intended for use by the front end api. The back end translates the Tapis LIKE wildcard
   *   characters (* and !) and deals with Tapis special characters.
   * @param searchListStr - String containing all search conditions
   * @return the list of extracted search conditions
   * @throws IllegalArgumentException if error encountered while parsing.
   */
  public static List<String> extractAndValidateSearchList(String searchListStr) throws IllegalArgumentException
  {
    var searchList = new ArrayList<String>();
    if (StringUtils.isBlank(searchListStr)) return searchList;
    // TODO: remove
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
    Matcher regexMatcher = SEARCH_PATTERN.matcher(searchListStr);
    while (regexMatcher.find()) { searchList.add(regexMatcher.group()); }
    // If we found only one match the searchList string may be a single condition that may or may not
    // be surrounded by parentheses. So handle that case.
    if (searchList.size() == 1)
    {
      String cond = searchList.get(0);
      // Add parentheses if not present, check start and end
      // Check for unbalanced parentheses in below loop over the search list
      if (!cond.startsWith("(") && !cond.endsWith(")")) cond = "(" + cond + ")";
      searchList.set(0, cond);
    }

    var retList = new ArrayList<String>();
    // Validate and process each condition. IllegalArg thrown if invalid
    for (String cond : searchList)
    {
      // Check for unbalanced parens. At this point the condition should not be blank and should be surrounded
      //   by parentheses
      if (StringUtils.isBlank(cond) || !cond.startsWith("(") || !cond.endsWith(")"))
      {
        String errMsg = MsgUtils.getMsg("SEARCH_COND_UNBALANCED", cond);
        throw new IllegalArgumentException(errMsg);
      }

      // At this point the condition must have surrounding parentheses. Strip them off.
      String bareCond = cond.substring(1, cond.length() - 1);
      // Validate the form of the condition
      validateSearchConditionForm(bareCond);
      retList.add(bareCond);
    }
    // Remove any empty matches, e.g. () might have been included one or more times
    retList.removeIf(item -> StringUtils.isBlank(item));
    return retList;
  }

  /**
   * Validate a search condition that must have the form <attr>.<op>.<value>
   * For operators that take a list (IN, NIN, BETWEEN, NBETWEEN) the value is processed as a CSV list
   * For the value(s):
   *   Check for any of the Tapis special chars that are unescaped
   *   Check that for BETWEEN/NBETWEEN it is a 2 element list
   * @param cond the condition to process
   * @throws IllegalArgumentException if condition is invalid
   */
  public static void validateSearchConditionForm(String cond) throws IllegalArgumentException
  {
    // TODO remove
    _log.trace("Validating form for search condition: " + cond);
    // A blank string is OK at this point and means we are done
    if (StringUtils.isBlank(cond)) return;

    // Validate the 3 components of a condition
    extractAttribute(cond);
    SearchOperator operator = extractOperator(cond);
    extractFullValueStr(cond, operator);
  }

  /**
   * Validate and process a search condition that must have the form <attr>.<op>.<value>
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
    _log.trace("Validate and process search condition: " + cond);
    String retCond = cond;
    // A blank string is OK at this point and means we are done
    if (StringUtils.isBlank(retCond)) return retCond;

    // Validate and extract the 3 components of a condition
    String attr = extractAttribute(retCond);
    SearchOperator operator = extractOperator(retCond);
    String fullValueStr = extractFullValueStr(retCond, operator);

    // <value> may be a list so always build a list to simplify the logic below
    // If the operator takes a list set a flag since it will require special processing
    List<String> valList;
    boolean isListOperator = listOpSet.contains(operator);
    if (isListOperator) valList = getValueList(fullValueStr); else valList = Collections.singletonList(fullValueStr);

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
    retCond = attr + "." + operator.name() + "." + fullValueStr;
    return retCond;
  }

  /**
   * Break up a string containing comma separated values
   * Match "," but not "\," (i.e. match only an unescaped comma)
   * @param valStr string containing comma separated list of values
   * @return Resulting list of strings
   */
  public static List<String> getValueList(String valStr)
  {
    if (StringUtils.isBlank(valStr)) return new ArrayList<>();
    else return Arrays.asList(COMMASPLIT_PATTERN.split(valStr));

  }

  /**
   * Convert a string value or list of string values into strings suitable for sql operators.
   * The string(s) must have already been checked for validity as a Tapis timestamp
   * @param op Search operator, indicates if value may be a list
   * @param valStr string containing comma separated list of values
   * @return Resulting list of strings
   */
  public static String convertValuesToTimestamps(SearchOperator op, String valStr)
  {
    List<String> valList;
    if (listOpSet.contains(op)) valList = getValueList(valStr); else valList = Collections.singletonList(valStr);
    StringJoiner sj = new StringJoiner(",");
    // For each value convert string to an Instant and then back into a string suitable for SQL
    for (String tStr : valList)
    {
      LocalDateTime t = TapisUtils.getUTCTimeFromString(tStr);
      sj.add(TapisUtils.getSQLStringFromUTCTime(t));
    }
    return sj.toString();
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
    List<String> valList;
    // Build list of values to check
    if (listOpSet.contains(op))
      valList = getValueList(valStr);
    else
      valList = new ArrayList<>(Collections.singletonList(valStr));
    // Check each value
    for (String val : valList)
    {
      if (!validTypeAndValue(sqlType, val, sqlTypeName))
      {
        String msg = MsgUtils.getMsg("SEARCH_DB_INVALID_SEARCH_VALUE", op.name(), sqlTypeName, val, tableName, colName);
        _log.error(msg);
        return false;
      }
    }
    return true;
  }

  /**
   * Build a search list from query parameters. Query parameters use the format <attr>.<op>=<value>
   * Reserved query parameters are ignored.
   * This method is intended for use by the front end api. The back end translates the Tapis LIKE wildcard
   *   characters (* and !) and deals with Tapis special characters.
   * @param queryParms map of query parameters
   * @return list of search conditions, null if no query parameters
   * @throws IllegalArgumentException if an invalid condition is encountered
   */
  public static List<String> buildListFromQueryParms(MultivaluedMap<String,String> queryParms)
    throws IllegalArgumentException
  {
    if (queryParms == null || queryParms.isEmpty()) return null;
    var searchList = new ArrayList<String>();
    // For each query parameter not on the reserved list add a search condition
    for (Map.Entry<String,List<String>> qParm: queryParms.entrySet())
    {
      String qKey = qParm.getKey();
      if (RESERVED_QUERY_PARMS.contains(qKey.toUpperCase())) continue;
      // Query parameters may appear more than once so there may be multiple values. Add condition for each one
      for (String val : qParm.getValue())
      {
        String cond = qKey + "." + val;
        // Validate the search condition form
        validateSearchConditionForm(cond);
        searchList.add(cond);
      }
    }
    return searchList;
  }

  /**
   * Convert a string from camelCase to snake_case.
   * If input string is null or empty then input string is returned
   * @param str string to convert
   * @return resulting string converted to snake_case
   */
  public static String camelCaseToSnakeCase(String str)
  {
    if (StringUtils.isBlank(str)) return str;
    String regex1 = "([A-Z]+)([A-Z][a-z])";
    String regex2 = "([a-z])([A-Z])";
    String retStr = str.replaceAll(regex1, "$1_$2").replaceAll(regex2,"$1_$2");
    return retStr.toLowerCase();
  }

  /**
   * Check validity of orderBy query parameter which must be a comma separated list of items in
   *   the form <attr_name>(<dir>) where (<dir>) is optional and <dir> = "asc" or "desc"
   * @param orderByQueryParamListStr - string value of orderBy query parameter
   * @return an error message if invalid else return null.
   */
  public static String checkOrderByQueryParam(String orderByQueryParamListStr, List<String> items)
  {
    // Must not be empty
    if (StringUtils.isBlank(orderByQueryParamListStr) || items == null || items.isEmpty()) return "Empty";
    for (String item: items)
    {
      // Check format of entry
      try { OrderBy.fromString(item); }
      catch (IllegalArgumentException e)
      {
        return "orderBy entry invalid. Entry: " + item + ". OrderBy list: " + orderByQueryParamListStr;
      }
      // Check that column name is valid
      if (invalidAttributeName(getOrderByAttribute(item)))
        return "Invalid attribute name for item: " + item + ". OrderBy list: " + orderByQueryParamListStr;
    }
    return null;
  }

  /**
   * Extract attributes from orderBy query parameter which must be a comma separated list of items in
   *   the form <attr_name>(<dir>) where (<dir>) is optional and <dir> = "asc" or "desc"
   * @param orderByQueryParamListStr - string value of orderBy query parameter
   * @return list of orderBy entries.
   */
  public static List<OrderBy> buildOrderByList(String orderByQueryParamListStr, List<String> items)
  {
    var retList = new ArrayList<OrderBy>();
    // if empty return empty list
    if (StringUtils.isBlank(orderByQueryParamListStr)) return retList;
    for (String item: items)
    {
      String attr = item.substring(0, item.indexOf('('));
      // Validation of attr should have already happened, but just in case let's make sure it is safe to
      //   use when constructing a new orderBy entry.
      if (StringUtils.isBlank(attr)) continue;
      OrderBy ob = OrderBy.valueOf(attr, getOrderByDirection(item));
      retList.add(ob);
    }
    return retList;
  }

  /**
   * Extract attribute name from orderBy query parameter which must be in the form <col_name>(<dir>)
   *   where (<dir>) is optional and <dir> = "asc" or "desc"
   * NOTE: This method assumes that value has been checked using checkOrderByQueryParam
   * @param orderByStr - string value of orderBy entry
   * @return the orderBy attribute name or null if not found
   */
  public static String getOrderByAttribute(String orderByStr)
  {
    if (StringUtils.isBlank(orderByStr)) return null;
    String attrName = orderByStr;
    if (orderByStr.indexOf('(') >= 0) attrName =  orderByStr.substring(0,orderByStr.indexOf('('));
    return attrName;
  }

  /**
   * Extract direction from orderBy entry which must be in the form <col_name>(<dir>)
   *   where (<dir>) is optional and <dir> = "asc" or "desc"
   * NOTE: This method assumes that value has been checked using checkOrderByQueryParam
   * @param orderByStr - string value of orderBy entry
   * @return the orderBy direction or default value if direction not specified
   */
  public static OrderByDir getOrderByDirection(String orderByStr)
  {
    OrderByDir orderByDir = DEFAULT_ORDERBY_DIRECTION;
    int orderDirStart = orderByStr.indexOf('(');
    if (orderDirStart > 0)
    {
      String orderDirStr = orderByStr.substring(orderDirStart+1, orderByStr.length()-1).toUpperCase();
      if (!StringUtils.isBlank(orderDirStr)) orderByDir = OrderByDir.valueOf(orderDirStr);
    }
    return orderByDir;
  }

  /**
   * Given json from a search request body put together the complete SQL-like search string.
   * @param rawJson - json containing search request
   * @return - Full search string
   * @throws JsonSyntaxException on error parsing json
   */
  public static String getSearchFromRequestJson(String rawJson) throws JsonSyntaxException
  {
    ReqSearch req;
    req = TapisGsonUtils.getGson().fromJson(rawJson, ReqSearch.class);
    // Concatenate all strings into a single SQL-like search string
    // When put together full string must be a valid SQL-like where clause. This will be validated in the service call.
    // Not all SQL syntax is supported. See SqlParser.jj in tapis-shared-searchlib.
    StringJoiner sj = new StringJoiner(" ");
    if (req != null && req.search != null)
    {
      for (String s : req.search) { sj.add(s); }
    }
    return sj.toString();
  }

  /**
   * Given json from a match request body put together the complete SQL-like search string.
   * @param rawJson - json containing match request
   * @return - Full match string
   * @throws JsonSyntaxException on error parsing json
   */
  public static String getMatchFromRequestJson(String rawJson) throws JsonSyntaxException
  {
    ReqMatch req;
    req = TapisGsonUtils.getGson().fromJson(rawJson, ReqMatch.class);
    // Concatenate all strings into a single SQL-like search string
    StringJoiner sj = new StringJoiner(" ");
    if (req != null & req.match != null) { for (String s : req.match) { sj.add(s); } }
    return sj.toString();
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
    return NumberUtils.isCreatable(valStr);
  }

  /**
   * Check that value given as a string is a valid Tapis Timestamp
   * Use default access instead of private for unit testing.
   * Examples of valid timestamp formats:
   * 2020-04-29T20:15:52:123456-06:00
   * 2020-04-29T20:15:52:126Z
   * 2020-04-29T20:15:52-06:00
   * 2020-04-29T20:15-06:00
   * 2020-04-29T20-06:00
   * 2020-04-29-06:00
   * 2020-04Z
   * 2020
   * @param valStr value to check
   * @return true if valid, else false
   */
  static boolean isTimestamp(String valStr)
  {
    // Use shared utility method to attempt to convert string to a timestamp
    try { TapisUtils.getUTCTimeFromString(valStr); } catch(DateTimeParseException e) { return false; }
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
   * Check that attribute name is valid
   * @param attrStr value to check
   * @return true if valid, else false
   */
  private static boolean invalidAttributeName(String attrStr)
  {
    // <attr> must start with [a-zA-Z] and contain only [a-zA-Z0-9_]
    // Turn the pattern into a matcher and make sure attrStr matches
    return !VALIDATTR_PATTERN.matcher(attrStr).find();
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
  private static boolean validTypeAndValue(int sqlType, String valStr, String sqlTypeName)
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
      case Types.TINYINT:
        if (isShort(valStr)) return true;
        break;
      case Types.FLOAT:
      case Types.DOUBLE:
        if (isDouble(valStr)) return true;
        break;
      case Types.REAL:
        if (isFloat(valStr)) return true;
        break;
      case Types.BOOLEAN:
        if (isBoolean(valStr)) return true;
        break;
      case Types.NUMERIC:
      case Types.DECIMAL:
        if (isNumeric(valStr)) return true;
        break;
      case Types.DATE:
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
   * Unescape Tapis special characters in a value
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

  /**
   * Extract and validate attribute name in a condition having the form attr.op.value
   * NOTE: Make package-private for use in test code
   * @param condStr condition string
   * @return attribute name
   */
  static String extractAttribute(String condStr)
  {
    int dot1 = condStr.indexOf('.');
    if (dot1 < 0)
    {
      String errMsg = MsgUtils.getMsg("SEARCH_COND_INVALID_NOATTR", condStr);
      throw new IllegalArgumentException(errMsg);
    }
    String attr = condStr.substring(0, dot1);
    // <attr> must not be empty
    if (StringUtils.isBlank(attr))
    {
      String errMsg = MsgUtils.getMsg("SEARCH_COND_INVALID_NOATTR", condStr);
      throw new IllegalArgumentException(errMsg);
    }

    // Validate <attr>
    if (invalidAttributeName(attr))
    {
      String errMsg = MsgUtils.getMsg("SEARCH_COND_INVALID_ATTR", condStr);
      throw new IllegalArgumentException(errMsg);
    }
    return attr;
  }

  /**
   * Extract and validate operator in a condition having the form attr.op.value
   * NOTE: Make package-private for use in test code
   * @param condStr condition string
   * @return search operator from condition
   */
  static SearchOperator extractOperator(String condStr)
  {
    int dot1 = condStr.indexOf('.');
    if (dot1 < 0)
    {
      String errMsg = MsgUtils.getMsg("SEARCH_COND_INVALID_NOOPER", condStr);
      throw new IllegalArgumentException(errMsg);
    }
    int dot2 = condStr.indexOf('.', dot1 + 1);
    if (dot2 < 0)
    {
      String errMsg = MsgUtils.getMsg("SEARCH_COND_INVALID_NOOPER", condStr);
      throw new IllegalArgumentException(errMsg);
    }
    String op = condStr.substring(dot1 + 1, dot2);
    // <op> must not be empty
    if (StringUtils.isBlank(op))
    {
      String errMsg = MsgUtils.getMsg("SEARCH_COND_INVALID_NOOPER", condStr);
      throw new IllegalArgumentException(errMsg);
    }

    // Validate <op>
    // Verify <op> is supported.
    if (!SEARCH_OP_SET.contains(op.toUpperCase()))
    {
      String errMsg = MsgUtils.getMsg("SEARCH_COND_INVALID_OP", op, condStr);
      throw new IllegalArgumentException(errMsg);
    }
    return SearchOperator.valueOf(op.toUpperCase());
  }

  /**
   * Extract and validate value in a condition having the form attr.op.value
   * NOTE: Make package-private for use in test code
   * @param condStr condition string
   * @return full value string which may be a list
   */
  static String extractFullValueStr(String condStr, SearchOperator operator)
  {
    int dot1 = condStr.indexOf('.');
    if (dot1 < 0)
    {
      String errMsg = MsgUtils.getMsg("SEARCH_COND_INVALID_NOVAL", condStr);
      throw new IllegalArgumentException(errMsg);
    }
    int dot2 = condStr.indexOf('.', dot1 + 1);
    if (dot2 < 0)
    {
      String errMsg = MsgUtils.getMsg("SEARCH_COND_INVALID_NOVAL", condStr);
      throw new IllegalArgumentException(errMsg);
    }
    String fullValueStr = condStr.substring(dot2 + 1);
    // <val> must not be empty
    if (StringUtils.isBlank(fullValueStr))
    {
      String errMsg = MsgUtils.getMsg("SEARCH_COND_INVALID_NOVAL", condStr);
      throw new IllegalArgumentException(errMsg);
    }

    // Validate and process <value>
    // <value> may be a list so always build a list to simplify the logic below
    List<String> valList;
    if (listOpSet.contains(operator)) valList = getValueList(fullValueStr); else valList = Collections.singletonList(fullValueStr);

    // Make sure each value does not have any of our special characters that are unescaped.
    for (String val : valList)
    {
      if (!validateValueSpecialChars(val))
      {
        String errMsg = MsgUtils.getMsg("SEARCH_COND_INVALID_VAL", condStr);
        throw new IllegalArgumentException(errMsg);
      }
    }

    // For BETWEEN/NBETWEEN the value must be a 2 element list
    if ((operator.equals(SearchOperator.BETWEEN) || operator.equals(SearchOperator.NBETWEEN))
            && valList.size() != 2)
    {
      String errMsg = MsgUtils.getMsg("SEARCH_COND_INVALID_OP2", operator.name(), condStr);
      throw new IllegalArgumentException(errMsg);
    }
    return fullValueStr;
  }
}
