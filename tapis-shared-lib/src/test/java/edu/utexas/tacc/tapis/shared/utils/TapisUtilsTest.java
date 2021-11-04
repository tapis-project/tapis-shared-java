package edu.utexas.tacc.tapis.shared.utils;

import java.io.FileNotFoundException;

import org.testng.Assert;
import org.testng.annotations.Test;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.exceptions.TapisJDBCException;
import edu.utexas.tacc.tapis.shared.exceptions.TapisJSONException;
import edu.utexas.tacc.tapis.shared.exceptions.recoverable.TapisDBConnectionException;
import edu.utexas.tacc.tapis.shared.exceptions.runtime.TapisRuntimeException;
import edu.utexas.tacc.tapis.shared.uri.TapisLocalUrl;
import edu.utexas.tacc.tapis.shared.uri.TapisUrl;

@Test(groups={"unit"})
public class TapisUtilsTest 
{
    /* **************************************************************************** */
    /*                                    Tests                                     */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* getLocalHostnameTest:                                                        */
    /* ---------------------------------------------------------------------------- */
    @Test(enabled=true)
    public void getLocalHostnameTest()
    {
        String hostname = TapisUtils.getLocalHostname();
        System.out.println("hostname: " + hostname);
        Assert.assertNotNull(hostname, "Expected non-null hostname.");
        Assert.assertNotEquals(hostname, "", "Expected non-empty hostname.");
        Assert.assertNotEquals(hostname, "localhost", "Expected a hostname other than localhost.");
    }
    
    /* ---------------------------------------------------------------------------- */
    /* tapisifyTest:                                                                */
    /* ---------------------------------------------------------------------------- */
    @Test(enabled=true)
    public void tapisifyTest()
    {
        // Wrapped exception.
        TapisException wrappedException;
        String originalMsg = "original msg";
        
        // ========= null new msg =========
        // The new message is contained in the returned exception.
        String newMsg = "new msg";
        
        // ----- TapisException
        TapisException e = new TapisException(originalMsg);
        wrappedException = TapisUtils.tapisify(e, newMsg);
        Assert.assertEquals(wrappedException.getClass(), e.getClass(), 
                            "Unexpected exception type returned: " + wrappedException.getClass().getSimpleName());
        Assert.assertEquals(wrappedException.getMessage(), newMsg,
                            "Unexpected message returned.");
        
        // ----- TapisJDBCException
        e = new TapisJDBCException(originalMsg);
        wrappedException = TapisUtils.tapisify(e, newMsg);
        Assert.assertEquals(wrappedException.getClass(), e.getClass(), 
                            "Unexpected exception type returned: " + wrappedException.getClass().getSimpleName());
        Assert.assertEquals(wrappedException.getMessage(), newMsg,
                            "Unexpected message returned.");
        
        // ----- TapisRuntimeException
        TapisRuntimeException erun = new TapisRuntimeException(originalMsg);
        wrappedException = TapisUtils.tapisify(erun, newMsg);
        Assert.assertEquals(wrappedException.getClass(), TapisException.class, 
                           "Unexpected exception type returned: " + wrappedException.getClass().getSimpleName());
        Assert.assertEquals(wrappedException.getMessage(), newMsg,
                            "Unexpected message returned.");
     
        // ========= null new msg =========
        // The original message is preserved in the returned exception.
        newMsg = null;
        
        // ----- TapisException
        e = new TapisException(originalMsg);
        wrappedException = TapisUtils.tapisify(e, newMsg);
        Assert.assertEquals(wrappedException.getClass(), e.getClass(), 
                            "Unexpected exception type returned: " + wrappedException.getClass().getSimpleName());
        Assert.assertEquals(wrappedException.getMessage(), originalMsg,
                            "Unexpected message returned.");
        
        // ----- TapisJDBCException
        e = new TapisJDBCException(originalMsg);
        wrappedException = TapisUtils.tapisify(e, newMsg);
        Assert.assertEquals(wrappedException.getClass(), e.getClass(), 
                            "Unexpected exception type returned: " + wrappedException.getClass().getSimpleName());
        Assert.assertEquals(wrappedException.getMessage(), originalMsg,
                            "Unexpected message returned.");
        
        // ----- TapisRuntimeException
        erun = new TapisRuntimeException(originalMsg);
        wrappedException = TapisUtils.tapisify(erun, newMsg);
        Assert.assertEquals(wrappedException.getClass(), TapisException.class, 
                           "Unexpected exception type returned: " + wrappedException.getClass().getSimpleName());
        Assert.assertEquals(wrappedException.getMessage(), originalMsg,
                            "Unexpected message returned.");
    }

    /* ---------------------------------------------------------------------------- */
    /* findInChainTest:                                                             */
    /* ---------------------------------------------------------------------------- */
    @Test(enabled=true)
    public void findInChainTest()
    {
        // Create an exception chain.
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("x");
        TapisDBConnectionException tapisDBConnectionException = new TapisDBConnectionException("x", illegalArgumentException);
        TapisException tapisException = new TapisException("x", tapisDBConnectionException);
        
        // Inspect the chain using declared types.
        Exception e = TapisUtils.findInChain(tapisException, TapisDBConnectionException.class);
        Assert.assertNotNull(e, "TapisDBConnectionException not found!");
        e = TapisUtils.findInChain(tapisException, TapisException.class);
        Assert.assertNotNull(e, "TapisException not found!");
        e = TapisUtils.findInChain(tapisException, IllegalArgumentException.class);
        Assert.assertNotNull(e, "IllegalArgumentException not found!");
        
        // More inspection.
        e = TapisUtils.findInChain(tapisException, Exception.class);
        Assert.assertNotNull(e, "Exception not found!");
        e = TapisUtils.findInChain(tapisException, FileNotFoundException.class);
        Assert.assertNull(e, "FileNotFoundException should not be found!");
        e = TapisUtils.findInChain(tapisException, FileNotFoundException.class, IllegalArgumentException.class);
        Assert.assertNotNull(e, "IllegalArgumentException not found!");
        e = TapisUtils.findInChain(tapisException, TapisJSONException.class);
        Assert.assertNull(e, "TapisJSONException should not be found!");
        
        // Start in middle of chain.
        e = TapisUtils.findInChain(tapisDBConnectionException, TapisException.class);
        Assert.assertNotNull(e, "TapisException not found!");
        e = TapisUtils.findInChain(tapisDBConnectionException, IllegalArgumentException.class);
        Assert.assertNotNull(e, "IllegalArgumentException not found!");
   }
    
   /* ---------------------------------------------------------------------------- */
   /* extractFilenameTest:                                                         */
   /* ---------------------------------------------------------------------------- */
   @Test(enabled=true)
   public void extractFilenameTest() throws TapisException
   {
       // tapis urls.
       String url = "tapis://mysystem/a/b/c.txt";
       String fileName = TapisUtils.extractFilename(url);
       Assert.assertEquals(fileName, "c.txt");

       url = "tapis://mysystem/a/b/c.txt";
       fileName = TapisUtils.extractFilename(TapisUrl.makeTapisUrl(url).toString());
       Assert.assertEquals(fileName, "c.txt");
      
       url = "tapis://mysystem/a.txt";
       fileName = TapisUtils.extractFilename(url);
       Assert.assertEquals(fileName, "a.txt");
      
       url = "tapis://mysystem/a.txt";
       fileName = TapisUtils.extractFilename(TapisUrl.makeTapisUrl(url).toString());
       Assert.assertEquals(fileName, "a.txt");
      
       url = "tapis://mysystem/";
       fileName = TapisUtils.extractFilename(url);
       Assert.assertEquals(fileName, "");
      
       url = "tapis://mysystem/";
       fileName = TapisUtils.extractFilename(TapisUrl.makeTapisUrl(url).toString());
       Assert.assertEquals(fileName, "");
      
       url = "tapis://mysystem";
       fileName = TapisUtils.extractFilename(url);
       Assert.assertEquals(fileName, "");

       url = "tapis://mysystem";
       fileName = TapisUtils.extractFilename(TapisUrl.makeTapisUrl(url).toString());
       Assert.assertEquals(fileName, "");
   
       url = "tapis://mysystem////";
       fileName = TapisUtils.extractFilename(url);
       Assert.assertEquals(fileName, "");
   
       url = "tapis://mysystem////";
       fileName = TapisUtils.extractFilename(TapisUrl.makeTapisUrl(url).toString());
       Assert.assertEquals(fileName, "");
   
       // tapislocal urls.
       url = "tapislocal://exec.tapis/a/b/c.txt";
       fileName = TapisUtils.extractFilename(url);
       Assert.assertEquals(fileName, "c.txt");

       url = "tapislocal://mysystem/a/b/c.txt";
       fileName = TapisUtils.extractFilename(TapisLocalUrl.makeTapisLocalUrl(url).toString());
       Assert.assertEquals(fileName, "c.txt");
      
       url = "tapislocal://exec.tapis/a.txt";
       fileName = TapisUtils.extractFilename(url);
       Assert.assertEquals(fileName, "a.txt");
      
       url = "tapislocal://mysystem/a.txt";
       fileName = TapisUtils.extractFilename(TapisLocalUrl.makeTapisLocalUrl(url).toString());
       Assert.assertEquals(fileName, "a.txt");
      
       // ----------- Non-tapis urls
       url = "http://mysystem/a/b/c.txt";
       fileName = TapisUtils.extractFilename(url);
       Assert.assertEquals(fileName, "c.txt");
      
       url = "https://mysystem/a.txt";
       fileName = TapisUtils.extractFilename(url);
       Assert.assertEquals(fileName, "a.txt");
      
       url = "ftp://mysystem/";
       fileName = TapisUtils.extractFilename(url);
       Assert.assertEquals(fileName, "");
      
       url = "sftp://mysystem";
       fileName = TapisUtils.extractFilename(url);
       Assert.assertEquals(fileName, "");

       url = "https://mysystem";
       fileName = TapisUtils.extractFilename(url);
       Assert.assertEquals(fileName, "");
   
       url = "xxx://mysystem////";
       fileName = TapisUtils.extractFilename(url);
       Assert.assertEquals(fileName, "");
   }
}
