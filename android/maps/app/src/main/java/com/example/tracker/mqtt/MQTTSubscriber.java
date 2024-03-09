package com.example.tracker.mqtt;

import android.util.Log;

import com.example.tracker.interafaces.IMQTTMessage;
import com.example.tracker.misc.Constants;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MQTTSubscriber {
    private final String TAG = "MQTTSubscriber";

    private MQTTEngine mqttEngine = null;
    private IMQTTMessage mqttMessages = null;

    public MQTTSubscriber(MQTTEntity mqttEntity, IMQTTMessage mqttMessageInterface) throws MqttException {
        this.mqttMessages = mqttMessageInterface;
        mqttEngine = new MQTTEngine(mqttEntity);
    }

    public void connectAndSubscribe(String topic) throws MqttException {
        connectToMqttBroker();
        subscribeToMqttTopic(topic);
        setMessageCallback();
    }

    public void disconnect() throws MqttException {
        mqttEngine.disconnectFromMqttBroker();
    }

    private void connectToMqttBroker() throws MqttException {
        mqttEngine.connectToMqttBroker();
    }

    private void subscribeToMqttTopic(String topic) {
        mqttEngine.subscribeToTopic(topic, Constants.QOS);
    }

    private void setMessageCallback() {
        MqttCallback mqttCallback = new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.i(TAG, "Connection to broker " + Constants.BROKER + " lost." + cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                mqttMessages.onMessageReceived(topic, new String(message.getPayload()));
                Log.i(TAG, "Message received: " +
                        "\n\t - Topic: " + topic +
                        "\n\t - Message: " + new String(message.getPayload()) +
                        "\n\t - Qos: " + message.getQos());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // Empty
            }

        };
        mqttEngine.setMessageReceiveCallback(mqttCallback);
    }
}
