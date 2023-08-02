package edu.utexas.tacc.tapis.shared.ssh;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.exceptions.recoverable.TapisRecoverableException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHConnection;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHExecChannel;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHSession;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHSftpClient;
import edu.utexas.tacc.tapis.systems.client.gen.model.AuthnEnum;
import edu.utexas.tacc.tapis.systems.client.gen.model.Credential;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents all connections and sessions for a given key in the pool.
 */
final class SshConnectionGroup {
    private static final Logger log = LoggerFactory.getLogger(SshConnectionGroup.class);

    private List<SshConnectionContext> connectionContextList;
    private SshSessionPoolPolicy poolPolicy;

    protected SshConnectionGroup(SshSessionPool pool, SshSessionPoolPolicy poolPolicy) {
        connectionContextList = new ArrayList<>();
        this.poolPolicy = poolPolicy;
    }

    /**
     * Returns information about this connection group.
     */
    public ConnectionGroupStats getGroupStats() {
        int connectionCount = 0;
        int expiredConnectionCount = 0;
        int activeConnectionCount = 0;
        int sessionCount = 0;
        int sessionsOnExpiredConnections = 0;
        int sessionsOnActiveConnections = 0;

        synchronized (connectionContextList) {
            for (SshConnectionContext context : connectionContextList) {
                if (context.isExpired()) {
                    expiredConnectionCount++;
                    sessionsOnExpiredConnections += context.getSessionCount();
                } else {
                    activeConnectionCount++;
                    sessionsOnActiveConnections += context.getSessionCount();
                }
                connectionCount++;
                sessionCount += context.getSessionCount();
            }
        }

       return new ConnectionGroupStats(connectionCount, expiredConnectionCount, activeConnectionCount,
                sessionCount, sessionsOnExpiredConnections, sessionsOnActiveConnections);
    }

    /**
     * Releases an ssh session (in this case SSHExecChannel).
     *
     * @param session session to be released
     * @return true if it could be release, false if not.
     */
    public boolean releaseSession(SSHExecChannel session) {
        synchronized(connectionContextList) {
            SshConnectionContext connectionContext = findConnectionContext(session);
            if (connectionContext != null) {
                connectionContext.releaseSession(session);
                connectionContextList.notify();
                return true;
            }
        }

        return false;
    }

    /**
     * Releases an ssh session (in this case SSHSftpClient).
     *
     * @param session session to be released
     * @return true if it could be release, false if not.
     */
    public boolean releaseSession(SSHSftpClient session) {
        synchronized(connectionContextList) {
            SshConnectionContext connectionContext = findConnectionContext(session);
            if (connectionContext != null) {
                try {
                    session.close();
                } catch (IOException ex) {
                    // nothing we can really do about this, so just log it.
                    String msg = MsgUtils.getMsg("SSH_POOL_UNABLE_TO_CLOSE_SESSION", "SSHSftpClient");
                    log.warn(msg);
                }
                if(connectionContext.releaseSession(session)) {
                    log.trace("Session released, notifying");
                } else {
                    // we will still notify in this case.  worst case is that a thread will wake and relaize it's
                    // not able to get a session, and go back to waiting.  No harm done.
                    log.trace("Unable to release session, notifying");
                }
                connectionContextList.notify();
                return true;
            }
        }

        return false;
    }

    protected SshConnectionContext findConnectionContext(SSHExecChannel session) {
        // the callers already synchronize on connectionContextList, so we don't need to do that here.
        for(SshConnectionContext connectionContext : connectionContextList) {
            if(connectionContext.containsChannel(session)) {
                return connectionContext;
            }
        }
        return null;
    }

    protected SshConnectionContext findConnectionContext(SSHSftpClient session) {
        // the callers already synchronize on connectionContextList, so we don't need to do that here.
        for(SshConnectionContext connectionContext : connectionContextList) {
            if(connectionContext.containsChannel(session)) {
                return connectionContext;
            }
        }
        return null;
    }

    protected void cleanup() {
        synchronized (connectionContextList) {
            connectionContextList.removeIf(sessionContext -> {
                return ((sessionContext.isExpired()) && (sessionContext.getSessionCount() == 0));
            });
        }
    }

    protected boolean isEmpty() {
        return connectionContextList.isEmpty();
    }

    /**
     * Reserve a session on a connection (connection is determined internally).
     *
     * @param host
     * @param port
     * @param effectiveUserId
     * @param authnMethod
     * @param credential
     * @param sessionConstructor lambda expression describing how to construct the session
     * @param wait how long to wait for a reservation on a connection before giving up.
     * @return
     * @param <T>
     * @throws TapisException
     */
    protected  <T extends SSHSession> T reserveSessionOnConnection(String tenant, String host, Integer port, String effectiveUserId,
                                             AuthnEnum authnMethod, Credential credential,
                                             SshConnectionContext.SessionConstructor<T> sessionConstructor,
                                             Duration wait) throws TapisException {
        long abortTime = System.currentTimeMillis() + wait.toMillis();

        // NOTE:  It's not impossible that you could have a situation where all connections are expired,
        // but they still have active sessions.  Hopefully sessions will be relatively short lived, and one
        // of the connections will be cleaned up, and then new ones can be created.  If that becomes a problem,
        // we may have to come up with a better way to handle expired connections.
        T session = null;
        while(session == null) {
            synchronized (connectionContextList) {
                // clear out any expired connections
                cleanup();
                switch(poolPolicy.getSessionCreationStrategy()) {
                    case MINIMIZE_SESSIONS -> session = getSessionMinimizingSessions(tenant, host, port, effectiveUserId,
                            authnMethod, credential, sessionConstructor);
                    case MINIMIZE_CONNECTIONS -> session = getSessionMinimizingConnections(tenant, host, port, effectiveUserId,
                            authnMethod, credential, sessionConstructor);
                }

                long waitTime = abortTime - System.currentTimeMillis();
                if ((session == null) && (waitTime > 0)) {
                    try {
                        log.trace("Waiting for a session");
                        connectionContextList.wait(waitTime);
                        log.trace("Wait complete - session was " + (session == null ? "NOT" : "") + " found");
                    } catch (InterruptedException ex) {
                        String msg = MsgUtils.getMsg("SSH_POOL_RESERVE_TIMEOUT_INTERRUPTED",
                                tenant, host, port, effectiveUserId, authnMethod, wait);
                        log.warn(msg);
                        break;
                    }
                } else {
                    break;
                }
            }
        }

        if(session == null) {
            String msg = MsgUtils.getMsg("SSH_POOL_RESERVE_TIMEOUT", tenant, host, port, effectiveUserId, authnMethod, wait);
            log.warn(msg);
            throw new TapisException(msg);
        }

        return session;
    }

    private <T extends SSHSession> T getSessionMinimizingConnections(String tenant, String host, Integer port,
            String effectiveUserId, AuthnEnum authnMethod, Credential credential,
            SshConnectionContext.SessionConstructor<T> sessionConstructor) throws TapisException {
        T session = null;
        for (SshConnectionContext sshConnectionContext : connectionContextList) {
            if (sshConnectionContext.hasAvailableSessions()) {
                session = sshConnectionContext.reserveSession(sessionConstructor);
                if (session != null) {
                    break;
                }
            }
        }

        if ((session == null) && (connectionContextList.size() < poolPolicy.getMaxConnectionsPerKey())) {
            SSHConnection sshConnection = createNewConnection(tenant, host, port, effectiveUserId, authnMethod, credential);
            SshConnectionContext sshConnectionContext = new SshConnectionContext(sshConnection,
                    poolPolicy.getMaxSessionsPerConnection(), poolPolicy.getMaxConnectionDuration(),
                    poolPolicy.getMaxConnectionIdleTime());
            connectionContextList.add(sshConnectionContext);
            session = sshConnectionContext.reserveSession(sessionConstructor);
        }

        return session;
    }

    private <T extends SSHSession> T getSessionMinimizingSessions(String tenant,
            String host, Integer port, String effectiveUserId, AuthnEnum authnMethod, Credential credential,
            SshConnectionContext.SessionConstructor<T> sessionConstructor) throws TapisException {
        T session = null;

        // first try and find a conneciton that has no sessions
        SshConnectionContext contextToUse = connectionContextList.stream().filter(context -> {
            return context.getSessionCount() == 0;
        }).findFirst().orElse(null);
        if(contextToUse != null) {
            session = contextToUse.reserveSession(sessionConstructor);
        }

        // If there are no connections with zero sessions, create a new connection if possible, and make
        // a session on that.
        if(session == null) {
            SshConnectionContext sshConnectionContext = null;
            if ((connectionContextList.size() < poolPolicy.getMaxConnectionsPerKey())) {
                SSHConnection sshConnection = createNewConnection(tenant, host, port, effectiveUserId, authnMethod, credential);
                sshConnectionContext = new SshConnectionContext(sshConnection,
                        poolPolicy.getMaxSessionsPerConnection(), poolPolicy.getMaxConnectionDuration(),
                        poolPolicy.getMaxConnectionIdleTime());
                connectionContextList.add(sshConnectionContext);
                session = sshConnectionContext.reserveSession(sessionConstructor);
            }
        }

        // if we still dont have a session, just find the connection with the minimum number of sessions, and
        // create the sessio there (if it can have one).
        if(session == null) {
            // look for the connection with the minimum number of sessions;
            contextToUse = connectionContextList.stream().min((contextOne, contextTwo) -> {
                return NumberUtils.compare(contextOne.getSessionCount(), contextTwo.getSessionCount());
            }).get();

            if ((contextToUse != null) && (contextToUse.hasAvailableSessions())) {
                session = contextToUse.reserveSession(sessionConstructor);
            }
        }

        return session;
    }

    protected SSHConnection createNewConnection(String tenant, String host, Integer port, String effectiveUserId,
                                                AuthnEnum authnMethod, Credential credential)
            throws TapisException
    {
        // check connection details for non-empty values
        if((StringUtils.isBlank(host))
                || (port == null)
                || (StringUtils.isBlank(effectiveUserId))
                || (authnMethod == null)) {
            String msg = MsgUtils.getMsg("SSH_POOL_MISSING_CONNECTION_INFORMATION",
                    tenant, host, port, effectiveUserId, authnMethod);
            log.warn(msg);
            throw new TapisException(msg);
        }

        // check credentials for non-empty values
        if(credential == null) {
            String msg = MsgUtils.getMsg("SSH_POOL_MISSING_CREDENTIALS",
                    tenant, host, port, effectiveUserId, authnMethod);
            log.warn(msg);
            throw new TapisException(msg);
        }

        // We currently only use two types of authn for target systems.
        if (authnMethod != AuthnEnum.PASSWORD &&
                authnMethod != AuthnEnum.PKI_KEYS)
        {
            String msg = MsgUtils.getMsg("SSH_POOL_UNSUPPORTED_AUTHN_METHOD",
                    tenant, host, port, effectiveUserId, authnMethod);
            log.warn(msg);
            throw new TapisException(msg);
        }

        // Connect.
        SSHConnection conn = null;
        try {
            if (authnMethod == AuthnEnum.PASSWORD) {
                conn = new SSHConnection(host, port,
                        effectiveUserId, credential.getPassword());
            } else {
                conn = new SSHConnection(host, port,
                        effectiveUserId,
                        credential.getPublicKey(), credential.getPrivateKey());
            }
        } catch (TapisRecoverableException e) {
            // Handle recoverable exceptions, let non-recoverable ones through.
            // We add the systemId to all recoverable exceptions.
            e.state.put("host", host);
            e.state.put("port", port.toString());
            e.state.put("effectiveUserId", effectiveUserId);
            throw e;
        }

        // Non-null if we get here.
        return conn;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for(SshConnectionContext connectionContext : connectionContextList) {
            builder.append(connectionContext.toString());
            builder.append(System.lineSeparator());
        }

        return builder.toString();
    }
}
