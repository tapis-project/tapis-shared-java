package edu.utexas.tacc.tapis.shared.utils;

import java.io.FileNotFoundException;
import java.util.regex.Pattern;

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
	// Control some of the output.
	private static final boolean QUIET = true;
	
	// Copy of same regex defined in TapisUtils.
	private static final Pattern _spaceSplitter = Pattern.compile("(?U)\\s+");
	
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
   
   /* ---------------------------------------------------------------------------- */
   /* safelySingleQuoteStringTest:                                                 */
   /* ---------------------------------------------------------------------------- */
   @Test(enabled=true)
   public void safelySingleQuoteStringTest()
   {
	   // Basically, each embedded ' is replaced with '\\''.
	   var s = "xxyy";
	   var t = TapisUtils.safelySingleQuoteString(s);
	   if (!QUIET) System.out.println(s + " -> " + t);
	   Assert.assertEquals(t, "'" + s + "'");
	   
	   s = "'xxyy";
	   t = TapisUtils.safelySingleQuoteString(s);
	   if (!QUIET) System.out.println(s + " -> " + t);
	   Assert.assertEquals(t, "''\\''xxyy'");
	   
	   s = "'xxyy'";
	   t = TapisUtils.safelySingleQuoteString(s);
	   if (!QUIET) System.out.println(s + " -> " + t);
	   Assert.assertEquals(t, "''\\''xxyy'\\'''");

	   s = "xx'yy";
	   t = TapisUtils.safelySingleQuoteString(s);
	   if (!QUIET) System.out.println(s + " -> " + t);
	   Assert.assertEquals(t, "'xx'\\''yy'");

	   s = "xx''yy";
	   t = TapisUtils.safelySingleQuoteString(s);
	   if (!QUIET) System.out.println(s + " -> " + t);
	   Assert.assertEquals(t, "'xx'\\'''\\''yy'");

	   s = "'";
	   t = TapisUtils.safelySingleQuoteString(s);
	   if (!QUIET) System.out.println(s + " -> " + t);
	   Assert.assertEquals(t, "''\\'''");
	   
	   s = "";
	   t = TapisUtils.safelySingleQuoteString(s);
	   if (!QUIET) System.out.println(s + " -> " + t);
	   Assert.assertEquals(t, "''");

	   s = null;
	   t = TapisUtils.safelySingleQuoteString(s);
	   if (!QUIET) System.out.println(s + " -> " + t);
	   Assert.assertEquals(t, null);
   }
   
   /* ---------------------------------------------------------------------------- */
   /* safelyDoubleQuoteStringTest:                                                 */
   /* ---------------------------------------------------------------------------- */
   @Test(enabled=true)
   public void safelyDoubleQuoteStringTest()
   {
	   // Basically, each embedded ' is replaced with '\\''.
	   var s = "xxyy";
	   var t = TapisUtils.safelyDoubleQuoteString(s);
	   if (!QUIET) System.out.println(s + " -> " + t);
	   Assert.assertEquals(t, "\"" + s + "\"");
	   
	   s = "\"xxyy";
	   t = TapisUtils.safelyDoubleQuoteString(s);
	   if (!QUIET) System.out.println(s + " -> " + t);
	   Assert.assertEquals(t, "\"\\\"xxyy\"");
	   
	   s = "\"xxyy\"";
	   t = TapisUtils.safelyDoubleQuoteString(s);
	   if (!QUIET) System.out.println(s + " -> " + t);
	   Assert.assertEquals(t, "\"\\\"xxyy\\\"\"");

	   s = "xx\"yy";
	   t = TapisUtils.safelyDoubleQuoteString(s);
	   if (!QUIET) System.out.println(s + " -> " + t);
	   Assert.assertEquals(t, "\"xx\\\"yy\"");

	   s = "xx\"\"yy";
	   t = TapisUtils.safelyDoubleQuoteString(s);
	   if (!QUIET) System.out.println(s + " -> " + t);
	   Assert.assertEquals(t, "\"xx\\\"\\\"yy\"");

	   s = "\"";
	   t = TapisUtils.safelyDoubleQuoteString(s);
	   if (!QUIET) System.out.println(s + " -> " + t);
	   Assert.assertEquals(t, "\"\\\"\"");

	   s = "";
	   t = TapisUtils.safelyDoubleQuoteString(s);
	   if (!QUIET) System.out.println(s + " -> " + t);
	   Assert.assertEquals(t, "\"\"");

	   s = null;
	   t = TapisUtils.safelyDoubleQuoteString(s);
	   if (!QUIET) System.out.println(s + " -> " + t);
	   Assert.assertEquals(t, null);
   }
   
   /* ---------------------------------------------------------------------------- */
   /* spaceSplitterTest:                                                           */
   /* ---------------------------------------------------------------------------- */
   @Test(enabled=true)
   public void spaceSplitterTest()
   {
	   String s = "key value";
	   String[] tokens = _spaceSplitter.split(s.strip(), 2);
	   Assert.assertEquals(tokens.length, 2, "Invalid split of '" + s + "'");
	   Assert.assertEquals(tokens[0], "key");
	   Assert.assertEquals(tokens[1], "value");
	   
	   s = "key";
	   tokens = _spaceSplitter.split(s.strip(), 2);
	   Assert.assertEquals(tokens.length, 1, "Invalid split of '" + s + "'");
	   Assert.assertEquals(tokens[0], "key");

	   s = " \rkey  ";
	   tokens = _spaceSplitter.split(s.strip(), 2);
	   Assert.assertEquals(tokens.length, 1, "Invalid split of '" + s + "'");
	   Assert.assertEquals(tokens[0], "key");

	   s = " key v1\tv2 v3 ";
	   tokens = _spaceSplitter.split(s.strip(), 2);
	   Assert.assertEquals(tokens.length, 2, "Invalid split of '" + s + "'");
	   Assert.assertEquals(tokens[0], "key");
	   Assert.assertEquals(tokens[1], "v1\tv2 v3");

	   s = " key\tv1\tv2 v3 ";
	   tokens = _spaceSplitter.split(s.strip(), 2);
	   Assert.assertEquals(tokens.length, 2, "Invalid split of '" + s + "'");
	   Assert.assertEquals(tokens[0], "key");
	   Assert.assertEquals(tokens[1], "v1\tv2 v3");

	   s = "\nkey\nv1\nv2 v3\n ";
	   tokens = _spaceSplitter.split(s.strip(), 2);
	   Assert.assertEquals(tokens.length, 2, "Invalid split of '" + s + "'");
	   Assert.assertEquals(tokens[0], "key");
	   Assert.assertEquals(tokens[1], "v1\nv2 v3");
   }
   
   /* ---------------------------------------------------------------------------- */
   /* conditionalQuoteTest:                                                        */
   /* ---------------------------------------------------------------------------- */
   @Test(enabled=true)
   public void conditionalQuoteTest()
   {
	   // ------------------- Basic tests -----------------------
	   // -------------------------------------------------------
	   String s = "singleValue";
	   String q = TapisUtils.conditionalQuote(s);
	   Assert.assertEquals(q, s);
	   
	   s = " paddedSingleValue1";
	   q = TapisUtils.conditionalQuote(s);
	   Assert.assertEquals(q.length(), s.length()+2);
	   Assert.assertEquals(q.substring(1, q.length()-1), s);
	   Assert.assertEquals(q.charAt(0), '"');
	   Assert.assertEquals(q.charAt(q.length()-1), '"');

	   s = "paddedSingleValue2 ";
	   q = TapisUtils.conditionalQuote(s);
	   Assert.assertEquals(q.length(), s.length()+2);
	   Assert.assertEquals(q.substring(1, q.length()-1), s);
	   Assert.assertEquals(q.charAt(0), '"');
	   Assert.assertEquals(q.charAt(q.length()-1), '"');

	   s = "double Value";
	   q = TapisUtils.conditionalQuote(s);
	   Assert.assertEquals(q.length(), s.length()+2);
	   Assert.assertEquals(q.substring(1, q.length()-1), s);
	   Assert.assertEquals(q.charAt(0), '"');
	   Assert.assertEquals(q.charAt(q.length()-1), '"');

	   s = "triple1 v1\tv2";
	   q = TapisUtils.conditionalQuote(s);
	   Assert.assertEquals(q.length(), s.length()+2);
	   Assert.assertEquals(q.substring(1, q.length()-1), s);
	   Assert.assertEquals(q.charAt(0), '"');
	   Assert.assertEquals(q.charAt(q.length()-1), '"');

	   s = "\ntriple2 v1\tv2 ";
	   q = TapisUtils.conditionalQuote(s);
	   Assert.assertEquals(q.length(), s.length()+2);
	   Assert.assertEquals(q.substring(1, q.length()-1), s);
	   Assert.assertEquals(q.charAt(0), '"');
	   Assert.assertEquals(q.charAt(q.length()-1), '"');

	   s = "\ntriple3 v1\tv2";
	   q = TapisUtils.conditionalQuote(s);
	   Assert.assertEquals(q.length(), s.length()+2);
	   Assert.assertEquals(q.substring(1, q.length()-1), s);
	   Assert.assertEquals(q.charAt(0), '"');
	   Assert.assertEquals(q.charAt(q.length()-1), '"');

	   // Control characters are NOT detected because they
	   // are assume to already have been removed.
	   s = "\ntriple4\tv1\rv2";
	   q = TapisUtils.conditionalQuote(s);
	   Assert.assertEquals(q, s);
	   
	   // ------------- Already double quoted tests -------------
	   // -------------------------------------------------------
	   s = "\"singleValue\"";
	   q = TapisUtils.conditionalQuote(s);
	   Assert.assertEquals(q, s);
	   
	   s = "\"double Value\"";
	   q = TapisUtils.conditionalQuote(s);
	   Assert.assertEquals(q, s);

	   s = "\"triple1 v1\tv2\"";
	   q = TapisUtils.conditionalQuote(s);
	   Assert.assertEquals(q, s);

	   s = "\"\ntriple2 v1\tv2 \"";
	   q = TapisUtils.conditionalQuote(s);
	   Assert.assertEquals(q, s);

	   s = "\"\ntriple3 v1\tv2\"";
	   q = TapisUtils.conditionalQuote(s);
	   Assert.assertEquals(q, s);

	   s = "\"\ntriple4\tv1\rv2\"";
	   q = TapisUtils.conditionalQuote(s);
	   
	   s = "\"\ntriple4\tv1\rv2\"";
	   q = TapisUtils.conditionalQuote(s);
	   Assert.assertEquals(q, s);
   
	   // --------------- Internal quote tests ------------------
	   // -------------------------------------------------------
	   // Result: s=double" Value
	   //         q="double\" Value"
	   // Quoted length equals 2 for enclosing quotes 
	   // plus N equal to the number of internal quotes.
	   s = "double\" Value";
	   q = TapisUtils.conditionalQuote(s);
//	   System.out.println("s="+s);
//	   System.out.println("q="+q);
	   Assert.assertEquals(q.length(), s.length()+2+1);

	   // This one doesn't get double quoted.
	   s = "double\"Value";
	   q = TapisUtils.conditionalQuote(s);
	   Assert.assertEquals(q, s);

	   // 3 internal quotes
	   s = "double\"\" V\"alue";
	   q = TapisUtils.conditionalQuote(s);
	   Assert.assertEquals(q.length(), s.length()+2+3);
   }
}
