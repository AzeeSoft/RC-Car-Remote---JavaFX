package com.azeesoft.rcremote.java.tools.wifi;

import com.azeesoft.rcremote.java.tools.Stools;
import javafx.application.Platform;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Created by azizt on 5/20/2017.
 */

public class IPClient {
    final String LOG_TAG = "IP CLIENT";

    public final static int DEFAULT_PORT = 6060;
    private final static long defaultCallbackTimeout = 10000;

    private int requestId = 0;

    private String serverAddress = "";
    private int serverPort = DEFAULT_PORT;

    private Socket clientSocket;
    private BufferedWriter bufferedWriter;

    private OnServerConnectedListener onServerConnectedListener;
    private OnServerDataReceivedListener onServerDataReceivedListener;
    private OnServerDisconnectedListener onServerDisconnectedListener;

    private static IPClient thisClient;

    private ArrayList<OnResponseReceivedCallback> onResponseReceivedCallbacks = new ArrayList<>();

    public static IPClient getIPClient() {
        if (thisClient == null) {

            Preferences preferences = Stools.getPreferences();
            String ipAddress = preferences.get("server_ip_address", "");
            String port = preferences.get("server_main_port", "6060");

            thisClient = new IPClient(ipAddress, Integer.parseInt(port));
        }

        return thisClient;
    }

    private IPClient(String serverAddress, int port) {
        this.serverAddress = serverAddress;
        serverPort = port;
    }

    public void connect(OnServerConnectedListener onServerConnectedListener, OnServerDataReceivedListener onServerDataReceivedListener) {
        setOnServerConnectedListener(onServerConnectedListener);
        setOnServerDataReceivedListener(onServerDataReceivedListener);
        initiateConnection();
    }

    private void initiateConnection() {
        new Thread(new ServerConnectRunnable()).start();
    }

    public void closeConnection() {
        try {
            if (bufferedWriter != null)
                bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (clientSocket.isConnected())
                clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void sendData(JSONObject data) {
        sendData(data, null);
    }

    public void sendData(JSONObject data, OnResponseReceivedCallback onResponseReceivedCallback) {
        if (bufferedWriter != null && clientSocket != null && clientSocket.isConnected()) {
            try {
                data.put(CommConstants.NAME_CLIENT_REQUEST_ID, requestId);
                bufferedWriter.write(data + "\n");
                bufferedWriter.flush();
                Stools.log(LOG_TAG, "Data written");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }


            if (onResponseReceivedCallback != null) {
                onResponseReceivedCallback.setRequestId(requestId);
                onResponseReceivedCallbacks.add(onResponseReceivedCallback);
            }
            requestId++;

        }
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    public void disconnect() {
        closeConnection();
        if (onServerDisconnectedListener != null) {
            onServerDisconnectedListener.onServerDisconnected();
        }
    }

    public void setOnServerDataReceivedListener(OnServerDataReceivedListener onServerDataReceivedListener) {
        this.onServerDataReceivedListener = onServerDataReceivedListener;
    }

    public void setOnServerConnectedListener(OnServerConnectedListener onServerConnectedListener) {
        this.onServerConnectedListener = onServerConnectedListener;
    }

    public void setOnServerDisconnectedListener(OnServerDisconnectedListener onServerDisconnectedListener) {
        this.onServerDisconnectedListener = onServerDisconnectedListener;
    }

    private class ServerConnectRunnable implements Runnable {

        @Override
        public void run() {
            try {
                InetAddress serverAddr = InetAddress.getByName(serverAddress);
                clientSocket = new Socket(serverAddr, serverPort);
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                new Thread(new ServerListenerRunnable(new BufferedReader(new InputStreamReader(clientSocket.getInputStream())))).start();

                JSONObject jsonObject = new JSONObject();
                jsonObject.put(CommConstants.NAME_SUCCESS, true);

                JSONArray flags = new JSONArray();
                flags.put(CommConstants.RESPONSE_DATA_FLAGS_SUCCESS.AZEE_IP_HANDSHAKE);

                jsonObject.put(CommConstants.NAME_MESSAGE, "Initializing Connection");

                sendData(jsonObject);

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        if (onServerConnectedListener != null) {
                            onServerConnectedListener.onServerConnectionSucceeded();
                        }
                    }
                });
            } catch (IOException | JSONException e) {
                e.printStackTrace();

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        if (onServerConnectedListener != null) {
                            onServerConnectedListener.onServerConnectionFailed();
                        }
                    }
                });
            }
        }
    }

    private class ServerListenerRunnable implements Runnable {

        private BufferedReader bufferedReader;

        ServerListenerRunnable(BufferedReader bReader) {
            bufferedReader = bReader;
        }

        @Override
        public void run() {

            try {
                while (!Thread.currentThread().isInterrupted()) {

                    if (bufferedReader != null) {
                        String incomingData = bufferedReader.readLine();
                        if (incomingData == null) {
                            break;
                        }

                        if (!incomingData.isEmpty()) {
                            Stools.log(LOG_TAG, "Incoming data from server: " + incomingData);

                            JSONObject jsonObject;
                            try {
                                jsonObject = new JSONObject(incomingData);
                            } catch (JSONException e) {
                                e.printStackTrace();
                                jsonObject = new JSONObject();
                                try {
                                    jsonObject.put(CommConstants.NAME_SUCCESS, false);

                                    JSONArray flags = new JSONArray();
                                    flags.put(CommConstants.RESPONSE_DATA_FLAGS_FAILURE.NON_JSON_DATA);

                                    jsonObject.put(CommConstants.NAME_FLAGS_ARRAY, flags);
                                    jsonObject.put(CommConstants.NAME_NON_JSON_DATA, incomingData);
                                } catch (JSONException e1) {
                                    e1.printStackTrace();
                                }
                            }

                            OnResponseReceivedCallback currentCallback = null;
                            List<OnResponseReceivedCallback> timedOutCallbacks = new ArrayList<>();
                            for (OnResponseReceivedCallback callback : onResponseReceivedCallbacks) {
                                try {
                                    if(callback.getTimeout()<System.currentTimeMillis() - callback.getInitMillis()){
                                        timedOutCallbacks.add(callback);
                                    }
                                    else if (jsonObject.has(CommConstants.NAME_CLIENT_REQUEST_ID)) {
                                        if (callback.getRequestId() == jsonObject.getInt(CommConstants.NAME_CLIENT_REQUEST_ID)) {
                                            currentCallback = callback;
                                        }
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            onResponseReceivedCallbacks.removeAll(timedOutCallbacks);
                            timedOutCallbacks.clear();

                            if (currentCallback != null) {
                                Platform.runLater(new UpdateUIResponseReceivedRunnable(currentCallback, jsonObject));
                                onResponseReceivedCallbacks.remove(currentCallback);
                            } else {
                                Platform.runLater(new UpdateUIRunnable(jsonObject));
                            }
                        }
                    } else {
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    disconnect();
                }
            });
        }
    }

    private class UpdateUIRunnable implements Runnable {

        JSONObject jsonObject;

        UpdateUIRunnable(JSONObject jsonObject) {
            this.jsonObject = jsonObject;
        }

        @Override
        public void run() {
            if (onServerDataReceivedListener != null) {
                onServerDataReceivedListener.onServerDataReceived(jsonObject);
            }
        }
    }

    private class UpdateUIResponseReceivedRunnable implements Runnable{

        OnResponseReceivedCallback onResponseReceivedCallback;
        JSONObject jsonObject;

        public UpdateUIResponseReceivedRunnable(OnResponseReceivedCallback onResponseReceivedCallback, JSONObject jsonObject){
            this.onResponseReceivedCallback = onResponseReceivedCallback;
            this.jsonObject = jsonObject;
        }

        @Override
        public void run() {
            onResponseReceivedCallback.onResponseReceived(jsonObject);
        }
    }

    public interface OnServerConnectedListener {
        void onServerConnectionSucceeded();

        void onServerConnectionFailed();
    }

    public interface OnServerDataReceivedListener {
        void onServerDataReceived(JSONObject jsonObject);
    }

    public interface OnServerDisconnectedListener {
        void onServerDisconnected();
    }

    public abstract static class OnResponseReceivedCallback {
        private int requestId;
        private long initMillis = 0;
        private long timeout = defaultCallbackTimeout;

        public abstract void onResponseReceived(JSONObject jsonObject);

//        abstract void onResponseTimedOut();

        private void setRequestId(int reqId) {
            requestId = reqId;
            initMillis = System.currentTimeMillis();
        }

        public int getRequestId() {
            return requestId;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }

        public long getTimeout() {
            return timeout;
        }

        public long getInitMillis() {
            return initMillis;
        }
    }
}
