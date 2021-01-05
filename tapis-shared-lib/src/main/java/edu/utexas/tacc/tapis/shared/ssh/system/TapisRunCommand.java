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
import edu.utexas.tacc.tapis.systems.client.gen.model.AuthnEnum;
import edu.utexas.tacc.tapis.systems.client.gen.model.TSystem;

public class TapisRunCommand 
{
    /* **************************************************************************** */
    /*                                   Constants                                  */
    /* **************************************************************************** */
    // Local logger.
    private static final Logger _log = LoggerFactory.getLogger(TapisRunCommand.class);
    
    // Default error stream buffer size.
    private static final int DEFAULT_RESULT_LEN = 1024;
    private static final int DEFAULT_READ_BUFFER_LEN = DEFAULT_RESULT_LEN;
    private static final int DEFAULT_ERR_BUFFER_LEN  = 2048;
    
    
    /* **************************************************************************** */
    /*                                    Fields                                    */
    /* **************************************************************************** */
    // From constructor.
    private final TSystem         _system;
    private final String          _command;
    private ByteArrayOutputStream _err; 
    
    /* **************************************************************************** */
    /*                                Constructors                                  */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* constructor:                                                                 */
    /* ---------------------------------------------------------------------------- */
    public TapisRunCommand(TSystem system, String command)
    {
        // These errors should never happen.
        if (system == null) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "TapisRunCommand", "system");
            throw new TapisRuntimeException(msg);
        }
        if (StringUtils.isBlank(command)) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "TapisRunCommand", "command");
            throw new TapisRuntimeException(msg);
        }
        
        _system  = system;
        _command = command;
    }

    /* **************************************************************************** */
    /*                                Public Methods                                */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* execute:                                                                     */
    /* ---------------------------------------------------------------------------- */
    public String execute() throws TapisException
    {
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
        try {
            // Initialize channel without connecting it.
            try {channel = configureExecChannel(conn);}
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
                                                 _command, getErrorStreamMessage());
                    throw new TapisException(msg, e);
                }
        } 
        finally {
            // Clean up.
            conn.closeSession();
        }
        
        return result;
    }
    
    /* ---------------------------------------------------------------------------- */
    /* accessors:                                                                   */
    /* ---------------------------------------------------------------------------- */
    public TSystem getSystem() {return _system;}
    public String getCommand() {return _command;}
    
    /* **************************************************************************** */
    /*                               Private Methods                                */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* getConnection:                                                               */
    /* ---------------------------------------------------------------------------- */
    /** Get a ssh connection to the system using one of the supported authn connection
     * methods.
     * 
     * @return the connection object
     * @throws IOException when unable to get a connection
     * @throws TapisException invalid or missing credentials
     */
    private SSHConnection getConnection() throws IOException, TapisException
    {
        // Determine which constructor to use based on the system credentials.
        var cred = _system.getAuthnCredential();
        if (cred == null) {
            String msg = MsgUtils.getMsg("SYSTEMS_MISSING_CREDENTIALS", getSystemHostMessage(),
                                         _system.getTenant());
            throw new TapisException(msg);
        }
        
        // We currently only use two types of authn for target systems.
        SSHConnection conn = null;
        if (_system.getDefaultAuthnMethod() == AuthnEnum.PASSWORD) 
            conn = new SSHConnection(_system.getHost(), _system.getPort(), 
                                     _system.getEffectiveUserId(), cred.getPassword());
        else if (_system.getDefaultAuthnMethod() == AuthnEnum.PKI_KEYS)
            conn = new SSHConnection(_system.getHost(), _system.getEffectiveUserId(), 
                                     _system.getPort(), cred.getPublicKey(), cred.getPrivateKey());
        else {
            String msg = MsgUtils.getMsg("SYSTEMS_CMD_UNSUPPORTED_AUTHN_METHOD", getSystemHostMessage(),
                                        _system.getTenant(), _system.getDefaultAuthnMethod());
            throw new TapisException(msg);
        }
        
        return conn;
    }
    
    /* ---------------------------------------------------------------------------- */
    /* configureExecChannel:                                                        */
    /* ---------------------------------------------------------------------------- */
    private ChannelExec configureExecChannel(SSHConnection conn) throws IOException
    {
        // Create the channel but don't connect it yet.
        ChannelExec channel = (ChannelExec) conn.createChannel("exec");
        channel.setCommand(_command);
        channel.setInputStream(null);
        _err = new ByteArrayOutputStream(DEFAULT_ERR_BUFFER_LEN);
        channel.setErrStream(_err);
        return channel;
    }
    
    /* ---------------------------------------------------------------------------- */
    /* sendCommand:                                                                 */
    /* ---------------------------------------------------------------------------- */
    private String sendCommand(ChannelExec channel) throws IOException, JSchException
    {
        // Initial the result string buffer.
        StringBuilder result = new StringBuilder(DEFAULT_RESULT_LEN);
        
        try {
            // Connect the channel and read the results.
            InputStream in = channel.getInputStream();
            channel.connect();
        
            // Read input chunks into a buffer until there's no more input.
            while (true) {
                byte[] buf = new byte[DEFAULT_READ_BUFFER_LEN];
                int bytesRead = in.read(buf, 0, buf.length);
                if (bytesRead < 0) break;
                result.append(new String(buf, 0, bytesRead));
            }
        
            // Check for an error if the channel got closed.
            if (channel.isClosed()) 
                if (channel.getExitStatus() != 0) {
                    String msg = MsgUtils.getMsg("SYSTEMS_CHANNEL_EXIT_WARN", 
                            getSystemHostMessage(), _system.getTenant(), 
                            channel.getExitStatus(), _command, getErrorStreamMessage());
                    _log.warn(msg);
                }
        }
        finally {channel.disconnect();}
        
        // The cumulative result string.
        return result.toString();
    }

    /* ---------------------------------------------------------------------------- */
    /* getErrorStreamMessage:                                                       */
    /* ---------------------------------------------------------------------------- */
    private String getErrorStreamMessage()
    {
        if (_err == null || _err.size() <= 0) return "";
        return _err.toString();
    }

    /* ---------------------------------------------------------------------------- */
    /* getSystemHostMessage:                                                        */
    /* ---------------------------------------------------------------------------- */
    private String getSystemHostMessage()
    {return _system.getId() + " (" + _system.getHost() + ")";}
}
