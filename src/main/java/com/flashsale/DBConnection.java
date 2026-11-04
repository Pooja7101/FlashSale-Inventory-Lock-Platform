
package com.flashsale;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    public static Connection getConnection() {

        try {
            String host = System.getenv("DB_HOST");

            // Cloud database: TiDB
            if (host != null && !host.isEmpty()) {

                String port = System.getenv("DB_PORT");
                String database = System.getenv("DB_NAME");
                String user = System.getenv("DB_USER");
                String password = System.getenv("DB_PASSWORD");

                String url = "jdbc:mysql://" + host + ":"
                        + port + "/" + database
                        + "?sslMode=REQUIRED";

                Class.forName("com.mysql.cj.jdbc.Driver");

                Connection connection = DriverManager.getConnection(
                        url, user, password
                );

                System.out.println("TiDB connected successfully!");

                return connection;
            }

            // Local database: XAMPP MariaDB
            String url = "jdbc:mariadb://localhost:3306/flashsale_db";

            Class.forName("org.mariadb.jdbc.Driver");

            Connection connection = DriverManager.getConnection(
                    url, "root", ""
            );

            System.out.println("Local MariaDB connected successfully!");

            return connection;

        } catch (Exception e) {

            System.out.println("Database connection failed!");
            e.printStackTrace();

            return null;
        }
    }
}
