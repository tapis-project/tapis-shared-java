package edu.utexas.tacc.tapis.shared.ssh;

import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHConnection;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHSession;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHSftpClient;
import edu.utexas.tacc.tapis.shared.ssh.apache.SshConnectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The name of this class is "SshSessionHolder" because it holds the class called SSHSession.  It's used to
 * hold the spot for an SshSession in the SshSessionPool (specifically in the connection context).  This allows
 * the session context to return a place holder to the pool so that actually establishing the connection can
 * be done outside of any synchronized block of code.
 * @param <T>
 */
class SshSessionHolder <T extends SSHSession> {
    private static Logger log = LoggerFactory.getLogger(SshSessionHolder.class);
    private final SshConnectionContext sshConnectionContext;
    private final SSHConnection sshConnection;
    private final SshConnectionContext.SessionConstructor<T> sessionConstructor;
    private final long threadId;
    private T session = null;

    private SshConnectionListener connectionListener;

    public SshSessionHolder(SshConnectionContext sshConnectionContext, SSHConnection sshConnection, SshConnectionContext.SessionConstructor<T> sessionConstructor) {
        this.sshConnectionContext = sshConnectionContext;
        this.sshConnection = sshConnection;
        this.sessionConstructor = sessionConstructor;
        this.threadId = Thread.currentThread().getId();
    }

    public boolean containsSession() {
        return session != null;
    }

    public T getSession() {
        return session;
    }

    public T createSession() throws Exception {
        session = this.sessionConstructor.constructSession(this.sshConnection);
        return session;
    }

    public long getThreadId() {
        return threadId;
    }

    public void expireConnection() {
        if(session != null) {
            this.sshConnectionContext.expireConnection();
        }
    }

    public void setConnectionListener(SshConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    public boolean release() {
        if (session instanceof SSHSftpClient sftpSession) {
            try {
                sftpSession.close();
            } catch (Exception ex) {
                // nothing we can really do about this, so just log it.
                String msg = MsgUtils.getMsg("SSH_POOL_UNABLE_TO_CLOSE_SESSION", "SSHSftpClient");
                log.warn(msg);
            }
        }

        boolean released = this.sshConnectionContext.releaseSessionHolder(this);
        this.connectionListener.onRelease(released);
        return released;
    }
}
