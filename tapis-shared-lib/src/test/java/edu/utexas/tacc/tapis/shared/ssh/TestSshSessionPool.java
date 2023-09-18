package edu.utexas.tacc.tapis.shared.ssh;

import edu.utexas.tacc.tapis.shared.ssh.apache.SSHExecChannel;
import edu.utexas.tacc.tapis.shared.ssh.apache.SSHSftpClient;
import edu.utexas.tacc.tapis.systems.client.gen.model.AuthnEnum;
import edu.utexas.tacc.tapis.systems.client.gen.model.Credential;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Properties;

/**
 * To run this test:
 *    Configure TestSshSessionPool.properties for running locally.
 *    Test is marked skip_for_build due to needing ssh keys to run.
 */
@Test(groups={"integration"})
public class TestSshSessionPool {

    private final String tenant_1;
    private final  String host_1;
    private final String userId_1;
    private final String publicKey_1;
    private final String privateKey_1;
    private final AuthnEnum authnMethod_1;
    private final Integer port_1;
    private final Credential credential_1;

    public TestSshSessionPool() throws IOException {

        Properties properties = new Properties();
        properties.load(getClass().getResourceAsStream("TestSshSessionPool.properties"));
        tenant_1 = properties.getProperty("TEST_POOL_TENANT_1", "testTenant");
        host_1 = properties.getProperty("TEST_POOL_HOST_1", "tapisv3-exec1");
        userId_1 = properties.getProperty("TEST_POOL_USERID_1", "sshTestUser1");
        publicKey_1 = properties.getProperty("TEST_POOL_PUBLIC_KEY_1");
        privateKey_1 = properties.getProperty("TEST_POOL_PRIVATE_KEY_1");
        authnMethod_1 = AuthnEnum.valueOf(properties.getProperty("TEST_POOL_AUTHN_METHOD_1", "PKI_KEYS"));
        port_1 = Integer.valueOf(properties.getProperty("TEST_POOL_PORT_1", "-1"));
        credential_1 = new Credential();
        credential_1.setPublicKey(publicKey_1);
        credential_1.setPrivateKey(privateKey_1);
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        // since we are testing the pool, we will need to be able to recreate it for each test.  This code
        // uses reflection to alter the visibility of the instance (static variable) to allow us to recreate
        // the pool.
        Field poolInstanceField = SshSessionPool.class.getDeclaredField("instance");
        poolInstanceField.setAccessible(true);
        poolInstanceField.set(null, null);
        if( ("YourPublicKeyGoesHere".equals(publicKey_1)) || ("YourPrivateKeyGoesHere").equals(privateKey_1)) {
            Assert.fail("Set your ssh public/private key in the file called \"TestSshSessionPool.properties\".  Convert private key to single line like tapis uses, and don't quote the value");
        }
    }

    @Test
    public void testPoolBasics() throws Exception {
        SshSessionPoolPolicy poolPolicy = SshSessionPoolPolicy.defaultPolicy()
                .setMaxConnectionDuration(Duration.ofSeconds(30))
                .setMaxConnectionsPerKey(2)
                .setMaxSessionsPerConnection(2);
        SshSessionPool.init(poolPolicy);
        Assert.assertTrue(SshSessionPool.getInstance().getConnectionStats().getConnectionCount() <= 0);
        Assert.assertEquals(SshSessionPool.getInstance().getInstance().getConnectionStats().getSessionCount(),0);

        SshSessionPool.PooledSshSession<SSHExecChannel> channel1 = SshSessionPool.getInstance().borrowExecChannel(tenant_1, host_1, port_1,
                userId_1, authnMethod_1, credential_1, Duration.ZERO);
        Assert.assertTrue(SshSessionPool.getInstance().getConnectionStats().getConnectionCount() <= 2);
        Assert.assertEquals(SshSessionPool.getInstance().getConnectionStats().getSessionCount(), 1);

        SshSessionPool.PooledSshSession<SSHExecChannel> channel2 = SshSessionPool.getInstance().borrowExecChannel(tenant_1, host_1, port_1,
                userId_1, authnMethod_1, credential_1, Duration.ZERO);
        Assert.assertTrue(SshSessionPool.getInstance().getConnectionStats().getConnectionCount() <= 2);
        Assert.assertEquals(SshSessionPool.getInstance().getConnectionStats().getSessionCount(), 2);

        SshSessionPool.PooledSshSession<SSHExecChannel> channel3 = SshSessionPool.getInstance().borrowExecChannel(tenant_1, host_1, port_1,
                userId_1, authnMethod_1, credential_1, Duration.ZERO);
        Assert.assertTrue(SshSessionPool.getInstance().getConnectionStats().getConnectionCount() <= 2);
        Assert.assertEquals(SshSessionPool.getInstance().getConnectionStats().getSessionCount(), 3);

        SshSessionPool.PooledSshSession<SSHExecChannel> channel4 = SshSessionPool.getInstance().borrowExecChannel(tenant_1, host_1, port_1,
                userId_1, authnMethod_1, credential_1, Duration.ZERO);
        Assert.assertTrue(SshSessionPool.getInstance().getConnectionStats().getConnectionCount() <= 2);
        Assert.assertEquals(SshSessionPool.getInstance().getConnectionStats().getSessionCount(), 4);

        channel1.close();
        Assert.assertTrue(SshSessionPool.getInstance().getConnectionStats().getConnectionCount() <= 2);
        Assert.assertEquals(SshSessionPool.getInstance().getConnectionStats().getSessionCount(), 3);

        SshSessionPool.PooledSshSession<SSHExecChannel> channel5 = SshSessionPool.getInstance().borrowExecChannel(tenant_1, host_1, port_1,
                userId_1, authnMethod_1, credential_1, Duration.ZERO);
        Assert.assertTrue(SshSessionPool.getInstance().getConnectionStats().getConnectionCount() <= 2);
        Assert.assertEquals(SshSessionPool.getInstance().getConnectionStats().getSessionCount(), 4);

        channel5.close();
        Assert.assertTrue(SshSessionPool.getInstance().getConnectionStats().getConnectionCount() <= 2);
        Assert.assertEquals(SshSessionPool.getInstance().getConnectionStats().getSessionCount(), 3);

        channel2.close();
        Assert.assertTrue(SshSessionPool.getInstance().getConnectionStats().getConnectionCount() <= 2);
        Assert.assertEquals(SshSessionPool.getInstance().getConnectionStats().getSessionCount(), 2);

        channel3.close();
        Assert.assertTrue(SshSessionPool.getInstance().getConnectionStats().getConnectionCount() <= 2);
        Assert.assertEquals(SshSessionPool.getInstance().getConnectionStats().getSessionCount(), 1);

        channel4.close();
        Assert.assertTrue(SshSessionPool.getInstance().getConnectionStats().getConnectionCount() <= 2);
        Assert.assertEquals(SshSessionPool.getInstance().getConnectionStats().getSessionCount(), 0);
    }

    @Test
    public void testWait() throws Exception {
        SshSessionPoolPolicy poolPolicy = SshSessionPoolPolicy.defaultPolicy()
                .setMaxConnectionDuration(Duration.ofSeconds(30))
                .setMaxConnectionsPerKey(2)
                .setMaxSessionsPerConnection(2);
        SshSessionPool.init(poolPolicy);
        SshSessionPool.PooledSshSession<SSHExecChannel> channel1 = SshSessionPool.getInstance().borrowExecChannel(tenant_1, host_1, port_1,
                userId_1, authnMethod_1, credential_1, Duration.ZERO);
        SshSessionPool.PooledSshSession<SSHExecChannel> channel2 = SshSessionPool.getInstance().borrowExecChannel(tenant_1, host_1, port_1,
                userId_1, authnMethod_1, credential_1, Duration.ZERO);
        SshSessionPool.PooledSshSession<SSHExecChannel> channel3 = SshSessionPool.getInstance().borrowExecChannel(tenant_1, host_1, port_1,
                userId_1, authnMethod_1, credential_1, Duration.ZERO);
        SshSessionPool.PooledSshSession<SSHExecChannel> channel4 = SshSessionPool.getInstance().borrowExecChannel(tenant_1, host_1, port_1,
                userId_1, authnMethod_1, credential_1, Duration.ZERO);
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("Begin Thread");
                    Thread.sleep(2000);
                    System.out.println("Releasing Channel");
                    channel1.close();
                    System.out.println("End Thread");
                } catch (InterruptedException ex) {
                    throw new RuntimeException("Sleep interruption", ex);
                }
            }
        });
        th.start();
        try {
            SshSessionPool.PooledSshSession<SSHExecChannel> channel5 = SshSessionPool.getInstance().borrowExecChannel(tenant_1, host_1, port_1,
                    userId_1, authnMethod_1, credential_1, Duration.ZERO);
            Assert.fail("Wait for session returned a session when it shouldn't have");
        } catch (Exception ex) {
        }
        SshSessionPool.PooledSshSession<SSHExecChannel> channel5 = SshSessionPool.getInstance().borrowExecChannel(tenant_1, host_1, port_1,
                userId_1, authnMethod_1, credential_1, Duration.ofSeconds(3));
        th.join();

    }

    @Test
    public void testAutoCloseSessions() throws Exception {
        SshSessionPoolPolicy poolPolicy = SshSessionPoolPolicy.defaultPolicy()
                .setMaxConnectionDuration(Duration.ofSeconds(30))
                .setMaxConnectionsPerKey(2)
                .setMaxSessionsPerConnection(2);
        SshSessionPool.init(poolPolicy);
        Assert.assertTrue(SshSessionPool.getInstance().getConnectionStats().getConnectionCount() <= 0);
        Assert.assertEquals(SshSessionPool.getInstance().getConnectionStats().getSessionCount(),0);

        try (SshSessionPool.PooledSshSession<SSHExecChannel> channel1 = SshSessionPool.getInstance().borrowExecChannel(tenant_1, host_1, port_1,
                userId_1, authnMethod_1, credential_1, Duration.ZERO)) {
            Assert.assertTrue(SshSessionPool.getInstance().getConnectionStats().getConnectionCount() <= 2);
            Assert.assertEquals(SshSessionPool.getInstance().getConnectionStats().getSessionCount(), 1);
        }
        Assert.assertTrue(SshSessionPool.getInstance().getConnectionStats().getConnectionCount() <= 2);
        Assert.assertEquals(SshSessionPool.getInstance().getConnectionStats().getSessionCount(), 0);

        try (SshSessionPool.PooledSshSession<SSHSftpClient> channel1 = SshSessionPool.getInstance().borrowSftpClient(tenant_1, host_1, port_1,
                userId_1, authnMethod_1, credential_1, Duration.ZERO)) {
            Assert.assertTrue(SshSessionPool.getInstance().getConnectionStats().getConnectionCount() <= 2);
            Assert.assertEquals(SshSessionPool.getInstance().getConnectionStats().getSessionCount(), 1);
        }
        Assert.assertTrue(SshSessionPool.getInstance().getConnectionStats().getConnectionCount() <= 2);
        Assert.assertEquals(SshSessionPool.getInstance().getConnectionStats().getSessionCount(), 0);


        try (SshSessionPool.PooledSshSession<SSHExecChannel> channel1 = SshSessionPool.getInstance().borrowExecChannel(tenant_1, host_1, port_1,
                userId_1, authnMethod_1, credential_1, Duration.ZERO);
             SshSessionPool.PooledSshSession<SSHSftpClient> channel2 = SshSessionPool.getInstance().borrowSftpClient(tenant_1, host_1, port_1,
                userId_1, authnMethod_1, credential_1, Duration.ZERO)) {
            Assert.assertTrue(SshSessionPool.getInstance().getConnectionStats().getConnectionCount() <= 2);
            Assert.assertEquals(SshSessionPool.getInstance().getConnectionStats().getSessionCount(), 2);
        }
        Assert.assertTrue(SshSessionPool.getInstance().getConnectionStats().getConnectionCount() <= 2);
        Assert.assertEquals(SshSessionPool.getInstance().getConnectionStats().getSessionCount(), 0);

    }

    @Test
    public void testMaxIdle() throws Exception {
        SshSessionPoolPolicy poolPolicy = SshSessionPoolPolicy.defaultPolicy()
                .setMaxConnectionDuration(Duration.ofSeconds(30))
                .setMaxConnectionIdleTime(Duration.ofSeconds(2))
                .setCleanupInterval(Duration.ofSeconds(1))
                .setMaxConnectionsPerKey(2)
                .setMaxSessionsPerConnection(2);
        SshSessionPool.init(poolPolicy);
        Assert.assertTrue(SshSessionPool.getInstance().getConnectionStats().getConnectionCount() <= 0);
        Assert.assertEquals(SshSessionPool.getInstance().getConnectionStats().getSessionCount(),0);

        try (SshSessionPool.PooledSshSession<SSHExecChannel> channel1 = SshSessionPool.getInstance().borrowExecChannel(tenant_1, host_1, port_1,
                userId_1, authnMethod_1, credential_1, Duration.ZERO)) {
            Assert.assertTrue(SshSessionPool.getInstance().getConnectionStats().getConnectionCount() <= 2);
            Assert.assertEquals(SshSessionPool.getInstance().getConnectionStats().getSessionCount(), 1);
        }
        Assert.assertTrue(SshSessionPool.getInstance().getConnectionStats().getConnectionCount() > 0);
        Thread.sleep(3000);
        Assert.assertTrue(SshSessionPool.getInstance().getConnectionStats().getConnectionCount() == 0);
    }

    @Test
    public void testMaxLifetime() throws Exception {
        SshSessionPoolPolicy poolPolicy = SshSessionPoolPolicy.defaultPolicy()
                .setMaxConnectionDuration(Duration.ofSeconds(10))
                .setMaxConnectionIdleTime(Duration.ofSeconds(2))
                .setCleanupInterval(Duration.ofSeconds(1))
                .setMaxConnectionsPerKey(2)
                .setMaxSessionsPerConnection(2);
        SshSessionPool.init(poolPolicy);
        Assert.assertTrue(SshSessionPool.getInstance().getConnectionStats().getConnectionCount() == 0);
        Assert.assertEquals(SshSessionPool.getInstance().getConnectionStats().getSessionCount(),0);

        long startTime = System.currentTimeMillis();

        SshSessionPool.PooledSshSession<SSHExecChannel> longRunningChannel = SshSessionPool.getInstance().borrowExecChannel(tenant_1, host_1, port_1,
                userId_1, authnMethod_1, credential_1, Duration.ZERO);
        boolean connectionExpired = false;
        for(int i = 0;i < 20;i++) {
            try (SshSessionPool.PooledSshSession<SSHExecChannel> channel1 = SshSessionPool.getInstance().borrowExecChannel(tenant_1, host_1, port_1,
                    userId_1, authnMethod_1, credential_1, Duration.ZERO)) {
                Assert.assertTrue(SshSessionPool.getInstance().getConnectionStats().getConnectionCount() <= 2);
                Assert.assertEquals(SshSessionPool.getInstance().getConnectionStats().getSessionCount(), 2);
            }
            Thread.sleep(1000);
            if(SshSessionPool.getInstance().getConnectionStats().getConnectionCount() == 2) {
                // if the pool has opened a second connection, it means the first one (with the long running connection) is
                // expired - 30 seconds or more should have elapsed.
                Assert.assertTrue(System.currentTimeMillis() - startTime > 10000);
                connectionExpired = true;
                break;
            }
        }
        Assert.assertTrue(connectionExpired);
        longRunningChannel.close();
        // give cleanup a change to run, then we should clean up the expired connection, and still have the one from the
        // long running channel.
        Thread.sleep(1000);
        Assert.assertEquals(SshSessionPool.getInstance().getConnectionStats().getConnectionCount(), 1);
        Thread.sleep(3000);
        // check for idle conneciont closing
        Assert.assertEquals(SshSessionPool.getInstance().getConnectionStats().getConnectionCount(), 0);
    }

}
