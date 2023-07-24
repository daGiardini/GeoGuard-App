package com.example.tracker.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.tracker.R;
import com.example.tracker.interafaces.INewLocation;
import com.example.tracker.interafaces.INewRadius;

public class LocationEventReceiver extends BroadcastReceiver {
    private INewLocation locationListener = null;
    private INewRadius radiusListener = null;
    private final String TAG = "LocationReceiver";


    public LocationEventReceiver(INewLocation iNewLocation) {
        locationListener = iNewLocation;
    }

    public LocationEventReceiver(INewRadius iNewRadius) {
        radiusListener = iNewRadius;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(context.getString(R.string.ACTION_MQTT_MESSAGE_RECEIVED))) {
            Log.i(TAG, "onReceive - ACTION_MQTT_MESSAGE_RECEIVED");

            // Retrieve the latitude and longitude from the intent
            if (locationListener != null) {
                locationListener.onNewLocation(
                        intent.getDoubleExtra("latitude", 91),
                        intent.getDoubleExtra("longitude", 181),
                        intent.getStringExtra("last_update")
                );
            }
        } else if (intent.getAction() != null && intent.getAction().equals(context.getString(R.string.ACTION_NEW_GUARD))) {
            Log.i(TAG, "onReceive - ACTION_NEW_GUARD");

            if (radiusListener != null) {
                radiusListener.updateGuard(
                        intent.getDoubleExtra("lat", 91),
                        intent.getDoubleExtra("lng", 181),
                        intent.getDoubleExtra("radius", 5000)
                );
            }
        } else if (intent.getAction() != null && intent.getAction().equals(context.getString(R.string.ACTION_DELETE_GUARD))) {
            Log.i(TAG, "onReceive - ACTION_DELETE_GUARD");

            if (radiusListener != null) {
                radiusListener.deleteGuard();
            }
        }
    }
}