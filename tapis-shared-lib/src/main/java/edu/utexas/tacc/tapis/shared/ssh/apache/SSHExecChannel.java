package edu.utexas.tacc.tapis.shared.ssh.apache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumSet;

import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.exceptions.runtime.TapisRuntimeException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;

/** The execute methods in this class acquire a new channel each time they are
 * invoked.  The exit code of the remote command is returned.  If an output or
 * error stream is provided, the remote command's output will be available in
 * those streams.  
 * 
 * No Apache data types are exposed on this interface.
 * 
 * @author rcardone
 */
public class SSHExecChannel 
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    public  static final int DEFAULT_OUTPUT_LEN = 1024;
    private static final EnumSet<ClientChannelEvent> _closedSet = EnumSet.of(ClientChannelEvent.CLOSED);
    
    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    // The parent connection.
    private final SSHConnection _sshConnection; 
    
    /* ********************************************************************** */
    /*                            Constructors                                */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    SSHExecChannel(SSHConnection sshConnection)
    {
        // Check input.
        if (sshConnection == null) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "SSHExecChannel", "sshConnection");
            throw new TapisRuntimeException(msg);
        }
        _sshConnection = sshConnection;
    }
    
    /* ********************************************************************** */
    /*                            Public Methods                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* execute:                                                               */
    /* ---------------------------------------------------------------------- */
    /** Execute a remote command in which the output is not required.
     * 
     * @param cmd the command to execute on the remote host
     * @return the remote command's exit code
     * @throws IOException 
     * @throws TapisException
     */
    public int execute(String cmd) throws IOException, TapisException
    {
        return execute(cmd, new ByteArrayOutputStream(DEFAULT_OUTPUT_LEN));
    }
    
    /* ---------------------------------------------------------------------- */
    /* execute:                                                               */
    /* ---------------------------------------------------------------------- */
    /** Execute a remote command and return its standard out and standard err 
     * content in the stream.
     * 
     * @param cmd the command to execute on the remote host
     * @param outErrStream the stream containing standard out and standard error
     *                     of the remote command
     * @return the remote command's exit code
     * @throws IOException 
     * @throws TapisException
     */
    public int execute(String cmd, OutputStream outErrStream) 
      throws IOException, TapisException
    {
        return execute(cmd, outErrStream, outErrStream);
    }
    
    /* ---------------------------------------------------------------------- */
    /* execute:                                                               */
    /* ---------------------------------------------------------------------- */
    /** Execute a remote command and return its standard out and standard err 
     * content in their respective streams.
     * 
     * @param cmd the command to execute on the remote host
     * @param outStream the stream containing standard out of the remote command
     * @param errStream the stream containing standard err of the remote command
     * @return the remote command's exit code
     * @throws IOException 
     * @throws TapisException
     */
    public int execute(String cmd, OutputStream outStream, OutputStream errStream) 
     throws IOException, TapisException
    {
        // Check call-specific input.
        if (outStream == null) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "execute", "outStream");
            throw new IOException(msg);
        }
        if (errStream == null) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "execute", "errStream");
            throw new IOException(msg);
        }
        
        // See if we still have a connection and session, if not restart one.
        if (_sshConnection.isClosed()) _sshConnection.restart();
        
        // Create and configure the execution channel.
        int exitCode = -1;  // default value when no remote code returned 
        var session  = _sshConnection.getSession();
        if (session == null) {
            String msg =  MsgUtils.getMsg("TAPIS_SSH_NO_SESSION");
            throw new TapisException(msg);
        }
        
        // Create the channel.
        ChannelExec channel = session.createExecChannel(cmd);
        channel.setOut(outStream);
        channel.setErr(errStream);
        try {    
            // Open the channel.
            channel.open().verify(_sshConnection.getTimeouts().getOpenChannelMillis());
            
            // Send the command and close its stream.
            try (OutputStream pipedIn = channel.getInvertedIn()) {
                pipedIn.write(cmd.getBytes());
                pipedIn.flush();
            }
            
            // Wait for the channel to close.
            channel.waitFor(_closedSet, _sshConnection.getTimeouts().getExecutionMillis());
            Integer status = channel.getExitStatus();
            if (status != null) exitCode = status;
        } 
        finally {channel.close(true);} // double down by closing immediately
            
        // Return the remote exit code or the default value.
        return exitCode;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getSSHConnection:                                                      */
    /* ---------------------------------------------------------------------- */
    public SSHConnection getSSHConnection() {return _sshConnection;}

    /* ---------------------------------------------------------------------- */
    /* pwd:                                                                   */
    /* ---------------------------------------------------------------------- */
    /** Convenience method to discover current directory. */
    public String pwd() throws IOException, TapisException
    {
        final int capacity = 256;
        var output = new ByteArrayOutputStream(capacity);
        var rc = execute("pwd", output);
        if (rc != 0) {
            String msg = MsgUtils.getMsg("TAPIS_SSH_EXEC_CMD_ERROR", "pwd", 
                _sshConnection.getHost(), _sshConnection.getUsername(), rc);
            throw new TapisException(msg);
        }
        return new String(output.toByteArray());
    }
}
