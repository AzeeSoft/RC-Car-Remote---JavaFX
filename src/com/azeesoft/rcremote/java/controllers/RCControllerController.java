package com.azeesoft.rcremote.java.controllers;

import com.azeesoft.rcremote.java.tools.Stools;
import com.azeesoft.rcremote.java.tools.wifi.CommConstants;
import com.azeesoft.rcremote.java.tools.wifi.CommConstants.RESPONSE_DATA_FLAGS_FAILURE;
import com.azeesoft.rcremote.java.tools.wifi.IPClient;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXToggleButton;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.web.WebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import static com.azeesoft.rcremote.java.tools.wifi.CommConstants.RESPONSE_DATA_FLAGS_FAILURE.*;
import static com.azeesoft.rcremote.java.tools.wifi.CommConstants.RESPONSE_DATA_FLAGS_FAILURE.MAX_CONN_REACHED;
import static com.azeesoft.rcremote.java.tools.wifi.CommConstants.RESPONSE_DATA_FLAGS_FAILURE.NON_JSON_DATA;

/**
 * Created by azizt on 5/27/2017.
 */
public class RCControllerController implements Initializable, IPClient.OnServerDataReceivedListener {

    IPClient ipClient;

    @FXML
    JFXToggleButton connectDisconnectArduino, startStopLiveStream;

    @FXML
    MediaView liveStreamMediaView;

    @FXML
    JFXTextField speechTextField;

    @FXML
    JFXButton speakBtn;

    @FXML
    HBox liveStreamHolder;

    @FXML
    AnchorPane noWifiOverlay;



    @Override
    public void initialize(URL location, ResourceBundle resources) {
        prepareIPClient();
        prepareUI();
    }

    private void prepareIPClient() {
        ipClient = IPClient.getIPClient();
        if (ipClient.getClientSocket() == null || !ipClient.getClientSocket().isConnected()) {
            showWifiErrorOverlay();
        }

        ipClient.setOnServerDataReceivedListener(this);

        ipClient.setOnServerDisconnectedListener(new IPClient.OnServerDisconnectedListener() {
            @Override
            public void onServerDisconnected() {
                showWifiErrorOverlay();
            }
        });
    }

    public void prepareUI() {
        disconnectFromHLSStream();
        liveStreamHolder.getChildren().remove(liveStreamMediaView);
    }

    private void showWifiErrorOverlay() {
        noWifiOverlay.setVisible(true);
    }

    @FXML
    public void connectToServer() {
        ipClient.connect(new IPClient.OnServerConnectedListener() {
            @Override
            public void onServerConnectionSucceeded() {
                noWifiOverlay.setVisible(false);
            }

            @Override
            public void onServerConnectionFailed() {
                noWifiOverlay.setVisible(true);
            }
        }, this);
    }

    @FXML
    public void closeConnectionWithServer() {
        ipClient.disconnect();
    }

    @FXML
    public void resetAllConnections() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(CommConstants.NAME_SUCCESS, true);
            jsonObject.put(CommConstants.REQUEST_NAME_RESET_WIFI_CONNECTIONS, true);

            ipClient.sendData(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void restartWifiServer() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(CommConstants.NAME_SUCCESS, true);
            jsonObject.put(CommConstants.REQUEST_NAME_START_WIFI_SERVER, true);

            ipClient.sendData(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void connectDisconnectArduino(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(CommConstants.NAME_SUCCESS, true);

            if (connectDisconnectArduino.isSelected()) {
                jsonObject.put(CommConstants.REQUEST_NAME_CONNECT_TO_RC_CAR, true);
            } else {
                jsonObject.put(CommConstants.REQUEST_NAME_DISCONNECT_FROM_RC_CAR, true);
            }

            ipClient.sendData(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void startStopLiveStream(){

        if (startStopLiveStream.isSelected()) {
            liveStreamHolder.getChildren().add(liveStreamMediaView);

            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put(CommConstants.NAME_SUCCESS,true);
                jsonObject.put(CommConstants.REQUEST_NAME_START_HLS_SERVER, true);
                ipClient.sendData(jsonObject, new IPClient.OnResponseReceivedCallback(){
                    @Override
                    public void onResponseReceived(JSONObject jsonObject) {
                        try {
                            if(jsonObject.getBoolean(CommConstants.NAME_SUCCESS)){
                                connectToHLSStream();
                            }else{
                                String msg="ERROR_UNKNOWN";
                                if(jsonObject.has(CommConstants.NAME_MESSAGE)){
                                    msg = jsonObject.getString(CommConstants.NAME_MESSAGE);
                                }
//                                Toast.makeText(getActivity(), "Error starting HLS Server: "+msg, Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            liveStreamHolder.getChildren().remove(liveStreamMediaView);

            disconnectFromHLSStream();
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put(CommConstants.NAME_SUCCESS,true);
                jsonObject.put(CommConstants.REQUEST_NAME_STOP_HLS_SERVER, true);
                ipClient.sendData(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void speakText(){
        String speechText = speechTextField.getText();
        if(!speechText.isEmpty()){
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put(CommConstants.NAME_SUCCESS,true);
                jsonObject.put(CommConstants.REQUEST_NAME_SPEAK, true);
                jsonObject.put(CommConstants.NAME_SPEECH_DATA, speechText);
                ipClient.sendData(jsonObject);
                speechTextField.setText("");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void transmitJoystickData(String data) {

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(CommConstants.NAME_SUCCESS, true);
            jsonObject.put(CommConstants.REQUEST_NAME_ARDUINO_BLUETOOTH_DATA, data);
            ipClient.sendData(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void connectToHLSStream() {
        Preferences sp = Stools.getPreferences();
        String serverIP = sp.get("server_ip_address", " ");
        String serverLiveStreamPort = sp.get("live_stream_port", " ");

        Stools.log("Connecting to hls...");

        Media media = new Media("http://" + serverIP + ":" + serverLiveStreamPort + "/");
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        liveStreamMediaView.setMediaPlayer(mediaPlayer);
        mediaPlayer.play();
    }

    private void disconnectFromHLSStream() {
        MediaPlayer mediaPlayer = liveStreamMediaView.getMediaPlayer();
        if(mediaPlayer!=null){
            mediaPlayer.stop();
        }
    }


    @Override
    public void onServerDataReceived(JSONObject jsonObject) {
        try {
            if (jsonObject.getBoolean(CommConstants.NAME_SUCCESS)) {

            } else {
                if (jsonObject.has(CommConstants.NAME_FLAGS_ARRAY)) {
                    JSONArray flagsArray = jsonObject.getJSONArray(CommConstants.NAME_FLAGS_ARRAY);
                    for (int i = 0; i < flagsArray.length(); i++) {
                        RESPONSE_DATA_FLAGS_FAILURE flag = valueOf(flagsArray.get(i).toString());
                        switch (flag) {
                            case MAX_CONN_REACHED:
                                closeConnectionWithServer();
                                showWifiErrorOverlay();
//                                Toast.makeText(getActivity(), "Max Connection limit reached for the robot!", Toast.LENGTH_LONG).show();
                                break;
                            case NON_JSON_DATA:

                                break;
                            default:
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
