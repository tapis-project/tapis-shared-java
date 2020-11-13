package edu.utexas.tacc.tapis.shared.ssh;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.Session;

import java.io.IOException;

public interface ISSHConnection {
    int getChannelCount();

    void returnChannel(Channel channel);

    Channel createChannel(String channelType) throws IOException;

    void closeSession();

    Session getSession();
}
