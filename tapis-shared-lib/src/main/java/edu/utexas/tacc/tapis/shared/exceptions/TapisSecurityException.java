package edu.utexas.tacc.tapis.shared.exceptions;

import java.io.Serial;

public class TapisSecurityException extends TapisException
{
  @Serial
  private static final long serialVersionUID = -1308604776352625945L;

  public TapisSecurityException(Throwable cause) {super(cause);}
  public TapisSecurityException(String message) {super(message);}
	public TapisSecurityException(String message, Throwable cause) {super(message, cause);}
}
