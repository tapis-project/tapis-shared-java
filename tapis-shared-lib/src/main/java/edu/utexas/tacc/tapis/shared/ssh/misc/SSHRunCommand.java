package edu.utexas.tacc.tapis.shared.ssh.misc;

import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;

import edu.utexas.tacc.tapis.shared.ssh.SSHConnectionJsch;
import edu.utexas.tacc.tapis.shared.ssh.SSHConnectionLogger;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;

/** This is a test program that can be manually run to check SSH access to a host
 * using the same SSH communication code as Tapis services do. 
 * 
 * To run this program on a host1 to test SSH access to host2, perform the following:
 * 
 *  1. Build the shaded jar using shaded-pom.xml
 *      - cd tapis-shared-java
 *      - mvn install -f tapis-shared-lib/shaded-pom.xml
 *  2. Copy the shaded jar to host1.
 *      - cd tapis-shared-java/tapis-shared-lib/target
 *      - scp shaded-sharedlib.jar 'user@host:/home/user'
 *  3. Copy the current JDK on host1.
 *      - scp openjdk-15.0.1_linux-x64_bin.tar.gz 'user@host:/home/user'
 *  4. Login to host1.
 *  5  Unpack the JDK and put the its bin directory on the PATH
 *  6. Issue the SSH call to host2.
 *      - java -cp shaded-sharedlib.jar edu.utexas.tacc.tapis.shared.ssh.misc.SSHRunCommand host2 user2
 *      - Input user2@host2's password when prompted.
 * 
 * @author rcardone
 */
public class SSHRunCommand 
{
    // Some simple command to run on target host.
    private static final String command = "echo $USER";
    
    // Collect argument and run command.
    public static void main(String[] args) throws Exception 
    {
        // Make sure we got the required arguments.
        // Format:  SSHRunCommand userid password host [port]
        if (args.length < 2) {
            System.out.println(getHelpMessage());
            return;
        }
        
        // Assign required user input.
        String host = args[0];
        String userid = args[1];
        
        // Assign optional user input.
        String password = null;
        if (args.length > 2) password = args[2];
          else password = TapisUtils.getPasswordFromConsole(userid);
        if (StringUtils.isBlank(password)) {
            var msg = getHelpMessage();
            msg += "No password provided.\n";
            System.out.println(msg);
            return;
        }

        // Assign port.
        int port = 22;
        if (args.length > 3) {
            try {port = Integer.parseInt(args[3]);}
                catch (Exception e) {
                    String msg = getHelpMessage();
                    msg += "An invalid port number was specified.\n";
                    System.out.println(msg);
                    return;
                }
        }
        
        // Run the command.
        var cmd = new SSHRunCommand();
        cmd.execute(userid, password, host, port);
    }

    // Run command on host.
    public void execute(String userid, String password, String host, int port) 
     throws Exception
    {
        // Provide a logger when debugging.
        SSHConnectionJsch.setJschLogger(new SSHConnectionLogger());
        var conn = new SSHConnectionJsch(host, port, userid, password);
        
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
    
    // Construct help message.
    private static String getHelpMessage()
    {
        String msg = "ERROR: Missing arguments.\n";
        msg += "Enter:  SSHRunCommand host userid [password] [port]\n\n";
        msg += "If password isn't provided, you will be prompted.\n";
        msg += "The default port is 22 if not provided.\n";
        return msg;
    }
}
