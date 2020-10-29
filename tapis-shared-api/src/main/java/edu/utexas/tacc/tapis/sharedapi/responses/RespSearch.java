package edu.utexas.tacc.tapis.sharedapi.responses;

import edu.utexas.tacc.tapis.sharedapi.responses.results.ResultSearch;

/*
  Class representing a response containing results returned when retrieving resources.
 */
public class RespSearch extends RespAbstract
{
  public ResultSearch result;

  // Zero arg constructor needed to use jersey's SelectableEntityFilteringFeature
  public RespSearch() {}

  public RespSearch(ResultSearch tmpResult)
  {
    result = tmpResult;
  }
}
