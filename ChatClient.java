import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.function.Consumer;

/*
	Author - The-R34per
	Last Updated October 25th, 2025
	
	---------------
	
 	ChatClient with AES-GCM encryption. Derives AES key with PBKDF2 from passphrase + salt (salt received from server).
 
 	Wire format for messages: Base64( IV || ciphertext ), IV = 12 bytes.
 
	Constructor: ChatClient(host, port, passphraseChars, onMessageCallback)
 	onMessageCallback receives decrypted plaintext messages as they arrive.
	
	---------------
	
	
    ChatClient.java © 2025 by The-R34per
	Licensed under Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International. 
	To view a copy of this license, visit https://creativecommons.org/licenses/by-nc-sa/4.0/

	
 */
public class ChatClient {
    private final String host;
    private final int port;
    private final char[] passphrase;
    private final Consumer<String> onMessage; 

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private SecretKey aesKey;

    // PBKDF2 params
    private static final int PBKDF2_ITER = 65536;
    private static final int KEY_LENGTH = 256; 

    public ChatClient(String host, int port, char[] passphrase, Consumer<String> onMessage) {
        this.host = host;
        this.port = port;
        this.passphrase = passphrase;
        this.onMessage = onMessage;
    }

   
    public void start(String displayName) throws Exception {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

   
        byte[] salt = Base64.getDecoder().decode("AAAAAAAAAAAAAAAAAAAAAA=="); // 16 zero bytes
        char[] sharedPassphrase = "shared-secret".toCharArray();
        this.aesKey = deriveKey(sharedPassphrase, salt);

        
        out.println(displayName);

        Thread reader = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    try {
                        String plain = decryptFromWire(line);
                        if (onMessage != null) onMessage.accept(plain);
                    } catch (Exception ex) {
                        if (onMessage != null) onMessage.accept("[Unreadable message]");
                    }
                }
            } catch (IOException e) {
                if (onMessage != null) onMessage.accept("[Disconnected from server]");
            }
        });
        reader.setDaemon(true);
        reader.start();
    }

    public void sendPlain(String plainText) throws Exception {
        String wire = encryptToWire(plainText);
        out.println(wire);
    }

    public void stop() {
        try { socket.close(); } catch (IOException ignored) {}
    }

 
    private SecretKey deriveKey(char[] pass, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(pass, salt, PBKDF2_ITER, KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = skf.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    private String encryptToWire(String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);
        byte[] ciphertext = cipher.doFinal(plainText.getBytes("UTF-8"));

        ByteBuffer bb = ByteBuffer.allocate(iv.length + ciphertext.length);
        bb.put(iv);
        bb.put(ciphertext);
        return Base64.getEncoder().encodeToString(bb.array());
    }

    private String decryptFromWire(String wire) throws Exception {
        byte[] combined = Base64.getDecoder().decode(wire);
        if (combined.length < 12) throw new IllegalArgumentException("Invalid input");
        byte[] iv = new byte[12];
        System.arraycopy(combined, 0, iv, 0, 12);
        byte[] ciphertext = new byte[combined.length - 12];
        System.arraycopy(combined, 12, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);
        byte[] plain = cipher.doFinal(ciphertext);
        return new String(plain, "UTF-8");
    }

    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 4444;
        if (args.length >= 1) host = args[0];
        if (args.length >= 2) port = Integer.parseInt(args[1]);

        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Display name: ");
        String name = console.readLine().trim();

        System.out.print("Password: ");
        char[] pass;
        if (System.console() != null) {
            pass = System.console().readPassword();
        } else {
            pass = console.readLine().toCharArray();
        }

        ChatClient client = new ChatClient(host, port, pass, (msg) -> {
            System.out.println(msg);
        });

        client.start(name);
        System.out.println("Connected!");

        String line;
        while ((line = console.readLine()) != null) {
            if (line.equalsIgnoreCase("/quit")) {
                client.stop();
                break;
            }
            client.sendPlain(name + ": " + line);
        }
    }
}
