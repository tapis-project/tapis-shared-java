package edu.utexas.tacc.tapis.shared.ssh;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/** This class can be use for offline testing of different PEM file loading implementations.
 * The code here is essentially a copy of the code in SSHConnection that converts a string
 * into a PrivateKey object.
 *  
 * @author rcardone
 *
 */
public class KeyLoadingTest2 
{
    private static final String PKCS_1_PEM_HEADER = "-----BEGIN RSA PRIVATE KEY-----";
    private static final String PKCS_1_PEM_FOOTER = "-----END RSA PRIVATE KEY-----";
    private static final String PKCS_8_PEM_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PKCS_8_PEM_FOOTER = "-----END PRIVATE KEY-----";
    private static final String SSH_HEADER        = "-----BEGIN OPENSSH PRIVATE KEY-----";
    private static final String SSH_FOOTER        = "-----END OPENSSH PRIVATE KEY-----";
    
    public static void main(String[] args) throws Exception 
    {
       PrivateKey prvKey = loadKey("/home/rcardone/work/keys/openssl-private-key.pem");
       System.out.println("Format: " + prvKey.getFormat() + ", Algorithm: " + prvKey.getAlgorithm());
       prvKey = loadKey("/home/rcardone/work/keys/openssl-private-key-pkcs8.pem");
       System.out.println("Format: " + prvKey.getFormat() + ", Algorithm: " + prvKey.getAlgorithm());
       try {prvKey = loadKey("/home/rcardone/work/keys/ssh-keygen-key");}
           catch (Exception e) {
               System.out.println(e.getMessage());
           }
    }

    public static PrivateKey loadKey(String keyFilePath) throws GeneralSecurityException, IOException {
        byte[] keyDataBytes = Files.readAllBytes(Paths.get(keyFilePath));
        String keyDataString = new String(keyDataBytes, StandardCharsets.UTF_8);

        if (keyDataString.startsWith(PKCS_1_PEM_HEADER)) {
            // OpenSSL / PKCS#1 Base64 PEM encoded file
            keyDataString = keyDataString.replace(PKCS_1_PEM_HEADER, "");
            keyDataString = keyDataString.replace(PKCS_1_PEM_FOOTER, "");
            keyDataString = keyDataString.replaceAll("\n", "");
            return readPkcs1PrivateKey(Base64.getDecoder().decode(keyDataString));
        }

        if (keyDataString.startsWith(PKCS_8_PEM_HEADER)) {
            // PKCS#8 Base64 PEM encoded file
            keyDataString = keyDataString.replace(PKCS_8_PEM_HEADER, "");
            keyDataString = keyDataString.replace(PKCS_8_PEM_FOOTER, "");
            keyDataString = keyDataString.replaceAll("\n", "");
            return readPkcs8PrivateKey(Base64.getDecoder().decode(keyDataString));
        }

        if (keyDataString.startsWith(SSH_HEADER)) {
            throw new IllegalArgumentException("ERROR: Keys generated using ssh-keygen that start with \""
                                               + SSH_HEADER + "\" are not supported.");
        }

        // We assume it's a PKCS#8 DER encoded binary file
        return readPkcs8PrivateKey(Files.readAllBytes(Paths.get(keyFilePath)));
    }

    private static PrivateKey readPkcs8PrivateKey(byte[] pkcs8Bytes) throws GeneralSecurityException {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA", "SunRsaSign");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Bytes);
        try {
            return keyFactory.generatePrivate(keySpec);
        } catch (InvalidKeySpecException e) {
            throw new IllegalArgumentException("Unexpected key format!", e);
        }
    }

    private static PrivateKey readPkcs1PrivateKey(byte[] pkcs1Bytes) throws GeneralSecurityException {
        // We can't use Java internal APIs to parse ASN.1 structures, so we build a PKCS#8 key Java can understand
        int pkcs1Length = pkcs1Bytes.length;
        int totalLength = pkcs1Length + 22;
        byte[] pkcs8Header = new byte[] {
                0x30, (byte) 0x82, (byte) ((totalLength >> 8) & 0xff), (byte) (totalLength & 0xff), // Sequence + total length
                0x2, 0x1, 0x0, // Integer (0)
                0x30, 0xD, 0x6, 0x9, 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0xD, 0x1, 0x1, 0x1, 0x5, 0x0, // Sequence: 1.2.840.113549.1.1.1, NULL
                0x4, (byte) 0x82, (byte) ((pkcs1Length >> 8) & 0xff), (byte) (pkcs1Length & 0xff) // Octet string + length
        };
        byte[] pkcs8bytes = join(pkcs8Header, pkcs1Bytes);
        return readPkcs8PrivateKey(pkcs8bytes);
    }

    private static byte[] join(byte[] byteArray1, byte[] byteArray2){
        byte[] bytes = new byte[byteArray1.length + byteArray2.length];
        System.arraycopy(byteArray1, 0, bytes, 0, byteArray1.length);
        System.arraycopy(byteArray2, 0, bytes, byteArray1.length, byteArray2.length);
        return bytes;
    }
}
