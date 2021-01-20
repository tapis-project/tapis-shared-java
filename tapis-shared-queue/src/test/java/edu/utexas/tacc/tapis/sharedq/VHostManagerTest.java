package edu.utexas.tacc.tapis.sharedq;

import org.testng.annotations.Test;

@Test(groups={"integration"})
public class VHostManagerTest 
{
    // Configuration.
    private static final String HOST       = "localhost";
    private static final int    PORT       = 15672;
    private static final String ADMIN_USER = "tapis";
    private static final String ADMIN_PASS = "password";
    private static final String JOBS_VHOST = "jobshost";
    private static final String JOBS_USER  = "jobs";
    private static final String JOBS_PASS  = "password";
    
    @Test
    public void createVHostTest() throws Exception
    {
        var parms = new VHostParms(HOST, PORT, ADMIN_USER, ADMIN_PASS);
        var mgr   = new VHostManager(parms);
        mgr.initVHost(JOBS_VHOST, JOBS_USER, JOBS_PASS);
    }
}
