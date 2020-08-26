package edu.utexas.tacc.tapis.search;

import org.apache.activemq.selector.SelectorParser;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 *  Basic tests of the generated SqlParser.
 * 
 * @author scblack
 */
@Test(groups= {"unit"})
public class SqlParserTest
{

  public static final String sysNamePrefix = "TestSys";
  public static final String testSuiteNameKey = "SrchEndpoint";

  /*
   * Check SqlParser.parse() - valid cases
   */
  @Test(groups = {"unit"})
  public void testSqlParseValid() throws Exception
  {
    // Create all input and validation data for tests
    String sys1Name = sysNamePrefix + "_" + testSuiteNameKey + "_" + String.format("%03d", 1);
    // Inputs
    var validCaseInputs = new HashMap<Integer, CaseInputData>();
    validCaseInputs.put(  1,new CaseInputData(0, "name = '" + sys1Name + "'"));
    validCaseInputs.put(  2,new CaseInputData(0, "enabled = 'true'"));
    validCaseInputs.put(  3,new CaseInputData(0, "enabled <> 'true'"));
    validCaseInputs.put(  4,new CaseInputData(0, "port < 7"));
    validCaseInputs.put(  5,new CaseInputData(0, "(port <= 7)"));
    validCaseInputs.put(  6,new CaseInputData(0, "(port > 7)"));
    validCaseInputs.put(  7,new CaseInputData(0, "(port >= 7)"));
    validCaseInputs.put(  8,new CaseInputData(0, "name LIKE 'testname%'"));
    validCaseInputs.put(  9,new CaseInputData(0, "name NOT LIKE 'testname%'"));
    validCaseInputs.put( 10,new CaseInputData(0, "port BETWEEN '0' AND '1024'"));
    validCaseInputs.put( 11,new CaseInputData(0, "port NOT BETWEEN '0' AND '1024'"));
    validCaseInputs.put( 12,new CaseInputData(0, "owner IN ('jdoe', 'msmith')"));
    validCaseInputs.put( 13,new CaseInputData(0, "owner NOT IN ('jdoe', 'msmith')"));
    validCaseInputs.put( 20,new CaseInputData(0, "owner IN ('jdoe')"));
    validCaseInputs.put( 21,new CaseInputData(0, "owner IN ('a','b','c')"));
    validCaseInputs.put( 22,new CaseInputData(0, "owner IN ('jdoe','msmith','jsmith','mdoe','a','b','c')"));
    validCaseInputs.put(101,new CaseInputData(0, "host = 'stampede2.tacc.utexas.edu' AND owner = 'jdoe'"));
    validCaseInputs.put(102,new CaseInputData(0, "host = 'stampede2.tacc.utexas.edu' OR owner = 'jdoe'"));
    validCaseInputs.put(103,new CaseInputData(0, "enabled = 'true' AND (owner = 'jdoe' OR proxy_port > 1024)"));

    // Iterate over test cases
    for (Map.Entry<Integer, CaseInputData> item : validCaseInputs.entrySet())
    {
      CaseInputData ci = item.getValue();
      int caseNum = item.getKey();
      System.out.println("Checking case # " + caseNum + " Input: " + ci.sqlStr);
//      BooleanExpression ast = SelectorParser.parse(ci.sqlStr);
      TNode ast = SqlParser.parse(ci.sqlStr);
      System.out.println("  ******* AST = " + ast.toString());
    }
  }

  /**
   * Case input data consists of the case number, result count and an input string
   */
  static class CaseInputData
  {
    public final int count; // TODO/TBD: Number of resulting nodes in the AST?
    public final String sqlStr;
    CaseInputData(int r, String s) { count=r; sqlStr =s; }
  }
}
