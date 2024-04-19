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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents all connections and sessions for a given key in the pool.
 */
final class SshConnectionGroup {
    private static final Logger log = LoggerFactory.getLogger(SshConnectionGroup.class);

    // report this group as "not ready for cleanup" for 30 mins after the last time
    // we "touched" it.  We always touch it just prior to creating a new session/connection
    // this prevents us from inadvertently cleaning up the connection group while we are
    // waiting to make a connection if there are currently no connections.
    private static final long LAST_TOUCHED_THRESHOLD = Duration.ofMinutes(30).toMillis();
    private List<SshConnectionContext> connectionContextList;
    private SshSessionPoolPolicy poolPolicy;
    private long lastTouched;
    protected SshConnectionGroup(SshSessionPool pool, SshSessionPoolPolicy poolPolicy) {
        connectionContextList = new ArrayList<>();
        this.poolPolicy = poolPolicy;
        lastTouched = System.currentTimeMillis();
    }

    public synchronized void touch() {
        lastTouched = System.currentTimeMillis();
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
     * Releases an ssh sessionHolder (used to free up failed sessions)
     *
     * @param sessionHolder sessionHolder to be released
     * @return true if it could be release, false if not.
     */
    protected <T extends SSHSession> boolean releaseSessionHolder(SshSessionHolder<T> sessionHolder) {
        synchronized(connectionContextList) {
            SshConnectionContext connectionContext = findConnectionContext(sessionHolder);
            if (connectionContext != null) {
                if(connectionContext.releaseSessionHolder(sessionHolder)) {
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

    /**
     * Releases an ssh session (in this case SSHExecChannel).
     *
     * @param session session to be released
     * @return true if it could be release, false if not.
     */
    protected boolean releaseSession(SSHExecChannel session) {
        synchronized(connectionContextList) {
            SshConnectionContext connectionContext = findConnectionContext(session);
            if (connectionContext != null) {
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

    /**
     * Releases an ssh session (in this case SSHSftpClient).
     *
     * @param session session to be released
     * @return true if it could be release, false if not.
     */
    protected boolean releaseSession(SSHSftpClient session) {
        synchronized(connectionContextList) {
            SshConnectionContext connectionContext = findConnectionContext(session);
            if (connectionContext != null) {
                try {
                    session.close();
                } catch (Exception ex) {
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

    protected SshConnectionContext findConnectionContext(SshSessionHolder sessionHolder) {
        // the callers already synchronize on connectionContextList, so we don't need to do that here.
        synchronized (connectionContextList) {
            for (SshConnectionContext connectionContext : connectionContextList) {
                if (connectionContext.containsSessionHolder(sessionHolder)) {
                    return connectionContext;
                }
            }
        }
        return null;
    }

    protected <T extends SSHSession> SshConnectionContext findConnectionContext(T session) {
        // the callers already synchronize on connectionContextList, so we don't need to do that here.
        synchronized (connectionContextList) {
            for (SshConnectionContext connectionContext : connectionContextList) {
                if (connectionContext.containsSession(session)) {
                    return connectionContext;
                }
            }
        }
        return null;
    }

    protected void cleanup() {
        synchronized (connectionContextList) {
            List<SshConnectionContext> contextsToRemove = new ArrayList<>();
            for (SshConnectionContext connectionContext : connectionContextList) {
                // if the connection is expired, and there are no sessions left on it, close the connection,
                // and add it to the list of connections to remove from the group.
                if ((connectionContext.isExpired()) && (connectionContext.getSessionCount() == 0)) {
                    connectionContext.close();
                    contextsToRemove.add(connectionContext);
                }
            }

            // remove all connections that were identified above.
            connectionContextList.removeAll(contextsToRemove);
        }
    }

    protected boolean isReadyForCleanup() {
        // if this group is recently touched, dont report it as empty.  This will keep us
        // from cleaning it up while we are trying to create a connection on it.
        if((System.currentTimeMillis() - lastTouched) < LAST_TOUCHED_THRESHOLD) {
            return false;
        }

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
        long startTime = System.currentTimeMillis();
        long phaseStartTime = startTime;
        long abortTime = System.currentTimeMillis() + wait.toMillis();

        checkForPotentialDeadlocksAndLog();

        // NOTE:  It's not impossible that you could have a situation where all connections are expired,
        // but they still have active sessions.  Hopefully sessions will be relatively short lived, and one
        // of the connections will be cleaned up, and then new ones can be created.  If that becomes a problem,
        // we may have to come up with a better way to handle expired connections.
        SshSessionHolder<T> sessionHolder = null;
        while (sessionHolder == null) {
            synchronized (connectionContextList) {
                // clear out any expired connections
                log.trace(String.format("Wait for lock time: %d", System.currentTimeMillis() - phaseStartTime));

                cleanup();
                log.trace(String.format("Cleanup time: %d", System.currentTimeMillis() - phaseStartTime));
                phaseStartTime = System.currentTimeMillis();

                switch (poolPolicy.getSessionCreationStrategy()) {
                    case MINIMIZE_SESSIONS -> sessionHolder = getSessionMinimizingSessions(tenant, host, port, effectiveUserId,
                            authnMethod, credential, sessionConstructor);
                    case MINIMIZE_CONNECTIONS -> sessionHolder = getSessionMinimizingConnections(tenant, host, port, effectiveUserId,
                            authnMethod, credential, sessionConstructor);
                }

                log.trace(String.format("Session search time: %d", System.currentTimeMillis() - phaseStartTime));
                phaseStartTime = System.currentTimeMillis();
                if (sessionHolder == null) {
                    if (hasAvailableSessions()) {
                        // in this case, there are session slots available to reserve, but we didn't get a session
                        // for some reason.  This means we must notify again so that some other thread can try it.
                        // notify gives up the montitor though, so in this case we must continue the while loop.
                        connectionContextList.notify();
                        log.warn("Reserved slot for session but unable to create the session");
                        continue;
                    }
                    long waitTime = abortTime - System.currentTimeMillis();
                    if (waitTime > 0) {
                        try {
                            log.trace("Waiting for a session");
                            connectionContextList.wait(waitTime);
                        } catch (InterruptedException ex) {
                            String msg = MsgUtils.getMsg("SSH_POOL_RESERVE_TIMEOUT_INTERRUPTED",
                                    tenant, host, port, effectiveUserId, authnMethod, wait);
                            log.warn(msg);
                            break;
                        }
                    } else {
                        log.trace("Wait complete - session was " + (sessionHolder == null ? "NOT" : "") + " found");
                        break;
                    }
                }
            }
        }

        // by now we should have a session holder - if not, we can assume we timed out.
        if (sessionHolder == null) {
            log.debug(String.format("Could not get session holder: %d", System.currentTimeMillis() - phaseStartTime));
            log.trace(String.format("Total elapsed time: %d", System.currentTimeMillis() - startTime));
            String msg = MsgUtils.getMsg("SSH_POOL_RESERVE_TIMEOUT", tenant, host, port, effectiveUserId, authnMethod, wait);
            log.warn(msg);
            throw new TapisException(msg);
        }
        log.trace(String.format("Reserved session holder time: %d", System.currentTimeMillis() - phaseStartTime));
        phaseStartTime = System.currentTimeMillis();

        // We have a session holder, but we need to make an actual session now.
        T session = null;

        // I'm leaving the comment below to remind myself of this stuff if it should happen again.  I believe it
        // is fixed.  The fix is in the SftpClient.close() method.  We are now requesting a close, and confirming
        // that it happens before moving on.
        // (old_comment) Sometimes sessions fail.  The symptom is that /var/log/secure on the server side will have
        // (old_comment) a message that says - error: no more sessions.  We are already managing our sessions, so
        // (old_comment) this really shouldn't be the case, but maybe the server hasn't caught up with it's cleanup.
        // (old_comment) Perhaps due to the server being under load.  We will retry a few  times before really giving up.
        int tries = 0;
        while(session == null) {
            try {
                tries ++;
                session = sessionHolder.createSession();
                if (session == null) {
                    log.error("SshSession was null");
                    Thread.sleep(100);
                } else {
                    log.trace(String.format("Session established time: %d", System.currentTimeMillis() - phaseStartTime));
                    phaseStartTime = System.currentTimeMillis();
                    break;
                }
            } catch (Exception ex) {
                if(tries >= 9) {
                    String msg = MsgUtils.getMsg("SSH_POOL_UNABLE_TO_CREATE_CHANNEL");
                    throw new TapisException(msg, ex);
                }

                // if we are unable to create new sessions on this connection, we will expire it
                if (sessionHolder != null) {
                    sessionHolder.expireConnection();
                    releaseSessionHolder(sessionHolder);
                }
            }
        }
        log.trace(String.format("Total elapsed time: %d", System.currentTimeMillis() - startTime));

        return session;
    }

    private <T extends SSHSession> SshSessionHolder<T> getSessionMinimizingConnections(String tenant, String host, Integer port,
            String effectiveUserId, AuthnEnum authnMethod, Credential credential,
            SshConnectionContext.SessionConstructor<T> sessionConstructor) throws TapisException {
        SshSessionHolder<T> sessionHolder = null;
        for (SshConnectionContext sshConnectionContext : connectionContextList) {
            if (sshConnectionContext.hasAvailableSessions()) {
                sessionHolder = sshConnectionContext.reserveSession(sessionConstructor);
                if (sessionHolder != null) {
                    break;
                }
            }
        }

        if ((sessionHolder == null) && (connectionContextList.size() < poolPolicy.getMaxConnectionsPerKey())) {
            SSHConnection sshConnection = createNewConnection(tenant, host, port, effectiveUserId, authnMethod, credential);
            SshConnectionContext sshConnectionContext = new SshConnectionContext(sshConnection,
                    poolPolicy.getMaxSessionsPerConnection(), poolPolicy.getMaxConnectionDuration(),
                    poolPolicy.getMaxConnectionIdleTime());
            connectionContextList.add(sshConnectionContext);
            sessionHolder = sshConnectionContext.reserveSession(sessionConstructor);
        }

        return sessionHolder;
    }

    private <T extends SSHSession> SshSessionHolder<T> getSessionMinimizingSessions(String tenant,
            String host, Integer port, String effectiveUserId, AuthnEnum authnMethod, Credential credential,
            SshConnectionContext.SessionConstructor<T> sessionConstructor) throws TapisException {
        SshSessionHolder<T> sessionHolder = null;

        // first try and find a conneciton that has no sessions
        SshConnectionContext contextToUse = connectionContextList.stream().filter(context -> {
            return context.getSessionCount() == 0;
        }).findFirst().orElse(null);
        if(contextToUse != null) {
            sessionHolder = contextToUse.reserveSession(sessionConstructor);
        }

        // If there are no connections with zero sessions, create a new connection if possible, and make
        // a session on that.
        if(sessionHolder == null) {
            SshConnectionContext sshConnectionContext = null;
            if ((connectionContextList.size() < poolPolicy.getMaxConnectionsPerKey())) {
                SSHConnection sshConnection = createNewConnection(tenant, host, port, effectiveUserId, authnMethod, credential);
                sshConnectionContext = new SshConnectionContext(sshConnection,
                        poolPolicy.getMaxSessionsPerConnection(), poolPolicy.getMaxConnectionDuration(),
                        poolPolicy.getMaxConnectionIdleTime());
                connectionContextList.add(sshConnectionContext);
                sessionHolder = sshConnectionContext.reserveSession(sessionConstructor);
            }
        }

        // if we still dont have a session, just find the connection with the minimum number of sessions, and
        // create the session there (if it can have one).
        if(sessionHolder == null) {
            synchronized (connectionContextList) {
                // look for the connection with the minimum number of sessions;
                contextToUse = connectionContextList.stream().min((contextOne, contextTwo) -> {
                    return NumberUtils.compare(contextOne.getSessionCount(), contextTwo.getSessionCount());
                }).get();
            }

            if ((contextToUse != null) && (contextToUse.hasAvailableSessions())) {
                sessionHolder = contextToUse.reserveSession(sessionConstructor);
            }
        }

        return sessionHolder;
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
    private boolean hasAvailableSessions() {
        synchronized (connectionContextList) {
            for(SshConnectionContext connectionContext : connectionContextList) {
                if(connectionContext.hasAvailableSessions()) {
                    return true;
                }
            }
        }

        return false;
    }

    // this method can provide a way to help find deadlocks, however it can have false
    // positives also.  The specific case we are looking for is when the same thread
    // asks for two sessions on the same pool key.  In heavy load it's a potential
    // deadlock situation.  If many threads do this, and they are all waiting for the second
    // session, no one can give up the first one (until the second pool wait times out).  This
    // is unlikely to deadlock, but possible - we should log the situation and avoid if possible.
    private void checkForPotentialDeadlocksAndLog() {
        synchronized (connectionContextList) {
            for (SshConnectionContext connectionContext : connectionContextList) {
                if (connectionContext.hasSessionsForThreadId(Thread.currentThread().getId())) {
                    log.warn("POTENTIAL DEADLOCK:  This thread already has a connection");
                    break;
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        synchronized (connectionContextList) {
            for (SshConnectionContext connectionContext : connectionContextList) {
                builder.append(connectionContext.toString());
                builder.append(System.lineSeparator());
            }
        }

        return builder.toString();
    }
}
