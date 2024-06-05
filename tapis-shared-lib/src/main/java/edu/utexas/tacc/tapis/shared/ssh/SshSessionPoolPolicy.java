package edu.utexas.tacc.tapis.shared.ssh;

import java.time.Duration;

public final class SshSessionPoolPolicy {

    /**
     * The number of connections per key.  A Key represents a unique combination of host/port/credentials.  We will never
     * exceed this number of connections for any combination of host/port/credential.
     *
     */
    private int DEFAULT_MAX_CONNECTIONS_PER_KEY = 10;

    /**
     * Max sessions per connection.  We will never exceed this number of sessions for a single connection.  The default
     * max sessions for sshd is 10, however I have noticed that we will get errors setting this to 10 for some reason.
     * I've tested a lot with up to 7 and that seems to be ok.
     */
    private int DEFAULT_MAX_SESSIONS_PER_CONNECTIONS = 5;

    /**
     * The maximum lifetime of a connection.  What this means is that if a connection is in the pool and is
     * older than this, new sessions will not be created on the connection.  When the last connection is closed
     * for the connection, the connection will be destroyed.  This is just the time from when it was added to
     * the pool, not how long it's been idle.
     *
     * This has an impact on how many new connections are made vs how often we reuse a connection.  A lot of the
     * details of this are hidden away in the sshclient (apache code), but in general longer duration means we leave
     * connections open longer, and shorter duration means we close them more frequently.
     */
    private Duration DEFAULT_MAX_CONNECTION_LIFETIME = Duration.ofHours(1);
    private Duration DEFAULT_MAX_SESSION_LIFETIME = Duration.ofMinutes(1);

    /**
     * The maximum time that a connection can be idle - i.e. have no open session on it - before we will remove it
     * from the pool.  Unless you really need to keep a connection around for a long time, you should probably
     * use idle time instead of duration, and set max connection lifetime to something large.  If either the
     * idle time is up or the max connection lifetime is up for a connection, it will be removed from the pool.
     *
     * You could set the max time to something like 4 hours, and the idle time to 10 mins.  This would mean that
     * if the connection doesn't get used for 10 mins it will be removed from the pool, either way if it ever
     * gets to be 4 hours old, it will be removed.  (removals are always after all sessions are closed - if
     * either timeout occurs, it will not be used for new sessions, and removed from the pool after all sessions
     * have been returned.
     */
    private Duration DEFAULT_MAX_CONNECTION_IDLE_TIME = Duration.ofMinutes(10);

    /**
     * Specifies how long between cleanup runs.  Cleanup should be run relatively frequently.  If you frequently
     * see expired connections with no session on them in the logs, you should decrease the duration (make it run
     * more frequently)
     */
    private Duration DEFAULT_CLEANUP_INTERVAL = Duration.ofSeconds(30);
    private int DEFAULT_TRACE_DURING_CLEANUP_FREQUENCY = 20;
    private Duration cleanupInterval;
    private int maxConnectionsPerKey;
    private int maxSessionsPerConnection;
    // Trace during cleanup every Nth iteration.  For example if set to
    // 4, statistics will be dumpted every 4th cleanup cycle.
    private int traceDuringCleanupFrequency;
    private Duration maxConnectionDuration;
    private Duration maxConnectionIdleTime;
    private Duration maxSessionLifetime;

    protected SshSessionPoolPolicy() {
        cleanupInterval = DEFAULT_CLEANUP_INTERVAL;
        maxConnectionsPerKey = DEFAULT_MAX_CONNECTIONS_PER_KEY;
        maxSessionsPerConnection = DEFAULT_MAX_SESSIONS_PER_CONNECTIONS;
        maxConnectionDuration = DEFAULT_MAX_CONNECTION_LIFETIME;
        maxConnectionIdleTime = DEFAULT_MAX_CONNECTION_IDLE_TIME;
        traceDuringCleanupFrequency = DEFAULT_TRACE_DURING_CLEANUP_FREQUENCY;
        maxSessionLifetime = DEFAULT_MAX_SESSION_LIFETIME;
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

    public Duration getMaxSessionLifetime() {
        return maxSessionLifetime;
    }

    public SshSessionPoolPolicy setMaxSessionLifetime(Duration maxSessionLifetime) {
        this.maxSessionLifetime = maxSessionLifetime;
        return this;
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

    public Duration getMaxConnectionIdleTime() {
        return maxConnectionIdleTime;
    }

    public SshSessionPoolPolicy setMaxConnectionIdleTime(Duration maxConnectionIdleTime) {
        this.maxConnectionIdleTime = maxConnectionIdleTime;
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
        builder.append("Max Session Lifetime: ");
        builder.append(maxSessionLifetime);
        builder.append(System.lineSeparator());
        builder.append("Max Connection Idle Time: ");
        builder.append(maxConnectionIdleTime);
        builder.append(System.lineSeparator());
        builder.append("Cleanup Interval: ");
        builder.append(cleanupInterval);
        builder.append(System.lineSeparator());
        builder.append(System.lineSeparator());
        builder.append("Trace During Cleanup Frequency: ");
        builder.append(traceDuringCleanupFrequency);
        builder.append(System.lineSeparator());
        return builder.toString();
    }
}
