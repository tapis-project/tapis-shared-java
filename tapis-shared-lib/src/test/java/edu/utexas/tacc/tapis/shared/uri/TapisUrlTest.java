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
        String host = "tapis.io";
        String system = "mysystem";
        String path = "my/file/path";
        var url = new TapisUrl(host, system, path);
        
        Assert.assertEquals(url.getHost(), host);
        Assert.assertEquals(url.getSystemId(), system);
        Assert.assertEquals(url.getPath(), "/" + path);
        
        // ---------------
        host = "tapis.io";
        system = "mysystem";
        path = "/my/other/file/path";
        url = new TapisUrl(host, system, path);
        
        Assert.assertEquals(url.getHost(), host);
        Assert.assertEquals(url.getSystemId(), system);
        Assert.assertEquals(url.getPath(), path);

        // ---------------
        host = "tapis.io";
        system = "mysystem";
        path = null;
        url = new TapisUrl(host, system, path);
        
        Assert.assertEquals(url.getHost(), host);
        Assert.assertEquals(url.getSystemId(), system);
        Assert.assertEquals(url.getPath(), "/");
    }

    @Test(enabled=true)
    public void test2() throws TapisException
    {
        final String host = "tapis.io"; 
        
        String baseUrl = "tapis://" + host;
        String system = "mysystem";
        String path = "my/file/path";
        var url = new TapisUrl(baseUrl, system, path);
        
        Assert.assertEquals(url.getHost(), host);
        Assert.assertEquals(url.getSystemId(), system);
        Assert.assertEquals(url.getPath(), "/" + path);
        
        // ---------------
        baseUrl = "tapis://" + host;
        system = "mysystem";
        path = "/my/other/file/path";
        url = new TapisUrl(baseUrl, system, path);
        
        Assert.assertEquals(url.getHost(), host);
        Assert.assertEquals(url.getSystemId(), system);
        Assert.assertEquals(url.getPath(), path);

        // ---------------
        baseUrl = "tapis://" + host;
        system = "mysystem";
        path = null;
        url = new TapisUrl(baseUrl, system, path);
        
        Assert.assertEquals(url.getHost(), host);
        Assert.assertEquals(url.getSystemId(), system);
        Assert.assertEquals(url.getPath(), "/");
    }

    @Test(enabled=true)
    public void test3() throws TapisException
    {
        final String host = "tapis.io"; 
        
        String baseUrl = "tapis://" + host + "/";
        String system = "mysystem";
        String path = "my/file/path";
        var url = new TapisUrl(baseUrl, system, path);
        
        Assert.assertEquals(url.getHost(), host);
        Assert.assertEquals(url.getSystemId(), system);
        Assert.assertEquals(url.getPath(), "/" + path);
        
        // ---------------
        baseUrl = "tapis://" + host + "/";
        system = "mysystem";
        path = "/my/other/file/path";
        url = new TapisUrl(baseUrl, system, path);
        
        Assert.assertEquals(url.getHost(), host);
        Assert.assertEquals(url.getSystemId(), system);
        Assert.assertEquals(url.getPath(), path);

        // ---------------
        baseUrl = "tapis://" + host + "/";
        system = "mysystem";
        path = null;
        url = new TapisUrl(baseUrl, system, path);
        
        Assert.assertEquals(url.getHost(), host);
        Assert.assertEquals(url.getSystemId(), system);
        Assert.assertEquals(url.getPath(), "/");
    }

    @Test(enabled=true)
    public void test4() throws TapisException
    {
        final String host = "tapis.io"; 
        
        String baseUrl = "tapis://" + host + "/";
        String system = "mysystem";
        String path = "/my/file/path";
        var url = new TapisUrl(baseUrl, system, path);
        
        String urlString = baseUrl + system + path;
        var url2 = TapisUrl.makeTapisUrl(urlString);
        Assert.assertEquals(url, url2);

        // ---------------
        baseUrl = "tapis://" + host + "/";
        system = "mysystem";
        path = null;
        url = new TapisUrl(baseUrl, system, path);

        urlString = baseUrl + system;
        url2 = TapisUrl.makeTapisUrl(urlString);
        Assert.assertEquals(url, url2);
    }
}
