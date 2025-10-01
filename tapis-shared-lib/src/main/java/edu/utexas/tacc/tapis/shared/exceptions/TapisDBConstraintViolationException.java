package edu.utexas.tacc.tapis.shared.exceptions;

public class TapisDBConstraintViolationException extends Exception {
    private static final long serialVersionUID = 1L;

    private String constraintName;

    public TapisDBConstraintViolationException() { super(); }

    public TapisDBConstraintViolationException(String constraintName, String message) { 
        super(message); 
        this.constraintName = constraintName;
    }
    public TapisDBConstraintViolationException(String constraintName, String message, Throwable cause) { 
        super(message, cause); 
        this.constraintName = constraintName;
    }

    public TapisDBConstraintViolationException(String constraintName, Throwable cause) { 
        super(cause); 
        this.constraintName = constraintName;
    }

    public String getConstraintName() {
        return constraintName;
    }

    public void setConstraintName(String constraintName) {
        this.constraintName = constraintName;
    }
    
}
