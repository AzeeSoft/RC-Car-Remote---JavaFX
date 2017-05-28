package com.azeesoft.rcremote.java.controllers;

import com.azeesoft.rcremote.java.tools.Stools;
import com.jfoenix.controls.JFXTextField;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Created by azizt on 5/27/2017.
 */
public class SettingsController implements Initializable {

    @FXML
    JFXTextField serverIP;

    @FXML
    JFXTextField serverMainPort;

    @FXML
    JFXTextField liveStreamPort;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        Preferences preferences = Stools.getPreferences();
        serverIP.setText(preferences.get("server_ip_address", ""));
        serverMainPort.setText(preferences.get("server_main_port", ""));
        liveStreamPort.setText(preferences.get("live_stream_port", ""));
    }

    @FXML
    public void saveSettings(){
        Preferences preferences = Stools.getPreferences();
        preferences.put("server_ip_address", serverIP.getText());
        preferences.put("server_main_port", serverMainPort.getText());
        preferences.put("live_stream_port", liveStreamPort.getText());

        Alert alert=new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText("Settings Updated");
        alert.setContentText("Preferences have been updated Successfully");
        alert.setTitle("Success");
        alert.show();
    }


}
