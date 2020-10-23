package edu.utexas.tacc.tapis.shared.utils;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class TapisUrlUtils 
{
    /* **************************************************************************** */
    /*                                   Constants                                  */
    /* **************************************************************************** */
	// The tapis protocol name.
	public static final String TAPIS_PROTOCOL = "tapis";
	public static final String TAPIS_PROTOCOL_PREFIX = TAPIS_PROTOCOL + "://";
	
	// Parse tapis urls.
	private static final Pattern _pattern = Pattern.compile("/");
	
    /* **************************************************************************** */
    /*                                Public Methods                                */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* makeTapisUrl:                                                                */
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
	 */
	public static String makeTapisUrl(String tenantBaseUrlOrHost, String systemId, String path)
	{
		// The caller can specify the tenant baseUrl or  just the host portion of 
		// the tenant baseUrl.  We remove the trailing slash if it exists.
	    String host = tenantBaseUrlOrHost;
		if (host != null) {
			host = StringUtils.removeEnd(host, "/");
			int index = host.lastIndexOf('/');
			if (index >= 0 && host.length() > index + 1) 
				host = host.substring(index+1);
		} 
		
		// Guard against double slashes after the system id.
		String pathSep = (path != null && path.startsWith("/")) ? "" : "/";
		if (path == null) path = ""; // null paths are ok
		
		// Return the tapis url.
		return TAPIS_PROTOCOL_PREFIX + host + "/" + systemId + pathSep + path;
	}
	
    /* ---------------------------------------------------------------------------- */
    /* getHost:                                                                     */
    /* ---------------------------------------------------------------------------- */
	public static String getHost(String tapisUrl)
	{
		String[] parts = _pattern.split(tapisUrl, 4);
		if (parts.length < 3) return null;
		return parts[2];
	}

    /* ---------------------------------------------------------------------------- */
    /* getSystemId:                                                                 */
    /* ---------------------------------------------------------------------------- */
	public static String getSystemId(String tapisUrl)
	{
		String[] parts = _pattern.split(tapisUrl, 5);
		if (parts.length < 4) return null;
		return parts[3];
	}

    /* ---------------------------------------------------------------------------- */
    /* getPath:                                                                     */
    /* ---------------------------------------------------------------------------- */
	public static String getPath(String tapisUrl)
	{
		String[] parts = _pattern.split(tapisUrl, 5);
		if (parts.length < 5) return null;
		return parts[4];
	}
}
