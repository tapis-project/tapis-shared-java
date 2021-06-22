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

/**
 * Class that can be used to run a command on a Tapis System
 * Captures stdout, stderr and exit code.
 * Once an instance is created for a system using new TapisRunCommand(system)
 *   it can be used to run one or more commands and get the results.
 * Connection is cached but can be closed as part of the execute() method.
 * Usage:
 *   var runCmd = new TapisRunCommand(system);
 *   runCmd.execute("ls /tmp"); // Run a command and leave connection open
 *   runCmd.execute("ls /tmp", true); // Run a command and close the connection
 *   runCmd.getExitStatus(); // Get exit code
 *   runCmd.getStdOut(); // Get output of command
 *   runCmd.getStdErr(); // Get error output of command
 */
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

    // Compile time flag for detailed debug output
    private static final boolean DEBUG = false;
    
    /* **************************************************************************** */
    /*                                    Fields                                    */
    /* **************************************************************************** */
    // Used for capturing stderr output
    private ByteArrayOutputStream _stdErr;
    // Used for capturing command output
    private String _stdOut;

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
    /**
     *  Execute the command without closing the connection afterwards.
     *  Collect stdout and stderr. Set the exit status
     *  Each invocation of this methods creates a new channel and then destroys it.
     *
     * @param command the command to execute on the target system
     * @return stdout output from command execution
     * @throws TapisException
     */
    public String execute(String command) throws TapisException
    {
        return execute(command, false);
    }

    /* ---------------------------------------------------------------------------- */
    /* execute:                                                                     */
    /* ---------------------------------------------------------------------------- */
    /**
     *  Execute the command and optionally close the connection afterwards.
     *  Collect stdout and stderr. Set the exit status
     *  Each invocation of this methods creates a new channel and then destroys it.
     * 
     * @param command the command to execute on the target system
     * @param closeConnection true to close the connection, false to leave open
     * @return stdout output from command execution
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

        if (DEBUG) _log.debug(String.format("Running command: %s", command));
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
            try {result = sendCommand(channel);}
                catch (Exception e) {
                    String msg = MsgUtils.getMsg("SYSTEMS_CMD_EXEC_ERROR", getSystemHostMessage(),
                                                 _system.getTenant(), e.getMessage(),
                                                 command, getStdErr());
                    throw new TapisException(msg, e);
                }
        } 
        finally {
          // Always clean up channel.
          conn.returnChannel(channel);
          if (closeConnection) closeConnection();
        }

        _stdOut = result;
        return result;
    }

    /* ---------------------------------------------------------------------------- */
    /* getExitStatus:                                                               */
    /* ---------------------------------------------------------------------------- */
    public int getExitStatus() {return _exitStatus;}

    /* ---------------------------------------------------------------------------- */
    /* getStdErr:                                                                   */
    /* ---------------------------------------------------------------------------- */
    public String getStdErr()
    {
      if (_stdErr == null || _stdErr.size() <= 0) return "";
      return _stdErr.toString();
    }

    /* ---------------------------------------------------------------------------- */
    /* getStdOut:                                                                   */
    /* ---------------------------------------------------------------------------- */
    public String getStdOut()
    {
      if (StringUtils.isBlank(_stdOut)) return "";
      else return _stdOut;
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
        _stdErr = new ByteArrayOutputStream(DEFAULT_ERR_BUFFER_LEN);
        channel.setErrStream(_stdErr);
        return channel;
    }
    
    /* ---------------------------------------------------------------------------- */
    /* sendCommand:                                                                 */
    /* ---------------------------------------------------------------------------- */
    /**
     * Connect to the channel and read any stdout until the channel closes.
     * Update the exit status.
     * Return stdout as a string.
     */
    private String sendCommand(ChannelExec channel)
     throws IOException, JSchException
    {
      // Initialize string buffer for capturing command output.
      StringBuilder result = new StringBuilder(DEFAULT_RESULT_LEN);

      // Create an input stream for receiving any stdout and connect the channel
      InputStream stdOutputStream = channel.getInputStream();
      channel.connect();

      // Read stdout into a buffer until the channel closes and no more stdout
      // Based on Jsch example: http://www.jcraft.com/jsch/examples/Exec.java.html
      int i = 0, j = 0; // Simple counters for DEBUG
      byte[] buf = new byte[DEFAULT_READ_BUFFER_LEN];
      // Loop forever until the channel closes and all stdout bytes have been read
      while (true)
      {
        i++;
        if (DEBUG) _log.debug(String.format("sendCmd: Starting available() while loop. Check # %d.%d",i,j));
        // While output available read the bytes and append them to the result
        // NOTE: Without this while loop we sometimes get exitStatus = -1.
        //       One factor is it seems to take about 80ms for stdout to start flowing.
        //       NOTE: It takes same amount of time to startup if re-using a connection or creating a new one.
        while (stdOutputStream.available() > 0)
        {
          j++;
          if (DEBUG) _log.debug(String.format("sendCmd: reading bytes # %d.%d",i,j));
          int bytesRead1 = stdOutputStream.read(buf, 0, buf.length);
          if (DEBUG) _log.debug(String.format("sendCmd after read poll_loop # %d.%d ByteCount: %d",i,j,bytesRead1));
          // If no bytes read then we have read in all currently available data
          if (bytesRead1 < 0) break;
          // We have some data, append to the result
          result.append(new String(buf, 0, bytesRead1));
        }
        // If channel is closed then the command is finished, but there may still be data coming in from stdout
        if (channel.isClosed())
        {
          if (DEBUG) _log.debug(String.format("sendCmd: channel is closed # %d.%d", i,j));
          if (DEBUG) _log.debug(String.format("sendCmd: final available() check # %d.%d",i,j));
          // While output available check for and read in any remaining stdout data
          // NOTE: Without this final available() it seems we always get exitStatus = -1
          if (stdOutputStream.available() > 0)
          {
            int bytesRead2 = stdOutputStream.read(buf, 0, buf.length);
            if (bytesRead2 < 0) break;
            if (DEBUG) _log.debug(String.format("sendCmd: final read # %d.%d ByteCount: %d",i,j,bytesRead2));
            result.append(new String(buf, 0, bytesRead2));
          }
          _exitStatus = channel.getExitStatus();
          if (DEBUG) _log.debug(String.format("sendCmd: Breakout at # %d.%d ExitStatus: %d", i,j,_exitStatus));
          // Channel is closed, all stdout has been collected so we are ready to finish
          break;
        }
        if (DEBUG) _log.debug(String.format("sendCmd: sleep poll_loop # %d.%d",i,j));
        // Channel has not closed, command may continue to generate stdout. Pause briefly while command runs.
        try {Thread.sleep(10); } catch (Exception ignored) {}
      }

      // Return the cumulative result string from stdout.
      _stdOut = result.toString();
      return _stdOut;
    }
}
