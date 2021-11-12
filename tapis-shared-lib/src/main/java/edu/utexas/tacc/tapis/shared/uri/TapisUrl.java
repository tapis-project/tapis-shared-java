package edu.utexas.tacc.tapis.shared.uri;

import org.apache.commons.lang3.StringUtils;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;

public final class TapisUrl
 extends AbstractTapisUrl
{
    /* **************************************************************************** */
    /*                                   Constants                                  */
    /* **************************************************************************** */
    // The tapis protocol name.
    public static final String TAPIS_PROTOCOL = "tapis";
    public static final String TAPIS_PROTOCOL_PREFIX = TAPIS_PROTOCOL + "://";
    
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
    public TapisUrl(String systemId, String path) 
     throws TapisException
    {
        super(systemId, path);
    }
    
    /* **************************************************************************** */
    /*                              Protected Methods                               */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* getProtocolPrefix:                                                           */
    /* ---------------------------------------------------------------------------- */
    @Override
    protected String getProtocolPrefix() {return TAPIS_PROTOCOL_PREFIX;}

    /* **************************************************************************** */
    /*                                Public Methods                                */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* makeTapisUrl:                                                                */
    /* ---------------------------------------------------------------------------- */
    public static TapisUrl makeTapisUrl(String url) 
     throws TapisException
    {
        // Make sure we have a tapis url.
        if (StringUtils.isBlank(url)) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "makeTapisUrl", "url");
            throw new TapisException(msg);
        }
        if (!url.startsWith(TAPIS_PROTOCOL_PREFIX)) {
            String msg = MsgUtils.getMsg("TAPIS_NONCONFORMING_URL", "tapis://systemId/path");
            throw new TapisException(msg);
        }
        
        // Split the string on slashes. Remove multiple 
        // slash sequences from the path.
        String[] parts = _pattern.split(url, 4);
        String systemId = parts.length < 3 ? null : parts[2];
        String path = parts.length < 4 ? null : parts[3];
        if (path != null) path = path.replaceAll("//+", "/");
        
        // Slash automatically prepended to path in constructor. 
        return new TapisUrl(systemId, path);
    }
}
