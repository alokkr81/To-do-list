package db;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/todo_db";
    private static final String USER = "root";
    private static final String PASSWORD = "hasanali7623"; // 👈 change this!

    public static Connection getConnection() {
        try {
            // Load driver (required in some setups)
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Connect
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
