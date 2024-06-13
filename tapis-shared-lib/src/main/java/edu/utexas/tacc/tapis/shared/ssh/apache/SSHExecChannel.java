package edu.utexas.tacc.tapis.shared.ssh.apache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.exceptions.runtime.TapisRuntimeException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import org.apache.sshd.common.future.CloseFuture;
import org.apache.sshd.common.future.SshFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The execute methods in this class acquire a new channel each time they are
 * invoked.  The exit code of the remote command is returned.  If an output or
 * error stream is provided, the remote command's output will be available in
 * those streams.  
 * 
 * No Apache data types are exposed on this interface.
 * 
 * @author rcardone
 */
public class SSHExecChannel implements SSHSession
{
    Logger log = LoggerFactory.getLogger(SSHExecChannel.class);

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
     * If an exception is thrown before the command can be issued, the 
     * sshConnection is closed.  After the command is issued, the exceptions
     * pass through to the caller.
     * 
     * @param cmd the command to execute on the remote host
     * @param outStream the stream containing standard out of the remote command
     * @param errStream the stream containing standard err of the remote command
     * @return the remote command's exit code
     * @throws IOException 
     * @throws TapisException
     */
    public int execute(String cmd, OutputStream outStream, OutputStream errStream) throws IOException, TapisException {
        return execute(cmd, outStream, errStream, true);
    }

    /** Execute a remote command and return its standard out and standard err
     * content in their respective streams.
     *
     * If an exception is thrown before the command can be issued, the
     * sshConnection is closed.  After the command is issued, the exceptions
     * pass through to the caller.
     *
     * @param cmd the command to execute on the remote host
     * @param outStream the stream containing standard out of the remote command
     * @param errStream the stream containing standard err of the remote command
     * @param errStream the stream containing standard err of the remote command
     * @param closeConnectionOnException if set to true, and an exception is thrown by this method,
     *                                   we will close the underlying SSHConnection also.  This should
     *                                   probably always be set to false except to support existing code that
     *                                   relies on this behavior.  In the future, the connections should be
     *                                   managed separately from the sessions since it's possible to have
     *                                   multiple sessions per connection.
     * @return the remote command's exit code
     * @throws IOException
     * @throws TapisException
     */
    public int execute(String cmd, OutputStream outStream, OutputStream errStream, boolean closeConnectionOnException)
     throws IOException, TapisException
    {
        // Check call-specific input.
        if (outStream == null) {
            if(closeConnectionOnException) {
                _sshConnection.close();
            }

            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "execute", "outStream");
            throw new IOException(msg);
        }
        if (errStream == null) {
            if(closeConnectionOnException) {
                _sshConnection.close();
            }
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "execute", "errStream");
            throw new IOException(msg);
        }
        
        // See if we still have a connection and session, if not restart one.
        if (_sshConnection.isClosed()) _sshConnection.restart();
        
        // Create and configure the execution channel.
        int exitCode = -1;  // default value when no remote code returned 
        var session  = _sshConnection.getSession();
        if (session == null) {
            if(closeConnectionOnException) {
                _sshConnection.close(); // probably not strictly necessary
            }
            String msg =  MsgUtils.getMsg("TAPIS_SSH_NO_SESSION");
            throw new TapisException(msg);
        }
        
        // Create the channel.
        ChannelExec channel = null;
        try {
            channel = session.createExecChannel(cmd);
            channel.setOut(outStream);
            channel.setErr(errStream);
        } catch (Exception e) {
            if(channel != null) {
                close(channel, false);
            }
            if (closeConnectionOnException) {
                _sshConnection.close();
            }
            String msg = MsgUtils.getMsg("TAPIS_SSH_CHANNEL_CREATE_ERROR",
                    _sshConnection.getHost(), _sshConnection.getUsername(), e.getMessage());
            throw new TapisException(msg);
        }

        // Issue the command and let execution exception flow to caller.
        try {
            // Open the channel.
            channel.open().verify(_sshConnection.getTimeouts().getOpenChannelMillis());

            // Wait for the channel to close.
            channel.waitFor(_closedSet, _sshConnection.getTimeouts().getExecutionMillis());
            Integer status = channel.getExitStatus();
            if (status != null) exitCode = status;
        } finally {
            try {
                close(channel, false);
            } catch (Exception e) {
            }
        } // double down by closing immediately, ignoring any secondary exceptions.
            
        // Return the remote exit code or the default value.
        return exitCode;
    }

    private void close(ChannelExec channel, boolean immediate) throws IOException {
        // I tried using the close future, or waiting for the channel to close.  None
        // of that really works.  In the end, I have found that sleeping a half second
        // seems to work the best.  This is really unsatisfying though ... I hope to figure
        // out a better solution one day.
        AtomicBoolean isClosed = new AtomicBoolean(false);
        var future = channel.close(immediate);
        future.addListener(new SshFutureListener<CloseFuture>() {
                               @Override
                               public void operationComplete(CloseFuture closeFuture) {
                                   isClosed.set(closeFuture.isClosed());
                                   if(!future.isDone()) {
                                       Thread.dumpStack();
                                   }
                               }
                           });

        try {
            for (int i = 0; i < 500; i++) {
                if (!isClosed.get()) {
                    Thread.sleep(10);
                } else {
                    // this should not be necessary, however I've noticed that occasionally even
                    // though the session reports that it's closed, we still get the "no more sessions"
                    // issue.  My current theory is that it's a race condition and this will fix it.
                    Thread.sleep(100);
                    break;
                }
                log.trace(MsgUtils.getMsg("TAPIS_SSH_EXEC_CLOSE_WAIT", i));
            }
        } catch (InterruptedException e) {
            // ignore.
            log.error(MsgUtils.getMsg("TAPIS_SSH_EXEC_CLOSE_INTERRUPTED"));
        }

        if(!isClosed.get()) {
            log.error(MsgUtils.getMsg("TAPIS_SSH_EXEC_CLOSE_FAILURE"));
        }
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
