package ru.otus.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

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
                    //  /служебные сообщения
                    if (message.startsWith("/")) {
                        if (message.equals("/exit")){
                            sendMsg("/exitok");
                            break;
                        }

//                        String[] token = "12 erter 234 werw we".split(" ", 3);
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
