package edu.utexas.tacc.tapis.shared.ssh.system;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.exceptions.runtime.TapisRuntimeException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.ssh.SSHConnection;
import edu.utexas.tacc.tapis.systems.client.gen.model.AuthnEnum;
import edu.utexas.tacc.tapis.systems.client.gen.model.TSystem;

public abstract class TapisAbstractCommand 
{
    /* **************************************************************************** */
    /*                                   Constants                                  */
    /* **************************************************************************** */
    // Local logger.
    private static final Logger _log = LoggerFactory.getLogger(TapisAbstractCommand.class);
    
    /* **************************************************************************** */
    /*                                    Fields                                    */
    /* **************************************************************************** */
    // From constructor.
    protected final TSystem         _system;
    
    // Used by subclasses for capturing error information.
    protected ByteArrayOutputStream _err; 
    
    // Cached connection.
    private SSHConnection           _conn;
    
    /* **************************************************************************** */
    /*                                Constructors                                  */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* constructor:                                                                 */
    /* ---------------------------------------------------------------------------- */
    /** This constructor is used by subclasses to create new connections to the 
     * specified system on demand. 
     * 
     * @param system the target system 
     */
    protected TapisAbstractCommand(TSystem system) 
    {
        // This should never happen.
        if (system == null) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "TapisRunCommand", "system");
            throw new TapisRuntimeException(msg);
        }
        
        _system = system;
    }
    
    /* ---------------------------------------------------------------------------- */
    /* constructor:                                                                 */
    /* ---------------------------------------------------------------------------- */
    /** This constructor can be used to share an existing connection to a system.
     * This is only valid if the same credentials would be used to connect as was used
     * to establish the existing connection.  If the connection is null, we revert
     * back to on-demand connection creation.
     * 
     * @param system the target system
     * @param conn an existing connection to the target system 
     */
    protected TapisAbstractCommand(TSystem system, SSHConnection conn) 
    {
        this(system);
        _conn = conn;
    }
        
    /* **************************************************************************** */
    /*                                Public Methods                                */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* getConnection:                                                               */
    /* ---------------------------------------------------------------------------- */
    /** Get a ssh connection to the system using one of the supported authn connection
     * methods.
     * 
     * @return the connection object
     * @throws IOException when unable to get a connection
     * @throws TapisException invalid or missing credentials
     */
    protected SSHConnection getConnection() throws IOException, TapisException
    {
        // Do we already have a connection?
        if (_conn != null) return _conn;
        
        // Determine which constructor to use based on the system credentials.
        var cred = _system.getAuthnCredential();
        if (cred == null) {
            String msg = MsgUtils.getMsg("SYSTEMS_MISSING_CREDENTIALS", getSystemHostMessage(),
                                         _system.getTenant());
            throw new TapisException(msg);
        }
        
        // We currently only use two types of authn for target systems.
        if (_system.getDefaultAuthnMethod() == AuthnEnum.PASSWORD) 
            _conn = new SSHConnection(_system.getHost(), _system.getPort(), 
                                      _system.getEffectiveUserId(), cred.getPassword());
        else if (_system.getDefaultAuthnMethod() == AuthnEnum.PKI_KEYS)
            _conn = new SSHConnection(_system.getHost(), _system.getEffectiveUserId(), 
                                      _system.getPort(), cred.getPublicKey(), cred.getPrivateKey());
        else {
            String msg = MsgUtils.getMsg("SYSTEMS_CMD_UNSUPPORTED_AUTHN_METHOD", getSystemHostMessage(),
                                        _system.getTenant(), _system.getDefaultAuthnMethod());
            throw new TapisException(msg);
        }
        
        return _conn;
    }

    /* ---------------------------------------------------------------------------- */
    /* getErrorStreamMessage:                                                       */
    /* ---------------------------------------------------------------------------- */
    protected String getErrorStreamMessage()
    {
        if (_err == null || _err.size() <= 0) return "";
        return _err.toString();
    }

    /* ---------------------------------------------------------------------------- */
    /* getSystemHostMessage:                                                        */
    /* ---------------------------------------------------------------------------- */
    protected String getSystemHostMessage()
    {return _system.getId() + " (" + _system.getHost() + ")";}
}
