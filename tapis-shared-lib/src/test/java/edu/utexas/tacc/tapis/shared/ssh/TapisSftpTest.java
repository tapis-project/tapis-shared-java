package edu.utexas.tacc.tapis.shared.ssh;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import edu.utexas.tacc.tapis.shared.ssh.system.TapisSftp;
import edu.utexas.tacc.tapis.systems.client.SystemsClient;
import edu.utexas.tacc.tapis.systems.client.gen.model.TSystem;

/** This test requires sshd to be running on the localhost and that a userid/password
 * be provided when prompted.  The goal is to provide a way to test command execution
 * using ssh.
 * 
 * @author rcardone
 */
public class TapisSftpTest 
{
    // The OBO user@tenant is hardcoded.
    private static final String USER = "testuser2";
    private static final String TENANT = "dev";
    private static final String DEST = "sftpTest/test.txt";
    
    public static void main(String[] args) 
     throws Exception 
    {
        if (args.length < 2 || System.getenv("jwt") == null) {
            String s = "This test requires 2 command line parameters and 1 environment variable.\n"
                       + "The command line variables are (1) the tenant base url and (2) the system id.\n"
                       + "A valid Tapis service JWT must be assigned to the \"jwt\" environment variable.\n";
            System.out.println(s);
            return;
        }
        
        var test = new TapisSftpTest();
        test.tapisSftpTest(args[0], args[1]);
    }
    
    private void tapisSftpTest(String baseUrl, String systemId) 
     throws Exception
    {
        // Get the system with credentials.
        TSystem system = getSystem(baseUrl, systemId);
        var sftp = new TapisSftp(system);
        
        // Put a file onto the system.
        String text = "This is test text.";
        InputStream in = new ByteArrayInputStream(text.getBytes());
        sftp.put(in, DEST);
        sftp.closeChannelAndConnection();
        
        System.out.println("DONE");
    }
    
    private TSystem getSystem(String baseUrl, String systemId) 
     throws Exception
    {
        // Get the system client information from the environment.
        // We have to use a Jobs or Files service JWT because of 
        // authorization restriction imposed by Systems on credentials.
        var serviceJWT = System.getenv("jwt");
        
        // Create the app.
        var clt = new SystemsClient(baseUrl, serviceJWT);
        clt.addDefaultHeader("X-Tapis-User", USER);
        clt.addDefaultHeader("X-Tapis-Tenant", TENANT);
        clt.addDefaultHeader("Content-Type", "application/json");
        var system = clt.getSystem(systemId, true, null, false);
        clt.close();
        return system;
    }
}
