package edu.utexas.tacc.tapis.shared.exceptions.recoverable;

import java.util.TreeMap;

public class TapisServiceConnectionException 
 extends TapisRecoverableException 
{
    private static final long serialVersionUID = 7813166268519660343L;

    public TapisServiceConnectionException(String message, Throwable cause, TreeMap<String,String> state) 
	{
	    super(message, cause, state);
	}
}
