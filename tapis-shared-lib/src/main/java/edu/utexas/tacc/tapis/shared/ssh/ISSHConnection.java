package edu.utexas.tacc.tapis.shared.ssh;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.Session;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.exceptions.recoverable.TapisRecoverableException;

import java.io.IOException;

public interface ISSHConnection {
    int getChannelCount();

    void returnChannel(Channel channel);

    Channel createChannel(String channelType) throws TapisException, TapisRecoverableException;

    void closeSession();

    Session getSession();
}
