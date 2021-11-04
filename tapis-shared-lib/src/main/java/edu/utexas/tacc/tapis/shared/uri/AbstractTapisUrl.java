package edu.utexas.tacc.tapis.shared.uri;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;

public abstract class AbstractTapisUrl 
{
    /* **************************************************************************** */
    /*                                   Constants                                  */
    /* **************************************************************************** */
    // Parse tapis url strings.
    protected static final Pattern _pattern = Pattern.compile("/");
    
    /* **************************************************************************** */
    /*                                    Fields                                    */
    /* **************************************************************************** */
    private final String _systemId;  // no slash
    private final String _path;      // always starts with a slash
    
    /* **************************************************************************** */
    /*                                Constructors                                  */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* constructor:                                                                 */
    /* ---------------------------------------------------------------------------- */
    /** Create one of the tapis protocol urls.  Examples include: 
     * 
     *      tapis://systemId/path
     *      tapislocal://systemId/path
     * 
     * @param systemId the system id defined in the tenant
     * @param path the resource path or null
     * @return the constructed tapis url
     * @throws TapisException 
     */
    protected AbstractTapisUrl(String systemId, String path) 
     throws TapisException
    {
        // Check input.
        if (StringUtils.isBlank(systemId)) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "AbstractTapisUrl", "systemId");
            throw new TapisException(msg);
        }
        
        // Idiot check.
        if (systemId.contains("/")) {
            String msg = MsgUtils.getMsg("TAPIS_INVALID_PARAMETER", "AbstractTapisUrl", "systemId", systemId);
            throw new TapisException(msg);
        }
    
        // Guard against double slashes after the system id.
        if (StringUtils.isBlank(path)) path = "/"; // null/empty paths are ok
          else if (!path.startsWith("/")) path = "/" + path;

        // Fill in the fields.
        _systemId = systemId;
        _path = path;
    }

    /* **************************************************************************** */
    /*                                Public Methods                                */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* getProtocolPrefix:                                                           */
    /* ---------------------------------------------------------------------------- */
    protected abstract String getProtocolPrefix();
    
    /* ---------------------------------------------------------------------------- */
    /* equals:                                                                      */
    /* ---------------------------------------------------------------------------- */
    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof AbstractTapisUrl)) return false;
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
        buf.append(getProtocolPrefix());
        buf.append(_systemId); // no slash
        buf.append(_path);     // leading slash
        return buf.toString();
    }
    
    // Accessors.
    public String getSystemId() {return _systemId;}
    public String getPath() {return _path;}
}
