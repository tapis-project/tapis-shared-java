package edu.utexas.tacc.tapis.shared.model;

public class InputSpec 
{
    private String      sourceUrl;
    private String      targetPath;
    private Boolean     inPlace;
    private ArgMetaSpec meta;
    
    
    public String getSourceUrl() {
        return sourceUrl;
    }
    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
    public String getTargetPath() {
        return targetPath;
    }
    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }
    public Boolean getInPlace() {
        return inPlace;
    }
    public void setInPlace(Boolean inPlace) {
        this.inPlace = inPlace;
    }
    public ArgMetaSpec getMeta() {
        return meta;
    }
    public void setMeta(ArgMetaSpec meta) {
        this.meta = meta;
    }
}
