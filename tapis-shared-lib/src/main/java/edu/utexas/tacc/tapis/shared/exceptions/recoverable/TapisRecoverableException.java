package edu.utexas.tacc.tapis.shared.exceptions.recoverable;

import java.util.TreeMap;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;

public abstract class TapisRecoverableException
extends TapisException
{
    private static final long serialVersionUID = 8857182330433990489L;
    
    public TreeMap<String,String> state = new TreeMap<String,String>();
    
    public TapisRecoverableException(String message, TreeMap<String,String> state) 
    {
        super(message);
        if (state != null) this.state = state;
    }
    
    public TapisRecoverableException(String message, Throwable cause, TreeMap<String,String> state) 
    {
        super(message, cause);
        if (state != null) this.state = state;
    }

}
