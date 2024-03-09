package com.example.tracker;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.tracker.databinding.ActivityMapsBinding;
import com.example.tracker.interafaces.INewLocation;
import com.example.tracker.receiver.LocationEventReceiver;
import com.example.tracker.services.MQTTService;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, INewLocation {
    private final String TAG = "MapsActivity";
    private String deviceId = "";
    private double radius;
    private Button bttFollowLocal = null, bttFollowRemote = null, bttGuard = null, bttService = null;
    private boolean followLocal = false, followRemote = false, isGuardEnable = false;
    private Marker localMarker = null, remoteMarker = null;
    private Circle circle = null;
    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private LocationManager locationManager = null;
    private LocationEventReceiver locationEventReceiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        deviceId = intent.getStringExtra("deviceId");
        radius = Double.parseDouble(intent.getStringExtra("radius"));

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        removeLocationUpdates();
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        getLocationUpdates();
    }

    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        if (!isGuardEnable) {
            stopMqttService();
        }
        removeLocationUpdates();
        unregisterReceiver(locationEventReceiver);
    }

    //Manipulates the map once available.
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.i(TAG, "onMapReady");
        mMap = googleMap;

        // Permission granted, request location updates
        getLocationUpdates();

        //Buttons initialization
        init();

        //Restore data (if service already running)
        restoreData();

        //Register receiver
        IntentFilter filter = new IntentFilter(getString(R.string.ACTION_MQTT_MESSAGE_RECEIVED));
        locationEventReceiver = new LocationEventReceiver(this);
        registerReceiver(locationEventReceiver, filter);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        Log.i(TAG, "New local location: " + location.getLatitude() + " " + location.getLongitude());
        runOnUiThread(() -> changeDeviceCords(location.getLatitude(), location.getLongitude()));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.i(TAG, "Provider: " + provider + ", Status: " + status);
    }

    @Override
    public void onNewLocation(double lat, double lng, String lastUpdate) {
        changeTrackerCords(lat, lng, lastUpdate);
    }

    private void startMqttService() {
        if (!MQTTService.getServiceStatus()) {
            Log.i(TAG, "startMqttService");
            Intent serviceIntent = new Intent(this, MQTTService.class);
            serviceIntent.putExtra("deviceId", deviceId);
            serviceIntent.putExtra("radius", String.valueOf(radius));
            serviceIntent.putExtra("guardFlag", isGuardEnable);
            startService(serviceIntent);
        }
    }

    private void stopMqttService() {
        Log.i(TAG, "stopMqttService");
        if (MQTTService.getServiceStatus()) {
            Intent serviceIntent = new Intent(this, MQTTService.class);
            stopService(serviceIntent);
        }
    }

    //Update remote tracker coordinates on the map
    private void changeTrackerCords(double lat, double lng, String lastUpdate) {
        if (Math.abs(lat) <= 90 && Math.abs(lng) <= 180) {
            Log.i(TAG, "Changing device position at: " + lat + " " + lng);
            if (remoteMarker != null) remoteMarker.remove();
            LatLng point = new LatLng(lat, lng);
            remoteMarker = mMap.addMarker(new MarkerOptions()
                    .position(point)
                    .title("Remote position")
                    .snippet(lastUpdate + " UTC")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            );
            if (followRemote) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 20));
            }
        }
    }

    //Update device coordinates on the map
    private void changeDeviceCords(double lat, double lng) {
        if (Math.abs(lat) <= 90 && Math.abs(lng) <= 180) {
            Log.i(TAG, "Changing main position at: " + lat + " " + lng);
            if (localMarker != null) localMarker.remove();
            LatLng point = new LatLng(lat, lng);
            localMarker = mMap.addMarker(new MarkerOptions()
                    .position(point)
                    .title("My position")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            if (followLocal) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 20));
            }
        }
    }

    private void addGuard(double lat, double lng, double radius) {
        circle = mMap.addCircle(new CircleOptions()
                .center(new LatLng(lat, lng))
                .radius(radius)
                .strokeColor(R.color.teal_700)
                .fillColor(R.color.teal_200)
        );
    }

    private void deleteGuard() {
        if (circle != null) {
            circle.remove();
        }
        isGuardEnable = false;
        Intent intent = new Intent(getString(R.string.ACTION_DELETE_GUARD));
        sendBroadcast(intent);
    }

    private void init() {
        Log.i(TAG, "init");
        bttFollowRemote = findViewById(R.id.bttFollowRemote);
        bttFollowLocal = findViewById(R.id.bttFollowLocal);
        bttGuard = findViewById(R.id.bttGuard);
        bttService = findViewById(R.id.bttService);

        bttFollowRemote.setOnClickListener(v -> {
            if (followRemote ^= true) {
                if (remoteMarker != null) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(remoteMarker.getPosition().latitude, remoteMarker.getPosition().longitude), 20));
                }
                if (followLocal) {
                    followLocal = false;
                    bttFollowLocal.setText("track\nlocal");
                }
                bttFollowRemote.setText("stop");
            } else {
                bttFollowRemote.setText("track\nremote");
            }
        });

        bttFollowLocal.setOnClickListener(v -> {
            if (followLocal ^= true) {
                if (localMarker != null) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(localMarker.getPosition().latitude, localMarker.getPosition().longitude), 20));
                }
                if (followRemote) {
                    followRemote = false;
                    bttFollowRemote.setText("track\nremote");
                }
                bttFollowLocal.setText("stop");
            } else {
                bttFollowLocal.setText("track\nlocal");
            }
        });

        bttGuard.setOnClickListener(v -> {
            if (remoteMarker != null && MQTTService.getServiceStatus()) {
                if ((isGuardEnable ^= true)) {
                    addGuard(remoteMarker.getPosition().latitude, remoteMarker.getPosition().longitude, radius);
                    Intent intent = new Intent(getString(R.string.ACTION_NEW_GUARD));
                    intent.putExtra("lat", remoteMarker.getPosition().latitude);
                    intent.putExtra("lng", remoteMarker.getPosition().longitude);
                    intent.putExtra("radius", radius);
                    sendBroadcast(intent);
                    bttGuard.setText("remove\nguard");
                } else {
                    deleteGuard();
                    bttGuard.setText("set\nguard");
                }
            } else {
                Toast.makeText(this, "Cannot set guard", Toast.LENGTH_SHORT).show();
            }
        });

        bttService.setText(MQTTService.getServiceStatus() ? "stop" : "start");
        bttService.setOnClickListener(v -> {
            if (MQTTService.getServiceStatus()) {
                stopMqttService();
                deleteGuard();
                bttGuard.setText("set\nguard");
                bttService.setText("start");
            } else {
                startMqttService();
                bttService.setText("stop");
            }
        });
    }

    private void restoreData() {
        if (MQTTService.getServiceStatus()) {
            Log.i(TAG, "restoreData");
            double[] coordinates = MQTTService.getCords();
            changeTrackerCords(coordinates[0], coordinates[1], MQTTService.getLastUpdate());
            addGuard(coordinates[2], coordinates[3], coordinates[4]);
            isGuardEnable = MQTTService.getServiceStatus();
            bttGuard.setText("remove\nguard");
            bttFollowRemote.performClick();
        }
    }

    private void getLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (locationManager == null) {
                locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 5, this);
            }
        }
    }

    private void removeLocationUpdates() {
        if (locationManager != null) {
            locationManager.removeUpdates(this);
            locationManager = null;
        }
    }
}