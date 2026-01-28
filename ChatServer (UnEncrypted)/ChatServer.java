import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.JOptionPane;

/*
 *================================================================================================ 																						    	||
 *||  Java Chat Server © 2026 by The-R34per is licensed under CC BY-NC-SA 4.0.					||
 *||  To view a copy of this license, visit https://creativecommons.org/licenses/by-nc-sa/4.0/	||
 *================================================================================================
*/

public class ChatServer {

    private final String host;     // Host address the server binds to
    private final int port;     // Port number the server listens on
    private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();     // Thread-safe collection of all connected clients
    private long startTime;     // Timestamp used for uptime calculations
    private String localIp = "Unknown";     // Local IP address for display purposes
    private final ServerGUI gui;     // Reference to the server GUI for logging and UI updates
    private ServerSocket serverSocket;     // Main server socket used to accept incoming connections

    // Stops the server and disconnects all connected clients
    public void stopServer() {
        log("[Shutting down server...]");
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}
        shutdownAllClients();
        log("[Server stopped]");
    }

    // Kicks a user by name when triggered by a client command
    public void kickUserFromServer(String name) {
        for (ClientHandler c : clients) {
            if (c.name.equals(name)) {
                try {
                    c.send("KICK");
                    c.socket.close();
                } catch (IOException ignored) {}
                clients.remove(c);
                updateClientList();
                broadcast("[Server]: " + name + " was kicked by an admin.", null);
                return;
            }
        }
    }

    // Kicks a user when selected from the server GUI list
    public void kickUser(String displayString) {
        String name = displayString.split(" - ")[0];

        for (ClientHandler c : clients) {
            if (c.name.equals(name)) {
                try {
                    c.send("KICK");
                    c.socket.close();
                } catch (IOException ignored) {}
                clients.remove(c);
                updateClientList();
                broadcast("[Server]: " + name + " was kicked from the server.", null);
                log("[Kicked user: " + name + "]");
                return;
            }
        }
    }

    // Updates the GUI with the current list of connected clients
    void updateClientList() {
        if (gui != null) {
            gui.updateClientList(clients);
        }
    }

    // Logs a message to the GUI or console
    void log(String msg) {
        if (gui != null) {
            gui.appendLog(msg);
        } else {
            System.out.println(msg);
        }
    }

    // Returns formatted uptime string
    private String getUptime() {
        long ms = System.currentTimeMillis() - startTime;
        long sec = (ms / 1000) % 60;
        long min = (ms / (1000 * 60)) % 60;
        long hr = (ms / (1000 * 60 * 60));
        return String.format("%02dh:%02dm:%02ds", hr, min, sec);
    }

    // Clears the console output for dashboard display
    private void clearConsole() {
        for (int i = 0; i < 60; i++) {
            System.out.println();
        }
    }

    // Optional file for saving chat logs
    private final File chatLogFile = new File("chat_log.txt");
    private PrintWriter logWriter;

    // Prints a formatted server status dashboard to the console
    private void printStatusDashboard() {
        clearConsole();

        System.out.println("Uptime: " + getUptime());
        System.out.println("Server IP: " + localIp);
        System.out.println("Port: " + port);
        System.out.println("Active Clients: " + clients.size());
        System.out.println("---------------------------------------------");
        System.out.println(" | Name                 | IP Address           |");
        System.out.println(" |----------------------|----------------------|");

        if (clients.isEmpty()) {
            System.out.println(" (No clients connected)");
        } else {
            for (ClientHandler c : clients) {
                String n = String.format("%-20s", c.name);
                String ip = String.format("%-20s", c.socket.getRemoteSocketAddress());
                System.out.println(" | " + n + " | " + ip + " |");
            }
        }
        System.out.println("---------------------------------------------");
    }

    // Constructs the server with a host, port, and GUI reference
    public ChatServer(String host, int port, ServerGUI gui) {
        this.host = host;
        this.port = port;
        this.gui = gui;
    }

    // Returns a formatted table of active clients
    private String getActiveClientsTable() {
        StringBuilder sb = new StringBuilder();
        sb.append("=============== Active Clients ===============\n");
        sb.append("| Name                 | IP Address           |\n");
        sb.append("|----------------------|----------------------|\n");

        for (ClientHandler c : clients) {
            String n = String.format("%-20s", c.name);
            String ip = String.format("%-20s", c.socket.getRemoteSocketAddress());
            sb.append("| " + n + " | " + ip + " |\n");
        }
        sb.append("==============================================\n");
        sb.append("Total Connected: ").append(clients.size()).append("\n");
        return sb.toString();
    }

    // Starts the server and begins accepting client connections
    public void start() throws IOException {
        gui.appendLog("[Starting server...]");

        ServerSocket serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(host, port));

        gui.appendLog("[Server running on " + host + ":" + port + "]");

        while (true) {
            Socket socket = serverSocket.accept();
            gui.appendLog("[Client connected: " + socket.getRemoteSocketAddress() + "]");

            ClientHandler handler = new ClientHandler(socket, this);
            clients.add(handler);
            gui.updateClientList(clients);

            new Thread(handler).start();
        }
    }

    // Sends a message to all connected clients except the excluded one
    private void broadcast(String message, ClientHandler exclude) {
        for (ClientHandler c : clients) {
            if (c != exclude) {
                c.send(message);
            }
        }
    }

    // Handles communication with a single connected client
    public class ClientHandler implements Runnable {

        public final Socket socket;         // Socket representing the client's connection
        private final ChatServer server;         // Reference to the server instance

        // Input and output streams for communication
        private BufferedReader in;
        private PrintWriter out;
        public String name = "Unknown";         // Display name of the connected client

        // Constructs a handler for a specific client socket
        public ClientHandler(Socket socket, ChatServer server) {
            this.socket = socket;
            this.server = server;
        }

        // Sends the list of connected users to the requesting client
        private void sendUserList() {
            StringBuilder sb = new StringBuilder("/userList ");
            for (ClientHandler c : server.clients) {
                sb.append(c.name).append(",");
            }
            send(sb.toString());
        }

        // Sends a raw message to the client
        public void send(String msg) {
            out.println(msg);
        }

        // Main loop for handling client communication
        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

                String first = in.readLine();
                if (first != null && !first.trim().isEmpty()) {
                    name = first.trim();
                    updateClientList();
                    log("===========================");
                    log("Client connected: " + name + " from " + socket.getRemoteSocketAddress());
                    log("===========================");
                } else {
                    log("===========================");
                    log("Client connected: " + socket.getRemoteSocketAddress());
                    log("===========================");
                    updateClientList();
                }

                String line;
                while ((line = in.readLine()) != null) {

                    if (line.equals("/requestUserList")) {
                        sendUserList();
                        continue;
                    }

                    if (line.startsWith("/kickuser ")) {
                        String target = line.substring(10);
                        server.kickUserFromServer(target);
                        continue;
                    }

                    log("Relaying message from " + name + " " + socket.getRemoteSocketAddress() + ": " + line);
                    broadcast(name + ": " + line, this);
                }
            } catch (IOException e) {
                log("===========================");
                log("Connection error for " + name + " " + socket.getRemoteSocketAddress() + ": " + e.getMessage());
                log("===========================");
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
                clients.remove(this);
                updateClientList();
                log("===========================");
                log("Client disconnected: " + name + " from " + socket.getRemoteSocketAddress());
                log("===========================");
            }
        }
    }

    // Disconnects all clients and clears the client list
    public void shutdownAllClients() {
        log("[Disconnecting all clients...]");

        for (ClientHandler c : clients) {
            try {
                c.send("SERVER_SHUTDOWN");
                c.socket.close();
            } catch (IOException ignored) {}
        }
        clients.clear();
        updateClientList();
    }

    // Entry point for launching the server with user-specified host and port
    public static void main(String[] args) {
        String host = JOptionPane.showInputDialog(
                null,
                "Enter IP address to bind the server:",
                "0.0.0.0"
        );
        if (host == null || host.isEmpty()) host = "0.0.0.0";

        String portStr = JOptionPane.showInputDialog(
                null,
                "Enter port to host the server on:",
                "5050"
        );
        int port = 5050;
        try { port = Integer.parseInt(portStr); } catch (Exception ignored) {}

        new ServerGUI(host, port);
    }
}
