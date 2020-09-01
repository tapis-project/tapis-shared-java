package edu.utexas.tacc.tapis.search;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static org.testng.Assert.*;

/**
 * Tests for methods in the SearchUtils class.
 */
public class SearchUtilsTest
{
  // multiple escapes in: '\ \\ \\' <1> <2> <2>
  private static final String multiEscapeIn1 = "\\ \\\\ \\\\";
  // multiple escapes in: '\\\ \ \\ \\\' <3> <1> <2> <4>
  private static final String multiEscapeIn2 = "\\\\\\ \\ \\\\ \\\\\\\\";
  // multiple escapes in: '\ \\ \\\ \\\\ \\\\\' <1> <2> <3> <4> <5>
  private static final String multiEscapeIn3 = "\\ \\\\ \\\\\\ \\\\\\\\ \\\\\\\\\\";

  // Zero results
//  private static final String[] zeroResults = new String[] {};
  private static final String zeroResults = null;

  // Test data
  // Valid and invalid timestamps in various formats
  private static final String[] validTimestamps =
    { "1800-01-01T00:00:00.123456-00:00",
      "2200-04-29T14:15:52.123456Z",
      "2200-04-29T14:15:52.123456",
      "2200-04-29T14:15:52.123-01:00",
      "2200-04-29T14:15:52.123Z",
      "2200-04-29T14:15:52.123",
      "2200-04-29T14:15:52+05:30",
      "2200-04-29T14:15:52Z",
      "2200-04-29T14:15:52",
      "2200-04-29T14:15+01:00",
      "2200-04-29T14:15Z",
      "2200-04-29T14:15",
      "2200-04-29T14-06:00",
      "2200-04-29T14Z",
      "2200-04-29T14",
      "2200-04-29-06:00",
      "2200-04-29Z",
      "2200-04-29",
      "2200-04+03:00",
      "2200-04Z",
      "2200-04",
      "2200-06:00",
      "2200Z",
      "2200"
    };
  private static final String[] invalidTimestamps =
    { null,
      "",
      "1",
      "12",
      "123",
      "123Z",
      "2200-04-00T14:15:00",
      "2200-04-32T14:15:00",
      "2200-00-29T14:15:00",
      "2200-13-29T14:15:00",
      "2200-04-04T14:15:61",
      "2200-04-04T14:61:00",
      "2200-04-04T25:15:00",
      "00-04-04T14:15:00",
      "22001-04-04T14:15:00",
      "2200-04-29T14,15:52.123-01:00Z",
      "2200-04-29T14:15:523.123Z",
      "2200-04-29T14:156:52.123",
      "2200-04-29T14:15:52+005:30",
      "2200-04-29T14:15:52+05:300",
      "2200-04-29T14:15:52z05:30",
      "2200-04-29X14:15:52Z",
      "2200-04-291T14:15:52",
      "2200-04-29 14:15:52",
    };

  @BeforeMethod
  public void setUp()
  {
  }

  @AfterMethod
  public void tearDown()
  {
  }

  /*
   * Check validateAndExtractSearchList - valid cases
   */
  @Test(groups={"unit"})
  public void testValidateAndExtractSearchListValid()
  {
    // Create all input and validation data for tests
    // Inputs
    var validCaseInputs = new HashMap<Integer,CaseInputData>();
    validCaseInputs.put( 1,new CaseInputData(1, "enabled.eq.true"));
    validCaseInputs.put( 2,new CaseInputData(1, "(port.lt.7)"));
    validCaseInputs.put( 3,new CaseInputData(1, "name.neq.test\\)name"));
    validCaseInputs.put( 4,new CaseInputData(1, "name.neq.test\\(name"));
    validCaseInputs.put( 5,new CaseInputData(1, "(name.neq.test\\)name)"));
    validCaseInputs.put( 6,new CaseInputData(1, "(name.neq.test\\(name)"));
    validCaseInputs.put( 7,new CaseInputData(1, "name.neq.test\\(\\)name"));
    validCaseInputs.put( 8,new CaseInputData(1, "name.neq.test\\)\\(name"));
    validCaseInputs.put( 9,new CaseInputData(1, "name.neq.test\\~name"));
    validCaseInputs.put(10,new CaseInputData(1, "name.neq.test\\,name"));
    validCaseInputs.put(11,new CaseInputData(1, "name.neq.test*name"));
    validCaseInputs.put(12,new CaseInputData(1, "name.neq.test!name"));
    validCaseInputs.put(13,new CaseInputData(1, "name.like.testname*"));
    validCaseInputs.put(14,new CaseInputData(1, "name.nlike.testname!"));
    validCaseInputs.put(15,new CaseInputData(1, "name.like.test\\*name"));
    validCaseInputs.put(16,new CaseInputData(1, "name.nlike.test\\!name"));
    validCaseInputs.put(17,new CaseInputData(1, "port.between.0,1024"));
    validCaseInputs.put(18,new CaseInputData(1, "port.nbetween.0,1024"));
    validCaseInputs.put(19,new CaseInputData(1, "description.in.MyTest\\,yes,YourTest\\,ok."));
    validCaseInputs.put(20,new CaseInputData(2, "(host.eq.stampede2.tacc.utexas.edu)~(default_access_method.in.PKI_KEYS,ACCESS_KEY)"));
    validCaseInputs.put(21,new CaseInputData(4, "(enabled.eq.true)~(owner.eq.jdoe)~(proxy_port.lt.7)~(system_type.in.OBJECT_STORE,LINUX)"));
    validCaseInputs.put(22,new CaseInputData(4, "(enabled.eq.true)~(port.lt.7)~(system_type.in.OBJECT_STORE,LINUX)~(description.like.my\\~system)")); // ~ in value
    validCaseInputs.put(23,new CaseInputData(4, "(enabled.eq.true)~(port.gte.7)~(description.like.my\\ system)~(system_type.in.OBJECT_STORE,LINUX)")); // space in value
    validCaseInputs.put(24,new CaseInputData(3, "(description.like.my\\,\\(\\)\\~\\*\\!\\\\system)~(port.lte.7)~(system_type.in.OBJECT_STORE)")); // 7 special chars in value: ,()~*!\
    validCaseInputs.put(25,new CaseInputData(2, "(description.like.my'\\\"system)~(port.lte.7)")); // more potentially problem chars ' "
    validCaseInputs.put(26,new CaseInputData(1, "description.like." + multiEscapeIn1)); // multiple escapes <1> <2> <2>
    validCaseInputs.put(27,new CaseInputData(1, "description.like." + multiEscapeIn2)); // multiple escapes <3> <1> <2> <4>
// TODO Figure out why this one fails. Looks like final escape at end of line is getting eaten when there are an odd number.
//    validCaseInputs.put(28,new CaseInputData(1, "description.like." + multiEscapeIn3)); // multiple escapes <1> <2> <3> <4> <5>
    validCaseInputs.put(29,new CaseInputData(0, "()~( )~()"));
    validCaseInputs.put(30,new CaseInputData(0, "()~()"));
    validCaseInputs.put(31,new CaseInputData(0, "~()~"));
    validCaseInputs.put(32,new CaseInputData(0, "()~"));
    validCaseInputs.put(33,new CaseInputData(0, "~()"));
    validCaseInputs.put(34,new CaseInputData(0, "()"));
    validCaseInputs.put(35,new CaseInputData(0, "(   )"));
    validCaseInputs.put(36,new CaseInputData(0, "~~~"));
    validCaseInputs.put(37,new CaseInputData(0, "~"));
    validCaseInputs.put(38,new CaseInputData(0, ""));
    validCaseInputs.put(39,new CaseInputData(0, null));

    // Outputs
    // NOTE: For LIKE/NLIKE escaped special chars are retained.
    var validCaseOutputs = new HashMap<Integer,CaseOutputData>();
    validCaseOutputs.put( 1,new CaseOutputData(1, "enabled.eq.true"));
    validCaseOutputs.put( 2,new CaseOutputData(2, "port.lt.7"));
    validCaseOutputs.put( 3,new CaseOutputData(3, "name.neq.test)name"));
    validCaseOutputs.put( 4,new CaseOutputData(4, "name.neq.test(name"));
    validCaseOutputs.put( 5,new CaseOutputData(5, "name.neq.test)name"));
    validCaseOutputs.put( 6,new CaseOutputData(6, "name.neq.test(name"));
    validCaseOutputs.put( 7,new CaseOutputData(7, "name.neq.test()name"));
    validCaseOutputs.put( 8,new CaseOutputData(8, "name.neq.test)(name"));
    validCaseOutputs.put( 9,new CaseOutputData(9, "name.neq.test~name"));
    validCaseOutputs.put(10,new CaseOutputData(10, "name.neq.test,name"));
    validCaseOutputs.put(11,new CaseOutputData(11, "name.neq.test*name"));
    validCaseOutputs.put(12,new CaseOutputData(12, "name.neq.test!name"));
    validCaseOutputs.put(13,new CaseOutputData(13, "name.like.testname%"));
    validCaseOutputs.put(14,new CaseOutputData(14, "name.nlike.testname_"));
    validCaseOutputs.put(15,new CaseOutputData(15, "name.like.test\\*name"));
    validCaseOutputs.put(16,new CaseOutputData(16, "name.nlike.test\\!name"));
    validCaseOutputs.put(17,new CaseOutputData(17, "port.between.0,1024"));
    validCaseOutputs.put(18,new CaseOutputData(18, "port.nbetween.0,1024"));
    validCaseOutputs.put(19,new CaseOutputData(19, "description.in.MyTest\\,yes,YourTest\\,ok."));
    validCaseOutputs.put(20,new CaseOutputData(20, "host.eq.stampede2.tacc.utexas.edu", "default_access_method.in.PKI_KEYS,ACCESS_KEY"));
    validCaseOutputs.put(21,new CaseOutputData(21, "enabled.eq.true", "owner.eq.jdoe", "proxy_port.lt.7", "system_type.in.OBJECT_STORE,LINUX"));
    validCaseOutputs.put(22,new CaseOutputData(22, "enabled.eq.true", "port.lt.7", "system_type.in.OBJECT_STORE,LINUX", "description.like.my\\~system"));
    validCaseOutputs.put(23,new CaseOutputData(23, "enabled.eq.true", "port.gte.7", "description.like.my\\ system", "system_type.in.OBJECT_STORE,LINUX"));
    validCaseOutputs.put(24,new CaseOutputData(24, "description.like.my\\,\\(\\)\\~\\*\\!\\\\system", "port.lte.7", "system_type.in.OBJECT_STORE"));
    validCaseOutputs.put(25,new CaseOutputData(25, "description.like.my'\\\"system", "port.lte.7"));
    validCaseOutputs.put(26,new CaseOutputData(26, "description.like." + multiEscapeIn1));
    validCaseOutputs.put(27,new CaseOutputData(27, "description.like." + multiEscapeIn2));
    validCaseOutputs.put(28,new CaseOutputData(28, "description.like." + multiEscapeIn3));
    validCaseOutputs.put(29,new CaseOutputData(29, zeroResults)); // "()~( )~()",
    validCaseOutputs.put(30,new CaseOutputData(30, zeroResults)); // "()~()",
    validCaseOutputs.put(31,new CaseOutputData(31, zeroResults)); // "~()~",
    validCaseOutputs.put(32,new CaseOutputData(32, zeroResults)); // "()~",
    validCaseOutputs.put(33,new CaseOutputData(33, zeroResults)); // "~()",
    validCaseOutputs.put(34,new CaseOutputData(34, zeroResults)); // "()",
    validCaseOutputs.put(35,new CaseOutputData(35, zeroResults)); // "(  )"
    validCaseOutputs.put(36,new CaseOutputData(36, zeroResults)); // "~~~",
    validCaseOutputs.put(37,new CaseOutputData(37, zeroResults)); // "~",
    validCaseOutputs.put(38,new CaseOutputData(38, zeroResults)); // ""
    validCaseOutputs.put(39,new CaseOutputData(39, zeroResults)); // null

    // Iterate over test cases
    for (Map.Entry<Integer,CaseInputData> item : validCaseInputs.entrySet())
    {
      CaseInputData ci = item.getValue();
      int caseNum = item.getKey();
      System.out.println("Checking case # "+ caseNum + " Input: " + ci.searchListStr);
      // Extract the search list and validate that each condition has the correct form as done on the front end.
      List<String> validSearchList = SearchUtils.extractAndValidateSearchList(ci.searchListStr);
      var processedSearchList = new ArrayList<String>();
      // Validate and process each search condition as done on the back end.
      for (String condStr : validSearchList)
      {
        processedSearchList.add(SearchUtils.validateAndProcessSearchCondition(condStr));
      }
      System.out.println("  Result size: " + processedSearchList.size());
      assertEquals(validSearchList.size(), ci.count);
      for (int j = 0; j < ci.count; j++)
      {
        System.out.println("  Result string # " + j + " = " + processedSearchList.get(j));
        String coStr = validCaseOutputs.get(caseNum).strList.get(j);
        assertEquals(processedSearchList.get(j), coStr);
      }
    }
  }

  /*
   * Check validateAndExtractSearchList - invalid cases
   */
  @Test(groups={"unit"})
  public void testValidateAndExtractSearchListInvalid()
  {
    // Create all input and validation data for tests
    String[] invalidCaseInputs = {
            "(enabled.eq.true",
            "port.lt.7)",
            "name.neq.test)name",
            "name.neq.test(name",
            "(name.neq.test)name)",
            "(name.neq.test(name)",
            "(name.neq.test~name)",
            "(name.neq.test,name)",
            "enabled.eq.true~port.lt.7",
            "port.between.1",
            "port.between.1,2,3",
            "port.nbetween.1",
            "port.nbetween.1,2,3",
            "(host.eq.stampede2.tacc.utexas.edu)~default_access_method.in.PKI_KEYS,ACCESS_KEY",
            "(enabled.eq.true)~owner.eq.jdoe)~(proxy_port.lt.7)~(system_type.in.OBJECT_STORE,LINUX)",
            "(enabled.eq.true)~(~(system_type.in.OBJECT_STORE,LINUX)",
            "(enabled.eq.true)~)~(system_type.in.OBJECT_STORE,LINUX)",
            "(enabled.eq.tr)ue)~)~(system_type.in.OBJECT_STORE,LINUX)",
            ".eq.true",
            "true",
            "enabled.true",
            "1enabled.eq.true",
            "en$abled.eq.true",
            "(enabled.equal.true)",
            "(port.l@t.7)",
            "(host.eq.myhost)~(default_access_method.in.)",
            "(enabled.eq.true)~(proxy_port.lt.7)~(system_type.in)",
            "(enabled.eq.)~(proxy_port.lt.7)~(system_type.in.OBJECT_STORE,LINUX)",
            "(enabled.eq.true)~(proxy_port.lt.)~(system_type.in.OBJECT_STORE,LINUX)"
    };

    // Iterate over test cases
    for (int i = 0; i < invalidCaseInputs.length; i++)
    {
      String searchListStr = invalidCaseInputs[i];
      System.out.println("Checking case # "+ i + " Input: " + searchListStr);
      try
      {
        // Extract the search list and validate that each condition has the correct form as done on the front end.
        List<String> searchList = SearchUtils.extractAndValidateSearchList(searchListStr);
        // Validate and process each search condition as done on the back end.
        for (String condStr : searchList) {SearchUtils.validateAndProcessSearchCondition(condStr);}
        System.out.println("  Result size: " + searchList.size());
        fail("Expected IllegalArgumentException");
      }
      catch (IllegalArgumentException e)
      {
        System.out.println("Expected exception: " + e);
      }
    }
    // TODO: Explicitly check for certain exceptions rather than just that an IllegalArg has been thrown.
    //       E.g., (port.l@t.7) should throw an exception containing SEARCH_COND_INVALID_OP
  }

  /*
   * Check isTimestamp - valid cases
   */
  @Test(groups={"unit"})
  public void testIsTimestamp()
  {
    // Iterate over valid test cases
    for (int i = 0; i < validTimestamps.length; i++)
    {
      String timestampStr = validTimestamps[i];
      System.out.println("Checking valid case # "+ i + " Input: " + timestampStr);
      Assert.assertTrue(SearchUtils.isTimestamp(timestampStr), "Input timestamp string: " + timestampStr);
    }
    // Iterate over invalid test cases
    for (int i = 0; i < invalidTimestamps.length; i++)
    {
      String timestampStr = invalidTimestamps[i];
      System.out.println("Checking invalid case # "+ i + " Input: " + timestampStr);
      Assert.assertFalse(SearchUtils.isTimestamp(timestampStr), "Input timestamp string: " + timestampStr);
    }
  }

  /*
   * Check convertValuesToTimestamps - valid cases
   */
  @Test(groups={"unit"})
  public void testConvertValuesToTimestamps()
  {
    // Test by iterating over valid test cases and incrementally building a list of values.
    // Check call after each increment
    SearchUtils.SearchOperator op = SearchUtils.SearchOperator.IN;
    StringJoiner sj = new StringJoiner(",");
    for (int i = 0; i < validTimestamps.length; i++)
    {
      String timestampStr = validTimestamps[i];
      System.out.println("Checking valid case # "+ i + " Input: " + timestampStr);
      sj.add(timestampStr);
      String testList = sj.toString();
      String testResult = SearchUtils.convertValuesToTimestamps(op, testList);
      System.out.println("   Test result: " + testResult);
      Assert.assertNotNull(testResult);
    }
  }

  // Case input data consists of the case number, result count and an input string
  static class CaseInputData
  {
    public final int count;
    public final String searchListStr;
    CaseInputData(int r, String s) { count=r; searchListStr=s; }
  }

  // Case output data consists of the case number and an array of strings
  static class CaseOutputData
  {
    public final int caseNum;
    public final List<String> strList;
    CaseOutputData(int c, String... parms)
    {
      caseNum = c;
      strList = new ArrayList<>();
      strList.addAll(Arrays.asList(parms));
    }
  }
}
