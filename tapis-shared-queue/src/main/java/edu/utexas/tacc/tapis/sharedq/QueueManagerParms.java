package edu.utexas.tacc.tapis.sharedq;

import org.apache.commons.lang3.StringUtils;

import edu.utexas.tacc.tapis.shared.exceptions.runtime.TapisRuntimeException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;

public class QueueManagerParms 
{
    // RabbitMQ configuration fields.
    private String  instanceName; // Name of program instance
    private String  service;      // Name of service
    private String  queueUser;
    private String  queuePassword;
    private String  queueHost;
    private int     queuePort;
    private boolean queueSSLEnabled;
    private boolean queueAutoRecoveryEnabled;
    private String  vhost;        // can be null 
    
    // Validation method should be called before first parameter use.
    public void validate() throws TapisRuntimeException
    {
        if (StringUtils.isBlank(instanceName)) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "validate", "instanceName");
            throw new TapisRuntimeException(msg);
        }
        if (StringUtils.isBlank(service)) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "validate", "service");
            throw new TapisRuntimeException(msg);
        }
        if (StringUtils.isBlank(queueUser)) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "validate", "queueUser");
            throw new TapisRuntimeException(msg);
        }
        if (StringUtils.isBlank(queuePassword)) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "validate", "queuePassword");
            throw new TapisRuntimeException(msg);
        }
        if (StringUtils.isBlank(queueHost)) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "validate", "queueHost");
            throw new TapisRuntimeException(msg);
        }
        if (queuePort <= 0) {
            String msg = MsgUtils.getMsg("TAPIS_INVALID_PARAMETER", "validate", "queuePort", 
                                         queuePort);
            throw new TapisRuntimeException(msg);
        }
    }
    
    // Accessors.
    public String getInstanceName() {
        return instanceName;
    }
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }
    public String getService() {
        return service;
    }
    public void setService(String service) {
        this.service = service;
    }
    public String getQueueUser() {
        return queueUser;
    }
    public void setQueueUser(String queueUser) {
        this.queueUser = queueUser;
    }
    public String getQueuePassword() {
        return queuePassword;
    }
    public void setQueuePassword(String queuePassword) {
        this.queuePassword = queuePassword;
    }
    public String getQueueHost() {
        return queueHost;
    }
    public void setQueueHost(String queueHost) {
        this.queueHost = queueHost;
    }
    public int getQueuePort() {
        return queuePort;
    }
    public void setQueuePort(int queuePort) {
        this.queuePort = queuePort;
    }
    public boolean isQueueSSLEnabled() {
        return queueSSLEnabled;
    }
    public void setQueueSSLEnabled(boolean queueSSLEnabled) {
        this.queueSSLEnabled = queueSSLEnabled;
    }
    public boolean isQueueAutoRecoveryEnabled() {
        return queueAutoRecoveryEnabled;
    }
    public void setQueueAutoRecoveryEnabled(boolean queueAutoRecoveryEnabled) {
        this.queueAutoRecoveryEnabled = queueAutoRecoveryEnabled;
    }
    public String getVhost() {
        return vhost;
    }
    public void setVhost(String vhost) {
        this.vhost = vhost;
    }
}
