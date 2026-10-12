package com.pixelpioneers.library.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database Layer: the single JDBC connection to MySQL.
 *
 * Connection settings are read from a file called db.properties in the folder
 * the program is started from, so the database password can be changed on any
 * computer without recompiling. If the file is missing the defaults below
 * (local MySQL, user "root", blank password) are used.
 */
public class DBConnection {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/library_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private static Connection connection;

    private DBConnection() { }

    /** Returns the shared connection, (re)opening it if needed. */
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Properties settings = loadSettings();
                String url = settings.getProperty("db.url", DEFAULT_URL);
                String user = settings.getProperty("db.user", DEFAULT_USER);
                String password = settings.getProperty("db.password", DEFAULT_PASSWORD);
                Class.forName(settings.getProperty("db.driver", DEFAULT_DRIVER));
                connection = DriverManager.getConnection(url, user, password);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("The MySQL JDBC driver was not found. Add mysql-connector-j to the "
                    + "project libraries (or keep the lib folder next to the .jar).", e);
        } catch (SQLException e) {
            throw new RuntimeException("Could not connect to the database. Check that MySQL is running, that "
                    + "the library_db database exists, and that db.user / db.password in db.properties are correct.", e);
        }
        return connection;
    }

    private static Properties loadSettings() {
        Properties props = new Properties();
        File file = new File("db.properties");
        if (file.exists()) {
            try (InputStream in = new FileInputStream(file)) {
                props.load(in);
            } catch (IOException e) {
                // fall back to defaults if the file cannot be read
            }
        }
        return props;
    }
}
