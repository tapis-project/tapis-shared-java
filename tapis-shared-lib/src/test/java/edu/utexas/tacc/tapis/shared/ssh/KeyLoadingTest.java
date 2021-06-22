package edu.utexas.tacc.tapis.shared.ssh;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import edu.utexas.tacc.tapis.shared.exceptions.TapisSecurityException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;

/** This program provides a way to test different approaches to loading 
 * key file content into PublicKey and PrivateKey objects.  Key's that 
 * work include key pairs created by openssl, though only pkcs8 private
 * keys are currently handled.  Here's one way to create such keys:
 * 
 *  openssl genrsa -out keypair.pem 4096
 *  openssl rsa -in keypair.pem -pubout -out publickey.crt
 *  openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in keypair.pem -out pkcs8.key
 * 
 * @author rcardone
 */
public class KeyLoadingTest 
{
    // Split strings on space characters.
    private static final Pattern _spaceSpitter = Pattern.compile(" ");
    
    
    // Fields.
    private final String      _host = "fakeHost";
    private final String      _username = "fakeUser";
    private String            _privateKey;
    private String            _publicKey;

    public static void main(String[] args) 
     throws Exception
    {
        if (args.length < 2) {
            System.out.println(getHelpMessage());
            return;
        }
        
        var test = new KeyLoadingTest();
        test.load(args[0], args[1]);
    }

    private void load(String privateFile, String publicFile)
     throws Exception
    {
        
        _privateKey = readFile(privateFile);
        _publicKey  = readFile(publicFile);
        KeyPair keyPair = getKeyPair();
    }
    
    private String readFile(String fileName) throws IOException
    {
        File file = new File(fileName);
        return FileUtils.readFileToString(file, Charset.defaultCharset());
    }
    
    /* ---------------------------------------------------------------------- */
    /* getKeyPair:                                                            */
    /* ---------------------------------------------------------------------- */
    private KeyPair getKeyPair()
      throws TapisSecurityException
    {
        // Remove an non-key material from the key strings.
        String trimmedPrivate = trimKeyMaterial(_privateKey, false);
        String trimmedPublic  = trimKeyMaterial(_publicKey, true);
        
        // Decode the key material into binary.
        byte[] privateBytes = base64Decode(trimmedPrivate, "private");
        byte[] publicBytes  = base64Decode(trimmedPublic,  "public");
        
        // Make into keys.
        PrivateKey prvKey = makeRsaPrivateKey(privateBytes);
        PublicKey  pubKey = makeRsaPublicKey(publicBytes);
        
        // Assign key pair.
        KeyPair keyPair = new KeyPair(pubKey, prvKey);
        return keyPair;
    }
    
    /* ---------------------------------------------------------------------- */
    /* trimKeyMaterial:                                                       */
    /* ---------------------------------------------------------------------- */
    /** Extract the key material from the encoded format and remove all newline
     * characters.
     * 
     * @param encodedKey the key in PEM or other encodings.
     * @param nonPEMCheck attempt to parse non-PEM encodings
     * @return
     */
    private String trimKeyMaterial(String encodedKey, boolean nonPEMCheck)
    {
        // First check for non-PEM formatting as found in authorized_key files.
        if (nonPEMCheck && encodedKey.startsWith("ssh-")) {
            // Split the key on space characters and return the next
            // non-empty part after the first one.
            var parts = _spaceSpitter.split(encodedKey);
            if (parts.length > 1) {
                for (int i = 1; i < parts.length; i++)
                    if (!parts[i].isEmpty()) {
                        encodedKey = parts[i];
                        break;
                    }
            }
            
            // We always return from here even if 
            // the key is unchanged.
            return encodedKey.replaceAll("\n", "");
        }
        
        // Remove prologue and epilogue if they exist.  For example, public
        // keys stored in PEM format have a prologue and epilogue (see
        // https://tools.ietf.org/html/rfc1421 for the specification):
        //
        //      "-----BEGIN PUBLIC KEY-----\n"
        //      "\n-----END PUBLIC KEY-----"
        //
        // In general, different messages can appear after the BEGIN and END text,
        // so stripping out the prologue and epilogue requires some care.  The  
        // approach below handles only unix-style line endings.  
        // 
        // Check for unix style prologue.
        int index = encodedKey.indexOf("-\n");
        if (index > 0) encodedKey = encodedKey.substring(index + 2);
        
        // Check for unix style epilogue.
        index = encodedKey.lastIndexOf("\n-");
        if (index > 0) encodedKey = encodedKey.substring(0, index);
        
        return encodedKey.replaceAll("\n", "");
    }

    /* ---------------------------------------------------------------------- */
    /* base64Decode:                                                          */
    /* ---------------------------------------------------------------------- */
    private byte[] base64Decode(String base64, String keyDesc) 
     throws TapisSecurityException
    {
        // Try to decode the key.
        try {return Base64.getDecoder().decode(base64);}
        catch (Exception e) {
            String msg = MsgUtils.getMsg("TAPIS_SECURITY_SSH_KEY_DECODE",
                                         _username, _host, keyDesc);
            throw new TapisSecurityException(msg, e);
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* makeRsaPublicKey:                                                      */
    /* ---------------------------------------------------------------------- */
    private RSAPublicKey makeRsaPublicKey(byte[] bytes)
     throws TapisSecurityException
    {
        Object obj = null;
        try {
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(bytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            obj = keyFactory.generatePublic(keySpec);
        }
        catch (Exception e) {
            String msg = MsgUtils.getMsg("TAPIS_SECURITY_SSH_KEY_CREATE", 
                                         _username, _host, "public",
                                         e.getMessage());
            throw new TapisSecurityException(msg, e);
        }
        
        return (RSAPublicKey) obj;
    }
    
    /* ---------------------------------------------------------------------- */
    /* makeRsaPrivateKey:                                                     */
    /* ---------------------------------------------------------------------- */
    private RSAPrivateKey makeRsaPrivateKey(byte[] bytes)
     throws TapisSecurityException
    {
        Object obj = null;
        try {
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            obj = keyFactory.generatePrivate(keySpec);
        }
        catch (Exception e) {
            String msg = MsgUtils.getMsg("TAPIS_SECURITY_SSH_KEY_CREATE", 
                                         _username, _host, "private",
                                         e.getMessage());
            throw new TapisSecurityException(msg, e);
        }
        
        return (RSAPrivateKey) obj;
    }

    
    private static String getHelpMessage()
    {
        String msg = "Please specify the private and public key file paths:\n\n"
                + "   KeyLoadingTest <private key file> <public key file>\n\n"
                + "Use an astersk in place of either file to skip processing that file.";
        return msg;
    }
}
