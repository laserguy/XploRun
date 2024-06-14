package com.xplorun;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.xplorun.R;

public class GetLocationActivity extends AppCompatActivity {

    private static final String TAG = "log_GetLocationActivity";
    private ActivityResultLauncher<String[]> locationPermissionRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_loading);

        locationPermissionRequest = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            var fineLocationGranted = result.getOrDefault(ACCESS_FINE_LOCATION, false);
            var coarseLocationGranted = result.getOrDefault(ACCESS_COARSE_LOCATION, false);
            var backgroundLocationGranted = true;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
                backgroundLocationGranted = result.getOrDefault(ACCESS_BACKGROUND_LOCATION, false);

            if (fineLocationGranted && backgroundLocationGranted) {
                finishActivity();
            } else if (coarseLocationGranted) {
                Log.e(TAG, "Only Coarse location granted");
                String text = "This app needs access to your precise location in order to provide you with real-time navigation.";
                makeDialogAndQuit(text);
            } else {
                Log.e(TAG, "No location granted");
                String text = "This app needs access to your location to generate routes around you.";
                makeDialogAndQuit(text);
            }
        });

        checkLocationPermissionOrRequest(locationPermissionRequest);
    }
    // Before you perform the actual permission request, check whether your app
    // already has the permissions, and whether your app needs to show a permission
    // rationale dialog. For more details, see Request permissions.
    private void checkLocationPermissionOrRequest(ActivityResultLauncher<String[]> locationPermissionRequest) {
        if (checkSelfPermission(ACCESS_FINE_LOCATION) == PERMISSION_GRANTED
                && checkSelfPermission(ACCESS_BACKGROUND_LOCATION) == PERMISSION_GRANTED) {
            finishActivity();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            Utils.requestAllTimeLocationPermission(this);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            locationPermissionRequest.launch(new String[]{
                    ACCESS_FINE_LOCATION,
                    ACCESS_BACKGROUND_LOCATION,
            });
        else locationPermissionRequest.launch(new String[]{
                    ACCESS_FINE_LOCATION,
                    ACCESS_COARSE_LOCATION
            });
    }

    private void makeDialogAndQuit(String text) {
        new AlertDialog.Builder(this)
                .setTitle("Location permission required")
                .setMessage(text)
                .setIcon(android.R.drawable.ic_menu_mylocation)
                .setPositiveButton("OK", (dialog, which) -> System.exit(0))
                .create().show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkLocationPermissionOrRequest(locationPermissionRequest);
    }

    public void finishActivity() {
        Intent intent = new Intent();
        setResult(Params.LOCATION_FETCHED, intent);
        finish();
    }
}
