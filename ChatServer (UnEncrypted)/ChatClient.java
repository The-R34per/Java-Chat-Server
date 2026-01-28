import java.awt.Font;
import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

/*
 *================================================================================================ 																						    	||
 *||  Java Chat Server © 2026 by The-R34per is licensed under CC BY-NC-SA 4.0.					||
 *||  To view a copy of this license, visit https://creativecommons.org/licenses/by-nc-sa/4.0/	||
 *================================================================================================
*/

public class ChatClient {

    private final String host;     // Server host address the client connects to
    private final int port;     // Server port number
    private final Consumer<String> onMessage;     // Callback used by the GUI to display incoming messages
    private Socket socket;     // Network socket and I/O streams for communication
    private BufferedReader in;
    private PrintWriter out;

    // Constructs a client instance with connection details and a message callback
    public ChatClient(String host, int port, Consumer<String> onMessage) {
        this.host = host;
        this.port = port;
        this.onMessage = onMessage;
    }

    // Displays a dropdown list of users and sends a kick command for the selected one
    private void showKickUserDropdown(String[] users) {
        String selected = (String) JOptionPane.showInputDialog(
                null,
                "Select a user to kick:",
                "Kick User",
                JOptionPane.PLAIN_MESSAGE,
                null,
                users,
                users.length > 0 ? users[0] : null
        );

        if (selected != null) {
            sendPlain("/kickuser " + selected);
        }
    }

    // Displays a countdown window when the server is shutting down
    private void startShutdownCountdown() {
        JFrame frame = new JFrame("Server Shutdown");
        frame.setSize(300, 150);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        JLabel label = new JLabel("", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 20));
        frame.add(label);

        frame.setVisible(true);

        new Thread(() -> {
            for (int i = 5; i >= 0; i--) {
                label.setText("Server Shutting down in " + i + "...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}
            }

            label.setText("Goodbye!");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}

            System.exit(0);
        }).start();
    }

    // Connects to the server, sends the display name, and starts the reader thread
    public void start(String name) throws IOException {
        socket = new Socket(host, port);

        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

        // First message sent to the server is the client's display name
        out.println(name);

        /*
         * Reader thread that continuously listens for messages from the server.
         * Handles special commands such as user list requests, shutdown notices,
         * and kick notifications.
         */
        Thread t = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {

                    // Server sent a list of users for admin kick selection
                    if (line.startsWith("/userList ")) {
                        String list = line.substring(10);
                        String[] users = list.split(",");
                        showKickUserDropdown(users);
                        continue;
                    }

                    // Server is shutting down
                    if (line.equals("SERVER_SHUTDOWN")) {
                        startShutdownCountdown();
                        break;
                    }

                    // Client has been kicked by an admin
                    if (line.equals("KICK")) {
                        JOptionPane.showMessageDialog(
                                null,
                                "You have been kicked from the server.",
                                "Disconnected",
                                JOptionPane.WARNING_MESSAGE
                        );
                        System.exit(0);
                    }

                    // Duplicate handler for user list (kept for compatibility)
                    if (line.startsWith("/userList")) {
                        String list = line.substring(10);
                        String[] users = list.split(",");
                        showKickUserDropdown(users);
                        continue;
                    }

                    // Normal chat message
                    onMessage.accept(line);
                }
            } catch (IOException e) {
                onMessage.accept("Disconnected: " + e.getMessage());
            }
        });

        t.setDaemon(true);
        t.start();
    }

    // Requests the server to send the user list for admin kick selection
    public void requestKickUser() {
        String password = JOptionPane.showInputDialog(
                null,
                "Enter admin password:",
                "Admin Authentication",
                JOptionPane.PLAIN_MESSAGE
        );

        if (password == null) return;

        if (!password.equals("admin123")) {
            JOptionPane.showMessageDialog(null, "Incorrect password.");
            return;
        }

        sendPlain("/requestUserList");
    }

    // Sends a plain text message to the server
    public void sendPlain(String msg) {
        if (out != null) {
            out.println(msg);
        }
    }

    // Closes the client socket connection
    public void close() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }
}
