package edu.utexas.tacc.tapis.shared.ssh;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.systems.client.gen.model.AuthnEnum;
import edu.utexas.tacc.tapis.systems.client.gen.model.Credential;
import edu.utexas.tacc.tapis.systems.client.gen.model.TapisSystem;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * Uniquely identifies the connection information for a system.  A single key can be used for multiple
 * tapis systems if all of the information matches (tenant, host, port, effectiveUserId, autenticationMethod,
 * and credentialHash)
 */
public class SshSessionPoolKey {
    private final String tenant;
    private final String host;
    private final Integer port;
    private final String effectiveUserId;
    private final AuthnEnum authnMethod;
    private final int credentialHash;

    /**
     * This will only keep the hash of the credential - not the actual credential.
     */
    public SshSessionPoolKey(String tenant, String host, Integer port, String effectiveUserId,
                             AuthnEnum authnMethod, Credential credential) throws TapisException {
        // check connection details for non-empty values
        if((StringUtils.isBlank(host))
                || (port == null)
                || (StringUtils.isBlank(effectiveUserId))
                || (authnMethod == null)) {
            String msg = MsgUtils.getMsg("SSH_POOL_MISSING_CONNECTION_INFORMATION",
                    tenant, host, port, effectiveUserId, authnMethod);
            throw new TapisException(msg);
        }

        // check credentials for non-empty values
        if(credential == null) {
            String msg = MsgUtils.getMsg("SSH_POOL_MISSING_CREDENTIALS",
                    tenant, host, port, effectiveUserId, authnMethod);
            throw new TapisException(msg);
        }

        this.tenant = tenant;
        this.host = host;
        this.port = port;
        this.effectiveUserId = effectiveUserId;
        this.authnMethod = authnMethod;

        // Is this good enough, or should we comput a sha256 or something?
        this.credentialHash = credential.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SshSessionPoolKey that = (SshSessionPoolKey) o;
        return credentialHash == that.credentialHash && Objects.equals(tenant, that.tenant)
                && Objects.equals(host, that.host) && Objects.equals(port, that.port)
                && Objects.equals(effectiveUserId, that.effectiveUserId) && authnMethod == that.authnMethod;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenant, host, port, effectiveUserId, authnMethod, credentialHash);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Tenant: ");
        builder.append(tenant);
        builder.append("Host: ");
        builder.append(host);
        builder.append(",  ");
        builder.append("Port: ");
        builder.append(port);
        builder.append(",  ");
        builder.append("EffectiveUserId: ");
        builder.append(effectiveUserId);
        return builder.toString();
    }
}
