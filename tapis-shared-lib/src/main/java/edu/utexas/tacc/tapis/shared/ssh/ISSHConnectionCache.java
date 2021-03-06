package edu.utexas.tacc.tapis.shared.ssh;

import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import edu.utexas.tacc.tapis.systems.client.gen.model.TSystem;

import java.io.IOException;

public interface ISSHConnectionCache {

    CacheStats  getCacheStats();
    LoadingCache<SSHConnectionCacheKey, SSHConnection> getCache();
    SSHConnection getConnection(TSystem system, String username) throws IOException;
}
