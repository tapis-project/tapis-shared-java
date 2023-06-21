package edu.utexas.tacc.tapis.shared.ssh.apache;

public class SSHTimeouts 
{
    // Default values.
    public static final long MAX_WAIT = Long.MAX_VALUE;
    public static final long DEFAULT_CONNECT_MILLIS = 30000;
    public static final long DEFAULT_AUTHENTICATE_MILLIS = 20000;
    public static final long DEFAULT_OPEN_CHANNEL_MILLIS = 20000;
    public static final long DEFAULT_EXECUTION_MILLIS = 600000; // 10 minutes.
    
    // Timed operations.
    private long _connectMillis = DEFAULT_CONNECT_MILLIS;
    private long _authenticateMillis = DEFAULT_AUTHENTICATE_MILLIS;
    private long _openChannelMillis = DEFAULT_OPEN_CHANNEL_MILLIS;
    private long _executionMillis = DEFAULT_EXECUTION_MILLIS;
    
    public long getConnectMillis() {
        return _connectMillis;
    }
    public void setConnectMillis(long connectMillis) {
        this._connectMillis = connectMillis;
    }
    public long getAuthenticateMillis() {
        return _authenticateMillis;
    }
    public void setAuthenticateMillis(long authenticateMillis) {
        this._authenticateMillis = authenticateMillis;
    }
    public long getOpenChannelMillis() {
        return _openChannelMillis;
    }
    public void setOpenChannelMillis(long openChannelMillis) {
        this._openChannelMillis = openChannelMillis;
    }
    public long getExecutionMillis() {
        return _executionMillis;
    }
    public void setExecutionMillis(long executionMillis) {
        this._executionMillis = executionMillis;
    }
}
