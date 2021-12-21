package edu.utexas.tacc.tapis.shared.ssh.apache;

import java.io.IOException;
import java.security.KeyPair;
import java.util.Base64;

import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.SshConstants;
import org.apache.sshd.common.SshException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.exceptions.recoverable.TapisRecoverableException;
import edu.utexas.tacc.tapis.shared.exceptions.recoverable.TapisSSHAuthException;
import edu.utexas.tacc.tapis.shared.exceptions.recoverable.TapisSSHConnectionException;
import edu.utexas.tacc.tapis.shared.exceptions.recoverable.TapisSSHTimeoutException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;

/** This class abstracts the idea of an authenticated SSH connection to a host.
 * Currently, this class supports exactly one session per client instance.  This is
 * another way of saying that in Tapis, there's a 1-to-1 correspondence between
 * connections and sessions.  Apache SSH allows multiple sessions per connection,
 * where each session is authenticated with its own credentials.
 * 
 * Future enhancements might allow for a single, long-lived connection to be
 * established to commonly used hosts in which the connection would manage many
 * sessions.  This type of multiplexed connection would act like a tunnel to
 * the remote host and avoid incurring frequent connection setup overhead.  
 * 
 * Some of the code used by this class concerning PEM file parsing is based on code
 * from a public MasterCard repository with MIT license.
 * 
 * Source: https://github.com/Mastercard/client-encryption-java/blob/master/src/main/java/com/mastercard/developer/utils/EncryptionUtils.java
 *         https://stackoverflow.com/questions/7216969/getting-rsa-private-key-from-pem-base64-encoded-private-key-file/55339208#55339208
 * 
 * @author rcardone
 */
public class SSHConnection
 implements AutoCloseable
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = LoggerFactory.getLogger(SSHConnection.class);
    
    // For logging purposes, allow the calling program to initialize the host 
    // that we are running on.  If not set, no extra logging occurs.
    private static String LOCAL_NODE_NAME; 
    
    /* ********************************************************************** */
    /*                            Initializers                                */
    /* ********************************************************************** */
    static {
        // Configure apache ssh logging by interfacing directly with logback.
        var sshLogger = 
           (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.apache.sshd");
        if (sshLogger != null) sshLogger.setLevel(ch.qos.logback.classic.Level.ERROR);
    }
    
    /* ********************************************************************** */
    /*                                 Enums                                  */
    /* ********************************************************************** */
    // Public enums.
    public enum AuthMethod {PUBLICKEY_AUTH, PASSWORD_AUTH}
    
    // Private enums.
    private enum ExceptionSource {CONNECT, AUTH}
    
    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    // Fixed at construction.
    private final String      _host;
    private final int         _port;
    private final String      _username;
    private final SSHTimeouts _timeouts;
    private final AuthMethod  _authMethod;
    
    // Optional constructor parameters.
    private String            _password;
    private String            _privateKey;
    private String            _publicKey;
    
    // Field assigned during processing.
    private SshClient         _client;
    private ClientSession     _session;
    
    /* ********************************************************************** */
    /*                            Constructors                                */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    public SSHConnection(String host, int port, String username, String password) 
     throws TapisException 
    {
        this(host, port, username, password, new SSHTimeouts());
    }

    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    /**
     *  Establish a password authenticated client+session
     * 
     * @param host
     * @param port
     * @param username
     * @param password
     * @param timeouts
     * @throws TapisException
     */
    public SSHConnection(String host, int port, String username, String password,
                         SSHTimeouts timeouts) 
     throws TapisException 
    {
        // Check call-specific input.
        if (password == null) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "SSHConnection", "password");
            throw new TapisException(msg);
        }
        
        // Assign fields.
        _host = host;
        _port = port > 0 ? port : 22;
        _username = username;
        _password = password;
        _timeouts = timeouts;
        _authMethod = AuthMethod.PASSWORD_AUTH;
        
        // Establish a connected and authenticated session.
        // Initialize and start a client. Use client.connect() to start a session.
        try {initSession();}
            catch (TapisException e) {
                stop();
                throw e;
            }
            catch (Exception e) {
                stop();
                String msg = MsgUtils.getMsg("TAPIS_SSH_CONNECT_ERROR", host, port,
                                             username, _authMethod, e.getMessage());
                throw new TapisException(msg, e);
            }
    }

    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    /**
     *  Establish a PKI authenticated client+session with default timeouts
     *
     * @param host
     * @param port
     * @param username
     * @param publicKey
     * @param privateKey
     * @throws TapisException
     */
    public SSHConnection(String host, int port, String username,
                         String publicKey, String privateKey) 
     throws TapisException 
    {
        this(host, port, username, publicKey, privateKey, new SSHTimeouts());
    }
    
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    /**
     *  Establish a PKI authenticated client+session with given timeouts
     *
     * @param host
     * @param port
     * @param username
     * @param publicKey
     * @param privateKey
     * @param timeouts
     * @throws TapisException
     */
    public SSHConnection(String host, int port, String username, String publicKey, 
                         String privateKey, SSHTimeouts timeouts) 
     throws TapisException 
    {
        // Check call-specific input.
        if (publicKey == null) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "SSHConnection", "publicKey");
            throw new TapisException(msg);
        }
        if (privateKey == null) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "SSHConnection", "privateKey");
            throw new TapisException(msg);
        }
        
        // Assign fields.
        _host = host;
        _username = username;
        _port = port > 0 ? port : 22;
        _privateKey = privateKey;
        _publicKey = publicKey;
        _timeouts = timeouts;
        _authMethod = AuthMethod.PUBLICKEY_AUTH;
        
        // Establish a connected and authenticated session.
        try {initSession();}
            catch (TapisException e) {throw e;}
            catch (Exception e) {
                String msg = MsgUtils.getMsg("TAPIS_SSH_CONNECT_ERROR", host, port,
                                             username, _authMethod, e.getMessage());
                throw new TapisException(msg, e);
            }
    }
    
    /* ********************************************************************** */
    /*                            Public Methods                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* getExecChannel:                                                        */
    /* ---------------------------------------------------------------------- */
    public SSHExecChannel getExecChannel()
    {
        return new SSHExecChannel(this);
    }
    
    /* ---------------------------------------------------------------------- */
    /* getSftpClient:                                                         */
    /* ---------------------------------------------------------------------- */
    public SSHSftpClient getSftpClient() throws IOException
    {
        return new SSHSftpClient(this);
    }
    
    /* ---------------------------------------------------------------------- */
    /* getScpClient:                                                          */
    /* ---------------------------------------------------------------------- */
    public SSHScpClient getScpClient() throws IOException
    {
        return new SSHScpClient(this);
    }
    
    /* ---------------------------------------------------------------------- */
    /* stop:                                                                  */
    /* ---------------------------------------------------------------------- */
    public synchronized void stop()
    {
        // Close the session immediately.
        closeSession(true);
        
        // Stop the client.
        if (_client != null) {
            _client.stop();
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* close:                                                                 */
    /* ---------------------------------------------------------------------- */
    public synchronized boolean isClosed()
    {
        if (_client == null  || _client.isClosed() || 
            _session == null || _session.isClosed()) 
            return true;
        return false;
    }

    /* ---------------------------------------------------------------------- */
    /* close:                                                                 */
    /* ---------------------------------------------------------------------- */
    @Override
    public void close() {stop();}
    
    /* ---------------------------------------------------------------------- */
    /* setLocalNodeName:                                                      */
    /* ---------------------------------------------------------------------- */
    public static void setLocalNodeName(String nodeName) {LOCAL_NODE_NAME = nodeName;}
    
    /* ********************************************************************** */
    /*                               Accessors                                */
    /* ********************************************************************** */
    public ClientSession getSession() {return _session;}
    public String getHost() {return _host;}
    public int getPort() {return _port;}
    public String getUsername() {return _username;}
    public SSHTimeouts getTimeouts() {return _timeouts;}
    public AuthMethod getAuthMethod() {return _authMethod;}
    
    /* ********************************************************************** */
    /*                           Protected Methods                            */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* restart:                                                               */
    /* ---------------------------------------------------------------------- */
    /** Restart a client and re-establish its session.  Typically, this method
     * is called with the caller detects that the client is stopped or the 
     * session is closed.
     * 
     * @throws TapisException
     * @throws IOException
     */
    protected synchronized void restart() throws TapisException, IOException
    {
        if (_client.isStarted()) stop();
        initSession();
    }
    
    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* initSession:                                                           */
    /* ---------------------------------------------------------------------- */
    private void initSession() throws TapisException, IOException 
    {
        // Check input.
        if (StringUtils.isBlank(_host)) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "initSession", "host");
            throw new TapisException(msg);
        }
        if (StringUtils.isBlank(_username)) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "initSession", "username");
            throw new TapisException(msg);
        }
        
        // Create a new client.
        _client = SshClient.setUpDefaultClient();
        _client.start();
        
        // Connect the session.
        try {
            _session = _client.connect(_username, _host, _port)
                       .verify(_timeouts.getConnectMillis())
                       .getSession();
        } catch (Exception e) {
            TapisRecoverableException rex = getRecoverable(e, ExceptionSource.CONNECT);
            if (rex == null) throw e;
            rex.state.put("hostname", _host);
            rex.state.put("username", _username);
            rex.state.put("port", String.valueOf(_port));
            rex.state.put("authMethod", _authMethod.name());
            throw rex;
        }
        if (LOCAL_NODE_NAME != null) logConnect();
        
        // Authenticate the user.
        if (_authMethod == AuthMethod.PASSWORD_AUTH) 
            _session.addPasswordIdentity(_password);
          else {
              var keyLoader = new SSHKeyLoader(_publicKey, _privateKey, _username, _host);
              KeyPair keyPair = keyLoader.getKeyPair();
              _session.addPublicKeyIdentity(keyPair);
          }
            
        // Authenticate the user.
        try {_session.auth().verify(_timeouts.getAuthenticateMillis());}
            catch (Exception e) {
                TapisRecoverableException rex = getRecoverable(e, ExceptionSource.AUTH);
                if (rex == null) throw e;
                rex.state.put("hostname", _host);
                rex.state.put("username", _username);
                rex.state.put("port", String.valueOf(_port));
                rex.state.put("authMethod", _authMethod.name());
                throw rex;
            }
        if (LOCAL_NODE_NAME != null) logAuth();
    }

    /* ---------------------------------------------------------------------- */
    /* closeSession:                                                          */
    /* ---------------------------------------------------------------------- */
    private void closeSession(boolean immediate)
    {
        // Close the session.
        if (_session != null) { 
            if (LOCAL_NODE_NAME != null) logDisconnect();
            // Close abruptly or gracefully.
            _session.close(immediate);
            _session = null;
        }
    }

    /* ---------------------------------------------------------------------- */
    /* getRecoverable:                                                        */
    /* ---------------------------------------------------------------------- */
    /** Return a TapisRecoverableException if a recoverable condition is detected.
     * The actual exception class returned depends on the call site from where the
     * exception occurred and the error condition.
     * 
     * @param e the exception
     * @param source the call site where the exception occurred
     * @return a subclass of TapisRecoverableException or null for unrecoverable exceptions
     */
    private TapisRecoverableException getRecoverable(Exception e, ExceptionSource source)
    {
        // Let's see what ssh is trying to tell us.
        if (e instanceof SshException)
        {
            // Certain connection and authentication errors are possibly recoverable.
            int disconnectCode = ((SshException)e).getDisconnectCode();
            switch (disconnectCode) {
                // This is the list of recoverable connection and authentication errors.
                case SshConstants.SSH2_DISCONNECT_CONNECTION_LOST:
                case SshConstants.SSH2_DISCONNECT_NO_MORE_AUTH_METHODS_AVAILABLE:
                case SshConstants.SSH2_DISCONNECT_TOO_MANY_CONNECTIONS:
                case SshConstants.SSH2_DISCONNECT_SERVICE_NOT_AVAILABLE:
                    if (source == ExceptionSource.CONNECT) 
                        return new TapisSSHConnectionException(e.getMessage(), e, null);
                      else 
                        return new TapisSSHAuthException(e.getMessage(), e, null);
            }
        }
        
        // We try to detect other recoverable conditions by examining the error message.
        var msg = e.getMessage();
        if (msg != null) {
            if (msg.contains("timeout")) return new TapisSSHTimeoutException(e.getMessage(), e, null);
        }
        
        // Didn't identify any recoverable condition.
        return null;
    }
    
    /* ---------------------------------------------------------------------- */
    /* logConnect:                                                            */
    /* ---------------------------------------------------------------------- */
    private void logConnect()
    {
        if (!_log.isDebugEnabled()) return;
        _log.debug(MsgUtils.getMsg("TAPIS_SSH_CONNECT", LOCAL_NODE_NAME, 
                                   _username, _host, _port));
    }

    /* ---------------------------------------------------------------------- */
    /* logDisconnect:                                                         */
    /* ---------------------------------------------------------------------- */
    private void logDisconnect()
    {
        if (!_log.isDebugEnabled()) return;
        byte[] bytes = _session == null ? null : _session.getSessionId();
        String id = (bytes == null) ? null : Base64.getEncoder().encodeToString(bytes);
        _log.debug(MsgUtils.getMsg("TAPIS_SSH_DISCONNECT", LOCAL_NODE_NAME,
                                   _username, _host, _port, id));
    }

    /* ---------------------------------------------------------------------- */
    /* logAuth:                                                               */
    /* ---------------------------------------------------------------------- */
    private void logAuth()
    {
        if (!_log.isDebugEnabled()) return;
        byte[] bytes = _session == null ? null : _session.getSessionId();
        String id = (bytes == null) ? null : Base64.getEncoder().encodeToString(bytes);
        _log.debug(MsgUtils.getMsg("TAPIS_SSH_AUTH", LOCAL_NODE_NAME, _username, 
                                   _host, _port, id, _authMethod.name()));
    }
}

