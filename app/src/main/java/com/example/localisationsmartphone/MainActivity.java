package com.example.localisationsmartphone;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private TextView tvLocationInfo;
    private LocationManager locationManager;
    private RequestQueue requestQueue;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private static final String SERVER_URL = "http://10.0.2.2/localisation/createPosition.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLocationInfo = findViewById(R.id.tv_location_info);
        requestQueue = Volley.newRequestQueue(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        checkPermissions();
    }

    private void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE},
                    PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) allGranted = false;
            }
            if (allGranted) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Permissions refusées.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startLocationUpdates() {
        try {
            LocationListener locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    Log.d(TAG, "Nouvelle position reçue : " + location.getLatitude() + ", " + location.getLongitude());
                    displayLocation(location);
                    addPosition(location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getAccuracy());
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}
                @Override
                public void onProviderEnabled(@NonNull String provider) {
                    Log.d(TAG, "Provider activé : " + provider);
                }
                @Override
                public void onProviderDisabled(@NonNull String provider) {
                    Log.d(TAG, "Provider désactivé : " + provider);
                    Toast.makeText(MainActivity.this, "Veuillez activer le GPS", Toast.LENGTH_SHORT).show();
                }
            };

            // On écoute le GPS
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 150, locationListener);
                Log.d(TAG, "Écoute du GPS_PROVIDER démarrée");
            }
            
            // On écoute aussi le réseau pour plus de réactivité (en intérieur notamment)
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 150, locationListener);
                Log.d(TAG, "Écoute du NETWORK_PROVIDER démarrée");
            }

        } catch (SecurityException e) {
            Log.e(TAG, "Erreur de permission : " + e.getMessage());
        }
    }

    private void displayLocation(Location location) {
        String info = "Latitude : " + location.getLatitude() + "\n" +
                "Longitude : " + location.getLongitude() + "\n" +
                "Altitude : " + location.getAltitude() + "\n" +
                "Précision : " + location.getAccuracy() + "m\n" +
                "Source : " + location.getProvider();
        tvLocationInfo.setText(info);
    }

    private void addPosition(final double lat, final double lon, final double alt, final float accuracy) {
        final String datePosition = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        final String imei = getIMEI();

        Log.d(TAG, "Tentative d'envoi vers " + SERVER_URL);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, SERVER_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "Réponse serveur : " + response);
                        Toast.makeText(MainActivity.this, "Envoyé : " + response, Toast.LENGTH_SHORT).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errMsg = error.getMessage() != null ? error.getMessage() : "Erreur réseau inconnue";
                        Log.e(TAG, "Erreur Volley : " + errMsg);
                        Toast.makeText(MainActivity.this, "Échec envoi : " + errMsg, Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("latitude", String.valueOf(lat));
                params.put("longitude", String.valueOf(lon));
                params.put("date_position", datePosition);
                params.put("imei", imei);
                return params;
            }
        };

        requestQueue.add(stringRequest);
    }

    private String getIMEI() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Sur Android 10+, l'accès à l'IMEI est restreint aux apps système ou avec privilèges
                    return "unknown_android_10_plus";
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    return telephonyManager.getImei();
                } else {
                    return telephonyManager.getDeviceId();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur IMEI : " + e.getMessage());
        }
        return "unknown";
    }
}
