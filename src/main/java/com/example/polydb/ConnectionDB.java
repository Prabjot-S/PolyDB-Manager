package com.example.polydb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConnectionDB {

    public static void main(String[] args) {
        // PostgreSQL connection check
        checkPostgreSQLConnection(
                "jdbc:postgresql://localhost:5432/TestD",
                "postgres",
                "samplePass"
        );

        // MySQL connection check
        checkMySQLConnection(
                "jdbc:mysql://localhost:3306/world",
                "root",
                "samplePass"
        );

        // ORACLE connection check
        checkOracleConnection(
                "jdbc:oracle:thin:@localhost:1521/free",
                "system",
                "samplePass"
        );

    }

    public static void checkPostgreSQLConnection(String url, String username, String password) {
        System.out.println("Attempting to connect to PostgreSQL database...");

        try {
            // Load the PostgreSQL JDBC driver
            Class.forName("org.postgresql.Driver");

            // Set connection properties
            Properties props = new Properties();
            props.setProperty("user", username);
            props.setProperty("password", password);
            props.setProperty("ssl", "false"); // Adjust SSL as needed

            // Try to establish connection
            try (Connection conn = DriverManager.getConnection(url, props)) {
                System.out.println("✅ Successfully connected to PostgreSQL database!");
                System.out.println("  Database: " + conn.getMetaData().getDatabaseProductName());
                System.out.println("  Version: " + conn.getMetaData().getDatabaseProductVersion());
            }
        } catch (ClassNotFoundException e) {
            System.err.println("❌ PostgreSQL JDBC driver not found!");
            System.err.println("Please add the PostgreSQL JDBC driver (JAR) to your classpath.");
        } catch (SQLException e) {
            System.err.println("❌ Failed to connect to PostgreSQL database!");
            System.err.println("Error: " + e.getMessage());
        }
    }

    public static void checkMySQLConnection(String url, String username, String password) {
        System.out.println("\nAttempting to connect to MySQL database...");

        try {
            // Load the MySQL JDBC driver (try new version first)
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                Class.forName("com.mysql.jdbc.Driver"); // Fallback to old driver
            }

            // Set connection properties
            Properties props = new Properties();
            props.setProperty("user", username);
            props.setProperty("password", password);
            props.setProperty("useSSL", "false"); // Adjust SSL as needed

            // Try to establish connection
            try (Connection conn = DriverManager.getConnection(url, props)) {
                System.out.println("✅ Successfully connected to MySQL database!");
                System.out.println("  Database: " + conn.getMetaData().getDatabaseProductName());
                System.out.println("  Version: " + conn.getMetaData().getDatabaseProductVersion());
            }
        } catch (ClassNotFoundException e) {
            System.err.println("❌ MySQL JDBC driver not found!");
            System.err.println("Please add the MySQL Connector/J driver (JAR) to your classpath.");
        } catch (SQLException e) {
            System.err.println("❌ Failed to connect to MySQL database!");
            System.err.println("Error: " + e.getMessage());
        }
    }

    public static void checkOracleConnection(String url, String username, String password){
        System.out.println("\nAttempting to connect to Oracle database...");

        try {
            //Load Oracle JDBC Driver
            Class.forName("oracle.jdbc.driver.OracleDriver");

            // Try to establish connection
            try (Connection conn = DriverManager.getConnection(url, username, password)) {
                System.out.println("✅ Successfully connected to Oracle database!");
                System.out.println("  Database: " + conn.getMetaData().getDatabaseProductName());
                System.out.println("  Version: " + conn.getMetaData().getDatabaseProductVersion());
            }
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Oracle JDBC driver not found!");
            System.err.println("Please add the Oracle JDBC driver (ojdbc10.jar or similar) to your classpath.");
        } catch (SQLException e) {
            System.err.println("❌ Failed to connect to Oracle database!");
            System.err.println("Error: " + e.getMessage());
        }
    }

}
