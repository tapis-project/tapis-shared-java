package edu.utexas.tacc.tapis.shared.ssh;

import java.util.concurrent.TimeUnit;

public class SSHTimeouts 
{
    // Default values;
    public static final int DEFAULT_CONNECT_SECONDS = 15;
    public static final int DEFAULT_AUTHENTICATE_SECONDS = 10;
    public static final int DEFAULT_OPEN_CHANNEL_SECONDS = 15;
    public static final TimeUnit DEFAULT_TIMEUNIT = TimeUnit.SECONDS;
    
    // Timeout fields and their unit.
    private int      _connectTime = DEFAULT_CONNECT_SECONDS;
    private TimeUnit _connectUnits = DEFAULT_TIMEUNIT;
    private int      _authenticateTime = DEFAULT_AUTHENTICATE_SECONDS;
    private TimeUnit _authenticateUnits = DEFAULT_TIMEUNIT;
    private int      _openChannelTime = DEFAULT_OPEN_CHANNEL_SECONDS;
    private TimeUnit _openChannelUnits = DEFAULT_TIMEUNIT;
    
    // Accessors
    public int getConnectTime() {
        return _connectTime;
    }
    public void setConnectTime(int connectTime) {
        this._connectTime = connectTime;
    }
    public TimeUnit getConnectUnits() {
        return _connectUnits;
    }
    public void setConnectUnits(TimeUnit connectUnits) {
        this._connectUnits = connectUnits;
    }
    public int getAuthenticateTime() {
        return _authenticateTime;
    }
    public void setAuthenticateTime(int authenticateTime) {
        this._authenticateTime = authenticateTime;
    }
    public TimeUnit getAuthenticateUnits() {
        return _authenticateUnits;
    }
    public void setAuthenticateUnits(TimeUnit authenticateUnits) {
        this._authenticateUnits = authenticateUnits;
    }
    public int getOpenChannelTime() {
        return _openChannelTime;
    }
    public void setOpenChannelTime(int openChannelTime) {
        this._openChannelTime = openChannelTime;
    }
    public TimeUnit getOpenChannelUnits() {
        return _openChannelUnits;
    }
    public void setOpenChannelUnits(TimeUnit openChannelUnits) {
        this._openChannelUnits = openChannelUnits;
    }

}
