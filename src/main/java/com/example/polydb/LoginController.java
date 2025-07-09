package com.example.polydb;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import javafx.event.ActionEvent;

import java.io.IOException;
import java.security.Key;

public class LoginController {

    @FXML
    private Button loginButton;
    @FXML
    private Button logoutButton;
    @FXML
    private BorderPane firstPane;
    @FXML
    private Label wrongLogin;
    @FXML
    private TextField username;
    @FXML
    private PasswordField password;

    Stage stage;

    public void initialize() {
        // Add key listeners to both fields
        //this (current controller class), :: (refer to an existing method without calling it immediately)
        username.setOnKeyPressed(this::handleKeyPress);
        password.setOnKeyPressed(this::handleKeyPress);
        //firstPane.setOnKeyPressed(this::handleKeyPress);
    }

    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            try {
                checkLogin(new ActionEvent(loginButton, null));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (event.getCode() == KeyCode.ESCAPE) {
            logout(new ActionEvent(logoutButton, null));
        }
    }


    public void userLogin(ActionEvent event) throws IOException {
        checkLogin(event);
    }

    private void checkLogin(ActionEvent event) throws IOException {

        if (username.getText().equals("admin") && password.getText().equals("admin")) {
            wrongLogin.setText("Success!");

            //get current stage from the event source
            Node node = (Node) event.getSource();
            Stage stage = (Stage) node.getScene().getWindow();

            //load new scene
            Parent root = FXMLLoader.load(getClass().getResource("tableManagement.fxml")); //will be used to go to the table overview screen
            stage.setScene(new Scene(root));
            stage.centerOnScreen();


        } else if (username.getText().isEmpty() && password.getText().isEmpty()) {
            wrongLogin.setText("Please enter your information");
        } else {
            wrongLogin.setText("Wrong username or password!");
        }


    }

    public void logout(ActionEvent event) {

        //making an alert box
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("You're about to logout!");
        alert.setContentText("Do you want to save before exiting?:");

        if(alert.showAndWait().get() == ButtonType.OK){
            stage = (Stage) firstPane.getScene().getWindow();
            System.out.println("You successfully logged out!");
            stage.close();
        }

    }
}
