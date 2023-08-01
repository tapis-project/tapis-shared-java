package edu.utexas.tacc.tapis.shared.ssh;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHExecChannel;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHSession;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHSftpClient;
import edu.utexas.tacc.tapis.systems.client.gen.model.AuthnEnum;
import edu.utexas.tacc.tapis.systems.client.gen.model.Credential;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class SshSessionPool {
    private static final Logger log = LoggerFactory.getLogger(SshSessionPool.class);
    private static SshSessionPool instance;

    private final SshSessionPoolPolicy poolPolicy;
    ScheduledFuture<?> poolCleanupTaskFuture;
    private AtomicInteger traceOnCleanupCounter = new AtomicInteger(0);

    public class AutoCloseSession<T> implements AutoCloseable {
        private final SshSessionPool sshSessionPool;
        private final T session;

        AutoCloseSession(SshSessionPool sshSessionPool, SSHExecChannel execChannel) {
            this.sshSessionPool = sshSessionPool;
            this.session = (T)execChannel;
        }

        AutoCloseSession(SshSessionPool sshSessionPool, SSHSftpClient sftpClient) {
            this.sshSessionPool = sshSessionPool;
            this.session = (T)sftpClient;
        }

        public T getSession() {
           return (T) session;
        }


        @Override
        public void close() {
            if(session != null) {
                if (session instanceof SSHExecChannel) {
                    sshSessionPool.returnExecChannel((SSHExecChannel) session);
                } else if (session instanceof SSHSftpClient) {
                    sshSessionPool.returnSftpClient((SSHSftpClient) session);
                }
            }

        }

    }

    /**
     * Executor for cleaning up the pool.  Removes expired connections, etc.  Uses a ThreadFactory to ensure
     * we always have daemon threads.
     */
    private final ScheduledExecutorService poolMaintaanenceExecutor = Executors.newSingleThreadScheduledExecutor(
        new ThreadFactory() {

            @Override
            public Thread newThread(@NotNull Runnable runnable) {
                Thread t = Executors.defaultThreadFactory().newThread(runnable);
                t.setDaemon(true);
                return t;
            }
        });

    // SYNCHRONIZATION NOTES:
    // Super Important!!!
    // synchronize on pool if you add or remove anything from the map
    // synchronize on the list (what is returned by pool.get(key) call) if you make any changes to any connection group in
    //   the list.  This could be adding or removing items in the list, or making actual changes to the connection group
    //   objects.
    private final Map<SshSessionPoolKey, SshConnectionGroup> pool;

    /**
     * Initiallizes the SshSessionPool.  One of the two init methods must be called exactly one time.
     * Subsequent calls * will result in an error.  After a call to init, the pool will be accessed
     * with getInstance();
     *
     * For Example:
     *
     * SshSessionPool.init();
     * SshSessionPool.getInstance().getConnectionStatus()
     */
    public static void init() {
        // constructing a new pool object will set "instance".
        new SshSessionPool(SshSessionPoolPolicy.defaultPolicy());
    }

    /**
     * Initiallizes the SshSessionPool.  One of the two init methods must be called exactly one time.
     * Subsequent calls * will result in an error.  After a call to init, the pool will be accessed
     * with getInstance();
     *
     * For Example:
     *
     * SshSessionPool.init(policy);
     * SshSessionPool.getInstance().getConnectionStatus()
     */
    public static void init(SshSessionPoolPolicy poolPolicy) {
        // constructing a new pool object will set "instance"
        new SshSessionPool(poolPolicy);
    }

    private SshSessionPool(SshSessionPoolPolicy poolPolicy) {
        if(instance != null) {
            // TODO: Dan - fix message
            String msg = "SshSessionPool has already been created.";
            throw new RuntimeException(msg);
        }
        this.poolPolicy = poolPolicy;
        pool = new HashMap<>();
        instance = this;

        poolCleanupTaskFuture = poolMaintaanenceExecutor.scheduleAtFixedRate(() -> {
            cleanup();
        }, poolPolicy.getCleanupInterval().toMillis(), poolPolicy.getCleanupInterval().toMillis(), TimeUnit.MILLISECONDS);
    }

    public static SshSessionPool getInstance() {
        return instance;
    }

    public SshSessionPoolStats getConnectionStats() {
        List<ConnectionGroupStats> groupStatsList = new ArrayList<>();
        synchronized (pool) {
            for (SshSessionPoolKey key : pool.keySet()) {
                SshConnectionGroup connectionGroup = pool.get(key);
                groupStatsList.add(connectionGroup.getGroupStats());
            }
        }
        return new SshSessionPoolStats(groupStatsList);
    }

    public AutoCloseSession<SSHExecChannel> borrowAutoCloseableExecChannel(String tenant, String host,
                Integer port, String effectiveUserId, AuthnEnum authnMethod,
                Credential credential, Duration wait) throws TapisException {
        SSHExecChannel execChannel = borrowExecChannel(tenant, host, port, effectiveUserId, authnMethod, credential, wait);
        return new AutoCloseSession(this, execChannel);
    }

    public SSHExecChannel borrowExecChannel(String tenant, String host, Integer port, String effectiveUserId,
                                            AuthnEnum authnMethod, Credential credential, Duration wait) throws TapisException {
        return reserveSessionOnConnection(tenant, host, port, effectiveUserId, authnMethod, credential,
                SshConnectionContext.ExecChannelConstructor, wait);
    }

    public void returnExecChannel(SSHExecChannel channel) {
        if(channel == null) {
            String msg = MsgUtils.getMsg("SSH_POOL_NULL_PARAM", "SSHExecChannel");
            log.warn(msg);
            return;
        }

        synchronized (pool) {
            for (SshSessionPoolKey key : pool.keySet()) {
                SshConnectionGroup connectionGroup = pool.get(key);
                if (connectionGroup.findConnectionContext(channel) != null) {
                    connectionGroup.releaseSession(channel);
                }
            }
        }
    }

    public AutoCloseSession<SSHSftpClient> borrowAutoCloseableSftpClient(String tenant, String host, Integer port,
            String effectiveUserId, AuthnEnum authnMethod, Credential credential, Duration wait) throws TapisException {
        SSHSftpClient sftpClient = borrowSftpClient(tenant, host, port, effectiveUserId, authnMethod, credential, wait);
        return new AutoCloseSession(this, sftpClient);
    }

    public SSHSftpClient borrowSftpClient(String tenant, String host, Integer port, String effectiveUserId,
                                             AuthnEnum authnMethod, Credential credential, Duration wait) throws TapisException {
        return reserveSessionOnConnection(tenant, host, port, effectiveUserId, authnMethod, credential,
                SshConnectionContext.SftpClientConstructor, wait);
    }

    public void returnSftpClient(SSHSftpClient sftpClient) {
        if(sftpClient == null) {
            String msg = MsgUtils.getMsg("SSH_POOL_NULL_PARAM", "SSHSftpClient");
            log.warn(msg);
            return;
        }

        synchronized (pool) {
            for (SshSessionPoolKey key : pool.keySet()) {
                SshConnectionGroup connectionGroup = pool.get(key);
                if (connectionGroup.findConnectionContext(sftpClient) != null) {
                    connectionGroup.releaseSession(sftpClient);
                }
            }
        }
    }

    private <T extends SSHSession> T reserveSessionOnConnection(String tenant, String host, Integer port, String effectiveUserId,
                                                                   AuthnEnum authnMethod, Credential credential,
                                                                   SshConnectionContext.SessionConstructor<T> channelConstructor,
                                                                   Duration wait) throws TapisException {

        SshSessionPoolKey key = new SshSessionPoolKey(tenant, host, port, effectiveUserId, authnMethod, credential);
        SshConnectionGroup connectionGroup = null;
        synchronized (pool) {
            connectionGroup = pool.get(key);
            if(connectionGroup == null) {
                connectionGroup = new SshConnectionGroup(this, poolPolicy);
                pool.put(key, connectionGroup);
            }
        }

        // reserveSessionOnConnection may block, so  be careful calling it - we don't want any locks
        // held while we make the call for example - this is why it's outside the synchronized block.
        // It doesn't modify the pool, so it's safe.
        T session = connectionGroup.reserveSessionOnConnection(tenant, host, port, effectiveUserId,
                authnMethod, credential, channelConstructor, wait);

        return session;
    }


    private SshConnectionContext findSessionContext(List<SshConnectionContext> sshSessionContexts, Object channel) {
        SshConnectionContext foundContext = null;

        if(sshSessionContexts != null) {
           for(SshConnectionContext context : sshSessionContexts) {
               if(context.containsChannel(channel)) {
                   foundContext = context;
                   break;
               }
           }
        }

        return foundContext;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(System.lineSeparator());
        builder.append("Policy:");
        builder.append(System.lineSeparator());
        builder.append(poolPolicy);
        builder.append(System.lineSeparator());
        builder.append("Keys: ");
        builder.append(pool.keySet().size());
        builder.append(System.lineSeparator());
        builder.append("Details:");
        builder.append(System.lineSeparator());
        builder.append(getConnectionStats());
        builder.append(System.lineSeparator());
        synchronized (pool) {
            for (SshSessionPoolKey key : pool.keySet()) {
                builder.append(" -> ");
                builder.append(key);
                builder.append(System.lineSeparator());
                SshConnectionGroup connectionGroup = pool.get(key);
                builder.append(connectionGroup.toString());
            }
        }
        return builder.toString();
    }

    private void cleanup() {
        for (SshSessionPoolKey key : pool.keySet()) {
            SshConnectionGroup connectionGroup = pool.get(key);
            connectionGroup.cleanup();
        }
        // remove any session context list that has no connections
        synchronized (pool) {
            pool.entrySet().removeIf(entry -> { return entry.getValue().isEmpty(); });
        }
        if(traceOnCleanupCounter.incrementAndGet() >= poolPolicy.getTraceDuringCleanupFrequency()) {
            traceOnCleanupCounter.set(0);
            log.debug("====================================================");
            log.debug(instance.toString());
            log.debug("====================================================");
        }
    }

}
