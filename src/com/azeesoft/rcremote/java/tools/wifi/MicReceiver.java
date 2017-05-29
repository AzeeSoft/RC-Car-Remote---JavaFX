package com.azeesoft.rcremote.java.tools.wifi;

import com.azeesoft.rcremote.java.tools.Stools;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Created by azizt on 5/28/2017.
 */
public class MicReceiver {
    AudioInputStream audioInputStream;
    static AudioInputStream ais;
    static AudioFormat format;
    static boolean isReceiving = true;
    static int port = 50005;
    static int sampleRate = 44100;

    static DataLine.Info dataLineInfo;
    static SourceDataLine sourceDataLine;

    DatagramSocket serverSocket;

    private static MicReceiver thisReceiver;

    public static MicReceiver getMicReceiver(){
        if(thisReceiver==null){
            thisReceiver = new MicReceiver();
        }

        return thisReceiver;
    }

    private MicReceiver(){

    }

    public void startReceiving(){

        if(isReceiving){
            stopReceiving();
        }

        Thread receiveAudioThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new DatagramSocket(port);

                    byte[] receiveData = new byte[3584];

                    format = new AudioFormat(sampleRate, 16, 1, true, false);
                    dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
                    sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
                    sourceDataLine.open(format);
                    sourceDataLine.start();

                    FloatControl volumeControl = (FloatControl) sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
                    volumeControl.setValue(1.00f);

                    DatagramPacket receivePacket = new DatagramPacket(receiveData,
                            receiveData.length);
                    ByteArrayInputStream baiss = new ByteArrayInputStream(
                            receivePacket.getData());
                    while (isReceiving) {
                        serverSocket.receive(receivePacket);
                        ais = new AudioInputStream(baiss, format, receivePacket.getLength());
                        toSpeaker(receivePacket.getData());
                    }
                    sourceDataLine.drain();
                    sourceDataLine.close();
                } catch (IOException | LineUnavailableException e) {
                    e.printStackTrace();
                }
            }
        });

        isReceiving=true;
        receiveAudioThread.start();
    }

    public void stopReceiving(){
        isReceiving=false;
        if(serverSocket!=null){
            try {
                serverSocket.disconnect();
                serverSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void toSpeaker(byte soundbytes[]) {
        try {
            sourceDataLine.write(soundbytes, 0, soundbytes.length);
        } catch (Exception e) {
            System.out.println("Not working in speakers...");
            e.printStackTrace();
        }
    }
}
