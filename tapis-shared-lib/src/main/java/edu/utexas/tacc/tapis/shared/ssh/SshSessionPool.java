package edu.utexas.tacc.tapis.shared.ssh;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHExecChannel;
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

public class SshSessionPool {
    private static final Logger log = LoggerFactory.getLogger(SshSessionPool.class);
    public static SshSessionPool INSTANCE;
    private final SshSessionPoolPolicy poolPolicy;
    ScheduledFuture<?> poolCleanupTaskFuture;
    private int traceOnCleanupCounter = 0;

    public class AutoCloseSession<T> implements AutoCloseable {
        private final SshSessionPool pool;
        private final T session;

        AutoCloseSession(SshSessionPool pool, SSHExecChannel execChannel) {
            this.pool = pool;
            this.session = (T)execChannel;
        }

        AutoCloseSession(SshSessionPool pool, SSHSftpClient sftpClient) {
            this.pool = pool;
            this.session = (T)sftpClient;
        }

        public T getSession() {
           return (T) session;
        }


        @Override
        public void close() {
            if(session != null) {
                if (session instanceof SSHExecChannel) {
                    pool.returnExecChannel((SSHExecChannel) session);
                } else if (session instanceof SSHSftpClient) {
                    pool.returnSftpClient((SSHSftpClient) session);
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
    // synchronize on the list (what is returned by pool.get(key) call) if you make any changes to any context in
    //   the list.  This could be adding or removing items in the list, or making actual changes to the context
    //   objects.
    private Map<SshSessionPoolKey, SshConnectionGroup> pool;

    public static void init() {
        INSTANCE = new SshSessionPool(SshSessionPoolPolicy.defaultPolicy());
    }

    public static void init(SshSessionPoolPolicy poolPolicy) {
        INSTANCE = new SshSessionPool(poolPolicy);
    }

    private SshSessionPool(SshSessionPoolPolicy poolPolicy) {
        this.poolPolicy = poolPolicy;
        pool = new HashMap<>();

        poolCleanupTaskFuture = poolMaintaanenceExecutor.scheduleAtFixedRate(() -> {
            cleanup();
        }, poolPolicy.getCleanupInterval().toMillis(), poolPolicy.getCleanupInterval().toMillis(), TimeUnit.MILLISECONDS);
    }

    public SshSessionPoolStats getConnectionStats() {
        List<ConnectionGroupStats> groupStatsList = new ArrayList<>();
        for(SshSessionPoolKey key : pool.keySet()) {
            SshConnectionGroup connectionGroup = pool.get(key);
            groupStatsList.add(connectionGroup.getGroupStats());
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
            log.info(msg);
        }

        for(SshSessionPoolKey key : pool.keySet()) {
            SshConnectionGroup connectionGroup = pool.get(key);
            if(connectionGroup.findConnectionContext(channel) != null) {
                connectionGroup.releaseSession(channel);
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
            log.info(msg);
        }

        for(SshSessionPoolKey key : pool.keySet()) {
            SshConnectionGroup connectionGroup = pool.get(key);
            if(connectionGroup.findConnectionContext(sftpClient) != null) {
                connectionGroup.releaseSession(sftpClient);
            }
        }
    }

    private <T> T reserveSessionOnConnection(String tenant, String host, Integer port, String effectiveUserId,
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
        for(SshSessionPoolKey key : pool.keySet()) {
            builder.append(" -> ");
            builder.append(key);
            builder.append(System.lineSeparator());
            SshConnectionGroup connectionGroup = pool.get(key);
            builder.append(connectionGroup.toString());
        }
        return builder.toString();
    }

    public void cleanup() {
        for (SshSessionPoolKey key : pool.keySet()) {
            SshConnectionGroup connectionGroup = pool.get(key);
            connectionGroup.cleanup();
        }
        // remove any session context list that has no connections
        synchronized (pool) {
            pool.entrySet().removeIf(entry -> { return entry.getValue().isEmpty(); });
        }
        traceOnCleanupCounter++;
        if(traceOnCleanupCounter >= poolPolicy.getTraceDuringCleanupFrequency()) {
            traceOnCleanupCounter = 0;
            log.debug("====================================================");
            log.debug(INSTANCE.toString());
            log.debug("====================================================");
        }
    }

}
