package edu.utexas.tacc.tapis.sharedq;

import org.apache.commons.lang3.StringUtils;

import edu.utexas.tacc.tapis.shared.exceptions.runtime.TapisRuntimeException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;

public class VHostParms 
{
    // Default port for rabbitmq management api.
    private static final int DEFAULT_MANAGEMENT_PORT = 15672;
    
    // Fields
    private final String _host;          // RabbitMQ host
    private final int    _adminPort;     // Management API port on host
    private final String _adminUser;     // Administrator user id in default vhost
    private final String _adminPassword; // Administrator password 
    
    // Constructor
    public VHostParms(String host, int port, String adminUser, String adminPassword)
    {
        // Check inputs.
        if (StringUtils.isBlank(host)) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "VHostParms", "host");
            throw new TapisRuntimeException(msg);
        }
        if (StringUtils.isBlank(adminUser)) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "VHostParms", "adminUser");
            throw new TapisRuntimeException(msg);
        }
        if (StringUtils.isBlank(adminPassword)) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "VHostParms", "adminPassword");
            throw new TapisRuntimeException(msg);
        }
        
        // Assign fields.
        _host = host;
        _adminUser = adminUser;
        _adminPassword = adminPassword;
        if (port <= 0) _adminPort = DEFAULT_MANAGEMENT_PORT;
          else _adminPort = port;
    }
    
    // Accessors.
    public String getHost() {
        return _host;
    }
    public int getAdminPort() {
        return _adminPort;
    }
    public String getAdminUser() {
        return _adminUser;
    }
    public String getAdminPassword() {
        return _adminPassword;
    }
}
