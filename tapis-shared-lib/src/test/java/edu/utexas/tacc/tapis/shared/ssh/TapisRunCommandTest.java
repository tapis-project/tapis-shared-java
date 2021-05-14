package edu.utexas.tacc.tapis.shared.ssh;

import edu.utexas.tacc.tapis.shared.ssh.system.TapisRunCommand;
import edu.utexas.tacc.tapis.systems.client.SystemsClient;
import edu.utexas.tacc.tapis.systems.client.gen.model.TapisSystem;

/**
 * This test runs several commands on a Tapis system using TapisRunCommand (i.e. ssh).
 * This test requires the tenant base url and system id to be passed in as the first 2 command line
 *   arguments. The environment variable "jwt" must be set to a valid Tapis JWT. For example:
 *   export jwt="..."
 *   java TapisRunCommandTest https://dev.develop.tapis.io tapisv3-storage
 *
 * NOTE: To see detailed debug output edit TapisRunCommand.java and set DEBUG = true;
 * 
 * @author rcardone
 */
public class TapisRunCommandTest 
{
    // The OBO user@tenant is hardcoded.
    private static final String USER = "testuser2";
    private static final String TENANT = "dev";
    private static final String COMMAND1 = "echo $USER";
    private static final String COMMAND2 = "ls /tmp";
    private static final String COMMAND3 = "touch /tmp/junk";
    private static final String COMMAND4 = "ls /adslfkjasdlfkjasdlkfjdkjf";
    private static final String COMMAND5 = "echo \"exiting with 3\"; exit 3";

    private static String _baseUrl;
    private static String _systemId;

    public TapisRunCommandTest() { }

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

        _baseUrl = args[0];
        _systemId = args[1];

        // Get TapisSystem to use for the connection
        TapisSystem system = getSystem(_baseUrl, _systemId);

        // Create a TapisRunCommand for the system and use the same connection to run each command
        var runCmd = new TapisRunCommand(system);
        System.out.printf("=======================================================%n");
        runCmd.execute(COMMAND1); System.out.printf("CMD: %s EXITCODE: %d%n", COMMAND1, runCmd.getExitStatus());
        runCmd.execute(COMMAND2); System.out.printf("CMD: %s EXITCODE: %d%n", COMMAND2, runCmd.getExitStatus());
        runCmd.execute(COMMAND3); System.out.printf("CMD: %s EXITCODE: %d%n", COMMAND3, runCmd.getExitStatus());
        runCmd.execute(COMMAND4); System.out.printf("CMD: %s EXITCODE: %d%n", COMMAND4, runCmd.getExitStatus());
        runCmd.execute(COMMAND5, true); System.out.printf("CMD: %s EXITCODE: %d%n", COMMAND5, runCmd.getExitStatus());
        System.out.printf("=======================================================%n");

        // Run the commands creating a new connection each time
        runCmd.execute(COMMAND1, true); System.out.printf("CMD: %s EXITCODE: %d%n", COMMAND1, runCmd.getExitStatus());
        runCmd.execute(COMMAND2, true); System.out.printf("CMD: %s EXITCODE: %d%n", COMMAND2, runCmd.getExitStatus());
        runCmd.execute(COMMAND3, true); System.out.printf("CMD: %s EXITCODE: %d%n", COMMAND3, runCmd.getExitStatus());
        runCmd.execute(COMMAND4, true); System.out.printf("CMD: %s EXITCODE: %d%n", COMMAND4, runCmd.getExitStatus());
        runCmd.execute(COMMAND5, true); System.out.printf("CMD: %s EXITCODE: %d%n", COMMAND5, runCmd.getExitStatus());
        System.out.printf("=======================================================%n");

        // Run the commands and generate formatted results
        var test = new TapisRunCommandTest();
        test.tapisRunCommandTest(system, COMMAND1);
        test.tapisRunCommandTest(system, COMMAND2);
        test.tapisRunCommandTest(system, COMMAND3);
        test.tapisRunCommandTest(system, COMMAND4);
        test.tapisRunCommandTest(system, COMMAND5);
        System.out.printf("=======================================================%n");
    }

    private void tapisRunCommandTest(TapisSystem system, String command)
     throws Exception
    {
        var runCmd = new TapisRunCommand(system);
        System.out.printf("=======================================================%n");
        System.out.printf("Running command: %s%n  on system: %s.%n", command, system.getId());
        String result = runCmd.execute(command, true); // closes connection
        int exitStatus = runCmd.getExitStatus();
        System.out.printf("stdout:%n-----------------------%n%s%n-----------------------%n",
                          result == null ? "null" : result.trim());
        System.out.printf("stderr:%n-----------------------%n%s%n-----------------------%n",
                          runCmd.getStdErr());
        System.out.printf("exitStatus: %d%n", exitStatus);
        System.out.printf("DONE running command: %s%n", command);
    }
    
    private static TapisSystem getSystem(String baseUrl, String systemId)
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
