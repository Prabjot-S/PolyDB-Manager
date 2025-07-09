/*
Project: Program that connects with various database showing JDBC

DESCRIPTION
 * This project is a Java based desktop application for managing multiple types of databases.
 * It features a simple login, a main dashboard for data interaction, and
   the ability to view and modify database records.
 * The GUI is built using JavaFX.

 LAB: NYIT ETIC (Entrepreneurship and Technology Innovation Center)
 DATE: 7/3/2025

 By: Prabjot Singh, Vraj Patel, Kabir Marwaha
 */




package com.example.polydb;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        //add custom logo
        try {
            InputStream iconStream = getClass().getResourceAsStream("/images/diamond.png");
            if (iconStream != null) {
                Image icon = new Image(iconStream);
                primaryStage.getIcons().add(icon);
            } else {
                System.err.println("Warning: Could not load application icon");
            }
        } catch (Exception e) {
            System.err.println("Error loading application icon: " + e.getMessage());
        }

        Parent root = FXMLLoader.load(getClass().getResource("login.fxml"));
        primaryStage.setTitle("PolyDB Studio");
        primaryStage.setScene(new Scene(root, 620, 400));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch();
    }


}


