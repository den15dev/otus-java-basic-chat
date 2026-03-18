package ru.otus.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DB {
    public static Connection connection;


    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5436/chat?user=postgres&password=postgres");
            }
            return connection;

        } catch (SQLException e) {
            throw new RuntimeException("Не удалось подключиться к БД: " + e.getMessage());
        }
    }
}
