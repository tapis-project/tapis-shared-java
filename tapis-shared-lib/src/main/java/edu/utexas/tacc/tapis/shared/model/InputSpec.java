package edu.utexas.tacc.tapis.shared.model;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;

public class InputSpec 
{
    private String      sourceUrl;
    private String      targetPath;
    private Boolean     inPlace;
    private ArgMetaSpec meta;
    
    /** Return the last component in a path, ignoring any trailing
     * slashes that might be stuck on the end of the path.  This 
     * method returns null if only an empty or null path is found. 
     * 
     * @return the segment after the last internal slash or null.
     * @throws URISyntaxException 
     * @throws MalformedURLException 
     */
    public String generateTargetPath() throws URISyntaxException, MalformedURLException
    {
        // Ignore all trailing slashes.
        if (StringUtils.isBlank(sourceUrl)) return null;
        
        // Get everything after the protocol.
        int index1 = sourceUrl.indexOf("://");
        String s;
        if (index1 < 0) s = sourceUrl;
        else if (index1 + 4 >= sourceUrl.length()) return null;
        else s = sourceUrl.substring(index1 + 4);
        
        // Ignore all trailing slashes.
        while (s.endsWith("/")) s = StringUtils.removeEnd(sourceUrl, "/");
        if (s.isEmpty()) return null;
        int index2 = s.lastIndexOf('/');
        if (index2 < 0) return s;
        if (index2 + 1 >= s.length()) return null;
        return s.substring(index2 + 1);
    }
    
    
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
