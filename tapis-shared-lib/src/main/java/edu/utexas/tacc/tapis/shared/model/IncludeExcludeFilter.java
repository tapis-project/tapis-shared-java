package edu.utexas.tacc.tapis.shared.model;

import java.util.List;

public class IncludeExcludeFilter 
{
    private List<String> includes;
    private List<String> excludes;
    
    public List<String> getIncludes() {
        return includes;
    }
    public void setIncludes(List<String> includes) {
        this.includes = includes;
    }
    public List<String> getExcludes() {
        return excludes;
    }
    public void setExcludes(List<String> excludes) {
        this.excludes = excludes;
    }
}
