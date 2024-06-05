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
import java.util.HashSet;
import java.util.Iterator;
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
    private final Set<SshSessionHolder<SSHExecChannel>> activeSshSessionHolders;
    private final Set<SshSessionHolder<SSHSftpClient>> activeSftpSessionHolders;
    private final Set<SshSessionHolder<SSHSftpClient>> parkedSftpSessionHolders;
    private final long lifetimeMs;
    private final long maxIdleTimeMs;
    private final long maxSessionLifetime;

    /**
     * ExecChannelConstructor can be used to construct an SSHExecChannel when calling reserveSession
     */
    protected static SessionConstructor<SSHExecChannel> ExecChannelConstructor = SshConnectionContext::constructExecChannel;

    /**
     * SftpClientConstructor can be used to construct an SSHSftpClient when calling reserveSession
     */
    protected static SessionConstructor<SSHSftpClient> SftpClientConstructor = SshConnectionContext::constructSftpClient;

    protected SshConnectionContext(SSHConnection sshConnection, SshSessionPoolPolicy poolPolicy) {
        this.sshConnection = sshConnection;
        this.maxSessions = poolPolicy.getMaxSessionsPerConnection();
        this.creationTime = System.currentTimeMillis();
        this.lifetimeMs = poolPolicy.getMaxConnectionDuration().toMillis();
        this.maxIdleTimeMs = poolPolicy.getMaxConnectionIdleTime().toMillis();
        this.maxSessionLifetime = poolPolicy.getMaxSessionLifetime().toMillis();
        this.idleSinceTime = System.currentTimeMillis();
        this.expired = false;
        activeSshSessionHolders = new HashSet<>();
        activeSftpSessionHolders = new HashSet<>();
        parkedSftpSessionHolders = new HashSet<>();




    }

    protected synchronized int getSessionCount() {
        // include all session holders in the count - even the ones with sessions that are not yet created.
        return (activeSshSessionHolders.size() + activeSftpSessionHolders.size());
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

    // not synchronized.  There's really no reason to synchronize this method, but if the caller is going
    // to make deciesions based on the result (such as reserveSessions) it probably should do this in a
    // synchronized block
    private boolean hasAvailableSessions() {
        if(isExpired()) {
            return false;
        }

        return (activeSshSessionHolders.size() + activeSftpSessionHolders.size()) < maxSessions;
    }

    protected synchronized SshSessionHolder<SSHSftpClient> reserveSftpSession() throws TapisException {
        if (hasAvailableSessions()) {
            SshSessionHolder<SSHSftpClient> sessionHolder = null;
            Iterator<SshSessionHolder<SSHSftpClient>> parkedSessionHolderIterator = parkedSftpSessionHolders.iterator();
            while (parkedSessionHolderIterator.hasNext()) {
                SshSessionHolder<SSHSftpClient> parkedSftpSessionHolder = parkedSessionHolderIterator.next();
                parkedSessionHolderIterator.remove();
                if ((!sessionIsExpired(parkedSftpSessionHolder)) && (parkedSftpSessionHolder.getSession().isOpen())) {
                    sessionHolder = parkedSftpSessionHolder;
                    break;
                } else {
                    IOUtils.closeQuietly(sessionHolder);
                }
            }

            if (sessionHolder == null) {
                try {
                    sessionHolder = new SshSessionHolder<SSHSftpClient>(this, this.sshConnection, SshConnectionContext.SftpClientConstructor);
                } catch (Exception ex) {
                    // if we are unable to create new sessions on this connection, we will expire it
                    this.expireConnection();
                    String msg = MsgUtils.getMsg("SSH_POOL_UNABLE_TO_CREATE_CHANNEL");
                    throw new TapisException(msg, ex);
                }
            }
            activeSftpSessionHolders.add(sessionHolder);
            return sessionHolder;
        }

        return null;
    }

    protected synchronized SshSessionHolder<SSHExecChannel> reserveSshSession() throws TapisException {
        if (hasAvailableSessions()) {
            SshSessionHolder<SSHExecChannel> sessionHolder = null;
            if (sessionHolder == null) {
                try {
                    sessionHolder = new SshSessionHolder<SSHExecChannel>(this, this.sshConnection, SshConnectionContext.ExecChannelConstructor);
                } catch (Exception ex) {
                    // if we are unable to create new sessions on this connection, we will expire it
                    this.expireConnection();
                    String msg = MsgUtils.getMsg("SSH_POOL_UNABLE_TO_CREATE_CHANNEL");
                    throw new TapisException(msg, ex);
                }
            }
            activeSshSessionHolders.add(sessionHolder);
            return sessionHolder;
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
    protected synchronized boolean releaseSessionHolder(SshSessionHolder sessionHolder) {
        boolean result = false;
        if (sessionHolder != null) {
            SSHSession session = sessionHolder.getSession();
            if (session instanceof SSHSftpClient client) {
                if (client.isOpen()) {
                    result = activeSftpSessionHolders.remove(sessionHolder);
                    if(sessionIsExpired(sessionHolder)) {
                        IOUtils.closeQuietly(sessionHolder);
                    } else {
                        parkedSftpSessionHolders.add(sessionHolder);
                    }
                }
            } else {
                result = activeSshSessionHolders.remove(sessionHolder);
            }
        } else {
            log.error("WARN SessionHolder Null!!!");
        }
        this.idleSinceTime = System.currentTimeMillis();
        return result;
    }

    protected synchronized long getIdleTime() {
        // if there is at least one session, just return 0 meaning it's not idle
        for (SshSessionHolder<SSHSftpClient> sessionHolder : activeSftpSessionHolders) {
            if (sessionHolder.getSession() != null) {
                return 0;
            }
        }

        for (SshSessionHolder<SSHExecChannel> sessionHolder : activeSshSessionHolders) {
            if (sessionHolder.getSession() != null) {
                return 0;
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

    protected synchronized void close() {
        // Close the SshConnection associated with this context.
        log.trace("Cleanup parked sessions");
        Iterator<SshSessionHolder<SSHSftpClient>> parkedSessionIterator = parkedSftpSessionHolders.iterator();
        while(parkedSessionIterator.hasNext()) {
            parkedSessionIterator.next();
            parkedSessionIterator.remove();
        }
        log.trace("Closing SSH connection");
        sshConnection.close();
    }

    protected static SSHExecChannel constructExecChannel(SSHConnection sshConnection) {
        return sshConnection.getExecChannel();
    }

    protected static SSHSftpClient constructSftpClient(SSHConnection sshConnection) throws IOException {
        return sshConnection.getSftpClient();
    }

    private <T extends SSHSession> boolean sessionIsExpired(SshSessionHolder<T> sessionHolder) {
        return sessionHolder.getSessionDuration() > maxSessionLifetime;
    }

    protected synchronized void cleanup() {
        Iterator<SshSessionHolder<SSHSftpClient>> activeSessionHolderIterator = parkedSftpSessionHolders.iterator();
        while (activeSessionHolderIterator.hasNext()) {
            SshSessionHolder<SSHSftpClient> sessionHolder = activeSessionHolderIterator.next();
            var session = sessionHolder.getSession();
            if(session == null) {
                activeSessionHolderIterator.remove();
            } else if(!session.isOpen()) {
                // this will ensure the session gets actually closed
                IOUtils.closeQuietly(session);
            }
        }
        Iterator<SshSessionHolder<SSHSftpClient>> parkedSessionHolderIterator = parkedSftpSessionHolders.iterator();
        while (parkedSessionHolderIterator.hasNext()) {
            SshSessionHolder<SSHSftpClient> sessionHolder = parkedSessionHolderIterator.next();
            if(sessionIsExpired(sessionHolder)) {
                parkedSessionHolderIterator.remove();
                IOUtils.closeQuietly(sessionHolder);
            }
        }
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
        builder.append("Active SSH Sessions: ");
        builder.append(activeSshSessionHolders.size());
        builder.append(", ");
        builder.append("Active Sftp Sessions: ");
        builder.append(activeSftpSessionHolders.size());
        builder.append(", ");
        builder.append("Parked Sessions: ");
        builder.append(parkedSftpSessionHolders.size());
        return builder.toString();
    }
}
