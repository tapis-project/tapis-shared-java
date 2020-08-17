package edu.utexas.tacc.tapis.search;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;


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


  @BeforeMethod
  public void setUp()
  {
  }

  @AfterMethod
  public void tearDown()
  {
  }

  /*
   * Check valid cases
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
      List<String> validSearchList = SearchUtils.validateAndExtractSearchList(ci.searchListStr);
      System.out.println("  Result size: " + validSearchList.size());
      assertEquals(validSearchList.size(), ci.count);
      for (int j = 0; j < ci.count; j++)
      {
        System.out.println("  Result string # " + j + " = " + validSearchList.get(j));
        String coStr = validCaseOutputs.get(caseNum).strList.get(j);
        assertEquals(validSearchList.get(j), coStr);
      }
    }
  }

  /*
   * Check invalid cases
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
        List<String> searchResults = SearchUtils.validateAndExtractSearchList(searchListStr);
        System.out.println("  Result size: " + searchResults.size());
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