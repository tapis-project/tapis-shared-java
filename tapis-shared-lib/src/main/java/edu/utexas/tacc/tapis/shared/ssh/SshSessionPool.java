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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class SshSessionPool {
    private static final Logger log = LoggerFactory.getLogger(SshSessionPool.class);
    private static SshSessionPool instance;

    private final SshSessionPoolPolicy poolPolicy;
    ScheduledFuture<?> poolCleanupTaskFuture;

    /**
     * This lock controls access to the pool.  There can be any number of readers, but only one
     * writer.  You only need to aquire the write lock if you are going to add or remove an
     * entry from the pool.  Everything else is read.  Modifying the values stored in the pool
     * is synchronized/protected by the SshConnectionGroup class.
     */
    ReadWriteLock poolRWLock = new ReentrantReadWriteLock(true);

    private AtomicInteger traceOnCleanupCounter = new AtomicInteger(0);

    public class PooledSshSession<T extends SSHSession> implements AutoCloseable {
        private final SshConnectionGroup sshConnectionGroup;
        private final SSHSession session;

        PooledSshSession(SshConnectionGroup sshConnectionGroup, T execChannel) {
            this.sshConnectionGroup = sshConnectionGroup;
            this.session = execChannel;
        }

        public T getSession() {
           return (T) session;
        }


        @Override
        public void close() {
            if(session != null) {
                if (session instanceof SSHExecChannel) {
                    sshConnectionGroup.releaseSession((SSHExecChannel) session);
                } else if (session instanceof SSHSftpClient) {
                    sshConnectionGroup.releaseSession((SSHSftpClient) session);
                }
            } else {
                String msg = MsgUtils.getMsg("SSH_POOL_NULL_PARAM", session.getClass().getCanonicalName());
                log.warn(msg);
                return;
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
    // write lock the poolRWLock if you add or remove anything from the map
    // read lock the poolRWLock if you are iterating or reading from the pool.  You can
    // just read a single value and just use the info if you want, but if you need for the
    // value to remain in the map (such as when we are waiting for a session on a connection group)
    // you must hold a read lock.
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
            String msg = MsgUtils.getMsg("SSH_POOL_ALREADY_CREATED");
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
        poolRWLock.readLock().lock();
        try {
            for (SshSessionPoolKey key : pool.keySet()) {
                SshConnectionGroup connectionGroup = pool.get(key);
                groupStatsList.add(connectionGroup.getGroupStats());
            }
        } finally {
            poolRWLock.readLock().unlock();
        }
        return new SshSessionPoolStats(groupStatsList);
    }

    public PooledSshSession<SSHExecChannel> borrowExecChannel(String tenant, String host, Integer port, String effectiveUserId,
                                                              AuthnEnum authnMethod, Credential credential, Duration wait) throws TapisException {
        return reserveSessionOnConnection(tenant, host, port, effectiveUserId, authnMethod, credential,
                SshConnectionContext.ExecChannelConstructor, wait);
    }

    public PooledSshSession<SSHSftpClient> borrowSftpClient(String tenant, String host, Integer port, String effectiveUserId,
                                                            AuthnEnum authnMethod, Credential credential, Duration wait) throws TapisException {
        return reserveSessionOnConnection(tenant, host, port, effectiveUserId, authnMethod, credential,
                SshConnectionContext.SftpClientConstructor, wait);
    }

    private <T extends SSHSession> PooledSshSession<T> reserveSessionOnConnection(String tenant, String host, Integer port, String effectiveUserId,
                                                                                  AuthnEnum authnMethod, Credential credential,
                                                                                  SshConnectionContext.SessionConstructor<T> channelConstructor,
                                                                                  Duration wait) throws TapisException {
        long startTime = System.currentTimeMillis();
        SshSessionPoolKey key = new SshSessionPoolKey(tenant, host, port, effectiveUserId, authnMethod, credential);
        SshConnectionGroup connectionGroup = null;
        T session = null;

        //  We must aquire the write version of the lock because we may need to add a key/value to the
        //  pool if it's not already there.
        poolRWLock.writeLock().lock();
        try {
            connectionGroup = pool.get(key);
            // if we didn't get the group, we must insert it which requires a write lock, so release read lock
            // and aquire write lock (rw lock doesn't allow upgrading the lock, only downgrading)
            if(connectionGroup == null) {
                // we must check again - something could have added the key between the point where we
                // released the read lock and aquired the write lock.
                connectionGroup = pool.get(key);
                if (connectionGroup == null) {
                    connectionGroup = new SshConnectionGroup(this, poolPolicy);
                    pool.put(key, connectionGroup);
                }
            }
            connectionGroup.touch();
        } finally {
            poolRWLock.writeLock().unlock();
        }

        // reserveSessionOnConnection may block, so  be careful calling it - no lock is held while
        // we call it, so we need to be sure that cleanup doesnt remove the connectionGroup while
        // we are waiting for our session.  For this reason, the group has a flag that says it's
        // newly created (set to true on construction).  After any connection/session attempt is made,
        // the group sets the newly created flag to false - meaning it can be removed if no connections
        // exist.

        // TODO:  MUST LOOK AT THIS - this won't handle the case that we are reusing a group that currently has no
        // TODO:  connections on it.  I need to fix that.  I'm not sure how though.

        session = connectionGroup.reserveSessionOnConnection(tenant, host, port, effectiveUserId,
                authnMethod, credential, channelConstructor, wait);
        String msg = MsgUtils.getMsg("SSH_POOL_RESERVE_ELAPSED_TIME", System.currentTimeMillis() - startTime);
        log.debug(msg);

        return new PooledSshSession<T>(connectionGroup, session);
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
        poolRWLock.readLock().lock();
        try {
            for (SshSessionPoolKey key : pool.keySet()) {
                builder.append(" -> ");
                builder.append(key);
                builder.append(System.lineSeparator());
                SshConnectionGroup connectionGroup = pool.get(key);
                builder.append(connectionGroup.toString());
            }
        } finally {
            poolRWLock.readLock().unlock();
        }
        return builder.toString();
    }

    private void cleanup() {
        for (SshSessionPoolKey key : pool.keySet()) {
            SshConnectionGroup connectionGroup = pool.get(key);
            connectionGroup.cleanup();
        }

        poolRWLock.writeLock().lock();
        try {
            pool.entrySet().removeIf(entry -> {
                return entry.getValue().isReadyForCleanup();
            });
        } finally {
            poolRWLock.writeLock().unlock();
        }

        if(traceOnCleanupCounter.incrementAndGet() >= poolPolicy.getTraceDuringCleanupFrequency()) {
            traceOnCleanupCounter.set(0);
            log.debug("====================================================");
            log.debug(instance.toString());
            log.debug("====================================================");
        }
    }

}
