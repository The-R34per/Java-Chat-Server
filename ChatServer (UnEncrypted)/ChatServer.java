package TestingPackage;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private final int port;
    private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();

    public ChatServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("ChatServer started on port " + port);

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

                // First line from client = display name
                String first = in.readLine();
                if (first != null && !first.trim().isEmpty()) {
                    name = first.trim();
                    System.out.println("Client connected: " + name + " from " + socket.getRemoteSocketAddress());
                } else {
                    System.out.println("Client connected: " + socket.getRemoteSocketAddress());
                }

                String line;
                while ((line = in.readLine()) != null) {
                    System.out.println("Relaying message from " + name + ": " + line);
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
        int port = 5050;
        if (args.length >= 1) port = Integer.parseInt(args[0]);
        new ChatServer(port).start();
    }
}
