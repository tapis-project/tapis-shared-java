package edu.utexas.tacc.tapis.shared.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups={"unit"})
public class TapisUrlUtilsTest 
{
	@Test(enabled=true)
	public void test1()
	{
		String host = "tapis.io";
		String system = "mysystem";
		String path = "my/file/path";
		String url = TapisUrlUtils.makeTapisUrl(host, system, path);
		
		Assert.assertEquals(TapisUrlUtils.getHost(url), host);
		Assert.assertEquals(TapisUrlUtils.getSystemId(url), system);
		Assert.assertEquals(TapisUrlUtils.getPath(url), path);
		
		// ---------------
		host = "tapis.io";
		system = "mysystem";
		path = "/my/other/file/path";
		url = TapisUrlUtils.makeTapisUrl(host, system, path);
		
		Assert.assertEquals(TapisUrlUtils.getHost(url), host);
		Assert.assertEquals(TapisUrlUtils.getSystemId(url), system);
		Assert.assertEquals(TapisUrlUtils.getPath(url), path.substring(1));

		// ---------------
		host = "tapis.io";
		system = "mysystem";
		path = null;
		url = TapisUrlUtils.makeTapisUrl(host, system, path);
		
		Assert.assertEquals(TapisUrlUtils.getHost(url), host);
		Assert.assertEquals(TapisUrlUtils.getSystemId(url), system);
		Assert.assertEquals(TapisUrlUtils.getPath(url), "");
	}

	@Test(enabled=true)
	public void test2()
	{
		final String host = "tapis.io"; 
		
		String baseUrl = "tapis://" + host;
		String system = "mysystem";
		String path = "my/file/path";
		String url = TapisUrlUtils.makeTapisUrl(baseUrl, system, path);
		
		Assert.assertEquals(TapisUrlUtils.getHost(url), host);
		Assert.assertEquals(TapisUrlUtils.getSystemId(url), system);
		Assert.assertEquals(TapisUrlUtils.getPath(url), path);
		
		// ---------------
		baseUrl = "tapis://" + host;
		system = "mysystem";
		path = "/my/other/file/path";
		url = TapisUrlUtils.makeTapisUrl(baseUrl, system, path);
		
		Assert.assertEquals(TapisUrlUtils.getHost(url), host);
		Assert.assertEquals(TapisUrlUtils.getSystemId(url), system);
		Assert.assertEquals(TapisUrlUtils.getPath(url), path.substring(1));

		// ---------------
		baseUrl = "tapis://" + host;
		system = "mysystem";
		path = null;
		url = TapisUrlUtils.makeTapisUrl(baseUrl, system, path);
		
		Assert.assertEquals(TapisUrlUtils.getHost(url), host);
		Assert.assertEquals(TapisUrlUtils.getSystemId(url), system);
		Assert.assertEquals(TapisUrlUtils.getPath(url), "");
	}

	@Test(enabled=true)
	public void test3()
	{
		final String host = "tapis.io"; 
		
		String baseUrl = "tapis://" + host + "/";
		String system = "mysystem";
		String path = "my/file/path";
		String url = TapisUrlUtils.makeTapisUrl(baseUrl, system, path);
		
		Assert.assertEquals(TapisUrlUtils.getHost(url), host);
		Assert.assertEquals(TapisUrlUtils.getSystemId(url), system);
		Assert.assertEquals(TapisUrlUtils.getPath(url), path);
		
		// ---------------
		baseUrl = "tapis://" + host + "/";
		system = "mysystem";
		path = "/my/other/file/path";
		url = TapisUrlUtils.makeTapisUrl(baseUrl, system, path);
		
		Assert.assertEquals(TapisUrlUtils.getHost(url), host);
		Assert.assertEquals(TapisUrlUtils.getSystemId(url), system);
		Assert.assertEquals(TapisUrlUtils.getPath(url), path.substring(1));

		// ---------------
		baseUrl = "tapis://" + host + "/";
		system = "mysystem";
		path = null;
		url = TapisUrlUtils.makeTapisUrl(baseUrl, system, path);
		
		Assert.assertEquals(TapisUrlUtils.getHost(url), host);
		Assert.assertEquals(TapisUrlUtils.getSystemId(url), system);
		Assert.assertEquals(TapisUrlUtils.getPath(url), "");
	}
}
