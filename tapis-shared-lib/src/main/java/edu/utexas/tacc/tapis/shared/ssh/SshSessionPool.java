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
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class SshSessionPool {
    private static final Logger log = LoggerFactory.getLogger(SshSessionPool.class);
    private static SshSessionPool instance;

    private final SshSessionPoolPolicy poolPolicy;
    ScheduledFuture<?> poolCleanupTaskFuture;
    private static final long MAX_CLEANUP_LOCK_WAIT_MS = 5000;

    /**
     * This lock controls access to the pool.  There can be any number of readers, but only one
     * writer.  You only need to aquire the write lock if you are going to add or remove an
     * entry from the pool.  Everything else is read.  Modifying the values stored in the pool
     * is synchronized/protected by the SshConnectionGroup class.
     */
    ReentrantReadWriteLock poolRWLock = new ReentrantReadWriteLock(true);

    private AtomicInteger traceOnCleanupCounter = new AtomicInteger(0);

    public class PooledSshSession<T extends SSHSession> implements AutoCloseable {
        private final SshConnectionGroup sshConnectionGroup;
        private final SshSessionHolder<T> sessionHolder;

        PooledSshSession(SshConnectionGroup sshConnectionGroup, SshSessionHolder<T> sessionHolder) {
            this.sshConnectionGroup = sshConnectionGroup;
            this.sessionHolder = sessionHolder;
        }

        public T getSession() {
           return (T) sessionHolder.getSession();
        }


        @Override
        public void close() {
            sessionHolder.release();
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
            try {
                cleanup();
            } catch (Throwable th) {
                String msg = MsgUtils.getMsg("SSH_POOL_CLEANUP_EXCEPTION");
                log.warn(msg, th);
            }
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
                SSHExecChannel.class, wait);
    }

    public PooledSshSession<SSHSftpClient> borrowSftpClient(String tenant, String host, Integer port, String effectiveUserId,
                                                            AuthnEnum authnMethod, Credential credential, Duration wait) throws TapisException {
        return reserveSessionOnConnection(tenant, host, port, effectiveUserId, authnMethod, credential,
                SSHSftpClient.class, wait);
    }

    private <T extends SSHSession> PooledSshSession<T> reserveSessionOnConnection(String tenant, String host, Integer port, String effectiveUserId,
                                                                                  AuthnEnum authnMethod, Credential credential,
                                                                                  Class<T> clazz,
                                                                                  Duration wait) throws TapisException {
        long startTime = System.currentTimeMillis();
        SshSessionPoolKey key = new SshSessionPoolKey(tenant, host, port, effectiveUserId, authnMethod, credential);
        SshConnectionGroup connectionGroup = null;
        SshSessionHolder<T> sessionHolder = null;

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
                    connectionGroup = new SshConnectionGroup(poolPolicy);
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
        sessionHolder = connectionGroup.reserveSessionOnConnection(tenant, host, port, effectiveUserId,
                authnMethod, credential, clazz, wait);
        long elapsedTime = System.currentTimeMillis() - startTime;

        // log the elapsed time here for getting a connection.  If it's more than a minute, make it a
        // warning.  It shouldn't be anywhere near that.  Under very heavy load, there could be longer
        // waiths though due to the sessions in the pool being exhausted.
        String msg = MsgUtils.getMsg("SSH_POOL_RESERVE_ELAPSED_TIME", elapsedTime);
        if(elapsedTime > 60000) {
            log.warn(msg);
        } else {
            log.debug(msg);
        }

        return new PooledSshSession<T>(connectionGroup, sessionHolder);
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
        log.info("SshSessionPool cleanup counter: " + traceOnCleanupCounter.get());
        for (SshSessionPoolKey key : pool.keySet()) {
            SshConnectionGroup connectionGroup = pool.get(key);
            connectionGroup.cleanup();
        }

        try {
            if (poolRWLock.writeLock().tryLock(MAX_CLEANUP_LOCK_WAIT_MS, TimeUnit.MILLISECONDS)) {
                pool.entrySet().removeIf(entry -> {
                    return entry.getValue().isReadyForCleanup();
                });
            }
        } catch (InterruptedException ex) {
            // We will just skip this part of the cleanup if we were interrupted while waiting for the lock.
            // the finally block handles that case already.
        } finally {
            if(poolRWLock.isWriteLocked()) {
                poolRWLock.writeLock().unlock();
            } else {
                //  If we didn't get the lock, we will just skip this part of the cleanup.  There's really no
                //  negatives to skipping the clenaup periodically since the cleanup that really matters is already
                //  done above, and when connections are attempted.  This cleanup step will just remove tenant/system/user
                //  combinations from the pool when they have no connections to them.  The only consequence to this is
                //  using a tiny bit more memory than we absolutely need to.
                String msg = MsgUtils.getMsg("SSH_POOL_CLEANUP_SKIPPED");
                log.warn(msg);
            }
        }

        if(traceOnCleanupCounter.incrementAndGet() >= poolPolicy.getTraceDuringCleanupFrequency()) {
            traceOnCleanupCounter.set(0);
            log.info("====================================================");
            log.info(instance.toString());
            log.info("====================================================");
        }
    }

}
