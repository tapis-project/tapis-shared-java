package edu.utexas.tacc.tapis.shared.model;

import java.util.ArrayList;
import java.util.List;

public class IncludeExcludeFilter 
{
    private List<String> includes;
    private List<String> excludes;
    
    // A simple way to make sure all fields are non-null.
    public void initAll()
    {
        if (includes == null) includes = new ArrayList<String>();
        if (excludes == null) excludes = new ArrayList<String>();
    }
    
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
