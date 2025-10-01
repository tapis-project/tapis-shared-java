package edu.utexas.tacc.tapis.shared.db;

import org.postgresql.util.PSQLException;

import edu.utexas.tacc.tapis.shared.exceptions.TapisDBConstraintViolationException;

/** Utility class for handling PostgreSQL exceptions */
public class PostgresExceptionHandler {
    /**
     * Check if the given PSQLException is a constraint violation.
     * If it is, throw a TapisDBConstraintViolationException with the constraint name.
     * 
     * @param ex The PSQLException to check
     * @throws TapisDBConstraintViolationException if the exception is a constraint violation
     */
    public static void checkDBConstraintViolation(PSQLException ex) throws TapisDBConstraintViolationException {
        // According to  https://www.postgresql.org/docs/16/errcodes-appendix.html , all the constraint violation errors starts with "23".
        if (ex.getSQLState().startsWith("23")) {
            String constraintName = null;
            if (ex.getServerErrorMessage() != null) {
                constraintName = ex.getServerErrorMessage().getConstraint();
            }
            throw new TapisDBConstraintViolationException(constraintName, ex);
        }
    }
}
