package edu.utexas.tacc.tapis.shared.ssh;

import edu.utexas.tacc.tapis.shared.ssh.apache.SSHConnection;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHSession;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHSftpClient;
import org.apache.sshd.sftp.client.SftpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

/**
 * The name of this class is "SshSessionHolder" because it holds the class called SSHSession.  It's used to
 * hold the spot for an SshSession in the SshSessionPool (specifically in the connection context).  This allows
 * the session context to return a place holder to the pool so that actually establishing the connection can
 * be done outside of any synchronized block of code.
 * @param <T>
 */
class SshSessionHolder <T extends SSHSession> implements Closeable {
    private static Logger log = LoggerFactory.getLogger(SshSessionHolder.class);
    private final SshConnectionContext sshConnectionContext;
    private final SSHConnection sshConnection;
    private final SshConnectionContext.SessionConstructor<T> sessionConstructor;
    private T session = null;
    private Long connectionTime = null;

    public SshSessionHolder(SshConnectionContext sshConnectionContext, SSHConnection sshConnection, SshConnectionContext.SessionConstructor<T> sessionConstructor) {
        this.sshConnectionContext = sshConnectionContext;
        this.sshConnection = sshConnection;
        this.sessionConstructor = sessionConstructor;
    }

    public T getSession() {
        return session;
    }

    public T createSession() throws Exception {
        if(session == null) {
            session = this.sessionConstructor.constructSession(this.sshConnection);
            connectionTime = Long.valueOf(System.currentTimeMillis());
        }
        return session;
    }

    public void close() throws IOException {
        if(session instanceof SSHSftpClient sftpClient) {
            sftpClient.close();
        }
    }

    protected long getSessionDuration() {
        if(connectionTime == null) {
            return 0;
        }

        return System.currentTimeMillis() - connectionTime.longValue();
    }

    public void expireConnection() {
        if(session != null) {
            this.sshConnectionContext.expireConnection();
        }
    }

    public boolean release() {
        boolean released = this.sshConnectionContext.releaseSessionHolder(this);
        return released;
    }
}
