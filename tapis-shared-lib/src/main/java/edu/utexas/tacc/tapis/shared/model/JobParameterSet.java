package edu.utexas.tacc.tapis.shared.model;

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
    private List<KeyValueString> envVariables;
    private IncludeExcludeFilter archiveFilter;
    
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
    public List<KeyValueString> getEnvVariables() {
        return envVariables;
    }
    public void setEnvVariables(List<KeyValueString> envVariables) {
        this.envVariables = envVariables;
    }
    public IncludeExcludeFilter getArchiveFilter() {
        return archiveFilter;
    }
    public void setArchiveFilter(IncludeExcludeFilter archiveFilter) {
        this.archiveFilter = archiveFilter;
    }
}
