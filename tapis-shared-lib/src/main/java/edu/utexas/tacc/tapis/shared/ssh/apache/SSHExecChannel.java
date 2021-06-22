package edu.utexas.tacc.tapis.shared.ssh.apache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumSet;

import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;

import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;

public class SSHExecChannel 
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    private static final int DEFAULT_OUTPUT_LEN = 1024;
    private static final EnumSet<ClientChannelEvent> _closedSet = EnumSet.of(ClientChannelEvent.CLOSED);
    
    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    // The parent connection.
    private final SSHConnection _sshConnection; 
    
    /* ********************************************************************** */
    /*                            Constructors                                */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    SSHExecChannel(SSHConnection sshConnection){_sshConnection = sshConnection;}
    
    /* ********************************************************************** */
    /*                            Public Methods                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* execute:                                                               */
    /* ---------------------------------------------------------------------- */
    public int execute(String cmd) throws IOException
    {
        return execute(cmd, new ByteArrayOutputStream(DEFAULT_OUTPUT_LEN));
    }
    
    /* ---------------------------------------------------------------------- */
    /* execute:                                                               */
    /* ---------------------------------------------------------------------- */
    public int execute(String cmd, ByteArrayOutputStream outErrStream) throws IOException
    {
        return execute(cmd, outErrStream, outErrStream);
    }
    
    /* ---------------------------------------------------------------------- */
    /* execute:                                                               */
    /* ---------------------------------------------------------------------- */
    public int execute(String cmd, ByteArrayOutputStream outStream,
                       ByteArrayOutputStream errStream) 
     throws IOException
    {
        // Check call-specific input.
        if (outStream == null) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "execute", "outStream");
            throw new IOException(msg);
        }
        if (errStream == null) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "execute", "errStream");
            throw new IOException(msg);
        }
        
        // Create and configure the execution channel.
        int exitCode = -1;  // default value when no remote code returned 
        var session  = _sshConnection.getSession();
        if (session == null) {
            String msg =  MsgUtils.getMsg("TAPIS_SSH_NO_SESSION");
            throw new IOException(msg);
        }
        
        // Create the channel.
        ChannelExec channel = session.createExecChannel(cmd);
        channel.setOut(outStream);
        channel.setErr(errStream);
        try {    
            // Open the channel.
            channel.open().verify(_sshConnection.getTimeouts().getConnectMillis());
            
            // Send the command and close its stream.
            try (OutputStream pipedIn = channel.getInvertedIn()) {
                pipedIn.write(cmd.getBytes());
                pipedIn.flush();
            }
            
            // Wait for the channel to close.
            channel.waitFor(_closedSet, _sshConnection.getTimeouts().getExecutionMillis());
            Integer status = channel.getExitStatus();
            if (status != null) exitCode = status;
        } 
        finally {channel.close(true);} // double down by closing immediately
            
        // Return the remote exit code or the default value.
        return exitCode;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getSSHConnection:                                                      */
    /* ---------------------------------------------------------------------- */
    public SSHConnection getSSHConnection() {return _sshConnection;}
}
