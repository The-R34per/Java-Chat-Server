import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/*
 *================================================================================================ 																						    	||
 *||  Java Chat Server © 2026 by The-R34per is licensed under CC BY-NC-SA 4.0.					||
 *||  To view a copy of this license, visit https://creativecommons.org/licenses/by-nc-sa/4.0/	||
 *================================================================================================
*/

public class ChatClientGUI extends JFrame {

    private final JTextArea chatArea = new JTextArea(20, 50);     // Main chat display area where incoming and outgoing messages appear
    private final JTextField inputField = new JTextField(40);     // Text field where the user types outgoing messages
    private ChatClient client;     // Networking client responsible for server communication
    private String displayName;     // Display name chosen by the user

    // Constructs the graphical chat client window and initializes UI components
    public ChatClientGUI() {
        setTitle("Un-Encrypted Chat - " + displayName);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        chatArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(chatArea);

        JPanel bottom = new JPanel();
        bottom.add(inputField);

        JButton sendBtn = new JButton("Send");
        bottom.add(sendBtn);

        getContentPane().add(scroll, BorderLayout.CENTER);
        getContentPane().add(bottom, BorderLayout.SOUTH);

        // Send message when the button is pressed
        sendBtn.addActionListener(e -> sendInput());

        // Send message when Enter is pressed inside the text field
        inputField.addActionListener(e -> sendInput());

        pack();
        setLocationRelativeTo(null);
    }

    // Handles sending user input to the server
    private void sendInput() {
        String text = inputField.getText().trim();
        if (text.isEmpty() || client == null) return;

        // Intercepts the admin command for kicking users
        if (text.equalsIgnoreCase("/kickuser")) {
            client.requestKickUser();
            inputField.setText("");
            return;
        }

        try {
            // Sends the message to the server
            client.sendPlain(text);

            // Displays the message locally in the chat window
            chatArea.append(displayName + ": " + text + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
            inputField.setText("");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Send failed: " + ex.getMessage());
        }
    }

    // Initializes the ChatClient and connects to the server
    private void startClient(String host, int port, String name) {
        this.displayName = name;

        // Callback for receiving messages from the server
        Consumer<String> onMessage = (msg) -> SwingUtilities.invokeLater(() -> {
            chatArea.append(msg + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });

        client = new ChatClient(host, port, onMessage);

        try {
            client.start(name);
            chatArea.append("Connected to " + host + ":" + port + "\n");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not start client: " + ex.getMessage());
        }

        // Updates the window title after the display name is known
        setTitle("UnEncrypted Chat Client - " + displayName);
    }

    // Entry point for launching the chat client GUI
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            ChatClientGUI gui = new ChatClientGUI();

            // Prompt user for connection details
            String host = JOptionPane.showInputDialog(gui, "Server host:", "localhost");
            String portStr = JOptionPane.showInputDialog(gui, "Server port:", "5050");
            String name = JOptionPane.showInputDialog(gui, "Display name:", "Anonymous");

            int port = 5000;
            try { port = Integer.parseInt(portStr); } catch (Exception ignored) {}

            gui.setVisible(true);
            gui.startClient(host, port, name);
        });
    }
}
