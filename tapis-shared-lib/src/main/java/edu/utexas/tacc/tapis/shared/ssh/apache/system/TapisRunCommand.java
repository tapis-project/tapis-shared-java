package edu.utexas.tacc.tapis.shared.ssh.apache.system;

import java.io.ByteArrayOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.exceptions.runtime.TapisRuntimeException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHConnection;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHExecChannel;
import edu.utexas.tacc.tapis.systems.client.gen.model.TapisSystem;

public class TapisRunCommand
 extends TapisAbstractConnection
{
    /* **************************************************************************** */
    /*                                   Constants                                  */
    /* **************************************************************************** */
    // Local logger.
    private static final Logger _log = LoggerFactory.getLogger(TapisRunCommand.class);
    
    // Initial exitCode value.
    public static final int INITIAL_EXITCODE = -1; 
    
    // Compile time flag for detailed debug output
    private static final boolean DEBUG = false;
    
    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    // Assigned on execute calls.
    private String _command;
    private ByteArrayOutputStream _out;
    private ByteArrayOutputStream _err;
    private int _exitCode = INITIAL_EXITCODE;
    
    /* **************************************************************************** */
    /*                                Constructors                                  */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* constructor:                                                                 */
    /* ---------------------------------------------------------------------------- */
    public TapisRunCommand(TapisSystem system) {super(system);}

    /* ---------------------------------------------------------------------------- */
    /* constructor:                                                                 */
    /* ---------------------------------------------------------------------------- */
    public TapisRunCommand(TapisSystem system, SSHConnection conn) {super(system, conn);}

    /* **************************************************************************** */
    /*                                Public Methods                                */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* execute:                                                                     */
    /* ---------------------------------------------------------------------------- */
    /** Execute the command using combined output and error streams and without closing
     * the connection afterwards.  
     *
     * @param command the command to execute on the remote system
     * @return the exit code of the remote command
     * @throws TapisException on error
     */
    public int execute(String command) throws TapisException
    {
        return execute(command, false, false);
    }

    /* ---------------------------------------------------------------------------- */
    /* execute:                                                                     */
    /* ---------------------------------------------------------------------------- */
    /** Execute the command using combined output and error streams.  
     *
     * @param command the command to execute on the remote system
     * @param closeConnection true to close the connection, false to leave open
     * @return the exit code of the remote command
     * @throws TapisException on error
     */
    public int execute(String command, boolean closeConnection) throws TapisException
    {
        return execute(command, closeConnection, false);
    }

    /* ---------------------------------------------------------------------------- */
    /* execute:                                                                     */
    /* ---------------------------------------------------------------------------- */
    /** Execute the command and specify connection closing semantics and output handling.
     * Each invocation of this methods creates a new channel and then destroys it.
     * 
     * If closeConnection is true, then the connection to the remote host will be 
     * closed after the command executes.  Otherwise, the connection will be left open.  
     * 
     * If separateStreams is true, then the standard out and standard error from the 
     * remote command are captured in separate streams.  Otherwise, the remote 
     * command's standard out and error are written to the same stream.  
     * 
     * Each invocation of this methods creates a new channel and then destroys it.
     * This method is not threadsafe, only a single thread at a time should access
     * instances of this class.  Different threads, however, can simultaneously use 
     * different instances of this class.
     * 
     * Instances of this class can be reused by simply calling the execute method
     * again.  All execute results are available until the next execute call is issued.
     *
     * @param command the command to execute on the target system
     * @param closeConnection true to close the connection, false to leave open
     * @param separateStreams true to have separate output and error streams,
     *                        false to combine output and error streams
     * @return stdout output from command execution
     * @throws TapisException on error
     */
    public int execute(String command, boolean closeConnection, boolean separateStreams)
     throws TapisException
    {
        // We need something to run.
        if (StringUtils.isBlank(command)) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "execute", "command");
            throw new TapisRuntimeException(msg);
        }
        
        // Reset the field values to their original state.
        resetFields(command);

        // Connect to the system unless an exception is thrown.
        if (DEBUG) _log.debug(String.format("**** Running command: %s", command));
        SSHConnection conn = getConnection();
        
        // Assign the output buffers.
        _out = new ByteArrayOutputStream(SSHExecChannel.DEFAULT_OUTPUT_LEN);
        if (separateStreams) 
            _err = new ByteArrayOutputStream(SSHExecChannel.DEFAULT_OUTPUT_LEN);
          else _err = _out;
        
        // Issue the command.
        var channel = conn.getExecChannel();
        try {_exitCode = channel.execute(command, _out, _err);}
            catch (TapisException e) {throw e;}
            catch (Exception e) {
                String msg = MsgUtils.getMsg("TAPIS_SSH_EXEC_CMD_ERROR", getSystemHostMessage(),
                                             conn.getUsername(), e.getMessage());
                throw new TapisException(msg, e);
            }
            finally {
                // Always close the connection if so requested.
                if (closeConnection) conn.close();
            }
        
        return _exitCode;
    }

    /* ---------------------------------------------------------------------------- */
    /* getOutAsString:                                                              */
    /* ---------------------------------------------------------------------------- */
    /** Return the output stream contents as a string.  If this method is called before
     * the execute method sends a command to the remote host, null is returned.
     * Otherwise, the value returned is a string of zero or more length created using
     * the default character set from the output stream contents.
     * 
     * @return the output stream content
     */
    public String getOutAsString() 
    {
        if (_out == null) return null;
        return new String(_out.toByteArray());
    }

    /* ---------------------------------------------------------------------------- */
    /* getErrAsString:                                                              */
    /* ---------------------------------------------------------------------------- */
    /** Return the error stream contents as a string.  If this method is called before
     * the execute method sends a command to the remote host, null is returned.
     * Otherwise, the value returned is a string of zero or more length created using
     * the default character set from the error stream contents.
     * 
     * By default, the output and error streams are combined into a single stream
     * unless explicitly specified as separate on the execute() call.
     * 
     * @return the error stream content
     */
    public String getErrAsString() 
    {
        if (_err == null) return null;
        return new String(_err.toByteArray());
    }
    
    /* **************************************************************************** */
    /*                                  Accessors                                   */
    /* **************************************************************************** */
    public String getCommand() {return _command;}
    public ByteArrayOutputStream getOut() {return _out;}
    public ByteArrayOutputStream getErr() {return _err;}
    public int getExitCode() {return _exitCode;}

    /* **************************************************************************** */
    /*                               Private Methods                                */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* resetFields:                                                                 */
    /* ---------------------------------------------------------------------------- */
    /** Reset the fields to their original values so that this object can be reused.
     */
     private void resetFields(String command)
    {
         _command = command;
         _out = null;
         _err = null;
         _exitCode = INITIAL_EXITCODE;
    }
}
