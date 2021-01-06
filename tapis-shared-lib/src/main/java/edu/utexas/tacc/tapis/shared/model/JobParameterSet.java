package edu.utexas.tacc.tapis.shared.model;

import java.util.ArrayList;
import java.util.List;

/** This is the java model for the parameterSet JSON value defined in 
 * SubmitJobRequest.json. 
 * 
 * @author rcardone
 */
public class JobParameterSet 
{
    private List<ArgSpec>        appArgs;
    private List<ArgSpec>        containerArgs;
    private List<ArgSpec>        schedulerOptions;
    private List<KeyValuePair>   envVariables;
    private IncludeExcludeFilter archiveFilter;
    
    
    // A simple way to make sure all lists and other fields are non-null.
    public void initAll()
    {
        // Don't stomp on existing data.
        if (appArgs == null) appArgs = new ArrayList<ArgSpec>();
        if (containerArgs == null) containerArgs = new ArrayList<ArgSpec>();
        if (schedulerOptions == null) schedulerOptions = new ArrayList<ArgSpec>();
        if (envVariables == null) envVariables = new ArrayList<KeyValuePair>();
        if (archiveFilter == null) archiveFilter = new IncludeExcludeFilter();
        archiveFilter.initAll();
    }
    
    public List<ArgSpec> getAppArgs() {
        return appArgs;
    }
    public void setAppArgs(List<ArgSpec> appArgs) {
        this.appArgs = appArgs;
    }
    public List<ArgSpec> getContainerArgs() {
        return containerArgs;
    }
    public void setContainerArgs(List<ArgSpec> containerArgs) {
        this.containerArgs = containerArgs;
    }
    public List<ArgSpec> getSchedulerOptions() {
        return schedulerOptions;
    }
    public void setSchedulerOptions(List<ArgSpec> schedulerOptions) {
        this.schedulerOptions = schedulerOptions;
    }
    public List<KeyValuePair> getEnvVariables() {
        return envVariables;
    }
    public void setEnvVariables(List<KeyValuePair> envVariables) {
        this.envVariables = envVariables;
    }
    public IncludeExcludeFilter getArchiveFilter() {
        return archiveFilter;
    }
    public void setArchiveFilter(IncludeExcludeFilter archiveFilter) {
        this.archiveFilter = archiveFilter;
    }
}
