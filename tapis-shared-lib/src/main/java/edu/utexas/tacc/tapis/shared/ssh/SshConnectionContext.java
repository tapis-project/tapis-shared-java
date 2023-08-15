package edu.utexas.tacc.tapis.shared.ssh;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHConnection;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHExecChannel;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHSession;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHSftpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * SshConnectionContext - This class contains the sshConnection, and counts and controls sessions
 * on the connection.
 */
final class SshConnectionContext {
    private static Logger log = LoggerFactory.getLogger(SshConnectionContext.class);
    public interface SessionConstructor<T> {
        T constructChannel(SSHConnection sshConnection) throws Exception;
    }

    private final SSHConnection sshConnection;
    private final int maxSessions;
    private final long creationTime;

    // This will be set to the currentTimeMillis() each time a release is done.   It's used by getIdleTime()
    // getIdleTime will return 0 if there are sessions, or it will return idleSince minus the current time
    // if there are no sessions (the elapsed time in milliseconds since the last session was closed).
    private long idleSinceTime;
    private final Set<SSHSession> sessions;
    private final long lifetimeMs;
    private final long maxIdleTimeMs;

    /**
     * ExecChannelConstructor can be used to construct an SSHExecChannel when calling reserveSession
     */
    public static SessionConstructor<SSHExecChannel> ExecChannelConstructor = SshConnectionContext::constructExecChannel;

    /**
     * SftpClientConstructor can be used to construct an SSHSftpClient when calling reserveSession
     */
    public static SessionConstructor<SSHSftpClient> SftpClientConstructor = SshConnectionContext::constructSftpClient;

    public SshConnectionContext(SSHConnection sshConnection, int maxSessions, Duration connectionDuration,
                                Duration maxConnectionIdleTime) {
        this.sshConnection = sshConnection;
        this.maxSessions = maxSessions;
        this.creationTime = System.currentTimeMillis();
        this.lifetimeMs = connectionDuration.toMillis();
        this.maxIdleTimeMs = maxConnectionIdleTime.toMillis();
        this.idleSinceTime = System.currentTimeMillis();
        sessions = new HashSet<>();
    }

    public int getSessionCount() {
        return sessions.size();
    }

    public long getConnectionAge() {
        return System.currentTimeMillis() - creationTime;
    }

    public boolean isExpired() {
        if(getConnectionAge() > lifetimeMs) {
            String msg = MsgUtils.getMsg("SSH_POOL_CONNECTION_AGE_EXCEEDED");
            log.debug(msg);
            return true;
        } else if (getIdleTime() > maxIdleTimeMs) {
            String msg = MsgUtils.getMsg("SSH_POOL_CONNECTION_IDLE_TIME_EXCEEDED");
            log.debug(msg);
            return true;
        }

        return false;
    }

    public boolean hasAvailableSessions() {
        if(isExpired()) {
            return false;
        }

        return sessions.size() < maxSessions;
    }

    boolean containsChannel(Object channel) {
       return sessions.contains(channel);
    }

    public synchronized  <T extends SSHSession> T reserveSession(SessionConstructor<T> sessionConstructor) throws TapisException {
        if(hasAvailableSessions()) {
            T session = null;
            try {
                session = sessionConstructor.constructChannel(this.sshConnection);
            } catch (Exception ex) {
                String msg = MsgUtils.getMsg("SSH_POOL_UNABLE_TO_CREATE_CHANNEL");
               throw new TapisException(msg, ex);
            }
            sessions.add(session);
            return session;
        }

        return null;
    }

    public synchronized boolean releaseSession(SSHExecChannel channel) {
        boolean result = sessions.remove(channel);
        this.idleSinceTime = System.currentTimeMillis();
        return result;
    }

    public synchronized boolean releaseSession(SSHSftpClient sftpClient) {
        boolean result = sessions.remove(sftpClient);
        this.idleSinceTime = System.currentTimeMillis();
        return result;
    }

    public long getIdleTime() {
        // if there are no sessions, return the idle time, but if we have
        // sessions, just return 0 meaning it's not idle
        if(sessions.size() == 0) {
            return System.currentTimeMillis() - idleSinceTime;
        }

        return 0;
    }

    protected void close() {
        // Close the SshConnection associated with this context.
        log.trace("Closing SSH connection");
        sshConnection.close();
    }

    public static SSHExecChannel constructExecChannel(SSHConnection sshConnection) {
        return sshConnection.getExecChannel();
    }

    public static SSHSftpClient constructSftpClient(SSHConnection sshConnection) throws IOException {
        return sshConnection.getSftpClient();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Age (ms): ");
        builder.append(getConnectionAge());
        builder.append(", ");
        builder.append("Idle (ms): ");
        builder.append(getIdleTime());
        builder.append(", ");
        builder.append(isExpired() ? "EXPIRED" : "ACTIVE");
        builder.append(", ");
        builder.append("Sessions: ");
        builder.append(getSessionCount());
        return builder.toString();
    }
}
