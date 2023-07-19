package edu.utexas.tacc.tapis.shared.ssh;

import java.time.Duration;
import java.util.List;

public class SshSessionPoolPolicy {
    public enum SessionCreationStrategy {
        MINIMIZE_SESSIONS,
        MINIMIZE_CONNECTIONS
    }
    private Duration cleanupInterval;
    private int maxConnectionsPerKey;
    private int maxSessionsPerConnection;
    // Trace during cleanup every Nth iteration.  For example if set to
    // 4, statistics will be dumpted every 4th cleanup cycle.
    private int traceDuringCleanupFrequency;
    private Duration maxConnectionDuration;
    private SessionCreationStrategy sessionCreationStrategy;

    protected SshSessionPoolPolicy() {
        cleanupInterval = Duration.ofSeconds(5);
        maxConnectionsPerKey = 10;
        maxSessionsPerConnection = 5;
        maxConnectionDuration = Duration.ofMinutes(1);
        sessionCreationStrategy = SessionCreationStrategy.MINIMIZE_CONNECTIONS;
        traceDuringCleanupFrequency = 4;
    }

    public int getMaxConnectionsPerKey() {
        return maxConnectionsPerKey;
    }

    /**
     * Sets the maximum number of ssh connection for each key in the pool.
     */
    public SshSessionPoolPolicy setMaxConnectionsPerKey(int maxConnectionsPerKey) {
        this.maxConnectionsPerKey = maxConnectionsPerKey;
        return this;
    }

    public int getMaxSessionsPerConnection() {
        return maxSessionsPerConnection;
    }

    /**
     * sets the maximum number of ssh session for each connection in the pool.
     */
    public SshSessionPoolPolicy setMaxSessionsPerConnection(int maxSessionsPerConnection) {
        this.maxSessionsPerConnection = maxSessionsPerConnection;
        return this;
    }

    public Duration getCleanupInterval() {
        return cleanupInterval;
    }

    /**
     * Sets how frequently the cleanup runs.  The cleanup method will mark connections
     * as expired, and will remove expired connections with no sessions on them.
     */
    public SshSessionPoolPolicy setCleanupInterval(Duration cleanupInterval) {
        this.cleanupInterval = cleanupInterval;
        return this;
    }

    public Duration getMaxConnectionDuration() {
        return maxConnectionDuration;
    }

    /**
     * Sets the max time that a connection will be active.  After this time, it will be marked expired,
     * and when there are no sessions left on it, the cleanup method will remove it.
     */
    public SshSessionPoolPolicy setMaxConnectionDuration(Duration maxConnectionDuration) {
        this.maxConnectionDuration = maxConnectionDuration;
        return this;
    }

    public SessionCreationStrategy getSessionCreationStrategy() {
        return sessionCreationStrategy;
    }

    /**
     * Sets the creation strategy:
     *
     * MINIMIZE_CONNECCTIONS will open the maximum number of sessions on all open connections before trying
     * to open a new connections.
     *
     * MINIMIZE_SESSIONS will first look for a connection with no session, and if found it will use that.  If
     * there are no connections with 0 sessions, it will create a new connection and open a session on that
     * (only if we have connections left that can be created).  If that doesnt result in a session it will
     * look for the connection with the minimum number of sessions and create a session on that.
     *
     * Expired connections will never be used to create new sessions regardless of creation strategy
     */
    public SshSessionPoolPolicy setSessionCreationStrategy(SessionCreationStrategy sessionCreationStrategy) {
        this.sessionCreationStrategy = sessionCreationStrategy;
        return this;
    }

    public static SshSessionPoolPolicy defaultPolicy() {
        return new SshSessionPoolPolicy();
    }

    public SshSessionPoolPolicy setTraceDuringCleanupFrequency(int traceDuringCleanupFrequency) {
        this.traceDuringCleanupFrequency = traceDuringCleanupFrequency;
        return this;
    }

    public int getTraceDuringCleanupFrequency() {
        return traceDuringCleanupFrequency;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Max Connections per Key: ");
        builder.append(maxConnectionsPerKey);
        builder.append(System.lineSeparator());
        builder.append("Max Sessions per Connection: ");
        builder.append(maxSessionsPerConnection);
        builder.append(System.lineSeparator());
        builder.append("Max Connection Lifetime: ");
        builder.append(maxConnectionDuration);
        builder.append(System.lineSeparator());
        builder.append("Cleanup Interval: ");
        builder.append(cleanupInterval);
        builder.append(System.lineSeparator());
        builder.append("Session CreationStrategy: ");
        builder.append(sessionCreationStrategy);
        builder.append(System.lineSeparator());
        builder.append("Trace During Cleanup Frequency: ");
        builder.append(traceDuringCleanupFrequency);
        builder.append(System.lineSeparator());
        return builder.toString();
    }
}
