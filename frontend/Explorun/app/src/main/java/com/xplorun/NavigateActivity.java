package com.xplorun;

import static com.xplorun.Utils.getEnvironment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.hardware.GeomagneticField;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.xplorun.R;
import com.xplorun.model.DB;
import com.xplorun.model.Route;
import com.xplorun.model.RunStat;
import com.xplorun.model.Step;
import com.google.android.material.button.MaterialButton;


import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.IOrientationConsumer;
import org.osmdroid.views.overlay.compass.IOrientationProvider;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.milestones.MilestoneManager;
import org.osmdroid.views.overlay.milestones.MilestonePathDisplayer;
import org.osmdroid.views.overlay.milestones.MilestonePixelDistanceLister;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class NavigateActivity extends AppCompatActivity implements LocationListener, IOrientationConsumer, ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "log_NavigateActivity";

    // Map and controller
    private MapView map;
    private IMapController mapController;
    @Inject
    DB db;
    @Inject
    OSRMRoadManagerCustom roadManager;
    @Inject
    BackgroundService service;
    @Inject
    TTSService tts;
    @Inject
    ExecutorService exe;

    // Variables of the route
    Route route;
    Polyline roadPolyline;
    Road road;
    private PowerManager powerManager;
    double elevationMarksDistance;

    // Vars for compass, location and features
    LocationManager locationManager;
    IOrientationProvider compass;
    int deviceOrientation = 0;
    float gpsspeed;
    float gpsbearing;
    float lat = 0;
    float lon = 0;
    float alt = 0;
    long timeOfFix = 0;
    boolean voiceEnabled = true;
    boolean centerMyLocation = true;
    boolean autoRotation = true;

    // UI elements
    TextView textSpeed, textEnvironment, textInstruction;
    ImageView imageManeuver;
    MaterialButton buttonInstructionsVolume, buttonAutoRotation, buttonCenterMyLocation, quitRunButton;
    ToggleButton unPauseButton;
    CompassOverlay compassOverlay;

    // Stats
    List<Utils.ElevationEntry> elevationHist = new ArrayList<>();
    Location lastElevationMark;

    PowerManager.WakeLock wakeLock;
    private String prevEnvironment;
    private Location prevLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        route = db.activeRoute();
        if (route == null) {
            Toast.makeText(this, "No route selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        road = route.road;
        checkRoad(road);
        powerManager = getSystemService(PowerManager.class);
        locationManager = this.getSystemService(LocationManager.class);
        prevLocation = Utils.getCurrentLocation(this);
        lastElevationMark = prevLocation;
        elevationMarksDistance = Utils.getRouteDistance(route) / Params.NUM_OF_ANALYSIS_CHART_SEGMENTS;

        setContentView(R.layout.activity_navigate);

        // UI elements
        textSpeed = findViewById(R.id.text_speed);
        textEnvironment = findViewById(R.id.text_environment);
        textInstruction = findViewById(R.id.text_instruction);
        imageManeuver = findViewById(R.id.image_maneuver);
        imageManeuver.setImageResource(R.drawable.maneuver_1);

        // Map
        map = findViewById(R.id.mapViewNavigate);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.setTilesScaledToDpi(true);

        addRoadOverlay();

        // Map controller
        mapController = map.getController();
        mapController.setZoom(19);
        mapController.setCenter(road.mNodes.get(0).mLocation);

        // Overlays
        addLocationOverlay();
        addCompassOverlay();
        addRotationGestures();
        drawRouteSteps();

        // Buttons
        buttonInstructionsVolume = findViewById(R.id.button_instructions_volume);
        buttonInstructionsVolume.setOnClickListener(view -> switchVoice());
        buttonCenterMyLocation = findViewById(R.id.button_center_my_location);
        buttonCenterMyLocation.setOnClickListener(view -> switchCenterMyLocation());
        buttonAutoRotation = findViewById(R.id.button_auto_rotation);
        buttonAutoRotation.setOnClickListener(view -> switchAutoRotation());
        unPauseButton = findViewById(R.id.un_pause_run);
        unPauseButton.setOnCheckedChangeListener((buttonView, isChecked) -> unPauseRun(isChecked));
        quitRunButton = findViewById(R.id.quit_run);
        quitRunButton.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Quit")
                .setMessage("Are you sure you want to quit?")
                .setCancelable(true)
                .setPositiveButton("Yes", (dialog, which) -> quitRun(route, Utils.getCurrentLocation(getApplicationContext())))
                .setNegativeButton("No", null)
                .show());
        map.invalidate();
        acquireWakeLock();
        startForegroundService(new Intent(this, NotificationService.class));
        exe.submit(()-> tts.speak("Please orient yourself and go to starting location!"));
    }

    private void checkRoad(Road road) {
        if (road.mNodes.isEmpty()) {
            Log.e(TAG, "checkRoad: road.mNodes.isEmpty()");
            Toast.makeText(this, "Missing route steps", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void quitRun(Route route, Location location) {
        handleUncompletedRun(route, location);
    }

    private void unPauseRun(boolean isChecked) {
        if (isChecked) {
            Log.d(TAG, "run paused");
        } else {
            Log.d(TAG, "run continue");
        }
    }

    private void acquireWakeLock() {
        int flags = PowerManager.PARTIAL_WAKE_LOCK | PowerManager.LOCATION_MODE_NO_CHANGE;
        wakeLock = powerManager.newWakeLock(flags, "explorun:WakeLock");
        wakeLock.acquire(60 * 60 * 1000L);
    }

    private void releaseWakeLock() {
        if (wakeLock != null) wakeLock.release();
    }

    private void addRoadOverlay() {
        roadPolyline = RoadManager.buildRoadOverlay(road);

        // Road overlay polyline with dynamic arrows
        roadPolyline.usePath(true);
        roadPolyline.getOutlinePaint().setStrokeWidth(20.0f);
        roadPolyline.getOutlinePaint().setColor(getColor(R.color.map_stroke_navigate));
        // roadPolyline.setPoints(waypoints);
        roadPolyline.getOutlinePaint().setStrokeCap(Paint.Cap.ROUND);
        final List<MilestoneManager> managers = createMilestoneManager();
        roadPolyline.setMilestoneManagers(managers);
        map.getOverlayManager().add(roadPolyline);
    }

    private void addLocationOverlay() {
        checkLocationPermission();
        requestLocationUpdate();
        MyLocationNewOverlay mLocationOverlay = new MyLocationNewOverlay(
                new GpsMyLocationProvider(getApplicationContext()), map);
        mLocationOverlay.enableMyLocation();
        map.getOverlays().add(mLocationOverlay);
    }

    private void addCompassOverlay() {
        compass = new InternalCompassOrientationProvider(this);
        compass.startOrientationProvider(this);
        compassOverlay = new CompassOverlay(getApplicationContext(),
                new InternalCompassOrientationProvider(this), map);
    }

    private void drawRouteSteps() {
        Drawable nodeIcon = getResources().getDrawable(R.drawable.marker_node);
        for (int i = 0; i < road.mNodes.size(); i++) {
            RoadNode node = road.mNodes.get(i);
            Marker nodeMarker = new Marker(map);
            nodeMarker.setSubDescription(Road.getLengthDurationText(this, node.mLength, node.mDuration));
            nodeMarker.setPosition(node.mLocation);
            if (i != 0) nodeMarker.setIcon(nodeIcon);
            else nodeMarker.setDefaultIcon();
            nodeMarker.setTitle("Step " + i);
            if (node.mInstructions != null)
                nodeMarker.setSnippet(node.mInstructions);
            map.getOverlays().add(nodeMarker);
        }
    }

    private void addRotationGestures() {
        RotationGestureOverlay mRotationGestureOverlay = new RotationGestureOverlay(getApplicationContext(), map);
        mRotationGestureOverlay.setEnabled(true);
        map.setMultiTouchControls(true);
        map.getOverlays().add(mRotationGestureOverlay);
    }

    @NonNull
    private List<MilestoneManager> createMilestoneManager() {
        final Paint arrowPaint = new Paint();
        arrowPaint.setColor(getColor(R.color.map_stroke_arrows));
        arrowPaint.setStrokeWidth(5.0f);
        arrowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        arrowPaint.setAntiAlias(true);
        final Path arrowPath = new Path(); // a simple arrow towards the right
        arrowPath.moveTo(-10, -10); // width
        arrowPath.lineTo(20, 0); // length of the arrow
        arrowPath.lineTo(-10, 10); // width
        arrowPath.close();
        final List<MilestoneManager> managers = new ArrayList<>();
        managers.add(new MilestoneManager(
                new MilestonePixelDistanceLister(50, 100), // distance between arrows
                new MilestonePathDisplayer(0, true, arrowPath, arrowPaint)));
        return managers;
    }

    private void checkLocationPermission() {
        try {
            // on API15 AVDs,network provider fails. no idea why
            if (Utils.noLocationPermission(this)) {
                Log.d(TAG, "Error getting location!");
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        } catch (Exception ex) {
            // usually permissions or
            // java.lang.IllegalArgumentException: provider doesn't exist: network
            ex.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    private void requestLocationUpdate() {
        if (Utils.noLocationPermission(this)) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            locationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 0, 0, this);
            locationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 0, 0, service);
        } else {
            var criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            locationManager.requestLocationUpdates(0,0, criteria,this, Looper.getMainLooper());
            locationManager.requestLocationUpdates(0,0, criteria,service, Looper.getMainLooper());
        }
    }

    private void stopLocationUpdate() {
        if (Utils.noLocationPermission(this)) {
            return;
        }
        locationManager.removeUpdates(this);
        locationManager.removeUpdates(service);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {

        // Prevent extreme location jumps
        if (prevLocation == null) prevLocation = location;
        if (lastElevationMark == null) lastElevationMark = location;
        if (location.distanceTo(prevLocation) > 1000) return;

        if (location.getLatitude() == 0 && location.getLongitude() == 0) {
            Log.w(TAG, "Location 0: " + location);
            return;
        }

        gpsbearing = location.getBearing();
        gpsspeed = location.getSpeed();
        lat = (float) location.getLatitude();
        lon = (float) location.getLongitude();
        alt = (float) location.getAltitude(); // meters
        timeOfFix = location.getTime();

        if (centerMyLocation) mapController.setCenter(new GeoPoint(lat, lon));

        // use gps bearing instead of the compass
        float t = (360 - gpsbearing - this.deviceOrientation);
        if (t < 0) t += 360;
        if (t > 360) t -= 360;
        if (gpsspeed >= 0.01 && autoRotation) map.setMapOrientation(t);

        var speed = Utils.to2Dstring(gpsspeed) + " m/s";
        textSpeed.setText(speed);

        if (route == null)
            Toast.makeText(getApplicationContext(), "No active route", Toast.LENGTH_SHORT).show();
        try {
            var environment = getEnvironment(route.waypoints(), location);
            if (environment == null && prevEnvironment != null) environment = prevEnvironment;
            textEnvironment.setText(environment);
            if (route.overallStat.getTarget() == null)
                route.overallStat.setTarget(newRunStat(location, environment));

            handleStats(location, route, environment);
            prevEnvironment = environment;
            prevLocation = location;

            // Elevation marks
            if (location.distanceTo(lastElevationMark) > elevationMarksDistance) {
                elevationHist.add(new Utils.ElevationEntry((float) location.getAltitude(), environment));
                lastElevationMark = location;
            }

        } catch (Exception e)  {Log.d(TAG, e.getMessage());}
        var nextStep = route.nextStep();
        if (nextStep != null) {
            var dist = Math.round(nextStep.distTo(location));
            setInstruction(nextStep, dist);
            NotificationService.notify("Run started", R.drawable.osm_ic_follow_me_on);
        } else {
            textInstruction.setText(R.string.reached_destination);
            handleCompleteRun(route, location);
        }

    }

    @Override
    public void onOrientationChanged(float orientation, IOrientationProvider source) {
        // Only use the compass bit if we aren't moving, since gps is more accurate when
        // we are moving
        if (gpsspeed < 0.01) {
            GeomagneticField gf = new GeomagneticField(lat, lon, alt, timeOfFix);
            Float trueNorth = orientation + gf.getDeclination();

            synchronized (trueNorth) {
                if (trueNorth > 360.0f) trueNorth = trueNorth - 360.0f;

                // This part adjusts the desired map rotation based on device orientation and compass heading
                float t = (360 - trueNorth - this.deviceOrientation);
                if (t < 0) t += 360;
                if (t > 360) t -= 360;
                if (autoRotation) map.setMapOrientation(t);
            }
        }
    }

    private void handleCompleteRun(Route route, Location location) {
        Log.i(TAG, "Run complete!");
        closeStat(route.overallStat.getTarget(), location);
        stopLocationUpdate();

        db.user().completedRoutes.add(db.activeRoute());
        //db.activeRoute(null);
        releaseWakeLock();
        stopNotificationService();
        Intent intent = new Intent(NavigateActivity.this, RunCompletedActivity.class);
        intent.putExtra( "elevation_hist", (Serializable) elevationHist);
        intent.putExtra("run_finished", true);
        startActivity(intent);
        finish();
    }

    public void handleUncompletedRun(Route route, Location location) {
        Log.i(TAG, "Run canceled!");
        closeStat(route.overallStat.getTarget(), location);
        stopLocationUpdate();
        releaseWakeLock();
        stopNotificationService();
        if (route.started) {
            var completed = route.currentOrder*1.0/route.steps.size();
            Intent intent = new Intent(NavigateActivity.this, RunCompletedActivity.class);
            intent.putExtra("elevation_hist", (Serializable) elevationHist);
            intent.putExtra("run_finished", completed >= 0.8);
            startActivity(intent);
        }
        finish();
    }

    private void handleStats(@NonNull Location location, Route route, String environment) {
        if (!route.started) return;
        RunStat overall = route.overallStat.getTarget();
        if (Math.round(location.distanceTo(prevLocation)) == 0) return;
        updateStat(overall, location);
//        Log.d(TAG, "handleStats: " + location.distanceTo(prevLocation)+"   Prov: "+location.getProvider());

        if (prevLocation == null || environment == null || prevEnvironment == null) return;
        // Urban / Nature distance
        switch (environment) {
            case "URBAN":
                overall.urbanDistance += location.distanceTo(prevLocation);
                break;
            case "NATURE":
                overall.natureDistance += location.distanceTo(prevLocation);
                break;
            default:
                break;
        }

    }

    private void updateStat(RunStat stat, Location location) {
        if (stat != null) {
            stat.counter += 1;
            stat.distance += location.distanceTo(prevLocation);
            stat.avgSpeed += location.getSpeed();
            stat.topSpeed = Math.max(stat.topSpeed, location.getSpeed());
            if (location.getAltitude() > prevLocation.getAltitude())
                stat.elevation += location.getAltitude() - prevLocation.getAltitude();
            db.update(stat);
        }
    }

    private void closeStat(RunStat stat, Location location) {
        if (stat != null) {
            stat.endTime = Instant.now().getEpochSecond();
            stat.endPoint = location == null ? stat.startPoint : new GeoPoint(location);
            stat.avgSpeed /= stat.counter;
            db.update(stat);
        }
    }

    @NonNull
    private RunStat newRunStat(@NonNull Location location, String environment) {
        var stat = new RunStat();
        stat.counter = 1;
        stat.startTime = Instant.now().getEpochSecond();
        stat.startPoint = new GeoPoint(location);
        stat.environment = environment;
        stat.distance = 0;
        stat.elevation = 0;
        stat.avgSpeed = location.getSpeed();
        stat.topSpeed = location.getSpeed();
        return stat;
    }

    private void setInstruction(Step nextStep, long dist) {
        var text = nextStep.uiInstruction.replace("{dist}", String.valueOf(dist));
        textInstruction.setText(text);
        String imageName = "maneuver_" + nextStep.maneuverType;
        int imageResId = getResources().getIdentifier(imageName, "drawable", getPackageName());
        if (imageResId == 0) {
            imageResId = R.drawable.maneuver_1;
        }
        NotificationService.notify(text, imageResId);
        imageManeuver.setImageResource(imageResId);
    }

    public void switchVoice() {
        if (voiceEnabled) {
            buttonInstructionsVolume.setIconResource(R.drawable.ic_baseline_volume_off_24);
            voiceEnabled = false;
        } else {
            buttonInstructionsVolume.setIconResource(R.drawable.ic_baseline_volume_up_24);
            voiceEnabled = true;
        }

        service.voiceEnabled = voiceEnabled;
    }

    public void switchCenterMyLocation() {
        if (centerMyLocation) {
            buttonCenterMyLocation.setIconResource(R.drawable.ic_baseline_location_searching_24);
            centerMyLocation = false;
        } else {
            buttonCenterMyLocation.setIconResource(R.drawable.ic_baseline_my_location_24);
            centerMyLocation = true;
        }
    }

    public void switchAutoRotation() {
        if (autoRotation) {
            buttonAutoRotation.setIconResource(R.drawable.ic_baseline_screen_rotation_24);
            compassOverlay.enableCompass();
            map.getOverlays().add(compassOverlay);
            map.setMapOrientation(0);
            autoRotation = false;
        } else {
            buttonAutoRotation.setIconResource(R.drawable.ic_baseline_screen_lock_rotation_24);
            map.getOverlays().removeIf(o -> o instanceof CompassOverlay);
            autoRotation = true;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        stopLocationUpdate();
        stopNotificationService();
        releaseWakeLock();
    }

    private void stopNotificationService() {
        stopService(new Intent(this, NotificationService.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
