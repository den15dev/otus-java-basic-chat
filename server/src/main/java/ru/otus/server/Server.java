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
    private AuthenticatedProvider authenticatedProvider;


    public Server(int port) {
        this.port = port;
        clients = new CopyOnWriteArrayList<>();
        this.authenticatedProvider = new InMemoryAuthenticatedProvider(this);
    }


    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started. Port: " + port);

            while (true) {
                Socket socket = serverSocket.accept();
//                subscribe(new ClientHandler(this, socket));
                new ClientHandler(this, socket);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void subscribe(ClientHandler clientHandler) {
        broadcastMessage("Admin", "Подключился пользователь " + clientHandler.getUsername());
        clients.add(clientHandler);
    }


    public void unsubscribe(ClientHandler clientHandler) {
        broadcastMessage("Admin", "Пользователь " + clientHandler.getUsername() + " покинул чат");
        clients.remove(clientHandler);
    }


    public void broadcastMessage(String sender, String message) {
        for (ClientHandler c : clients) {
            c.sendMsg(ConsoleColors.CYAN_BOLD + sender + " : " + ConsoleColors.BLUE + message + ConsoleColors.RESET);
        }
    }


    public void sendMessageTo(String message, Set<String> usernames) {
        for (ClientHandler c : clients) {
            if (usernames.contains(c.getUsername())) {
                c.sendMsg(message);
            }
        }
    }


    public boolean isUsernameBusy(String username){
        for (ClientHandler c : clients) {
            if (c.getUsername().equals(username)){
                return true;
            }
        }
        return false;
    }


    public AuthenticatedProvider getAuthenticatedProvider() {
        return authenticatedProvider;
    }
}
