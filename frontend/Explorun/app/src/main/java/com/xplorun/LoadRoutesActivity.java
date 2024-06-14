package com.xplorun;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.xplorun.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;

// Activity to display splash screen while the routes are loaded from the API to resources directory
public class LoadRoutesActivity extends AppCompatActivity {

    private static final String TAG = "log_LoadRoutesActivity";

    private FusedLocationProviderClient fusedLocationClient;

    @RequiresApi(api = Build.VERSION_CODES.N)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_routes);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container_load_routes, LoadingTipsFragment.newInstance(), "fragment_tips")
                .commitNow();

        switch (getIntent().getStringExtra("routesType")) {
            case "main":
                if (Params.USE_SAMPLE_ROUTES) loadSampleRoutes();
                else loadMainRoutes();
                break;
            case "custom":
                loadCustomRoutes();
                break;
        }
    }

    @SuppressLint("MissingPermission")
    private void loadMainRoutes() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location == null) {
                    location = Utils.getCurrentLocation(getApplicationContext());
                }
                if (location == null) {
                    Toast.makeText(getApplicationContext(), "Could not get location", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d(TAG, "Location fetched :" + location.toString());
                VolleySingleton.getInstance(getApplicationContext()).getRoutes(location, new VolleyListener() {
                    @Override
                    public void onError(String message) {
                        Log.d(TAG, "Error getting routes: " + message);
                        Toast.makeText(getApplicationContext(), "Failed to fetch routes from the server.",
                                Toast.LENGTH_LONG).show();
                        finish();
                    }

                    @Override
                    public void onResponse(JSONObject response) {
                        JSONObject routeJson;
                        int counter = 0;

                        RoutesSingleton.getInstance().clearMainRoutes();

                        // Filling arraylist in RoutesSingleton with the routes
                        while (true) {
                            try {
                                routeJson = response.getJSONObject(String.valueOf(counter));
                                RoutesSingleton.getInstance().addToMainRoutes(counter, routeJson);
                                counter++;
                                Log.d(TAG, routeJson.toString() + "was loaded");
                            } catch (JSONException e) {
                                // Expected
                                finishActivity(Params.MAIN_ROUTES_LOADED);
                                return;
                            } catch (Exception e) {
                                // Unexpected
                                finishActivity(Params.ERROR_LOADING_ROUTES);
                                return;
                            }
                        }
                    }
                });
            }
        });

    }

    @SuppressLint("MissingPermission")
    public void loadCustomRoutes() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(@NonNull Location location) {
                Log.d(TAG, "Location fetched :" + location.toString());
                VolleySingleton.getInstance(getApplicationContext()).getCustomRoutes(location, RoutesSingleton.getInstance().getLikedFeaturesJson(), new VolleyListener() {
                    @Override
                    public void onError(String message) {
                        Log.d(TAG, message);
                        Toast.makeText(getApplicationContext(), "Failed to fetch routes from the server.",
                                Toast.LENGTH_LONG).show();
                        System.exit(1);
                    }

                    @Override
                    public void onResponse(JSONObject response) {
                        JSONObject routeJson;
                        int counter = 0;

                        RoutesSingleton.getInstance().clearCustomRoutes();

                        // Filling arraylist in RoutesSingleton with the routes
                        while (true) {
                            try {
                                routeJson = response.getJSONObject(String.valueOf(counter));
                                RoutesSingleton.getInstance().addToCustomRoutes(counter, routeJson);
                                counter++;
                            } catch (JSONException e) {
                                // Expected
                                finishActivity(Params.CUSTOM_ROUTES_LOADED);
                                return;
                            } catch (Exception e) {
                                // Unexpected
                                finishActivity(Params.ERROR_LOADING_ROUTES);
                                return;
                            }
                        }
                    }
                });
            }
        });
    }

    public void loadSampleRoutes() {
        JSONObject routeJson = null;
        InputStream is;

        RoutesSingleton.getInstance().clearMainRoutes();

        try {
            for (int i=0; i<=getAssets().list("sampleRoutes").length; i++) {
                is = getAssets().open("sampleRoutes/sample_route"+i+".geojson");
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                String routeJsonString = new String(buffer, "UTF-8");
                routeJson = new JSONObject(routeJsonString);
                RoutesSingleton.getInstance().addToMainRoutes(i, routeJson);
            }
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
        finishActivity(Params.MAIN_ROUTES_LOADED);
    }

    public void finishActivity(int resultCode) {
        getSupportFragmentManager().beginTransaction().remove(getSupportFragmentManager().findFragmentByTag("fragment_tips")).commitNow();
        setResult(resultCode);
        finish();
    }

    @Override
    public void onBackPressed() {
        return;
    }

}
