package edu.utexas.tacc.tapis.shared.threadlocal;

import java.util.ArrayList;
import java.util.List;

/**
 * Parameters related to search, sort and filter
 *    search - String indicating search conditions to use when retrieving results
 *    limit - Integer indicating maximum number of results to be included, -1 for unlimited
 *    orderBy - e.g. orderBy=owner(asc), orderBy=created(desc), orderBy=name,created(desc)
 *    orderByList - List of orderBy entries, attr+direction
 *    skip - number of results to skip
 *    startAfter - e.g. systems?limit=10&orderBy=id(asc)&startAfter=101
 *    computeTotal - Boolean indicating if total count should be computed. Default is false.
 *    select - String indicating which attributes (i.e. fields) to include when retrieving results
 */
public final class SearchParameters
{
  // ************************************************************************
  // *********************** Constants **************************************
  // ************************************************************************
  public static final int DEFAULT_LIMIT = 100;
  public static final int DEFAULT_SKIP = 0;
  public static final boolean DEFAULT_COMPUTETOTAL = false;
  public static final String DEFAULT_SELECT_ALL = "allAttributes";
  public static final String DEFAULT_SELECT_SUMMARY = "summaryAttributes";

  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************
  private boolean computeTotal = DEFAULT_COMPUTETOTAL;
  private List<String> selectList = new ArrayList<>();
  private List<String> searchList = new ArrayList<>();
  private Integer limit = null;  // Set to null so users of this class can determine if value is set on incoming request.
  private String orderBy = null; // Maintain original query parameter for returning in response.
  private List<OrderBy> orderByList = new ArrayList<>();
  private Integer skip = DEFAULT_SKIP;
  private String startAfter = null;

  // ************************************************************************
  // *********************** Accessors **************************************
  // ************************************************************************
  public boolean getComputeTotal() { return computeTotal; }
  public void setComputeTotal(boolean b) { computeTotal = b; }
  public List<String> getSelectList() { return (selectList == null ? new ArrayList<>() : selectList); }
  public void setSelectList(List<String> fl) { selectList = fl; }
  public List<String> getSearchList() { return (searchList == null ? new ArrayList<>() : searchList); }
  public void setSearchList(List<String> sl) { searchList = sl; }
  public Integer getLimit() { return limit; }
  public void setLimit(Integer i) { limit = i; }
  public String getOrderBy() { return orderBy; }
  public void setOrderBy(String s) { orderBy = s; }
  public List<OrderBy> getOrderByList() { return (orderByList == null ? new ArrayList<>() : orderByList); }
  public void setOrderByList(List<OrderBy> ol) { orderByList = ol; }
  public Integer getSkip() { return skip; }
  public void setSkip(Integer i) { skip = i; }
  public String getStartAfter() { return startAfter; }
  public void setStartAfter(String s) { startAfter = s; }
}
