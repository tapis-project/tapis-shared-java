package edu.utexas.tacc.tapis.sharedapi.responses.results;

/*
 Class representing metadata about results returned when retrieving resources.
 */
public final class ResultMetadata
{
  public int recordCount; // Number of records actually returned
  public int recordLimit; // Requested limit on number of records to be returned, -1 for unlimited. Default is unlimited.
  public int recordsSkipped; // Requested number of records to be skipped. Use one of skip or startAfter. Default is 0.
  public String sortBy; // Attribute used for sorting.
  public String startAfter; // Where to start when sorting. Use one of skip or startAfter.
  public int totalCount; // Number records that would have been returned if unlimited
}
