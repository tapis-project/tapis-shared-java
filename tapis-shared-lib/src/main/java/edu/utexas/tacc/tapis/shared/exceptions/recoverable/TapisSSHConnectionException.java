package edu.utexas.tacc.tapis.shared.exceptions.recoverable;

import java.io.Serial;
import java.util.TreeMap;

public class TapisSSHConnectionException 
 extends TapisRecoverableException 
{
    @Serial
	private static final long serialVersionUID = 6143550896805006147L;
    
	public TapisSSHConnectionException(String message, Throwable cause, TreeMap<String,String> state) 
	{
	    super(message, cause, state);
	}
}
