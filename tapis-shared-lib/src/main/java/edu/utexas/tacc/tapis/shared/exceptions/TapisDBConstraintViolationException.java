package edu.utexas.tacc.tapis.shared.exceptions;

/**
 * This exception is thrown when a database constraint violation is detected.
 * The constraintName parameter should be the name of the constraint that was violated.
 *
 * The reason why this exception is a sub-class of TapisJDBCException is that,
 * when there is a need to capture the constraint name, we can catch an exception of this type,
 * but if no one cares about what db constraint really is, we can simply catch TapisJDBCException.
 *
 * @author wei.zhang@tacc.utexas.edu
 */
public class TapisDBConstraintViolationException extends TapisJDBCException {
    private static final long serialVersionUID = -3467261565783040179L;

    private String constraintName;

    public TapisDBConstraintViolationException(String constraintName, String message) { 
        super(message); 
        this.constraintName = constraintName;
    }
    public TapisDBConstraintViolationException(String constraintName, String message, Throwable cause) { 
        super(message, cause); 
        this.constraintName = constraintName;
    }

    public TapisDBConstraintViolationException(String constraintName, Throwable cause) {
        super(cause.getMessage(), cause);
        this.constraintName = constraintName;
    }

    public String getConstraintName() {
        return constraintName;
    }

    public void setConstraintName(String constraintName) {
        this.constraintName = constraintName;
    }
    
}
