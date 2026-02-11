package edu.utexas.tacc.tapis.shared.ssh;

public final class ConnectionGroupStats {
    private final int connectionCount;
    private final SshConnectionGroup.Status groupStatus;
    private final int expiredConnectionCount;
    private final int activeConnectionCount;
    private final int sessionCount;
    private final int sessionsOnActiveConnections;
    private final int sessionsOnExpiredConnections;
    private final int sessionsOnParkedSftpConnections;

    protected ConnectionGroupStats(int connectionCount, int expiredConnectionCount, int activeConnectionCount, int sessionCount,
                                   int sessionsOnExpiredConnections, int sessionsOnActiveConnections, int sessionsOnParkedSftpConnections,
                                   SshConnectionGroup.Status groupStatus) {
        this.connectionCount = connectionCount;
        this.activeConnectionCount = activeConnectionCount;
        this.expiredConnectionCount = expiredConnectionCount;
        this.sessionCount = sessionCount;
        this.sessionsOnActiveConnections = sessionsOnActiveConnections;
        this.sessionsOnExpiredConnections = sessionsOnExpiredConnections;
        this.sessionsOnParkedSftpConnections = sessionsOnParkedSftpConnections;
        this.groupStatus = groupStatus;
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

    public int getSessionsOnParkedSftpConnections() {
        return sessionsOnParkedSftpConnections;
    }

    public String getGroupStatus() {
        return groupStatus == null ? "null" : groupStatus.toString();
    }
}
