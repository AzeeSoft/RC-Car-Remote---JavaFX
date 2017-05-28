package com.azeesoft.rcremote.java.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;


/**
 * Created by azizt on 5/27/2017.
 */
public class MainController implements Initializable {

    @FXML
    AnchorPane contentPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        showRCController();
    }

    @FXML
    public void showRCController(){
        try {
            AnchorPane anchorPane = FXMLLoader.load(getClass().getResource("../../res/layouts/rccontroller_layout.fxml"));
            setContent(anchorPane);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showSettings(){
        try {
            AnchorPane anchorPane = FXMLLoader.load(getClass().getResource("../../res/layouts/settings_layout.fxml"));
            setContent(anchorPane);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setContent(Node node){
        contentPane.getChildren().clear();
        contentPane.getChildren().add(node);
    }
}
