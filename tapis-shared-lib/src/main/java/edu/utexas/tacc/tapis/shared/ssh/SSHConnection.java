package edu.utexas.tacc.tapis.shared.ssh;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.exceptions.recoverable.TapisSSHAuthException;
import edu.utexas.tacc.tapis.shared.exceptions.recoverable.TapisSSHConnectionException;


/**
 * A SSHConnection is basically a wrapper around a Jsch session. Channels that are in use
 * are stored in a concurrent Set data structure. If that set is empty, the session can be safely
 * closed. The openChannel and returnChannel methods are synchronized for thread safety. These objects are
 * stored in the Cache.
 *
 * The connection/session is opened upon instantiation
 *
 */
public class SSHConnection implements ISSHConnection {
    
    // Public enums.
    public enum AuthMethod {PUBLICKEY_AUTH, PASSWORD_AUTH}
    
    // Timeouts.
    private static final int CONNECT_TIMEOUT_MILLIS = 15000; // 15 seconds

    // A  set that will be used to store the channels that are open
    // on the SSH session.
    private final Set<Channel> channels = new HashSet<>();


    // Indicates what to do if the server's host key changed or the server is
    // unknown. One of yes (refuse connection), ask (ask the user whether to add/change the
    // key) and no (always insert the new key).
    private static final String STRICT_HOSTKEY_CHECKIN_KEY = "StrictHostKeyChecking";
    private static final String STRICT_HOSTKEY_CHECKIN_VALUE = "no";

    private static final Logger log = LoggerFactory.getLogger(SSHConnection.class);

    private final String host;
    private final int port;
    private final String username;
    private final AuthMethod authMethod;
    private String password;
    private String privateKey;
    private String publicKey;
    private Session session;

    /**
     * Public/Private key auth
     * @param host Hostname
     * @param username username of user on the remote system
     * @param port Port to connect to, defaults to 22
     * @param publicKey The public key
     * @param privateKey The private key
     * @throws TapisException Throws an exception if the session can't connect.
     */
    public SSHConnection(String host, String username, int port, String publicKey, String privateKey)  throws TapisException {
        this.host = host;
        this.username = username;
        this.port = port > 0 ? port : 22;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        authMethod = AuthMethod.PUBLICKEY_AUTH;
        initSession();
    }

    /**
     * Username/password auth
     * @param host Hostname
     * @param port port, defaults to 22
     * @param username username
     * @param password password
     * @throws TapisException Throws an exception if the session can't connect
     */
    public SSHConnection(String host, int port, String username, String password) throws TapisException {
        this.host = host;
        this.port = port > 0 ? port : 22;
        this.username = username;
        this.password = password;
        authMethod = AuthMethod.PASSWORD_AUTH;
        initSession();
    }

    @Override
    public synchronized int getChannelCount() {
        return channels.size();
    }

    private void initSession() throws TapisException {
        final JSch jsch = new JSch();
        try {
            session = jsch.getSession(username, host, port);
            session.setConfig(STRICT_HOSTKEY_CHECKIN_KEY, STRICT_HOSTKEY_CHECKIN_VALUE);
            session.setConfig("PreferredAuthentications", "password,publickey");
            session.setTimeout(CONNECT_TIMEOUT_MILLIS);

        } catch (JSchException e) {
            // Will only catch here if things are really out of sorts above, like null values,
            String msg = String.format("SSH_CONNECTION_GET_SESSION_ERROR for user %s on host %s at port %s", username, host, port);
            throw new TapisException(msg, e);
        }

        if (authMethod == AuthMethod.PUBLICKEY_AUTH) {
            try {
                jsch.addIdentity(host, privateKey.getBytes(), publicKey.getBytes(), (byte[]) null);
            } catch (JSchException e) {
                String msg = String.format("SSH_CONNECTION_ADD_KEY_ERROR Invalid SSH key for user %s on host %s at port %s", username, host, port);
                throw new TapisException(msg, e);
            }
        } else {
            UserInfo ui;
            session.setPassword(password);
            ui = new UserInfoImplementation(username, password);
            session.setUserInfo(ui);
        }

        try {
            session.connect();
        } catch (JSchException e) {
            if (e.getMessage().contains("UnknownHostException")) {
                // Could not resolve the host
                String msg = String.format("SSH_CONNECT_SESSION_ERROR Unknown host for user %s on host %s at port %s", username, host, port);
                throw new TapisException(msg);
            } else if (e.getMessage().contains("Too many authentication failures")) {
                // This will get hit if the credentials are bad. Jsch can hit the host on the given port.
                String msg = String.format("SSH_CONNECT_SESSION_ERROR Auth failed for user %s on host %s at port %s", username, host, port);
                TreeMap<String, String> state = new TreeMap<>();
                state.put("hostname", host);
                state.put("username", username);
                state.put("port", String.valueOf(port));
                state.put("authMethod", authMethod.name());
                throw new TapisSSHAuthException(msg, e, state);
            } else if (e.getMessage().contains("timeout:")) {
                String msg = String.format("SSH_CONNECT_SESSION_ERROR Connection timeout for user %s on host %s at port %s", username, host, port);
                throw new TapisException(msg, e);
            } else {
                String msg = String.format("SSH_CONNECT_SESSION_ERROR for user %s on host %s at port %s", username, host, port);
                throw new TapisException(msg, e);
            }
        }
    }

    /**
     * Synchronized to allow for thread safety, this method closes a channel and removes it from the
     * set of open channels.
     * @param channel returns and closes a channel from the pool
     */
    @Override
    public synchronized void returnChannel(Channel channel) {
        if (channel == null) return;
        channels.remove(channel);
        channel.disconnect();
    }

    /**
     * Create and open a channel on the session, placing it into the concurrent set of channels
     * that are active on the session.
     * @param channelType One of "sftp", "exec" or "shell"
     * @return Channel
     * @throws TapisException Non-recoverable,
     * @throws TapisSSHConnectionException A recoverable error occured.
     */
    @Override
    public synchronized Channel createChannel(String channelType) throws TapisSSHConnectionException, TapisException {
        Channel channel;
        try {
            if (!session.isConnected()) {
                initSession();
            }
            switch (channelType) {
                case "sftp":
                    channel = session.openChannel(channelType);
                    channel.connect(); // needed to avoid "io_in" NPE
                    break;
                case "exec":
                case "shell":
                    channel = session.openChannel(channelType);
                    break;
                default:
                    throw new TapisException("Invalid channel type: " + channelType);
            }
            channels.add(channel);
            return channel;

        } catch (JSchException e) {
            // The session is authenticated but a channel could not be opened. This is the case
            // when the max number of connections is reached. Should be a recoverable error condition.
            String msg = String.format("SSH_OPEN_CHANNEL_ERROR for user %s on host %s at port %s", username, host, port);
            TreeMap<String, String> state = new TreeMap<>();
            state.put("hostname", host);
            state.put("username", username);
            state.put("port", String.valueOf(port));
            state.put("authMethod", authMethod.name());
            state.put("channelType", channelType);
            throw new TapisSSHConnectionException(msg, e, state);
        }
    }

    @Override
    public synchronized void closeSession() {
        if (session != null && session.isConnected()) {
            for (var c : channels) c.disconnect();
            session.disconnect();
            channels.clear();
        }
    }

    @Override
    public Session getSession() {
        return this.session;
    }


}
