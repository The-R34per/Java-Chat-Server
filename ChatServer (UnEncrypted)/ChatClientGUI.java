package TestingPackage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;

/*
 	Author - The-R34per
	Last Updated October 20th, 2025
	
	
    ChatClientGUI.java  © 2025 by The-R34per
	Licensed under Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International. 
	To view a copy of this license, visit https://creativecommons.org/licenses/by-nc-sa/4.0/

	
*/

public class ChatClientGUI extends JFrame {
    private final JTextArea chatArea = new JTextArea(20, 50);
    private final JTextField inputField = new JTextField(40);
    private ChatClient client;
    private String displayName;

    public ChatClientGUI() {
        setTitle("Encrypted Chat (AES-GCM)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        chatArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(chatArea);

        JPanel bottom = new JPanel();
        bottom.add(inputField);
        JButton sendBtn = new JButton("Send");
        bottom.add(sendBtn);

        getContentPane().add(scroll, BorderLayout.CENTER);
        getContentPane().add(bottom, BorderLayout.SOUTH);

        sendBtn.addActionListener(e -> sendInput());
        inputField.addActionListener(e -> sendInput());

        pack();
        setLocationRelativeTo(null);
    }

    private void sendInput() {
        String text = inputField.getText().trim();
        if (text.isEmpty() || client == null) return;

        String fullMessage = displayName + ": " + text;

        try {
            client.sendPlain(fullMessage);
            chatArea.append(fullMessage + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
            inputField.setText("");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Send failed: " + ex.getMessage());
        }
    }


    private void startClient(String host, int port, String name) {
        this.displayName = name;

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
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChatClientGUI gui = new ChatClientGUI();

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
