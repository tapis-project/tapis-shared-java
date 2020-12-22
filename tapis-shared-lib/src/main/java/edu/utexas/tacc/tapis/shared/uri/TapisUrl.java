package edu.utexas.tacc.tapis.shared.uri;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;

public final class TapisUrl 
{
    /* **************************************************************************** */
    /*                                   Constants                                  */
    /* **************************************************************************** */
    // The tapis protocol name.
    public static final String TAPIS_PROTOCOL = "tapis";
    public static final String TAPIS_PROTOCOL_PREFIX = TAPIS_PROTOCOL + "://";
    
    // Parse tapis url strings.
    private static final Pattern _pattern = Pattern.compile("/");
    
    /* **************************************************************************** */
    /*                                    Fields                                    */
    /* **************************************************************************** */
    private final String _host;      // no slash
    private final String _systemId;  // no slash
    private final String _path;      // always starts with a slash
    
    /* **************************************************************************** */
    /*                                Constructors                                  */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* constructor:                                                                 */
    /* ---------------------------------------------------------------------------- */
    /** Create a tapis url: tapis://host/systemId/path
     * 
     * The first parameter can be the tenant's base url (ex: https://dev.develop.tapis.io)
     * or simply the host name of the base url (ex: dev.develop.tapis.io).  T
     * 
     * @param tenantBaseUrlOrHost the tenant base url or host dns name
     * @param systemId the system id defined in the tenant
     * @param path the resource path or null
     * @return the constructed tapis url
     * @throws TapisException 
     */
    public TapisUrl(String tenantBaseUrlOrHost, String systemId, String path) 
     throws TapisException
    {
        // Check input.
        if (StringUtils.isBlank(tenantBaseUrlOrHost)) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "TapisUrl", "tenantBaseUrlOrHost");
            throw new TapisException(msg);
        }
        if (StringUtils.isBlank(systemId)) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "TapisUrl", "systemId");
            throw new TapisException(msg);
        }
        
        // Idiot check.
        if (systemId.contains("/")) {
            String msg = MsgUtils.getMsg("TAPIS_INVALID_PARAMETER", "TapisUrl", "systemId", systemId);
            throw new TapisException(msg);
        }
    
        // Assign host.
        String host = tenantBaseUrlOrHost;
        host = StringUtils.removeEnd(host, "/");
        int index = host.lastIndexOf('/');
        if (index >= 0 && host.length() > index + 1) 
            host = host.substring(index+1);
        if (StringUtils.isBlank(host)) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "TapisUrl", "host");
            throw new TapisException(msg);
        }
        
        // Guard against double slashes after the system id.
        if (path == null) path = "/"; // null paths are ok
          else if (!path.startsWith("/")) path = "/" + path;

        // Fill in the fields.
        _host = host;
        _systemId = systemId;
        _path = path;
    }

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
            String msg = MsgUtils.getMsg("TAPIS_NONCONFORMING_URL", "tapis://host/systemId/path");
            throw new TapisException(msg);
        }
        
        // Split the string on slashes.
        String[] parts = _pattern.split(url, 5);
        String host = parts.length < 3 ? null : parts[2];
        String systemId = parts.length < 4 ? null : parts[3];
        String path = parts.length < 5 ? null : parts[4];
        
        // Slash automatically prepended to path in constructor. 
        return new TapisUrl(host, systemId, path);
    }

    /* ---------------------------------------------------------------------------- */
    /* equals:                                                                      */
    /* ---------------------------------------------------------------------------- */
    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof TapisUrl)) return false;
        return toString().equals(obj.toString());
    }
    
    /* ---------------------------------------------------------------------------- */
    /* hashCode:                                                                    */
    /* ---------------------------------------------------------------------------- */
    public int hashCode() {return toString().hashCode();}
    
    /* ---------------------------------------------------------------------------- */
    /* toString:                                                                    */
    /* ---------------------------------------------------------------------------- */
    @Override
    public String toString() 
    {
        // Start with a buffer size that usually avoids resizing.
        var buf = new StringBuilder(128);
        buf.append(TAPIS_PROTOCOL_PREFIX);
        buf.append(_host);
        buf.append("/");
        buf.append(_systemId);
        buf.append(_path);
        return buf.toString();
    }
    
    // Accessors.
    public String getHost() {return _host;}
    public String getSystemId() {return _systemId;}
    public String getPath() {return _path;}
}
