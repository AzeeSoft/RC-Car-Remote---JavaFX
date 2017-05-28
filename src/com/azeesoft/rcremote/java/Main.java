package com.azeesoft.rcremote.java;

import com.azeesoft.rcremote.java.controllers.MainController;
import com.azeesoft.rcremote.java.tools.wifi.IPClient;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("../res/layouts/main_layout.fxml"));
        Parent root = fxmlLoader.load();
        MainController mainController = fxmlLoader.getController();

        primaryStage.setTitle("RC Remote");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();

        mainController.setCurrentStage(primaryStage);
        mainController.showRCController();

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                IPClient ipClient = IPClient.getIPClient();
                if (ipClient.getClientSocket() != null && ipClient.getClientSocket().isConnected()) {
                    ipClient.disconnect();
                }
            }
        });
    }


    public static void main(String[] args) {
        launch(args);
    }
}
