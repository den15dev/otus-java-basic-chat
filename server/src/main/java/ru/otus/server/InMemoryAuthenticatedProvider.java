package ru.otus.server;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryAuthenticatedProvider implements AuthenticatedProvider {
    private class User {
        private String login;
        private String password;
        private String username;
        private Role role;

        public User(String login, String password, String username) {
            this.login = login;
            this.password = password;
            this.username = username;
            this.role = Role.USER;
        }

        public void setRole(Role role) {
            this.role = role;
        }

        public String getUsername() {
            return username;
        }

        public Role getRole() {
            return role;
        }
    }

    private Server server;
    private List<User> users;

    public InMemoryAuthenticatedProvider(Server server) {
        this.server = server;
        this.users = new CopyOnWriteArrayList<>();
        this.users.add(new User("qwe", "qwe", "qwe1"));
        this.users.add(new User("asd", "asd", "asd1"));
        this.users.add(new User("zxc", "zxc", "zxc1"));

        User admin = new User("adm", "adm", "admin1");
        admin.setRole(Role.ADMIN);
        this.users.add(admin);
    }

    @Override
    public void initialize() {
        System.out.println("Сервер аутентификации запущен в режиме InMemory");
    }

    private User getUserByLoginAndPassword(String login, String password) {
        for (User u : users) {
            if (u.login.equals(login) && u.password.equals(password)) {
                return u;
            }
        }
        return null;
    }

    private boolean isLoginAlreadyExists(String login) {
        for (User u : users) {
            if (u.login.equals(login)) {
                return true;
            }
        }
        return false;
    }

    private boolean isUsernameAlreadyExists(String username) {
        for (User u : users) {
            if (u.username.equals(username)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean authenticate(ClientHandler clientHandler, String login, String password) {
        User user = getUserByLoginAndPassword(login, password);
        if (user == null) {
            clientHandler.sendMsg("Некоректный логин / пароль");
            return false;
        }

        String authUsername = user.getUsername();
        if (server.isUsernameBusy(authUsername)) {
            clientHandler.sendMsg("Указанная учетная запись уже занята");
            return false;
        }

        clientHandler.setUsername(authUsername);
        clientHandler.setRole(user.getRole());
        clientHandler.sendMsg("Вы подключились под ником: " + authUsername);
        server.subscribe(clientHandler);
        clientHandler.sendMsg("/authok " + authUsername);

        return true;
    }

    @Override
    public boolean register(ClientHandler clientHandler, String login, String password, String username) {
        if (login.trim().length() < 3) {
            clientHandler.sendMsg("Логин должен состоять из 3+ символов");
            return false;
        }
        if (username.trim().length() < 3) {
            clientHandler.sendMsg("Имя пользователя должна состоять из 3+ символов");
            return false;
        }
        if (isLoginAlreadyExists(login)) {
            clientHandler.sendMsg("Указанный логин уже занят");
            return false;
        }
        if (isUsernameAlreadyExists(username)) {
            clientHandler.sendMsg("Указанное имя пользователя уже занято");
            return false;
        }
        users.add(new User(login, password, username));
        clientHandler.setUsername(username);
        clientHandler.sendMsg("Вы успешно зарегистрировались и подключились под ником: " + username);
        server.subscribe(clientHandler);
        clientHandler.sendMsg("/regok " + username);
        return true;
    }
}
