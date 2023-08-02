package edu.utexas.tacc.tapis.shared.ssh;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.utils.HashUtils;
import edu.utexas.tacc.tapis.systems.client.gen.model.AuthnEnum;
import edu.utexas.tacc.tapis.systems.client.gen.model.Credential;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Uniquely identifies the connection information for a system.  A single key can be used for multiple
 * tapis systems if all of the information matches (tenant, host, port, effectiveUserId, autenticationMethod,
 * and credentialHash)
 *
 * Note about hashing - this is the key to a map, so the equals/hashcode are important.  This uses the apache
 * hash code utility - I looked at it, and it appears to pretty much be the method suggested by Josh Bloch in
 * Effective Java, so I feel pretty good about the hash code being fairly unique.  The equals will always be
 * unique, so the only issue with hashcode would be performance, but again it looks fine.
 *
 * The field called credentialHash is the SHA256 hash of the key credential fields.  This must be able to uniquely
 * identify the credentials to avoid the case where we have to keys that differ only by credential hash.
 *
 */
final class SshSessionPoolKey {
    private static final Logger log = LoggerFactory.getLogger(SshSessionPoolKey.class);
    private final String tenant;
    private final String host;
    private final Integer port;
    private final String effectiveUserId;
    private final AuthnEnum authnMethod;
    private final String credentialHash;

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
        if(!AuthnEnum.PASSWORD.equals(authnMethod) && (!AuthnEnum.PKI_KEYS.equals(authnMethod))) {
            // We currently only use two types of authn for target systems.
            if (authnMethod != AuthnEnum.PASSWORD &&
                    authnMethod != AuthnEnum.PKI_KEYS)
            {
                String msg = MsgUtils.getMsg("SSH_POOL_UNSUPPORTED_AUTHN_METHOD",
                        tenant, host, port, effectiveUserId, authnMethod);
                log.error(msg);
                throw new TapisException(msg);
            }

        }
        this.authnMethod = authnMethod;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(credential.getLoginUser());
        stringBuilder.append("|");
        stringBuilder.append(credential.getPassword());
        stringBuilder.append("|");
        stringBuilder.append(credential.getPrivateKey());
        stringBuilder.append("|");
        stringBuilder.append(credential.getPublicKey());

        // Is this good enough, or should we comput a sha256 or something?
        this.credentialHash = HashUtils.computeSHA256(stringBuilder.toString().getBytes());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SshSessionPoolKey that = (SshSessionPoolKey) o;
        return Objects.equals(credentialHash, that.credentialHash) && Objects.equals(tenant, that.tenant)
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
        builder.append(",  ");
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
