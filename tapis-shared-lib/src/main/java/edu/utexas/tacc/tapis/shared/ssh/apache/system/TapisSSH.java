package edu.utexas.tacc.tapis.shared.ssh.apache.system;

import java.io.IOException;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHConnection;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHScpClient;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHSftpClient;
import edu.utexas.tacc.tapis.systems.client.gen.model.TapisSystem;

/** Top-level class for SSH access to Tapis systems.  This class's superclass understands
 * how to access the credentials in a TapisSystem object.  A TapisRunCommand instance
 * and SCP and SFTP clients can be created by this class.  Together, these objects provide
 * access to all Tapis SSH support.  
 * 
 * For lower-level, customized control of SSH sessions and channels, see the classes in the 
 * edu.utexas.tacc.tapis.shared.ssh.apache package.
 * 
 * @author rcardone
 */
public class TapisSSH 
 extends TapisAbstractConnection
{
    /* **************************************************************************** */
    /*                                   Fields                                     */
    /* **************************************************************************** */
    // The same run command can be reused for multiple calls to its system.
    private TapisRunCommand _tapisRunCommand;
    
    /* **************************************************************************** */
    /*                                Constructors                                  */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* constructor:                                                                 */
    /* ---------------------------------------------------------------------------- */
    public TapisSSH(TapisSystem system) {super(system);}

    /* ---------------------------------------------------------------------------- */
    /* constructor:                                                                 */
    /* ---------------------------------------------------------------------------- */
    public TapisSSH(TapisSystem system, SSHConnection conn) {super(system, conn);}

    /* **************************************************************************** */
    /*                                Public Methods                                */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* getRunCommand:                                                               */
    /* ---------------------------------------------------------------------------- */
    public TapisRunCommand getRunCommand() throws IOException, TapisException 
    {
        // Create the run command instance on first use.
        if (_tapisRunCommand == null) 
            synchronized(this) {
                if (_tapisRunCommand == null)
                    _tapisRunCommand = new TapisRunCommand(getSystem(), getConnection());
            }   
            
        return _tapisRunCommand; 
    }

    /* ---------------------------------------------------------------------------- */
    /* getScpClient:                                                                */
    /* ---------------------------------------------------------------------------- */
    public SSHScpClient getScpClient() throws IOException, TapisException 
    {
        return getConnection().getScpClient();
    }

    /* ---------------------------------------------------------------------------- */
    /* getSftpClient:                                                               */
    /* ---------------------------------------------------------------------------- */
    public SSHSftpClient getSftpClient() throws IOException, TapisException 
    {
        return getConnection().getSftpClient();
    }
}
