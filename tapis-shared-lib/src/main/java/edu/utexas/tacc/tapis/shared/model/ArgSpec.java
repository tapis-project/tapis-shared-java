package edu.utexas.tacc.tapis.shared.model;

/** This is the java model for the ArgSpec JSON value defined in 
 * SubmitJobRequest.json. 
 * 
 * @author rcardone
 */
public class ArgSpec 
{
    private String      arg;
    private ArgMetaSpec meta;
    
    public String getArg() {
        return arg;
    }
    public void setArg(String arg) {
        this.arg = arg;
    }
    public ArgMetaSpec getMeta() {
        return meta;
    }
    public void setMeta(ArgMetaSpec meta) {
        this.meta = meta;
    }
}
