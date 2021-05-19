package edu.utexas.tacc.tapis.shared.exceptions.recoverable;

import java.io.Serial;
import java.util.TreeMap;

public class TapisSSHTimeoutException
 extends TapisRecoverableException
{
	@Serial
	private static final long serialVersionUID = 902348593245889434L;

	public TapisSSHTimeoutException(String message, Throwable cause, TreeMap<String,String> state)
	{
	    super(message, cause, state);
	}
}
