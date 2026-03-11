package ru.otus.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private final int port;
    private List<ClientHandler> clients;


    public Server(int port) {
        this.port = port;
        clients = new CopyOnWriteArrayList<>();
    }


    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started. port: " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                subscribe(new ClientHandler(this, socket));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void subscribe(ClientHandler clientHandler) {
        broadcastMessage("Подключился пользователь " + clientHandler.getUsername());
        clients.add(clientHandler);
    }


    public void unsubscribe(ClientHandler clientHandler) {
        broadcastMessage("Пользователь " + clientHandler.getUsername() + " покинул чат");
        clients.remove(clientHandler);
    }


    public void broadcastMessage(String message) {
        for (ClientHandler c : clients) {
            c.sendMsg(message);
        }
    }


    public void sendMessageTo(String message, Set<String> usernames) {
        for (ClientHandler c : clients) {
            if (usernames.contains(c.getUsername())) {
                c.sendMsg(message);
            }
        }
    }
}
