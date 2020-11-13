package edu.utexas.tacc.tapis.shared.ssh;

import com.google.common.cache.CacheLoader;
import edu.utexas.tacc.tapis.systems.client.gen.model.TSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

public class SSHConnectionCacheLoader extends CacheLoader<SSHConnectionCacheKey, SSHConnection> {

    private static final Logger log = LoggerFactory.getLogger(SSHConnectionCache.class);

    @Override
    public SSHConnection load(SSHConnectionCacheKey key) throws IOException, IllegalArgumentException {
        TSystem system = key.getSystem();
        String username = key.getUsername();
        int port;
        SSHConnection sshConnection;
        TSystem.DefaultAccessMethodEnum accessMethodEnum = system.getDefaultAccessMethod();
        // This should not be null, but if it is, we default to password.
        accessMethodEnum = accessMethodEnum == null ? TSystem.DefaultAccessMethodEnum.PASSWORD : accessMethodEnum;
        switch (accessMethodEnum) {
            case PASSWORD:
                String password = system.getAccessCredential().getPassword();
                port = system.getPort();
                if (port <= 0) port = 22;
                sshConnection = new SSHConnection(
                        system.getHost(),
                        port,
                        username,
                        password);
                return sshConnection;
            case PKI_KEYS:
                String pubKey = system.getAccessCredential().getPublicKey();
                String privateKey = system.getAccessCredential().getPrivateKey();
                port = system.getPort();
                if (port <= 0) port = 22;
                sshConnection = new SSHConnection(system.getHost(), username, port, pubKey, privateKey);
                return sshConnection;
            default:
                String msg = String.format("Access method of %s is not valid.", accessMethodEnum.getValue());
                throw new IllegalArgumentException(msg);

        }
    }
}
