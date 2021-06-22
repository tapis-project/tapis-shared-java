package edu.utexas.tacc.tapis.shared.ssh.apache.misc;

import java.io.ByteArrayOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHConnection;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;

public class ApacheRunCommand 
{

    // Some simple command to run on target host.
    private static final String command = "echo $USER";
    
    // Collect argument and run command.
    public static void main(String[] args) throws Exception 
    {
        // Turn off apache ssh logging.
        Logger minaLogger = (Logger) LoggerFactory.getLogger("org.apache.sshd");
        if(minaLogger!=null)
        {
            minaLogger.setLevel(Level.ERROR);
        }
        
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
        msg += "Enter:  SSHRunCommand host userid [password] [port]\n\n";
        msg += "If password isn't provided, you will be prompted.\n";
        msg += "The default port is 22 if not provided.\n";
        return msg;
    }
}
