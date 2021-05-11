package edu.utexas.tacc.tapis.shared.ssh.system;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.exceptions.runtime.TapisRuntimeException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.ssh.SSHConnection;
import edu.utexas.tacc.tapis.systems.client.gen.model.TapisSystem;

public final class TapisRunCommand 
 extends TapisAbstractConnection
{
    /* **************************************************************************** */
    /*                                   Constants                                  */
    /* **************************************************************************** */
    // Local logger.
    private static final Logger _log = LoggerFactory.getLogger(TapisSftp.class);
    
    // Default error stream buffer size.
    private static final int DEFAULT_RESULT_LEN = 1024;
    private static final int DEFAULT_READ_BUFFER_LEN = DEFAULT_RESULT_LEN;
    private static final int DEFAULT_ERR_BUFFER_LEN  = 2048;
    
    /* **************************************************************************** */
    /*                                    Fields                                    */
    /* **************************************************************************** */
    // Used by subclasses for capturing error information.
    private ByteArrayOutputStream _err; 
    
    // Final exit status of command.
    private int _exitStatus = -1;
    
    /* **************************************************************************** */
    /*                                Constructors                                  */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* constructor:                                                                 */
    /* ---------------------------------------------------------------------------- */
    public TapisRunCommand(TapisSystem system)
    {
        // Save system in superclass.
        super(system);
    }

    /* ---------------------------------------------------------------------------- */
    /* constructor:                                                                 */
    /* ---------------------------------------------------------------------------- */
    public TapisRunCommand(TapisSystem system, SSHConnection conn)
    {
        // Save system and connection in superclass.
        super(system, conn);
    }

    /* **************************************************************************** */
    /*                                Public Methods                                */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* execute:                                                                     */
    /* ---------------------------------------------------------------------------- */
    /** Execute the command without closing the connection afterwards.
     *
     * @param command the command to execute on the target system
     * @return command results
     * @throws TapisException
     */
    public String execute(String command) throws TapisException
    {
        return execute(command, false);
    }

  /* ---------------------------------------------------------------------------- */
    /* execute:                                                                     */
    /* ---------------------------------------------------------------------------- */
    /** Execute the command and optionally close the connection afterwards.  Each
     * invocation of this methods creates a new channel and then destroys it.
     * 
     * @param command the command to execute on the target system
     * @param closeConnection true to close the connection, false to leave open
     * @return command results
     * @throws TapisException
     */
    public String execute(String command, boolean closeConnection)
     throws TapisException
    {
        // We need something to run.
        if (StringUtils.isBlank(command)) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "execute", "command");
            throw new TapisRuntimeException(msg);
        }
        
        // Connect to the system.
        SSHConnection conn = null;
        try {conn = getConnection();}
            catch (Exception e) {
                String msg = MsgUtils.getMsg("SYSTEMS_CONNECTION_FAILED", getSystemHostMessage(),
                                             _system.getTenant(), e.getMessage());
                throw new TapisException(msg, e);
            }
        
        // Get an exec channel and issue the command.
        String result = null;
        ChannelExec channel = null;
        _exitStatus = -1;
        try {
            // Initialize channel without connecting it.
            try {channel = configureExecChannel(command, conn);}
                catch (Exception e) {
                    String msg = MsgUtils.getMsg("SYSTEMS_CHANNEL_CONFIG_ERROR", getSystemHostMessage(),
                                                 _system.getTenant(), e.getMessage());
                    throw new TapisException(msg, e);
                }
        
            // Issue command and read results; the channel is connected and disconnected.
            try {result = sendCommand(command, channel);}
                catch (Exception e) {
                    String msg = MsgUtils.getMsg("SYSTEMS_CMD_EXEC_ERROR", getSystemHostMessage(),
                                                 _system.getTenant(), e.getMessage(),
                                                 command, getErrorStreamMessage());
                    throw new TapisException(msg, e);
                }
        } 
        finally {
          // Always clean up channel.
          conn.returnChannel(channel);
          if (closeConnection) closeConnection();
        }
        
        return result;
    }

    /* ---------------------------------------------------------------------------- */
    /* getExitStatus:                                                               */
    /* ---------------------------------------------------------------------------- */
    public int getExitStatus() {return _exitStatus;}

    /* ---------------------------------------------------------------------------- */
    /* getErrorStreamMessage:                                                       */
    /* ---------------------------------------------------------------------------- */
    public String getErrorStreamMessage()
    {
      if (_err == null || _err.size() <= 0) return "";
      return _err.toString();
    }

    /* **************************************************************************** */
    /*                               Private Methods                                */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* configureExecChannel:                                                        */
    /* ---------------------------------------------------------------------------- */
    private ChannelExec configureExecChannel(String command, SSHConnection conn) 
     throws TapisException
    {
        // Create the channel but don't connect it yet.
        ChannelExec channel = (ChannelExec) conn.createChannel("exec");
        channel.setCommand(command);
        channel.setInputStream(null);
        _err = new ByteArrayOutputStream(DEFAULT_ERR_BUFFER_LEN);
        channel.setErrStream(_err);
        return channel;
    }
    
    /* ---------------------------------------------------------------------------- */
    /* sendCommand:                                                                 */
    /* ---------------------------------------------------------------------------- */
    private String sendCommand(String command, ChannelExec channel)
     throws IOException, JSchException
    {
        // Initial the result string buffer.
        StringBuilder result = new StringBuilder(DEFAULT_RESULT_LEN);
        
        // Connect the channel and read the results.
        InputStream in = channel.getInputStream();
        channel.connect();
        
        // Read input chunks into a buffer until there's no more input.
        while (true) {
          byte[] buf = new byte[DEFAULT_READ_BUFFER_LEN];
          while (in.available() > 0) {
            int bytesRead = in.read(buf, 0, buf.length);
            if (bytesRead < 0) break;
            result.append(new String(buf, 0, bytesRead));
          }
          if (channel.isClosed()) {
            if (in.available() > 0) continue;
            _exitStatus = channel.getExitStatus();
            break;
          }
          // Pause to free up some CPU
          try {Thread.sleep(100); } catch (Exception e) {}
        }

        // Check for an error if the channel got closed.
        _exitStatus = channel.getExitStatus();

        if (channel.isClosed())
            if (_exitStatus != 0) {
                String msg = MsgUtils.getMsg("SYSTEMS_CHANNEL_EXIT_WARN", 
                               getSystemHostMessage(), _system.getTenant(), 
                               channel.getExitStatus(), command, getErrorStreamMessage());
                _log.warn(msg);
            }
        
        // The cumulative result string.
        return result.toString();
    }
}
