package TestingPackage;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class ChatClient {
    private final String host;
    private final int port;
    private final Consumer<String> onMessage;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public ChatClient(String host, int port, Consumer<String> onMessage) {
        this.host = host;
        this.port = port;
        this.onMessage = onMessage;
    }

    public void start(String name) throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

        // Send display name as first line
        out.println(name);

        // Reader thread
        Thread t = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    onMessage.accept(line);
                }
            } catch (IOException e) {
                onMessage.accept("Disconnected: " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void sendPlain(String msg) {
        if (out != null) {
            out.println(msg);
        }
    }

    public void close() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}
