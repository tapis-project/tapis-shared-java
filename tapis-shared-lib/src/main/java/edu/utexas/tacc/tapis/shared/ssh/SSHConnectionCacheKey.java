package edu.utexas.tacc.tapis.shared.ssh;

import edu.utexas.tacc.tapis.systems.client.gen.model.TSystem;

import java.util.Objects;


/**
 * Cache key to put into the LoadingCache. Equality is based on the username, the
 * system's tenantId and the system's ID.
 */
public class SSHConnectionCacheKey {

    private final TSystem system;
    private final String username;

    public SSHConnectionCacheKey(TSystem sys, String uname) {
        system = sys;
        username = uname;
    }

    public TSystem getSystem() {
        return system;
    }

    public String getUsername() {
        return username;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SSHConnectionCacheKey that = (SSHConnectionCacheKey) o;
        return Objects.equals(this.system.getId(), that.system.getId()) &&
                Objects.equals(this.username, that.getUsername()) &&
                Objects.equals(this.system.getTenant(), that.system.getTenant());
    }

    @Override
    public int hashCode() {
        return Objects.hash(system.getId(), system.getTenant(), username);
    }
}
