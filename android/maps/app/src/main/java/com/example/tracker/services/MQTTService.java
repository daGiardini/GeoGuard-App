package com.example.tracker.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.tracker.MapsActivity;
import com.example.tracker.R;
import com.example.tracker.interafaces.IMQTTMessage;
import com.example.tracker.interafaces.INewRadius;
import com.example.tracker.misc.Constants;
import com.example.tracker.mqtt.MQTTEntity;
import com.example.tracker.mqtt.MQTTSubscriber;
import com.example.tracker.receiver.LocationEventReceiver;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MQTTService extends Service implements IMQTTMessage, INewRadius {
    private final String TAG = "MQTTForegroundService";
    private MQTTSubscriber mqttSubscriber;
    private LocationEventReceiver locationEventReceiver = null;
    private static String deviceId;
    private static boolean isRunning = false, guardFlag = false, notificationFlag = false;
    private static double lastLat, lastLng, guardLat, guardLng, guardRadius;
    private static String lastUpdate = "";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        // Initialize and configure MQTTSubscriber
        MQTTEntity mqttEntity = new MQTTEntity(Constants.BROKER, Constants.PORT, Constants.USER, Constants.PASSWORD);
        try {
            mqttSubscriber = new MQTTSubscriber(mqttEntity, this);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        // Try to start MQTT connection
        try {
            startService();
            isRunning = true;

            deviceId = intent.getStringExtra("deviceId");
            guardRadius = Double.parseDouble(intent.getStringExtra("radius"));
            guardFlag = intent.getBooleanExtra("guardFlag", false);

            mqttSubscriber.connectAndSubscribe(deviceId);

            Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        } catch (MqttException e) {
            Toast.makeText(this, "Error while connecting", Toast.LENGTH_SHORT).show();
        }

        IntentFilter filter = new IntentFilter(getString(R.string.ACTION_NEW_GUARD));
        filter.addAction(getString(R.string.ACTION_DELETE_GUARD));
        locationEventReceiver = new LocationEventReceiver(this);
        registerReceiver(locationEventReceiver, filter);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        // Disconnect MQTT connection
        try {
            mqttSubscriber.disconnect();
            deleteGuard();
            isRunning = false;
        } catch (MqttException e) {
            Toast.makeText(this, "Error while disconnecting", Toast.LENGTH_SHORT).show();
        }
        unregisterReceiver(locationEventReceiver);
    }

    private void startService() {
        // Display the notification
        startForeground(Constants.SERVICE_NOTIFICATION_CODE, addNotification("Waiting for a valid location..."));
    }

    private Notification addNotification(String text) {
        // Create the notification channel (required for Android 8.0 and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(Constants.MQTT_CHANNEL_ID, Constants.MQTT_NOTIFICATION, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Channel for service notifications");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        // Create the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.MQTT_CHANNEL_ID)
                .setContentTitle("Tracking in Progress")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setSilent(true);

        return builder.build();
    }

    private void sendNotification(String title, String text) {
        // Create the notification channel (required for Android 8.0 and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(Constants.NOTIFICATION_CHANNEL_ID, Constants.GUARD_NOTIFICATION, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Channel for guard notifications");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        Intent notifyIntent = new Intent(this, MapsActivity.class);
        notifyIntent.putExtra("deviceId", deviceId);
        notifyIntent.putExtra("radius", String.valueOf(guardRadius));

        PendingIntent notifyPendingIntent = PendingIntent.getActivity(
                this, 0, notifyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(notifyPendingIntent)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(Constants.GUARD_NOTIFICATION_CODE, builder.build());
        }
    }

    private void updateNotification(double latitude, double longitude) {
        Geocoder geocoder;
        List<Address> address;
        geocoder = new Geocoder(this, Locale.getDefault());

        try {
            address = geocoder.getFromLocation(latitude, longitude, 1);
            Notification notification = addNotification(address.get(0).getAddressLine(0));
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(Constants.SERVICE_NOTIFICATION_CODE, notification);
        } catch (IOException e) {
            Log.i(TAG, "Error while getting address");
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onMessagePublished(String topic, String message) {

    }

    @Override
    public void onMessageReceived(String topic, String message) throws JSONException {
        Log.i(TAG, "onMessageReceived");

        JSONObject jObject = new JSONObject(message);
        lastLat = Double.parseDouble(jObject.getString("lat"));
        lastLng = Double.parseDouble(jObject.getString("lng"));
        lastUpdate = jObject.getString("last_update");

        Intent intent = new Intent(getString(R.string.ACTION_MQTT_MESSAGE_RECEIVED));
        intent.putExtra("latitude", lastLat);
        intent.putExtra("longitude", lastLng);
        intent.putExtra("last_update", lastUpdate);
        sendBroadcast(intent);

        updateNotification(lastLat, lastLng);

        checkGuard(lastLat, lastLng);
    }

    @Override
    public void updateGuard(double lat, double lng, double radius) {
        Log.i(TAG, "updateGuard");
        guardLat = lat;
        guardLng = lng;
        guardRadius = radius;
        guardFlag = true;
        notificationFlag = true;
    }

    @Override
    public void deleteGuard() {
        Log.i(TAG, "deleteGuard");
        notificationFlag = false;
        guardFlag = false;
    }

    void checkGuard(double latitude, double longitude) {
        if (guardFlag) {
            float[] distance = new float[1];
            Location.distanceBetween(guardLat, guardLng, latitude, longitude, distance);
            Log.i(TAG, guardLat + " " + guardLng + " " + distance[0]);

            if (notificationFlag && distance[0] > guardRadius) {
                Log.i(TAG, "checkGuard - Outside");
                sendNotification("Outside Guard", "Your tracker is outside the virtual guard!");
                notificationFlag = false;
            } else if (distance[0] <= guardRadius && !notificationFlag) {
                Log.i(TAG, "checkGuard - Returned inside guard");
                sendNotification("Inside Guard", "Your tracker has returned inside the virtual guard!");
                notificationFlag = true;
            }
        }
    }

    public static boolean getServiceStatus() {
        return isRunning;
    }

    public static double[] getCords() {
        return new double[]{lastLat, lastLng, guardLat, guardLng, guardRadius};
    }

    public static String getDeviceId() {
        return deviceId;
    }

    public static String getLastUpdate() {
        return lastUpdate;
    }
}
