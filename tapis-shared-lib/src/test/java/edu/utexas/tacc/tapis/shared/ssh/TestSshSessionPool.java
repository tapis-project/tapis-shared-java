package edu.utexas.tacc.tapis.shared.ssh;

import edu.utexas.tacc.tapis.shared.ssh.apache.SSHExecChannel;
import edu.utexas.tacc.tapis.systems.client.gen.model.AuthnEnum;
import edu.utexas.tacc.tapis.systems.client.gen.model.Credential;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.Properties;

/**
 * To run this test:
 *
 *
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

    @Test
    public void testPoolBasics() throws Exception {
        SshSessionPoolPolicy poolPolicy = SshSessionPoolPolicy.defaultPolicy()
                .setMaxConnectionDuration(Duration.ofSeconds(30))
                .setMaxConnectionsPerKey(2)
                .setMaxSessionsPerConnection(2);
        SshSessionPool.init(poolPolicy);
        SshSessionPool pool = SshSessionPool.INSTANCE;
        Assert.assertTrue(pool.getConnectionStats().getConnectionCount() <= 0);
        Assert.assertEquals(pool.getConnectionStats().getSessionCount(),0);

        SSHExecChannel channel1 = pool.borrowExecChannel(tenant_1, host_1, port_1,
                userId_1, authnMethod_1, credential_1, Duration.ZERO);
        Assert.assertTrue(pool.getConnectionStats().getConnectionCount() <= 2);
        Assert.assertEquals(pool.getConnectionStats().getSessionCount(), 1);

        SSHExecChannel channel2 = pool.borrowExecChannel(tenant_1, host_1, port_1,
                userId_1, authnMethod_1, credential_1, Duration.ZERO);
        Assert.assertTrue(pool.getConnectionStats().getConnectionCount() <= 2);
        Assert.assertEquals(pool.getConnectionStats().getSessionCount(), 2);

        SSHExecChannel channel3 = pool.borrowExecChannel(tenant_1, host_1, port_1,
                userId_1, authnMethod_1, credential_1, Duration.ZERO);
        Assert.assertTrue(pool.getConnectionStats().getConnectionCount() <= 2);
        Assert.assertEquals(pool.getConnectionStats().getSessionCount(), 3);

        SSHExecChannel channel4 = pool.borrowExecChannel(tenant_1, host_1, port_1,
                userId_1, authnMethod_1, credential_1, Duration.ZERO);
        Assert.assertTrue(pool.getConnectionStats().getConnectionCount() <= 2);
        Assert.assertEquals(pool.getConnectionStats().getSessionCount(), 4);

        pool.returnExecChannel(channel1);
        Assert.assertTrue(pool.getConnectionStats().getConnectionCount() <= 2);
        Assert.assertEquals(pool.getConnectionStats().getSessionCount(), 3);

        SSHExecChannel channel5 = pool.borrowExecChannel(tenant_1, host_1, port_1,
                userId_1, authnMethod_1, credential_1, Duration.ZERO);
        Assert.assertTrue(pool.getConnectionStats().getConnectionCount() <= 2);
        Assert.assertEquals(pool.getConnectionStats().getSessionCount(), 4);

        pool.returnExecChannel(channel5);
        Assert.assertTrue(pool.getConnectionStats().getConnectionCount() <= 2);
        Assert.assertEquals(pool.getConnectionStats().getSessionCount(), 3);

        pool.returnExecChannel(channel2);
        Assert.assertTrue(pool.getConnectionStats().getConnectionCount() <= 2);
        Assert.assertEquals(pool.getConnectionStats().getSessionCount(), 2);

        pool.returnExecChannel(channel3);
        Assert.assertTrue(pool.getConnectionStats().getConnectionCount() <= 2);
        Assert.assertEquals(pool.getConnectionStats().getSessionCount(), 1);

        pool.returnExecChannel(channel4);
        Assert.assertTrue(pool.getConnectionStats().getConnectionCount() <= 2);
        Assert.assertEquals(pool.getConnectionStats().getSessionCount(), 0);
    }

    @Test
    public void testWait() throws Exception {
        SshSessionPoolPolicy poolPolicy = SshSessionPoolPolicy.defaultPolicy()
                .setMaxConnectionDuration(Duration.ofSeconds(30))
                .setMaxConnectionsPerKey(2)
                .setMaxSessionsPerConnection(2);
        SshSessionPool.init(poolPolicy);
        SshSessionPool pool = SshSessionPool.INSTANCE;
        SSHExecChannel channel1 = pool.borrowExecChannel(tenant_1, host_1, port_1,
                userId_1, authnMethod_1, credential_1, Duration.ZERO);
        SSHExecChannel channel2 = pool.borrowExecChannel(tenant_1, host_1, port_1,
                userId_1, authnMethod_1, credential_1, Duration.ZERO);
        SSHExecChannel channel3 = pool.borrowExecChannel(tenant_1, host_1, port_1,
                userId_1, authnMethod_1, credential_1, Duration.ZERO);
        SSHExecChannel channel4 = pool.borrowExecChannel(tenant_1, host_1, port_1,
                userId_1, authnMethod_1, credential_1, Duration.ZERO);
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("Begin Thread");
                    Thread.sleep(2000);
                    System.out.println("Releasing Channel");
                    pool.returnExecChannel(channel1);
                    System.out.println("End Thread");
                } catch (InterruptedException ex) {
                    throw new RuntimeException("Sleep interruption", ex);
                }
            }
        });
        th.start();
        try {
            SSHExecChannel channel5 = pool.borrowExecChannel(tenant_1, host_1, port_1,
                    userId_1, authnMethod_1, credential_1, Duration.ZERO);
            Assert.fail("Wait for session returned a session when it shouldn't have");
        } catch (Exception ex) {
        }
        SSHExecChannel channel5 = pool.borrowExecChannel(tenant_1, host_1, port_1,
                userId_1, authnMethod_1, credential_1, Duration.ofSeconds(3));
        th.join();

    }

//    private TapisSystem getSshSystem(String tenant, String systemId, String host, Integer port,
//                                     String effectiveUserId, Credential credential,
//                                     String rootDir, AuthnEnum defaultAuthMethod) {
//        TapisSystem tapisSystem = new TapisSystem();
//        tapisSystem.setId(systemId);
//        tapisSystem.setHost(host);
//        tapisSystem.setEffectiveUserId(effectiveUserId);
//        tapisSystem.setPort(port);
//        tapisSystem.setRootDir(rootDir);
////        Credential credential = new Credential();
////        credential.setLoginUser(effectiveUserId);
////        credential.setPassword(password);
//        tapisSystem.setAuthnCredential(credential);
//        tapisSystem.setDefaultAuthnMethod(defaultAuthMethod);
//        tapisSystem.setTenant(tenant);
//        return tapisSystem;
//    }
}
