package edu.utexas.tacc.tapis.shared.ssh;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHConnection;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHExecChannel;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHSftpClient;

import java.io.IOException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * SshConnectionContext - This class contains the sshConnection, and counts and controls sessions
 * on the connection.
 */
public class SshConnectionContext {
    public interface SessionConstructor<T> {
        T constructChannel(SSHConnection sshConnection) throws Exception;
    }

    final SSHConnection sshConnection;
    final int maxSessions;
    final long creationTime;
    Set<Object> sessions;
    long lifetimeMs;

    /**
     * ExecChannelConstructor can be used to construct an SSHExecChannel when calling reserveSession
     */
    public static SessionConstructor<SSHExecChannel> ExecChannelConstructor = SshConnectionContext::constructExecChannel;

    /**
     * SftpClientConstructor can be used to construct an SSHSftpClient when calling reserveSession
     */
    public static SessionConstructor<SSHSftpClient> SftpClientConstructor = SshConnectionContext::constructSftpClient;

    public SshConnectionContext(SSHConnection sshConnection, int maxSessions, Duration connectionDuration) {
        this.sshConnection = sshConnection;
        this.maxSessions = maxSessions;
        this.creationTime = System.currentTimeMillis();
        this.lifetimeMs = connectionDuration.toMillis();
        sessions = new HashSet<>();
    }

    public int getSessionCount() {
        return sessions.size();
    }

    public long getConnectionAge() {
        return System.currentTimeMillis() - creationTime;
    }

    public boolean isExpired() {
        return getConnectionAge() > lifetimeMs;
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

    public synchronized  <T> T reserveSession(SessionConstructor<T> sessionConstructor) throws TapisException {
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
        return sessions.remove(channel);
    }

    public synchronized boolean releaseSession(SSHSftpClient sftpClient) {
        return sessions.remove(sftpClient);
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
        builder.append("ConnectionAge (ms): ");
        builder.append(getConnectionAge());
        builder.append(", ");
        builder.append(isExpired() ? "EXPIRED" : "ACTIVE");
        builder.append(", ");
        builder.append("Sessions: ");
        builder.append(getSessionCount());
        return builder.toString();
    }
}
