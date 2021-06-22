package edu.utexas.tacc.tapis.shared.ssh.apache.misc;

import java.io.ByteArrayOutputStream;

import org.apache.commons.lang3.StringUtils;

import edu.utexas.tacc.tapis.shared.ssh.apache.SSHConnection;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;

/** This test program is bundled with the non-test code for ease of use with
 * the standard shaded jar file.  It's main utility is to test that simple
 * password access to a host is possible using SSH and, in particular, that
 * MFA exemption is in effect.  The command format is:
 * 
 *      ApacheRunCommand host userid [password] [port]
 *   
 * If you specify only the host and userid, you will be prompted for the
 * password without displaying your keystrokes.  The port defaults to 22
 * if it's not provided.  
 * 
 * This program runs a simple, hardcoded command and writes the exitcode 
 * and response to standard out.
 * 
 * Installation and Execution
 * --------------------------
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
 *      - java -cp shaded-sharedlib.jar eedu.utexas.tacc.tapis.shared.ssh.apache.misc.ApacheRunCommand host2 user2
 *      - Input user2@host2's password when prompted.
 *      
 * @author rcardone
 */
public class ApacheRunCommand 
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
        password = password.trim();

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
        var cmd = new ApacheRunCommand();
        cmd.execute(userid, password, host, port);
    }

    // Run command on host.
    public void execute(String userid, String password, String host, int port) 
     throws Exception
    {
        try (SSHConnection conn = new SSHConnection(host, port, userid, password)) {
            var execChannel = conn.getExecChannel();
            var outStream = new ByteArrayOutputStream(1024);
            int exitCode = execChannel.execute(command, outStream);
        
            String outString = new String(outStream.toByteArray());
            System.out.println("------> " + exitCode);
            System.out.println("------> " + outString.trim());
        }
    }
    
    // Construct help message.
    private static String getHelpMessage()
    {
        String msg = "ERROR: Missing arguments.\n";
        msg += "Enter:  ApacheRunCommand host userid [password] [port]\n\n";
        msg += "If password isn't provided, you will be prompted.\n";
        msg += "The default port is 22 if not provided.\n";
        return msg;
    }
}
