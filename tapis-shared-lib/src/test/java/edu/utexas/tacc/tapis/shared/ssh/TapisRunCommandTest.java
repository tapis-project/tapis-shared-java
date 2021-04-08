package edu.utexas.tacc.tapis.shared.ssh;

import edu.utexas.tacc.tapis.shared.ssh.system.TapisRunCommand;
import edu.utexas.tacc.tapis.systems.client.SystemsClient;
import edu.utexas.tacc.tapis.systems.client.gen.model.ResultSystem;

/** This test requires sshd to be running on the localhost and that a userid/password
 * be provided when prompted.  The goal is to provide a way to test command execution
 * using ssh.
 * 
 * @author rcardone
 */
public class TapisRunCommandTest 
{
    // The OBO user@tenant is hardcoded.
    private static final String USER = "testuser2";
    private static final String TENANT = "dev";
    private static final String COMMAND = "echo $USER";
    
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
        
        var test = new TapisRunCommandTest();
        test.tapisRunCommandTest(args[0], args[1]);
    }
    
    private void tapisRunCommandTest(String baseUrl, String systemId) 
     throws Exception
    {
        ResultSystem system = getSystem(baseUrl, systemId);
        var runCmd = new TapisRunCommand(system);
        String result = runCmd.execute(COMMAND, true); // closes connection
        
        System.out.println(result == null ? "null" : result.trim());
        System.out.println("DONE");
    }
    
    private ResultSystem getSystem(String baseUrl, String systemId)
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
