package com.example.tracker.interafaces;

import android.os.Bundle;

import org.json.JSONException;

public interface IMQTTMessage {
    void onStatusChanged(String provider, int status, Bundle extras);

    void onMessagePublished(String topic, String message) throws JSONException;

    void onMessageReceived(String topic, String message) throws JSONException;
}
