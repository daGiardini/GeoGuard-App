package com.example.tracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.tracker.misc.Constants;
import com.example.tracker.services.MQTTService;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";
    private Button bttStart = null, bttResume = null;
    private EditText etDeviceId = null, etRadius = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        checkNotificationPermissions();
        checkLocationPermissions();

        bttStart = findViewById(R.id.bttStart);
        bttResume = findViewById(R.id.bttResume);
        etDeviceId = findViewById(R.id.etDeviceId);
        etRadius = findViewById(R.id.etRadius);

        if (MQTTService.getServiceStatus()) {
            bttResume.setVisibility(View.VISIBLE);
        }

        bttResume.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(getString(R.string.LAUNCH_ACTIVITY));
                intent.putExtra("deviceId", MQTTService.getDeviceId());
                intent.putExtra("radius", String.valueOf(MQTTService.getCords()[4]));
                startActivity(intent);
            } catch (Exception e) {
                Log.i(TAG, "Cannot launch activity " + e.getMessage());
                Toast.makeText(this, "An error occurred. Please try reopening the app.", Toast.LENGTH_SHORT).show();
            }
        });

        bttStart.setOnClickListener(v -> {
            String deviceId = etDeviceId.getText().toString().trim().toUpperCase();
            String radius = etRadius.getText().toString().trim();

            if (!deviceId.matches("TX\\d{4}[A-Za-z]{2}")) {
                etDeviceId.setTextColor(ContextCompat.getColor(this, R.color.red));
                Toast.makeText(this, "Check device id value!", Toast.LENGTH_SHORT).show();
            } else if (!radius.matches("\\d+")) {
                etRadius.setTextColor(ContextCompat.getColor(this, R.color.red));
                Toast.makeText(this, "Check radius value!", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(getString(R.string.LAUNCH_ACTIVITY));
                intent.putExtra("deviceId", deviceId);
                intent.putExtra("radius", radius);
                try {
                    if (MQTTService.getServiceStatus()) {
                        Intent serviceIntent = new Intent(getString(R.string.ACTION_DELETE_GUARD));
                        sendBroadcast(serviceIntent);
                        stopMqttService();
                    }
                    startActivity(intent);
                } catch (Exception e) {
                    Log.i(TAG, "Cannot launch activity " + e.getMessage());
                    Toast.makeText(this, "An error occurred. Please try reopening the app.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        etDeviceId.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // This method is called before the text is changed.
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // This method is called when the text is changed.
                etDeviceId.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.black));
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // This method is called after the text is changed.
            }
        });

        etRadius.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // This method is called before the text is changed.
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // This method is called when the text is changed.
                etRadius.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.black));
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // This method is called after the text is changed.
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    private void stopMqttService() {
        Log.i(TAG, "stopMqttService");
        Intent serviceIntent = new Intent(this, MQTTService.class);
        stopService(serviceIntent);
    }

    private void checkNotificationPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            // Location permission already granted
            Log.i(TAG, "Notification permission already granted");
        } else {
            // Location permission not granted
            Log.i(TAG, "Request notification permission from the user");
            // Request the permission from the user
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, Constants.NOTIFICATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Location permission already granted
            Log.i(TAG, "Location permission already granted");
            // You can proceed with location-related operations
        } else {
            // Location permission not granted
            Log.i(TAG, "Request location permission from the user");
            // Request the permission from the user
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, Constants.LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Constants.LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, request location updates
                Log.i(TAG, "Location permission granted");
            } else {
                Log.i(TAG, "Location permission denied by user");
                // Permission denied, handle accordingly (e.g., show an error message)
                Toast.makeText(this, "Permission denied. Please enable location access.", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == Constants.NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                Log.i(TAG, "Notification permission denied by user");
            } else {
                Log.i(TAG, "Location permission denied by user");
                // Permission denied, handle accordingly (e.g., show an error message)
                Toast.makeText(this, "Permission denied. Please enable location access.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
