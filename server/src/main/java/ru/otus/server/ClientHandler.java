package ru.otus.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Set;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private String username;


    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        System.out.println("Client connected port:" + socket.getPort());
        username = "user" + socket.getPort();
        sendMsg("Вы подключились под ником: " + username);

        new Thread(() -> {
            try {
                while (true) {
                    String message = in.readUTF();

                    //  Служебные сообщения
                    if (message.startsWith("/")) {
                        String[] tokens = message.split(" ", 3);
                        String command = tokens[0];

                        if (command.equals("/exit")){
                            sendMsg("/exitok");
                            break;
                        }

                        if (command.equals("/w")) {
                            String recipientUsername = tokens[1];
                            String privateMessage = tokens[2];

                            Set<String> recipients = Set.of(username, recipientUsername);

                            server.sendMessageTo(username + ": " + privateMessage, recipients);
                        }

                    } else {
                        server.broadcastMessage(username + ": " + message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();

            } finally {
                disconnect();
            }
        }).start();
    }


    public void sendMsg(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public String getUsername() {
        return username;
    }


    public void setUsername(String username) {
        this.username = username;
    }


    private void disconnect() {
        server.unsubscribe(this);
        System.out.println("Client disconnected port:" + socket.getPort());

        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
