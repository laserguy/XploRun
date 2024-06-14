package com.xplorun;

import static com.xplorun.Utils.preProcessRoute;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.xplorun.R;
import com.xplorun.model.DB;
import com.google.android.material.button.MaterialButton;

import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MapViewFragmentOSM extends Fragment {

    private static final String TAG = "log_MapViewFragment";

    private int position;
    private String routesType;
    private View rootView;
    private MapView map = null;
    private MapTileProviderBasic tileProvider;

    @Inject
    OSRMRoadManagerCustom roadManager;
    @Inject
    ExecutorService exe;
    @Inject
    Context appContext;
    @Inject
    DB db;
    private Polyline roadPolyline;
    private GpsMyLocationProvider myLocationProvider;
    private Road road;

    public MapViewFragmentOSM(int position, String routesType) {
        this.position = position;
        this.routesType = routesType;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.layout_route_suggestion, container, false);
        Context ctx = getContext().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue("Explorun/1.0");

        // Map view
        map = (MapView) rootView.findViewById(R.id.mapView);
        tileProvider = new MapTileProviderBasic(getContext(), TileSourceFactory.MAPNIK);
        map.setTileProvider(tileProvider);
        map.setMultiTouchControls(true);
        myLocationProvider = new GpsMyLocationProvider(getActivity());

        // Thread policy
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        MaterialButton buttonStartRun = rootView.findViewById(R.id.button_start_run);
        buttonStartRun.setOnClickListener(view -> startNavigateActivity());
        ImageButton button = rootView.findViewById(R.id.share_button);
        try {
            var routeJson = getRouteJson();
            onMapReady(routeJson);
            button.setOnClickListener(view -> shareRoute(routeJson));
        } catch (RuntimeException e) {
            Log.e(TAG, "No route found", e);
            Toast.makeText(getContext(), "Could not fetch routes.", Toast.LENGTH_LONG).show();
            return null; //to not to show empty map
        }
        return rootView;
    }

    private void shareRoute(JSONObject routeJson) {
        var data = Utils.compress(routeJson.toString());
        var userId = UserSingleton.getInstance().getUser_id();
        VolleySingleton.getInstance(getContext()).postShare(userId, data, new VolleyListener() {
            @Override
            public void onError(String message) {}

            @Override
            public void onResponse(JSONObject response) {
                var url = response.optString("url");
                var sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, url);
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
            }
        });
    }

    public void onMapReady(JSONObject routeJson) {
        // UI elements
        TextView textLength, textUniqueness, textPedFriend, textNature, textElevation;
        textLength = rootView.findViewById(R.id.text_length);
        textUniqueness = rootView.findViewById(R.id.text_uniqueness);
        textPedFriend = rootView.findViewById(R.id.text_ped_friend);
        textNature = rootView.findViewById(R.id.text_nature);
        textElevation = rootView.findViewById(R.id.text_elevation);

        // Switch to handle main activity viewpager, custom route, feedback route fragments
        setRouteFeatures(textLength, textUniqueness, textPedFriend, textNature, textElevation, routeJson);

        // Route to road
        supplyAsync(() -> getRoad(routeJson), exe)
                .thenAccept(this::updateMap)
                .exceptionally(throwable -> {
                    Log.d(TAG, throwable.getMessage());
                    throwable.printStackTrace();
                    return null;
                });
    }

    private JSONObject getRouteJson() {
        JSONObject routeJson;
        switch (routesType) {
            case "main":
                routeJson = RoutesSingleton.getInstance().getRouteAtIndex(position);
                break;
            case "custom":
                routeJson = RoutesSingleton.getInstance().getCustomRouteAtIndex(position);
                break;
            case "feedback":
                routeJson = RoutesSingleton.getInstance().getActiveRoute();
                MaterialButton startRunButton = rootView.findViewById(R.id.button_start_run);
                startRunButton.setVisibility(View.GONE);
                break;
            default:
                throw new IllegalStateException("Unexpected value");
        }
        return routeJson;
    }

    private void setRouteFeatures(TextView textLength, TextView textUniqueness, TextView textPedFriend, TextView textNature, TextView textElevation, JSONObject routeJson) {
        // Route features
        double uniqueness, ped_friend, nature;
        uniqueness = ped_friend = nature = 0;
        int length, elevation;
        length = elevation = 0;
        try {
            uniqueness = routeJson.getDouble("uniqueness");
            length = routeJson.getInt("length");
            elevation = routeJson.getInt("elevation");
            ped_friend = routeJson.getDouble("ped_friend");
            nature = routeJson.getDouble("nature");
        } catch (JSONException e) {
            Log.d(TAG, e.getMessage());
            e.printStackTrace();
        }

        // Display metadata
        String toDisplay;
        DecimalFormat df = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));

        toDisplay = String.format("Length: %dm ", length);
        if (length < (1/3d * 20000)) toDisplay += "\uD83D\uDD50";
        else if (length < (2/3d * 20000)) toDisplay += "\uD83D\uDD51";
        else toDisplay += "\uD83D\uDD52";
        textLength.setText(toDisplay);

        toDisplay = "Nature: " + df.format(nature) + " ";
        if (nature < 1/3d) toDisplay += "\uD83C\uDFE0";
        else if (nature < 2/3d) toDisplay += "\uD83C\uDFE1";
        else toDisplay += "\uD83C\uDF32";
        textNature.setText(toDisplay);

        toDisplay = "Pedestrian friendliness: " + df.format(ped_friend) + " ";
        if (ped_friend < 1/3d) toDisplay += "\uD83D\uDE41";
        else if (ped_friend < 2/3d) toDisplay += "\uD83D\uDE10";
        else toDisplay += "\uD83D\uDE42";
        textPedFriend.setText(toDisplay);

        toDisplay = "Uniqueness: " + df.format(uniqueness) + " ";
        if (uniqueness < 1/3d) toDisplay += "\uD83D\uDC9B";
        else if (uniqueness < 2/3d) toDisplay += "\uD83E\uDDE1";
        else toDisplay += "\u2764\uFE0F";
        textUniqueness.setText(toDisplay);

        toDisplay = String.format("Elevation: %dm ", elevation);
        if (elevation < (1/3d * 1000)) toDisplay += "\u27A1\uFE0F";
        else if (elevation < (2/3d) * 1000) toDisplay += "\u2197\uFE0F";
        else toDisplay += "\u2B06\uFE0F";
        textElevation.setText(toDisplay);
    }

    private void updateMap(Road road) {
        getActivity().runOnUiThread(() -> {
            // Road overlay
            roadPolyline = RoadManager.buildRoadOverlay(road, getContext().getColor(R.color.map_stroke_preview), 10.0f);
            map.getOverlays().add(roadPolyline);

            // Zoom and center map
            IMapController mapController = map.getController();
            map.zoomToBoundingBox(road.mBoundingBox, true, 64);
            map.getController().setCenter(road.mBoundingBox.getCenterWithDateLine());

            if (!routesType.equals("feedback")) {
                // Add marker with current location to map
                var overlay = new MyLocationNewOverlay(myLocationProvider, map);
                overlay.enableMyLocation();
                overlay.setDrawAccuracyEnabled(true);
                map.getOverlays().add(overlay);
            }
            map.invalidate();
        });
    }

    private Road getRoad(JSONObject routeJson) {
        try {
            road = roadManager.getRoad(Utils.getWaypoints(routeJson));
        } catch (JSONException e) {
            e.printStackTrace();
            throw new IllegalStateException("Json Parse");
        }
        return road;
    }

    public void startNavigateActivity() {
        final var activeRoute = getActiveRoute();
        try {
            if(!Utils.isLocationEnabled()){
                Utils.checkLocationEnabledOrQuit(getActivity(), true);
                return;
            }
            var myLocation = myLocationProvider.getLastKnownLocation();
            myLocation = myLocation == null ? Utils.getCurrentLocation(getContext()) : myLocation;
            if (myLocation == null) {
                Toast.makeText(getContext(), "Could not get current location", Toast.LENGTH_LONG).show();
                return;
            }
            final var waypoints = Utils.getWaypoints(activeRoute);
            final var start = new GeoPoint(myLocation);
            supplyAsync(() -> preProcessRoute(waypoints, start))
                    .thenApplyAsync(roadManager::getRoad)
                    .thenAccept(road -> {
                        var route = db.create(road);
                        route.json = activeRoute;
                        db.activeRoute(route);
                        db.user().lastDist = Double.MAX_VALUE;
                        Intent intent = new Intent(appContext, NavigateActivity.class);
                        startActivity(intent);
                    }).exceptionally(throwable -> {
                        Log.d(TAG, throwable.getMessage());
                        throwable.printStackTrace();
                        return null;
                    });
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG,"startNavigateActivity", e);
        }
    }

    @Nullable
    private JSONObject getActiveRoute() {
        JSONObject activeRoute = null;
        switch (routesType) {
            case "main":
                activeRoute = RoutesSingleton.getInstance().getRouteAtIndex(position);
                break;
            case "custom":
                activeRoute = RoutesSingleton.getInstance().getCustomRouteAtIndex(position);
                break;
        }
        RoutesSingleton.getInstance().setActiveRoute(activeRoute);
        return activeRoute;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        map.onDetach();
    }

    @Override
    public void onResume() {
        map.onResume();
        try{
            if (road != null)
                map.zoomToBoundingBox(road.mBoundingBox, true, 64);
        } catch (Exception e) {
            Log.e(TAG, "onResume", e);
        }
        super.onResume();
    }

    @Override
    public String toString() {
        return "MapViewFragmentOSM{" +
                "position=" + position +
                '}';
    }
}
