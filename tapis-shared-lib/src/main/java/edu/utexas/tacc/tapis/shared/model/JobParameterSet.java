package edu.utexas.tacc.tapis.shared.model;

import java.util.List;

/** This is the java model for the parameterSet JSON value defined in 
 * SubmitJobRequest.json. 
 * 
 * @author rcardone
 */
public class JobParameterSet 
{
    public List<ArgSpec>        appArgs;
    public List<ArgSpec>        containerArgs;
    public List<ArgSpec>        schedulerOptions;
    public List<KeyValueString> envVariables;
    public IncludeExcludeFilter archiveFilter;
}
