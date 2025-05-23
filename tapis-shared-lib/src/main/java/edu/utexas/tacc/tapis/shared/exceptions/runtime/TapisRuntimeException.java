package edu.utexas.tacc.tapis.shared.exceptions.runtime;

import java.io.Serial;

public class TapisRuntimeException extends RuntimeException
{
	@Serial
	private static final long serialVersionUID = -6574079815352309369L;

	public TapisRuntimeException(Throwable cause) {super(cause);}
	public TapisRuntimeException(String message) {super(message);}
	public TapisRuntimeException(String message, Throwable cause) {super(message, cause);}
}
