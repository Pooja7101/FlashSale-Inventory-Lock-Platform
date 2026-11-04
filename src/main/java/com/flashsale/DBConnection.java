
package com.flashsale;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    private static final String URL =
            "jdbc:mariadb://localhost:3306/flashsale_db";

    private static final String USER = "root";

    // Enter your MariaDB root password here.
    // If root has no password, keep this empty.
    private static final String PASSWORD = "";

    public static Connection getConnection() {

        try {
            Class.forName("org.mariadb.jdbc.Driver");

            Connection connection = DriverManager.getConnection(
                    URL, USER, PASSWORD
            );

            System.out.println("Database connected successfully!");

            return connection;

        } catch (Exception e) {

            System.out.println("Database connection failed!");
            e.printStackTrace();

            return null;
        }
    }
}
