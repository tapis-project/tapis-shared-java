package edu.utexas.tacc.tapis.shared.uri;

import org.apache.commons.lang3.StringUtils;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;

public final class TapisLocalUrl
 extends AbstractTapisUrl
{
    /* **************************************************************************** */
    /*                                   Constants                                  */
    /* **************************************************************************** */
    // The tapis protocol name.
    public static final String TAPISLOCAL_PROTOCOL = "tapislocal";
    public static final String TAPISLOCAL_PROTOCOL_PREFIX = TAPISLOCAL_PROTOCOL + "://";
    
    // Placeholder name for execution system.
    public static final String TAPISLOCAL_EXEC_SYSTEM = "exec.tapis";
    public static final String TAPISLOCAL_FULL_PREFIX =
        TAPISLOCAL_PROTOCOL_PREFIX + TAPISLOCAL_EXEC_SYSTEM + "/";
    
    /* **************************************************************************** */
    /*                                Constructors                                  */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* constructor:                                                                 */
    /* ---------------------------------------------------------------------------- */
    /** Create a tapis url: tapis://host/systemId/path
     * 
     * The first parameter can be the tenant's base url (ex: https://dev.develop.tapis.io)
     * or simply the host name of the base url (ex: dev.develop.tapis.io).
     * 
     * @param systemId the system id defined in the tenant
     * @param path the resource path or null
     * @return the constructed tapis url
     * @throws TapisException 
     */
    public TapisLocalUrl(String path) 
     throws TapisException
    {
        super(TAPISLOCAL_EXEC_SYSTEM, path);
    }
    
    /* **************************************************************************** */
    /*                              Protected Methods                               */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* getProtocolPrefix:                                                           */
    /* ---------------------------------------------------------------------------- */
    @Override
    protected String getProtocolPrefix() {return TAPISLOCAL_PROTOCOL_PREFIX;}

    /* **************************************************************************** */
    /*                                Public Methods                                */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* makeTapisLocalUrl:                                                           */
    /* ---------------------------------------------------------------------------- */
    public static TapisLocalUrl makeTapisLocalUrl(String url) 
     throws TapisException
    {
        // Make sure we have a tapis url.
        if (StringUtils.isBlank(url)) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "makeTapisUrl", "url");
            throw new TapisException(msg);
        }
        if (!url.startsWith(TAPISLOCAL_PROTOCOL_PREFIX)) {
            String msg = MsgUtils.getMsg("TAPIS_NONCONFORMING_URL", 
                                         "tapislocal://" + TAPISLOCAL_EXEC_SYSTEM + "/path");
            throw new TapisException(msg);
        }
        
        // Split the string on slashes.  We ignore whatever systemId
        // the caller provided and use the placeholder constant. 
        // Remove multiple slash sequences from the path.
        String[] parts = _pattern.split(url, 4);
        String path = parts.length < 4 ? null : parts[3];
        if (path != null) path = path.replaceAll("//+", "/");
        
        // Slash automatically prepended to path in constructor. 
        return new TapisLocalUrl(path);
    }
}
