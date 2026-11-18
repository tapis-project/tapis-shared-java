package edu.utexas.tacc.tapis.shared.exceptions.recoverable;

import java.io.Serial;
import java.util.TreeMap;

public class TapisSSHUnresolvedException extends TapisRecoverableException
{
	@Serial
	private static final long serialVersionUID = 1L;

	public TapisSSHUnresolvedException(String message, Throwable cause, TreeMap<String,String> state)
	{
	    super(message, cause, state);
	}
}
