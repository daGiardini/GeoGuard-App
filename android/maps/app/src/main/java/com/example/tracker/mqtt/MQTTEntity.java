package com.example.tracker.mqtt;

public class MQTTEntity {
    private final String TAG = "MQTTEntity";
    private String broker;
    private int port;
    private String user;
    private String password;

    public MQTTEntity(String broker, int port, String user, String password) {
        this.broker = broker;
        this.port = port;
        this.user = user;
        this.password = password;
    }

    public String getBroker() {
        return this.broker;
    }

    public int getPort() {
        return this.port;
    }

    public String getUser() {
        return this.user;
    }

    public String getPassword() {
        return this.password;
    }
}
