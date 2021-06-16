package edu.utexas.tacc.tapis.shared.ssh;

import java.io.IOException;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.systems.client.gen.model.AuthnEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheLoader;

import edu.utexas.tacc.tapis.systems.client.gen.model.TapisSystem;

public class SSHConnectionCacheLoader extends CacheLoader<SSHConnectionCacheKey, SSHConnectionJsch> {

    private static final Logger log = LoggerFactory.getLogger(SSHConnectionCache.class);

    @Override
    public SSHConnectionJsch load(SSHConnectionCacheKey key) throws TapisException, IllegalArgumentException {
        TapisSystem system = key.getSystem();
        String username = key.getUsername();
        int port;
        SSHConnectionJsch sshConnection;
        AuthnEnum accessMethodEnum = system.getDefaultAuthnMethod();
        // This should not be null, but if it is, we default to password.
        accessMethodEnum = accessMethodEnum == null ? AuthnEnum.PASSWORD : accessMethodEnum;
        switch (accessMethodEnum) {
            case PASSWORD:
                String password = system.getAuthnCredential().getPassword();
                port = system.getPort();
                if (port <= 0) port = 22;
                sshConnection = new SSHConnectionJsch(
                        system.getHost(),
                        port,
                        username,
                        password);
                return sshConnection;
            case PKI_KEYS:
                String pubKey = system.getAuthnCredential().getPublicKey();
                String privateKey = system.getAuthnCredential().getPrivateKey();
                port = system.getPort();
                if (port <= 0) port = 22;
                sshConnection = new SSHConnectionJsch(system.getHost(), username, port, pubKey, privateKey);
                return sshConnection;
            default:
                String msg = String.format("Access method of %s is not valid.", accessMethodEnum.getValue());
                throw new IllegalArgumentException(msg);

        }
    }
}
