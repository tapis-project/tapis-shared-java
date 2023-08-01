package edu.utexas.tacc.tapis.shared.ssh;

public final class ConnectionGroupStats {
    private final int connectionCount;
    private final int expiredConnectionCount;
    private final int activeConnectionCount;
    private final int sessionCount;
    private final int sessionsOnActiveConnections;
    private final int sessionsOnExpiredConnections;

    protected ConnectionGroupStats(int connectionCount, int expiredConnectionCount, int activeConnectionCount, int sessionCount, int sessionsOnExpiredConnections, int sessionsOnActiveConnections) {
        this.connectionCount = connectionCount;
        this.activeConnectionCount = activeConnectionCount;
        this.expiredConnectionCount = expiredConnectionCount;
        this.sessionCount = sessionCount;
        this.sessionsOnActiveConnections = sessionsOnActiveConnections;
        this.sessionsOnExpiredConnections = sessionsOnExpiredConnections;
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

}
