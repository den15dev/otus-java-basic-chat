package ru.otus.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.EOFException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Set;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private String username;
    private Role role;

    private boolean isAuthenticate;

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        System.out.println("Client connected port:" + socket.getPort());

        new Thread(() -> {
            try {
                // цикл аутентификации
                while (!isAuthenticate) {
                    sendMsg("Перед работой с чатом необходимо выполнить аутентификацию \n" +
                            ConsoleColors.GREEN_BOLD + "/auth login password" + ConsoleColors.RESET +
                            " или зарегистрироваться \n" +
                            ConsoleColors.GREEN_BOLD + "/reg login password username" + ConsoleColors.RESET);

                    String message = in.readUTF();

                    if (message.startsWith("/")) {
                        if (message.equals("/exit")) {
                            sendMsg("/exitok");
                            break;
                        }
                        // /auth login password
                        if (message.startsWith("/auth ")) {
                            String[] token = message.trim().split(" ");
                            if (token.length != 3) {
                                sendMsg(ConsoleColors.RED + "Неверный формат команды /auth " + ConsoleColors.RESET);
                                continue;
                            }
                            if (server.getAuthenticatedProvider()
                                    .authenticate(this, token[1], token[2])) {
                                isAuthenticate = true;
                                break;
                            }
                            continue;
                        }
                        // /reg login password username
                        if (message.startsWith("/reg")) {
                            String[] token = message.trim().split(" ");
                            if (token.length != 4) {
                                sendMsg(ConsoleColors.RED + "Неверный формат команды /reg " + ConsoleColors.RESET);
                                continue;
                            }
                            if (server.getAuthenticatedProvider()
                                    .register(this, token[1], token[2], token[3])) {
                                isAuthenticate = true;
                                break;
                            }
                        }
                    }
                }

                while (isAuthenticate) {
                    String message = in.readUTF();

                    //  Служебные сообщения
                    if (message.startsWith("/")) {
                        String[] tokens = message.split(" ", 3);
                        String command = tokens[0];

                        if (command.equals("/exit")) {
                            sendMsg("/exitok");
                            server.broadcastMessage("Admin", "Пользователь " + username + " покинул чат.");
                            break;
                        }

                        if (command.equals("/w")) {
                            String recipientUsername = tokens[1];
                            String privateMessage = tokens[2];

                            Set<String> recipients = Set.of(username, recipientUsername);

                            server.sendMessageTo(username + ": " + privateMessage, recipients);
                        }

                        if (command.equals("/kick") && isAdmin()) {
                            String usernameToKick = tokens[1];
                            ClientHandler clientToKick = server.getClientByUsername(usernameToKick);

                            if (clientToKick == null) {
                                sendMsg("Такого пользователя не существует, или он уже отключился.");
                                continue;
                            }

                            Set<String> recipients = Set.of(usernameToKick);
                            server.sendMessageTo("/kick", recipients);
                            clientToKick.disconnect();

                            server.broadcastMessage(
                                "Admin",
                                ConsoleColors.YELLOW +
                                "Пользователь " + usernameToKick + " был исключён из чата админом." +
                                ConsoleColors.RESET
                            );
                        }

                    } else {
                        server.broadcastMessage(username, message);
                    }
                }

            } catch (EOFException | SocketException e) {
                // соединение было закрыто

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

    public void setRole(Role role) {
        this.role = role;
    }

    private boolean isAdmin() {
        return this.role == Role.ADMIN;
    }

    public void disconnect() {
        server.unsubscribe(this);
        System.out.println("Client disconnected username: " + username);

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
