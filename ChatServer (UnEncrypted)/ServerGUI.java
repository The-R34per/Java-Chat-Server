import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Set;

/*
 *================================================================================================ 																						    	||
 *||  Java Chat Server © 2026 by The-R34per is licensed under CC BY-NC-SA 4.0.					||
 *||  To view a copy of this license, visit https://creativecommons.org/licenses/by-nc-sa/4.0/	||
 *================================================================================================
*/

public class ServerGUI extends JFrame {

    // Buttons for saving logs and stopping the server
    private JButton saveLogButton;
    private JButton stopServerButton;
    private JTextArea logArea;     // Text area used to display server logs and status messages
    private DefaultListModel<String> clientListModel;     // Model backing the list of connected clients
    private ChatServer server;     // Reference to the server instance controlled by this GUI

    // Constructs the server control panel GUI and initializes all components
    public ServerGUI(String host, int port) {
        super("Chat Server Control Panel");
        setTitle("Chat Server Control Panel - Hosted on " + host + ":" + port);

        // Create the server instance using the provided host and port
        server = new ChatServer(host, port, this);

        setLayout(new BorderLayout());
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        /*
         * Top banner section containing ASCII art and version information.
         * Displayed inside a JTextArea to preserve formatting.
         */
        JTextArea banner = new JTextArea(
                " ____ _           _      ____                          \n" +
                "/ ___| |__   __ _| |_   / ___|  ___ _ ____   _____ _ __\n" +
                "| |  | '_ \\ / _` | __|  \\___ \\ / _ \\ '__\\ \\ / / _ \\ '__|\n" +
                "| |__| | | | (_| | |_    ___) |  __/ |   \\ V /  __/ |   \n" +
                "\\____|_| |_|\\__,_|\\__|  |____/ \\___|_|    \\_/ \\___|_|   \n" +
                "             (UnEncrypted Version 1.2.0)\n" +
                "                Created by The-R34per\n" +
                "--------------------------------------------------------"
        );
        banner.setEditable(false);
        banner.setFont(new Font("Monospaced", Font.PLAIN, 12));
        banner.setBackground(new Color(240, 240, 240));

        JPanel bannerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bannerPanel.add(banner);
        add(bannerPanel, BorderLayout.NORTH);

        /*
         * Center panel containing the server log output.
         * Wrapped in a scroll pane with a titled border.
         */
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(),
                        "Server Log",
                        javax.swing.border.TitledBorder.CENTER,
                        javax.swing.border.TitledBorder.TOP
                )
        );
        add(logScroll, BorderLayout.CENTER);

        /*
         * Right-side panel showing the list of currently connected users.
         * Updated dynamically by the server.
         */
        clientListModel = new DefaultListModel<>();
        JList<String> clientList = new JList<>(clientListModel);
        clientList.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane clientScroll = new JScrollPane(clientList);
        clientScroll.setBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(),
                        "Active Users",
                        javax.swing.border.TitledBorder.CENTER,
                        javax.swing.border.TitledBorder.TOP
                )
        );
        clientScroll.setPreferredSize(new Dimension(300, 0));
        add(clientScroll, BorderLayout.EAST);

        /*
         * Bottom panel containing server control buttons.
         * Includes Save Log, Stop Server, and Kick User.
         */
        JPanel bottomPanel = new JPanel();
        saveLogButton = new JButton("Save Log");
        stopServerButton = new JButton("Stop Server");
        JButton kickUserButton = new JButton("Kick User");

        bottomPanel.add(saveLogButton);
        bottomPanel.add(stopServerButton);
        bottomPanel.add(kickUserButton);
        add(bottomPanel, BorderLayout.SOUTH);

        // Button actions
        saveLogButton.addActionListener(e -> saveLog());
        stopServerButton.addActionListener(e -> stopServer());
        kickUserButton.addActionListener(e -> showKickUserDialog());

        /*
         * Custom window close behavior.
         * Ensures the server shuts down cleanly before exiting.
         */
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                stopServer();
                System.exit(0);
            }
        });

        setVisible(true);

        
        // Start the server on a background thread to avoid blocking the GUI.
         
        new Thread(() -> {
            try {
                server.start();
            } catch (IOException ex) {
                appendLog("Server error: " + ex.getMessage());
            }
        }).start();
    }

    // Displays a dialog allowing the admin to select and kick a connected user
    private void showKickUserDialog() {
        if (clientListModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No users are currently connected.");
            return;
        }

        String[] users = new String[clientListModel.size()];
        for (int i = 0; i < clientListModel.size(); i++) {
            users[i] = clientListModel.get(i);
        }

        String selected = (String) JOptionPane.showInputDialog(
                this,
                "Select a user to kick:",
                "Kick User",
                JOptionPane.PLAIN_MESSAGE,
                null,
                users,
                users[0]
        );

        if (selected != null) {
            server.kickUser(selected);
        }
    }

    // Appends a message to the log area safely from any thread
    public void appendLog(String text) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(text + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // Updates the list of connected clients displayed in the GUI
    public void updateClientList(Set<ChatServer.ClientHandler> clients) {
        SwingUtilities.invokeLater(() -> {
            clientListModel.clear();
            for (ChatServer.ClientHandler c : clients) {
                clientListModel.addElement(c.name + " - " + c.socket.getRemoteSocketAddress());
            }
        });
    }

    // Saves the current server log to a text file chosen by the user
    private void saveLog() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Server Log");
        chooser.setSelectedFile(new File("server_log.txt"));

        int result = chooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();

            try (PrintWriter out = new PrintWriter(new FileWriter(file, false))) {
                out.println(logArea.getText());
                appendLog("[Log saved to: " + file.getAbsolutePath() + "]");
            } catch (IOException e) {
                appendLog("[Failed to save log: " + e.getMessage() + "]");
            }
        } else {
            appendLog("[Log save canceled]");
        }
    }

    // Stops the server and logs the action
    private void stopServer() {
        appendLog("[Stopping server...]");
        server.stopServer();
    }

    // Entry point for launching the server GUI with user-specified host and port
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
