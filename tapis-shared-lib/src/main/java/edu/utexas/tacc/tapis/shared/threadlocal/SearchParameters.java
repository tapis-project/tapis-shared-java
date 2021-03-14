package edu.utexas.tacc.tapis.shared.threadlocal;

import java.util.List;

/**
 * Parameters related to search, sort and filter
 *    search - String indicating search conditions to use when retrieving results
 *    limit - Integer indicating maximum number of results to be included, -1 for unlimited
 *    orderBy - e.g. orderBy=owner(asc), orderBy=created(desc)
 *    skip - number of results to skip
 *    startAfter - e.g. systems?limit=10&orderBy=id(asc)&startAfter=101
 *    computeTotal - Boolean indicating if total count should be computed. Default is false.
 *    filter - String indicating which attributes (i.e. fields) to include when retrieving results
 */
public final class SearchParameters
{
  // ************************************************************************
  // *********************** Constants **************************************
  // ************************************************************************
  public static final String ORDERBY_DIRECTION_ASC = "ASC";
  public static final String ORDERBY_DIRECTION_DESC = "DESC";

  public static final int DEFAULT_LIMIT = -1;
  public static final String DEFAULT_ORDERBY = "";
  public static final String DEFAULT_ORDERBY_DIRECTION = ORDERBY_DIRECTION_ASC;
  public static final int DEFAULT_SKIP = -1;
  public static final String DEFAULT_STARTAFTER = "";
  public static final boolean DEFAULT_COMPUTETOTAL = false;

  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************
  private boolean computeTotal = DEFAULT_COMPUTETOTAL;
  private List<String> filterList;
  private List<String> searchList;
  private int limit = DEFAULT_LIMIT;
  private String orderBy = DEFAULT_ORDERBY;
  private String orderByDirection = DEFAULT_ORDERBY_DIRECTION;
  private int skip = DEFAULT_SKIP;
  private String startAfter = DEFAULT_STARTAFTER;

  // ************************************************************************
  // *********************** Accessors **************************************
  // ************************************************************************
  public boolean getComputeTotal() { return computeTotal; }
  public void setComputeTotal(boolean b) { computeTotal = b; }
  public List<String> getFilterList() { return filterList; }
  public void setFilterList(List<String> fl) { filterList = fl; }
  public List<String> getSearchList() { return searchList; }
  public void setSearchList(List<String> sl) { searchList = sl; }
  public int getLimit() { return limit; }
  public void setLimit(int i) { limit = i; }
  public String getOrderBy() { return orderBy; }
  public void setOrderBy(String s) { orderBy = s; }
  public String getOrderByDirection() { return orderByDirection; }
  public void setOrderByDirection(String s) { orderByDirection = s; }
  public int getSkip() { return skip; }
  public void setSkip(int i) { skip = i; }
  public String getStartAfter() { return startAfter; }
  public void setStartAfter(String s) { startAfter = s; }
}
