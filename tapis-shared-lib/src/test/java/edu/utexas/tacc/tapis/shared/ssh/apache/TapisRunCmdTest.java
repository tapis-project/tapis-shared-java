package edu.utexas.tacc.tapis.shared.ssh.apache;

import edu.utexas.tacc.tapis.shared.ssh.apache.system.TapisRunCommand;
import edu.utexas.tacc.tapis.systems.client.SystemsClient;
import edu.utexas.tacc.tapis.systems.client.SystemsClient.AuthnMethod;
import edu.utexas.tacc.tapis.systems.client.gen.model.TapisSystem;

/** This runs multiple commands using the TapisRunCommand class.
 * Here' the command format:
 * 
 *  TapisRunCommandTest host userid [password] [port]
 *   
 *      
 * @author rcardone
 */
public class TapisRunCmdTest 
{
    // The OBO user@tenant is hardcoded.
    private static final String USER = "testuser2";
    private static final String TENANT = "dev";
    
    // Some simple command to run on target host.
    private static final String command1 = "echo $USER";
    private static final String command2 = "pwd";
    private static final String command3 = "ls -la";
    private static final String command4 = "uname -v";
    private static final String command5 = "sdfadfasde";
    private static final String command6 = "echo \"exiting with 3\"; exit 3";

    private String _baseUrl;
    private String _systemId;

    private TapisRunCmdTest() { }

    public static void main(String[] args)
     throws Exception 
    {
        if (args.length < 2 || System.getenv("jwt") == null) {
            String s = """
                    This test requires 2 command line parameters and 1 environment variable.
                    The command line variables are (1) the tenant base url and (2) the system id.
                    A valid Tapis service JWT must be assigned to the "jwt" environment variable.
                    """;
            System.out.println(s);
            return;
        }

        var test = new  TapisRunCmdTest();
        test.execute(args);
    }     
    
    private void execute(String[] args) throws Exception
    {
        // Set fields.
        _baseUrl = args[0];
        _systemId = args[1];

        // Get TapisSystem to use for the connection
        TapisSystem system = getSystem(_baseUrl, _systemId);
        
        // Create a TapisRunCommand for the system.
        var runCmd = new TapisRunCommand(system);
        
        // Reuse same connection.
        int rc = -1;
        rc = runCmd.execute(command1);
        printResults(runCmd, rc);
        rc = runCmd.execute(command2);
        printResults(runCmd, rc);
        rc = runCmd.execute(command3);
        printResults(runCmd, rc);
        rc = runCmd.execute(command4);
        printResults(runCmd, rc);
        rc = runCmd.execute(command5);
        printResults(runCmd, rc);
        rc = runCmd.execute(command6);
        printResults(runCmd, rc);
        
        // Get a new connection each time.
        rc = runCmd.execute(command1, true);
        printResults(runCmd, rc);
        rc = runCmd.execute(command2, true);
        printResults(runCmd, rc);
        rc = runCmd.execute(command3, true);
        printResults(runCmd, rc);
        rc = runCmd.execute(command4, true);
        printResults(runCmd, rc);
        rc = runCmd.execute(command5, true);
        printResults(runCmd, rc);
        rc = runCmd.execute(command6, true);
        printResults(runCmd, rc);
    }
    
    private void printResults(TapisRunCommand runCmd, int rc)
    {
        // The command has already been run, so we just need to retrieve the output stream.
        System.out.println("=======================================================");
        System.out.println("command: " + runCmd.getCommand());
        System.out.println("rc: " + rc);
        System.out.println("result: " + runCmd.getOutAsString());
    }

    private static TapisSystem getSystem(String baseUrl, String systemId)
     throws Exception
    {
        // Get the system client information from the environment.
        // We have to use a Jobs or Files service JWT because of 
        // authorization restriction imposed by Systems on credentials.
        var serviceJWT = System.getenv("jwt");
        
        // Client set up.
        var clt = new SystemsClient(baseUrl, serviceJWT);
        clt.addDefaultHeader("X-Tapis-User", USER);
        clt.addDefaultHeader("X-Tapis-Tenant", TENANT);
        clt.addDefaultHeader("Content-Type", "application/json");
        
        // Get the system.
        var system = clt.getSystemWithCredentials(systemId);
        clt.close();
        return system;
    }
}
