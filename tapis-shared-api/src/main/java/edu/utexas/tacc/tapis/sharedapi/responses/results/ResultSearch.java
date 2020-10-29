package edu.utexas.tacc.tapis.sharedapi.responses.results;

import java.util.List;

/*
   Class representing results returned when retrieving resources.
 */
public final class ResultSearch
{
  public ResultMetadata metadata; // Metadata about results. recordCount, recordLimit, etc.
  public List search; // Actual search results
}
