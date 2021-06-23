package edu.utexas.tacc.tapis.shared.ssh.apache.system;

import edu.utexas.tacc.tapis.shared.ssh.apache.SSHConnection;
import edu.utexas.tacc.tapis.systems.client.gen.model.TapisSystem;

public class TapisSftp 
 extends TapisAbstractConnection
{
    /* **************************************************************************** */
    /*                                Constructors                                  */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* constructor:                                                                 */
    /* ---------------------------------------------------------------------------- */
    public TapisSftp(TapisSystem system) {super(system);}

    /* ---------------------------------------------------------------------------- */
    /* constructor:                                                                 */
    /* ---------------------------------------------------------------------------- */
    public TapisSftp(TapisSystem system, SSHConnection conn) {super(system, conn);}


}
