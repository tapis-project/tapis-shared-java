package edu.utexas.tacc.tapis.shared.ssh.apache;

public interface SshConnectionListener {
    public void onRelease(boolean succeeded);
}
