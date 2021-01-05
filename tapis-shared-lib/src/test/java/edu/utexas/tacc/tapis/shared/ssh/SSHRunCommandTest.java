package edu.utexas.tacc.tapis.shared.ssh;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;

import edu.utexas.tacc.tapis.shared.utils.TapisUtils;

/** This test requires sshd to be running on the localhost and that a userid/password
 * be provided when prompted.  The goal is to provide a way to test command execution
 * using ssh.
 * 
 * @author rcardone
 */
@Test(groups = {"interactive"})
public class SSHRunCommandTest 
{
    // Constants.
    private static final String host = "localhost";
    private static final int port = 22;
    
    private static final String command = "echo $USER";
    
    // Interactively assigned fields.
    private String userid;
    private String password;
    
    @BeforeSuite
    public void setUp() {
        // Interactive prompts.
        userid = TapisUtils.getInputFromConsole("Enter userid: ");
        password = TapisUtils.getPasswordFromConsole(userid);
        
        // Strip newline.
        if (userid != null) userid = userid.strip();
        if (password != null) password = password.strip();
    }
    
    @Test
    public void envVarTest() throws IOException, JSchException
    {
        var conn = new SSHConnection(host, port, userid, password);
        
        Channel channel = conn.createChannel("exec");
        ((ChannelExec)channel).setCommand(command);
        channel.setInputStream(null);
        ((ChannelExec)channel).setErrStream(System.err);
        
        InputStream in=channel.getInputStream();
        channel.connect();
        
          while(true){
              byte[] tmp=new byte[1024];
              int i=in.read(tmp, 0, 1024);
              if(i<0)break;
              System.out.print(new String(tmp, 0, i));
          }
          
          if(channel.isClosed()){
            System.out.println("exit-status: "+channel.getExitStatus());
          }
          
        channel.disconnect();
        conn.closeSession();
        System.out.println("DONE");
    }
}
