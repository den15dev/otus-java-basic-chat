package ru.otus.server;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBAuthenticatedProvider implements AuthenticatedProvider {
    private class User {
        private final String login;
        private final String username;
        private final Role role;

        public User(String login, String username, Role role) {
            this.login = login;
            this.username = username;
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


    DBAuthenticatedProvider(Server server) {
        this.server = server;
        createUserTable();
    }


    @Override
    public void initialize() {
        System.out.println("Сервер аутентификации запущен в режиме Database");
    }


    public void createUserTable() {
        try (Statement stmt = DB.getConnection().createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS \"user\" (" +
                    "id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                    "name TEXT NOT NULL," +
                    "login TEXT NOT NULL," +
                    "password TEXT NOT NULL," +
                    "role VARCHAR(100) DEFAULT 'user' NOT NULL," +
                    "CONSTRAINT user_name_uniq UNIQUE (name)," +
                    "CONSTRAINT user_login_uniq UNIQUE (login)" +
                    ")");

        } catch (SQLException e) {
            throw new RuntimeException("Не удалось подключиться к БД: " + e.getMessage());
        }
    }


    private boolean isLoginAlreadyExists(String login) {
        String query = "SELECT 1 FROM \"user\" WHERE login = ? LIMIT 1";

        try (PreparedStatement stmt = DB.getConnection().prepareStatement(query)) {
            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка базы данных: " + e.getMessage());
        }
    }


    private boolean isUsernameAlreadyExists(String username) {
        String query = "SELECT 1 FROM \"user\" WHERE name = ? LIMIT 1";

        try (PreparedStatement stmt = DB.getConnection().prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка базы данных: " + e.getMessage());
        }
    }


    private void addUser(String username, String login, String password) {
        String command = "INSERT INTO \"user\" (name, login, password) VALUES(?, ?, ?)";
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        try (PreparedStatement stmt = DB.getConnection().prepareStatement(command)) {
            stmt.setString(1, username);
            stmt.setString(2, login);
            stmt.setString(3, hashedPassword);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка базы данных: " + e.getMessage());
        }
    }


    private User getUserByLoginAndPassword(String login, String password) {
        String query = "SELECT * FROM \"user\" WHERE login = ? LIMIT 1";

        try (PreparedStatement stmt = DB.getConnection().prepareStatement(query)) {
            stmt.setString(1, login);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                if (!BCrypt.checkpw(password, rs.getString("password"))) {
                    return null;
                }

                return new User(
                    rs.getString("login"),
                    rs.getString("name"),
                    Role.fromString(rs.getString("role"))
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка базы данных: " + e.getMessage());
        }
    }


    @Override
    public boolean authenticate(ClientHandler clientHandler, String login, String password) {
        User user = getUserByLoginAndPassword(login, password);

        if (user == null) {
            clientHandler.sendMsg(ConsoleColors.RED + "Некоректный логин / пароль" + ConsoleColors.RESET);
            return false;
        }

        String authUsername = user.getUsername();
        if (server.isUsernameBusy(authUsername)) {
            clientHandler.sendMsg(ConsoleColors.RED + "Указанная учетная запись уже занята" + ConsoleColors.RESET);
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
            clientHandler.sendMsg(ConsoleColors.RED + "Логин должен состоять из 3+ символов" + ConsoleColors.RESET);
            return false;
        }
        if (username.trim().length() < 3) {
            clientHandler.sendMsg(ConsoleColors.RED + "Имя пользователя должна состоять из 3+ символов" + ConsoleColors.RESET);
            return false;
        }
        if (isLoginAlreadyExists(login)) {
            clientHandler.sendMsg(ConsoleColors.RED + "Указанный логин уже занят" + ConsoleColors.RESET);
            return false;
        }
        if (isUsernameAlreadyExists(username)) {
            clientHandler.sendMsg(ConsoleColors.RED + "Указанное имя пользователя уже занято" + ConsoleColors.RESET);
            return false;
        }

        addUser(username, login, password);

        clientHandler.setUsername(username);
        clientHandler.sendMsg("Вы успешно зарегистрировались и подключились под ником: " + username);
        server.subscribe(clientHandler);
        clientHandler.sendMsg("/regok " + username);

        return true;
    }
}
