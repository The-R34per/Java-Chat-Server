import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/*
  	Author - The-R34per
	Last Updated October 25th, 2025
	
	
    ChatServer.java © 2025 by The-R34per
	Licensed under Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International. 
	To view a copy of this license, visit https://creativecommons.org/licenses/by-nc-sa/4.0/

*/

public class ChatServer {
    private final int port;
    private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private final byte[] serverSalt; 

    public ChatServer(int port) {
        this.port = port;
        this.serverSalt = generateSalt(16);
    }

    private static byte[] generateSalt(int len) {
        byte[] s = new byte[len];
        new SecureRandom().nextBytes(s);
        return s;
    }

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("ChatServer started on port " + port);
        System.out.println("Server salt (base64): " + Base64.getEncoder().encodeToString(serverSalt));

        while (true) {
            Socket socket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(socket);
            clients.add(handler);
            new Thread(handler).start();
        }
    }

    private void broadcast(String message, ClientHandler exclude) {
        for (ClientHandler c : clients) {
            if (c != exclude) {
                c.send(message);
            }
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String name = "Unknown";

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void send(String msg) {
            out.println(msg);
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

                out.println("SALT:" + Base64.getEncoder().encodeToString(serverSalt));

                String first = in.readLine();
                
                if (first != null && !first.trim().isEmpty()) {
                    name = first.trim();
                    System.out.println("Client connected: " + name + " from " + socket.getRemoteSocketAddress());
                } else {
                    System.out.println("Client connected: " + socket.getRemoteSocketAddress());
                }

                String line;
                while ((line = in.readLine()) != null) {
                   
                    System.out.println("Relaying message from " + name + " (" + line.length() + " chars)");
                    broadcast(line, this);
                }
            } catch (IOException e) {
                System.err.println("Connection error for " + name + ": " + e.getMessage());
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
                clients.remove(this);
                System.out.println("Client disconnected: " + name);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        int port = 5000;
        if (args.length >= 1) port = Integer.parseInt(args[0]);
        new ChatServer(port).start();
        
        // password is "mysecret123"
    }
}
