package edu.utexas.tacc.tapis.shared.exceptions.recoverable;

import java.util.TreeMap;

public class TapisSSHAuthException
 extends TapisRecoverableException
{
    private static final long serialVersionUID = 4067077570294103659L;

    public TapisSSHAuthException(String message, TreeMap<String,String> state)
	{
	    super(message, state);
	}
    public TapisSSHAuthException(String message, Throwable cause, TreeMap<String,String> state)
    {
        super(message, cause, state);
    }
}
