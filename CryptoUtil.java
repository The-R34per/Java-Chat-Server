import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;

/*
	
    CryptoUtil.java © 2025 by The-R34per
	Licensed under Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International. 
	To view a copy of this license, visit https://creativecommons.org/licenses/by-nc-sa/4.0/

	Portions of this code were developed with assistance from Microsoft Copilot.

*/

public class CryptoUtil {
    private static final String SECRET_KEY = "SharedPassword123";
    private static final String SALT = "StaticSaltValue";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;

    private static SecretKeySpec getKey() throws Exception {
        byte[] saltBytes = SALT.getBytes();
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(SECRET_KEY.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    public static String encrypt(String strToEncrypt) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec key = getKey();
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        byte[] encrypted = cipher.doFinal(strToEncrypt.getBytes("UTF-8"));
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
        return Base64.getEncoder().encodeToString(combined);
    }

    public static String decrypt(String strToDecrypt) throws Exception {
        byte[] combined = Base64.getDecoder().decode(strToDecrypt);
        byte[] iv = new byte[16];
        byte[] encrypted = new byte[combined.length - 16];
        System.arraycopy(combined, 0, iv, 0, 16);
        System.arraycopy(combined, 16, encrypted, 0, encrypted.length);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec key = getKey();
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        byte[] original = cipher.doFinal(encrypted);
        return new String(original, "UTF-8");
    }
}
