package edu.utexas.tacc.tapis.shared.threadlocal;

import java.util.ArrayList;
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

  public static final String DEFAULT_ORDERBY_DIRECTION = ORDERBY_DIRECTION_ASC;
  public static final int DEFAULT_LIMIT = 100;
  public static final int DEFAULT_SKIP = 0;
  public static final boolean DEFAULT_COMPUTETOTAL = false;

  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************
  private boolean computeTotal = DEFAULT_COMPUTETOTAL;
  private List<String> filterList = new ArrayList<>();
  private List<String> searchList = new ArrayList<>();
  private Integer limit = null;  // Set to null so users of this class can determine if value is set on incoming request.
  private List<String> orderByAttrList = new ArrayList<>();
  private List<String> orderByDirList = new ArrayList<>();
  private Integer skip = DEFAULT_SKIP;
  private String startAfter = null;

  // ************************************************************************
  // *********************** Accessors **************************************
  // ************************************************************************
  public boolean getComputeTotal() { return computeTotal; }
  public void setComputeTotal(boolean b) { computeTotal = b; }
  public List<String> getFilterList() { return (filterList == null ? new ArrayList<>() : filterList); }
  public void setFilterList(List<String> fl) { filterList = fl; }
  public List<String> getSearchList() { return (searchList == null ? new ArrayList<>() : searchList); }
  public void setSearchList(List<String> sl) { searchList = sl; }
  public Integer getLimit() { return limit; }
  public void setLimit(Integer i) { limit = i; }
  public List<String> getOrderByAttrList() { return (orderByAttrList == null ? new ArrayList<>() : orderByAttrList); }
  public void setOrderByAttrList(List<String> sl) { orderByAttrList = sl; }
  public List<String> getOrderByDirList() { return (orderByDirList == null ? new ArrayList<>() : orderByDirList); }
  public void setOrderByDirList(List<String> sl) { orderByDirList = sl; }
  public Integer getSkip() { return skip; }
  public void setSkip(Integer i) { skip = i; }
  public String getStartAfter() { return startAfter; }
  public void setStartAfter(String s) { startAfter = s; }
}
