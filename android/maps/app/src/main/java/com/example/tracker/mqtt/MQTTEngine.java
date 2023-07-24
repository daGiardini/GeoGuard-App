package com.example.tracker.mqtt;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MQTTEngine {
    private final String TAG = "MQTTEngine";
    private MqttClient mqttClient = null;
    private MqttConnectOptions mqttConnectOptions = null;

    public MQTTEngine(MQTTEntity mqttEntity) throws MqttException {
        initMqttOptions(mqttEntity.getUser(), mqttEntity.getPassword());
        initMqttClient(mqttEntity.getBroker(), mqttEntity.getPort());
    }

    public void connectToMqttBroker() throws MqttException {
            mqttClient.connect(mqttConnectOptions);
            Log.i(TAG, "Error while connection.");
    }

    public void disconnectFromMqttBroker() {
        try {
            mqttClient.disconnect();
        } catch (MqttException e) {
            Log.i(TAG, "Error while disconnecting. " + e);
        }
    }

    public void subscribeToTopic(String topic, int qos) {
        try {
            mqttClient.subscribe(topic, qos);
        } catch (MqttException e) {
            Log.i(TAG, "Error while subscribing. " + e);
        }
    }

    public void setMessageReceiveCallback(MqttCallback mqttCallback) {
        mqttClient.setCallback(mqttCallback);
    }

    private void initMqttOptions(String user, String password) {
        mqttConnectOptions = new MqttConnectOptions();
        //mqttConnectOptions.setUserName(user);
        //mqttConnectOptions.setPassword(password.toCharArray());
    }

    private void initMqttClient(String broker, int port) throws MqttException {
        mqttClient = new MqttClient(
                "tcp://" + broker + ":" + port,
                MqttClient.generateClientId(),
                new MemoryPersistence()
        );
    }
}
