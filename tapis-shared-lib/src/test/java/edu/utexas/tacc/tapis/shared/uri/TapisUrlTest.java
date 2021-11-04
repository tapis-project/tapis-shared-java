package edu.utexas.tacc.tapis.shared.uri;

import org.testng.Assert;
import org.testng.annotations.Test;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;

@Test(groups={"unit"})
public class TapisUrlTest 
{
    @Test(enabled=true)
    public void test1() throws TapisException
    {
        String system = "mysystem";
        String path = "my/file/path";
        var url = new TapisUrl(system, path);
        
        Assert.assertEquals(url.getSystemId(), system);
        Assert.assertEquals(url.getPath(), "/" + path);
        
        // ---------------
        system = "mysystem";
        path = "/my/other/file/path";
        url = new TapisUrl(system, path);
        
        Assert.assertEquals(url.getSystemId(), system);
        Assert.assertEquals(url.getPath(), path);

        // ---------------
        system = "mysystem";
        path = null;
        url = new TapisUrl(system, path);
        
        Assert.assertEquals(url.getSystemId(), system);
        Assert.assertEquals(url.getPath(), "/");
    }

    @Test(enabled=true)
    public void test2() throws TapisException
    {
        String system = "mysystem";
        String path = "my/file/path";
        var url = new TapisUrl(system, path);
        
        Assert.assertEquals(url.getSystemId(), system);
        Assert.assertEquals(url.getPath(), "/" + path);
        
        // ---------------
        system = "mysystem";
        path = "/my/other/file/path";
        url = new TapisUrl(system, path);
        
        Assert.assertEquals(url.getSystemId(), system);
        Assert.assertEquals(url.getPath(), path);

        // ---------------
        system = "mysystem";
        path = null;
        url = new TapisUrl(system, path);
        
        Assert.assertEquals(url.getSystemId(), system);
        Assert.assertEquals(url.getPath(), "/");
    }

    @Test(enabled=true)
    public void test3() throws TapisException
    {
        String system = "mysystem";
        String path = "my/file/path";
        var url = new TapisUrl(system, path);
        
        Assert.assertEquals(url.getSystemId(), system);
        Assert.assertEquals(url.getPath(), "/" + path);
        
        // ---------------
        system = "mysystem";
        path = "/my/other/file/path";
        url = new TapisUrl(system, path);
        
        Assert.assertEquals(url.getSystemId(), system);
        Assert.assertEquals(url.getPath(), path);

        // ---------------
        system = "mysystem";
        path = null;
        url = new TapisUrl(system, path);
        
        Assert.assertEquals(url.getSystemId(), system);
        Assert.assertEquals(url.getPath(), "/");
    }

    @Test(enabled=true)
    public void test4() throws TapisException
    {
        String system = "mysystem";
        String path = "/my/file/path";
        var url = new TapisUrl(system, path);
        
        String urlString = system + path;
        var url2 = TapisUrl.makeTapisUrl(TapisUrl.TAPIS_PROTOCOL_PREFIX + urlString );
        Assert.assertEquals(url, url2);

        // ---------------
        system = "mysystem";
        path = null;
        url = new TapisUrl(system, path);

        urlString = system;
        url2 = TapisUrl.makeTapisUrl(TapisUrl.TAPIS_PROTOCOL_PREFIX + urlString);
        Assert.assertEquals(url, url2);
    }

    @Test(enabled=true)
    public void test5() throws TapisException
    {
        String system = TapisLocalUrl.TAPISLOCAL_EXEC_SYSTEM;
        String path = "/my/file/path";
        var url = new TapisLocalUrl(path);
        
        String urlString = system + path;
        var url2 = TapisLocalUrl.makeTapisLocalUrl(TapisLocalUrl.TAPISLOCAL_PROTOCOL_PREFIX + urlString );
        Assert.assertEquals(url, url2);

        // ---------------
        system = TapisLocalUrl.TAPISLOCAL_EXEC_SYSTEM;
        path = null;
        url = new TapisLocalUrl(path);

        urlString = system;
        url2 = TapisLocalUrl.makeTapisLocalUrl(TapisLocalUrl.TAPISLOCAL_PROTOCOL_PREFIX + urlString);
        Assert.assertEquals(url, url2);
    }
}
