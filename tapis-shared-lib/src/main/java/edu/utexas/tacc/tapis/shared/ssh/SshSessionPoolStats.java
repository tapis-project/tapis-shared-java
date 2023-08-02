package edu.utexas.tacc.tapis.shared.ssh;

import java.util.List;

/**
 * Contains statistics about the contents of the Session Pool.
 */
public final class SshSessionPoolStats {

    // Total number of connections (active + expired)
    private int connectionCount = 0;

    // Connections that are expired and waiting for disposal.  These may or may not have open sessions,
    // and will not be disposed until the sessions are closed.
    private int expiredConnectionCount = 0;
    private int activeConnectionCount = 0;
    private int sessionCount = 0;
    private int sessionsOnActiveConnections = 0;
    private int sessionsOnExpiredConnections = 0;

    protected SshSessionPoolStats(List<ConnectionGroupStats> groupStatsList) {
        for(ConnectionGroupStats groupStats : groupStatsList) {
            this.connectionCount += groupStats.getConnectionCount();
            this.activeConnectionCount += groupStats.getActiveConnectionCount();
            this.expiredConnectionCount += groupStats.getExpiredConnectionCount();
            this.sessionCount += groupStats.getSessionCount();
            this.sessionsOnActiveConnections += groupStats.getSessionsOnActiveConnections();
            this.sessionsOnExpiredConnections += groupStats.getSessionsOnExpiredConnections();
        }
    }

    public int getActiveConnectionCount() {
        return activeConnectionCount;
    }

    public int getConnectionCount() {
        return connectionCount;
    }

    public int getExpiredConnectionCount() {
        return expiredConnectionCount;
    }

    public int getSessionCount() {
        return sessionCount;
    }

    public int getSessionsOnExpiredConnections() {
        return sessionsOnExpiredConnections;
    }

    public int getSessionsOnActiveConnections() {
        return sessionsOnActiveConnections;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Connections: ");
        builder.append(connectionCount);
        builder.append(System.lineSeparator());
        builder.append("Expired Connections: ");
        builder.append(expiredConnectionCount);
        builder.append(System.lineSeparator());
        builder.append("Active Connections: ");
        builder.append(activeConnectionCount);
        builder.append(System.lineSeparator());
        builder.append("Sessions: ");
        builder.append(sessionCount);
        builder.append(System.lineSeparator());
        builder.append("Sessions on Expired Connections: ");
        builder.append(sessionsOnExpiredConnections);
        builder.append(System.lineSeparator());
        builder.append("Sessions on Active Connections: ");
        builder.append(sessionsOnActiveConnections);
        builder.append(System.lineSeparator());
        return builder.toString();
    }
}
