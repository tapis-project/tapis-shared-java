package edu.utexas.tacc.tapis.shared.ssh.apache;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** This test program is used to read public and private key out of 
 * files and use them to issue a simple command on a host.  The
 * command format is:
 * 
 *  SSHExecCmdOnSystem <private key file> <public key file> <host> <user>
 *  
 * All 4 arguments are required.  The keys are used to login to user@host.
 * The command exit code and response are printed to standard out.
 * 
 * @author rcardone
 */
public class SSHExecCmdOnSystem 
{
    private static final String CMD = "echo $USER";

    public static void main(String[] args) throws Exception 
    {
        if (args.length < 4) {
            System.out.println(getHelpMessage());
            return;
        }
        
        var prvKeyFilePath = args[0];
        var pubKeyFilePath = args[1];
        var hostAddr       = args[2];
        var user           = args[3];
        
        var execCmd = new SSHExecCmdOnSystem();
        execCmd.execute(prvKeyFilePath, pubKeyFilePath, hostAddr, user);
    }
    
    private void execute(String prvKeyFilePath, String pubKeyFilePath,
                         String hostAddr, String user) 
     throws Exception
    {
        // Read the private key file.
        byte[] prvBytes = Files.readAllBytes(Paths.get(prvKeyFilePath));
        String prvString = new String(prvBytes, StandardCharsets.UTF_8);
        
        // Read the private key file.
        byte[] pubBytes = Files.readAllBytes(Paths.get(pubKeyFilePath));
        String pubString = new String(pubBytes, StandardCharsets.UTF_8);
        
        // Issue the command.
        SSHConnection conn = new SSHConnection(hostAddr, 0, user, pubString, prvString);
        var channel = conn.getExecChannel();
        var outStream = new ByteArrayOutputStream(256);
        int rc = channel.execute(CMD, outStream);
        conn.close();
        
        // Print results.
        System.out.println("--> rc = " + rc);
        System.out.println("--> result = " + new String(outStream.toByteArray()));
    }

    private static String getHelpMessage()
    {
        String msg = "Please specify the private and public key file paths, host address, login username:\n\n"
                     + "   SSHExecCmdOnSystem <private key file> <public key file> <host> <user>\n";
        return msg;
    }
}
