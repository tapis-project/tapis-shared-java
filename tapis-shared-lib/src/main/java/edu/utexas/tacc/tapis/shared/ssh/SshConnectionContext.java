package edu.utexas.tacc.tapis.shared.ssh;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHConnection;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHExecChannel;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHSession;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHSftpClient;
import org.apache.commons.io.IOUtils;
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
        T constructSession(SSHConnection sshConnection) throws Exception;
    }

    private final SSHConnection sshConnection;
    private final int maxSessions;
    private final long creationTime;
    private boolean expired;

    // This will be set to the currentTimeMillis() each time a release is done.   It's used by getIdleTime()
    // getIdleTime will return 0 if there are sessions, or it will return idleSince minus the current time
    // if there are no sessions (the elapsed time in milliseconds since the last session was closed).
    private long idleSinceTime;
    //  SessionHolder is a class that reserves a spot for a session.  The session will get connected later.
    private final Set<SshSessionHolder> sessionHolders;
    private final long lifetimeMs;
    private final long maxIdleTimeMs;

    /**
     * ExecChannelConstructor can be used to construct an SSHExecChannel when calling reserveSession
     */
    protected static SessionConstructor<SSHExecChannel> ExecChannelConstructor = SshConnectionContext::constructExecChannel;

    /**
     * SftpClientConstructor can be used to construct an SSHSftpClient when calling reserveSession
     */
    protected static SessionConstructor<SSHSftpClient> SftpClientConstructor = SshConnectionContext::constructSftpClient;

    protected SshConnectionContext(SSHConnection sshConnection, int maxSessions, Duration connectionDuration,
                                Duration maxConnectionIdleTime) {
        this.sshConnection = sshConnection;
        this.maxSessions = maxSessions;
        this.creationTime = System.currentTimeMillis();
        this.lifetimeMs = connectionDuration.toMillis();
        this.maxIdleTimeMs = maxConnectionIdleTime.toMillis();
        this.idleSinceTime = System.currentTimeMillis();
        this.expired = false;
        sessionHolders = new HashSet<>();
    }

    protected int getSessionCount() {
        // include all session holders in the count - even the ones with sessions that are not yet created.
        synchronized (sessionHolders) {
            return sessionHolders.size();
        }
    }


    protected long getConnectionAge() {
        return System.currentTimeMillis() - creationTime;
    }

    public boolean isExpired() {
        if(expired) {
            String msg = MsgUtils.getMsg("SSH_POOL_CONNECTION_EXPIRED");
            log.debug(msg);
            return true;
        } else if(getConnectionAge() > lifetimeMs) {
            String msg = MsgUtils.getMsg("SSH_POOL_CONNECTION_AGE_EXCEEDED");
            log.debug(msg);
            expireConnection();
            return true;
        } else if (getIdleTime() > maxIdleTimeMs) {
            String msg = MsgUtils.getMsg("SSH_POOL_CONNECTION_IDLE_TIME_EXCEEDED");
            log.debug(msg);
            return true;
        }

        return false;
    }

    protected boolean hasAvailableSessions() {
        if(isExpired()) {
            return false;
        }

        // count all session holders here - even the ones that don't have a session yet.
        synchronized (sessionHolders) {
            return sessionHolders.size() < maxSessions;
        }
    }

    /**
     * Returns true if the session is contained in this ConnectionContext
     * @param session session to search for.
     * @return true if it's in the context, or false if not.
     * @param <T>
     */
    protected <T extends SSHSession> boolean containsSession(T session) {
        return findSessionHolder(session) != null;
    }

    /**
     * Returns true if the session holder is contained in this ConnectionContext
     * @param searchSessionHolder sessionHolder to search for.
     * @return true if the sessionHolder is contained in the context, or false if not.
     */
    protected boolean containsSessionHolder(SshSessionHolder searchSessionHolder) {
        synchronized(sessionHolders) {
            for (SshSessionHolder sessionHolder : sessionHolders) {
                if (sessionHolder == searchSessionHolder) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Find a SessionHolder that holds the given session.
     * @param session SSHSession to located
     * @return SshSessionHolder if found, or null if not found.
     */
    private SshSessionHolder findSessionHolder(SSHSession session) {
        synchronized(sessionHolders) {
            for (SshSessionHolder sessionHolder : sessionHolders) {
                SSHSession containedSession = sessionHolder.getSession();
                if ((containedSession != null) && (containedSession == session)) {
                    return sessionHolder;
                }
            }
        }

        return null;
    }

    protected <T extends SSHSession> SshSessionHolder<T> reserveSession(SessionConstructor<T> sessionConstructor) throws TapisException {
        synchronized(sessionHolders) {
            if (hasAvailableSessions()) {
                SshSessionHolder<T> sessionHolder = null;
                try {
                    sessionHolder = new SshSessionHolder<T>(this, this.sshConnection, sessionConstructor);
                } catch (Exception ex) {
                    // if we are unable to create new sessions on this connection, we will expire it
                    this.expireConnection();
                    String msg = MsgUtils.getMsg("SSH_POOL_UNABLE_TO_CREATE_CHANNEL");
                    throw new TapisException(msg, ex);
                }
                sessionHolders.add(sessionHolder);
                return sessionHolder;
            }
        }

        return null;
    }

    /**
     * Release the provided session holder.  The session is expected to be closed, however if it looks
     * like it's left open this method will attempt to close it.
     * been done
     * @param sessionHolder SessionHolder to release.
     * @return true for success, or false for failure.
     */
    protected boolean releaseSessionHolder(SshSessionHolder sessionHolder) {
        boolean result = false;
        if(sessionHolder != null) {
            synchronized(sessionHolders) {
                SSHSession session = sessionHolder.getSession();
                if (session instanceof SSHSftpClient client) {
                    if (client.isOpen()) {
                        log.warn("Found open SftpClient session.");
                        IOUtils.closeQuietly(client);
                    }
                }
                result = sessionHolders.remove(sessionHolder);
            }
        } else {
            log.error("WARN SessionHolder Null!!!");
        }
        this.idleSinceTime = System.currentTimeMillis();
        return result;
    }

    protected <T extends SSHSession> boolean releaseSession(T session) {
        synchronized(sessionHolders) {
            SshSessionHolder<T> sessionHolder = findSessionHolder(session);
            return releaseSessionHolder(sessionHolder);
        }
    }

    protected long getIdleTime() {
        // if there is at least one session, just return 0 meaning it's not idle
        synchronized(sessionHolders) {
            for (SshSessionHolder sessionHolder : sessionHolders) {
                if (sessionHolder.getSession() != null) {
                    return 0;
                }
            }
        }

        // if there are no sessions, return the idle time
        return System.currentTimeMillis() - idleSinceTime;
    }

    protected void expireConnection() {
        this.expired = true;
        String msg = MsgUtils.getMsg("SSH_POOL_CONNECTION_EXPIRATION_REQUESTED");
        log.debug(msg);
    }

    protected void close() {
        // Close the SshConnection associated with this context.
        log.trace("Closing SSH connection");
        sshConnection.close();
    }

    protected static SSHExecChannel constructExecChannel(SSHConnection sshConnection) {
        return sshConnection.getExecChannel();
    }

    protected static SSHSftpClient constructSftpClient(SSHConnection sshConnection) throws IOException {
        return sshConnection.getSftpClient();
    }

    protected boolean hasSessionsForThreadId(long threadId) {
        synchronized(sessionHolders) {
            for (SshSessionHolder sessionHolder : sessionHolders) {
                if ((sessionHolder != null) && (threadId == sessionHolder.getThreadId())) {
                    return true;
                }
            }
        }

        return false;
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
        builder.append(", ");
        return builder.toString();
    }
}
