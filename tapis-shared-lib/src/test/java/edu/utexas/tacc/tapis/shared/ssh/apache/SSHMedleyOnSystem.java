package edu.utexas.tacc.tapis.shared.ssh.apache;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;

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
public class SSHMedleyOnSystem 
{
    private static final String CMD = "echo $USER";

    public static void main(String[] args) throws Exception 
    {
        if (args.length < 5) {
            System.out.println(getHelpMessage());
            return;
        }
        
        var prvKeyFilePath = args[0];
        var pubKeyFilePath = args[1];
        var hostAddr       = args[2];
        var user           = args[3];
        var sourceDir      = args[4];
                
        var execCmd = new SSHMedleyOnSystem();
        execCmd.execute(prvKeyFilePath, pubKeyFilePath, hostAddr, user, sourceDir);
    }
    
    private void execute(String prvKeyFilePath, String pubKeyFilePath,
                         String hostAddr, String user, String sourceDir) 
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
        makeCalls(conn, sourceDir);
        conn.close();
    }
    
    private void makeCalls(SSHConnection conn, String sourceDir) 
     throws IOException, TapisException
    {
        // Get the current directory.
        String pwd = conn.getExecChannel().pwd();
        System.out.println("--> pwd: " + pwd);
        if (pwd != null) pwd = pwd.trim();
        
        // Specify the remote directory that will get created.
        final String remoteDirName = "DeleteMe";
        
        // Get an sftp client.
        SSHSftpClient sftpClient = conn.getSftpClient();
        String testDir = pwd + "/" + remoteDirName;
        sftpClient.mkdir(testDir);
        
        // Get the current directory.
        boolean foundTestDir = false;
        var it = sftpClient.readDir(".");
        System.out.println("--> readDir(.):");
        for (var elem : it) {
            System.out.println("  --> " + elem.getFilename());
            if (elem.getFilename().equals(remoteDirName)) foundTestDir = true;
        }
        if (!foundTestDir) System.out.println("ERROR: Didn't find " + testDir + "!");
          else System.out.println(testDir + " created.");

        // Copy the user-specified source directory to the remote directory.
        SSHScpClient scpClient = conn.getScpClient();
        scpClient.uploadDirToDir(sourceDir, testDir, false);
        
        // Read the test directory.
        it = sftpClient.readDir(testDir);
        System.out.println("--> readDir(" + testDir + "):");
        for (var elem : it) System.out.println("  --> " + elem.getFilename());
        
        // Go into newly created child directory.
        String subDir = testDir + "/" + FilenameUtils.getName(sourceDir);
        int rc = conn.getExecChannel().execute("cd " + subDir);
        it = sftpClient.readDir(subDir);
        System.out.println("--> readDir(" + subDir + "):");
        for (var elem : it) System.out.println("  --> " + elem.getFilename());
        
        // Delete the remote test directory and its contents.
        rc = conn.getExecChannel().execute("rm -fr " + testDir);
        System.out.println("delete dir rc = " + rc);
        
        // See if the delete worked.
        foundTestDir = false;
        it = sftpClient.readDir(".");
        System.out.println("--> readDir(.):");
        for (var elem : it) {
            if (elem.getFilename().equals(remoteDirName)) {
               System.out.println("  --> " + elem.getFilename());
               foundTestDir = true;
               break;
            }
        }
        if (!foundTestDir) System.out.println(testDir + " deleted.");
          else System.out.println("ERROR: " + testDir + " NOT deleted!");        
    }

    private static String getHelpMessage()
    {
        String msg = "Please specify the private and public key file paths, host address, login username, source directory:\n\n"
                     + "   SSHMeldleyOnSystem <private key file> <public key file> <host> <user> <srcDir> \n";
        return msg;
    }
}
