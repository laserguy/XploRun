package com.xplorun;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.location.LocationManagerCompat;

import com.xplorun.R;
import com.xplorun.model.Route;
import com.xplorun.model.RunStat;
import com.xplorun.model.Step;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.util.GeoPoint;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class Utils {

    public static final String TAG = "log_Utils";

    public static ArrayList<GeoPoint> getWaypoints(JSONObject routeJson) throws JSONException {
        ArrayList<GeoPoint> waypoints = new ArrayList<>();
        JSONArray points = routeJson.getJSONObject("geometry").getJSONArray("coordinates").getJSONArray(0);
        for (int i = 0; i < points.length(); i++) {
            // Log.d(TAG, points.getJSONArray(i).getString(0));
            double lat = points.getJSONArray(i).getDouble(1);
            double lon = points.getJSONArray(i).getDouble(0);
            GeoPoint geoPoint = new GeoPoint(lat, lon);
            waypoints.add(geoPoint);
        }
        return waypoints;
    }

    public static ArrayList<Pair<GeoPoint, String>> getWaypointsEnv(JSONObject routeJson) throws JSONException {
        var waypoints = new ArrayList<Pair<GeoPoint, String>>();
        JSONArray points = routeJson.getJSONObject("geometry").getJSONArray("coordinates").getJSONArray(0);
        for (int i = 0; i < points.length(); i++) {
            var lon = points.getJSONArray(i).getDouble(0);
            var lat = points.getJSONArray(i).getDouble(1);
            var env = points.getJSONArray(i).getString(2);
            var geoPoint = new GeoPoint(lat, lon);
            var pair = Pair.create(geoPoint, env);
            waypoints.add(pair);
        }
        return waypoints;
    }

    public static ArrayList<GeoPoint> preProcessRoute(ArrayList<GeoPoint> waypoints, GeoPoint loc) {
        if (loc == null) return waypoints;
//       rotate the route so that the first point is the closest to the current location
        var point = new GeoPoint(loc);
        waypoints.remove(waypoints.size() - 1);
        var minDist = Double.MAX_VALUE;
        var minIndex = 0;

        for (int i = 0; i < waypoints.size(); i++) {
            var dist = waypoints.get(i).distanceToAsDouble(point);
            if (dist < minDist) {
                minDist = dist;
                minIndex = i;
            }
        }
        var newWaypoints = new ArrayList<GeoPoint>();
        for (int i = minIndex; i < waypoints.size(); i++) newWaypoints.add(waypoints.get(i));
        for (int i = 0; i < minIndex; i++) newWaypoints.add(waypoints.get(i));
        newWaypoints.add(newWaypoints.get(0));
        return newWaypoints;
    }

    public static boolean noLocationPermission(Context context) {
        return ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED;
    }

    // Utils for NavigateActivity
    public static Pair<GeoPoint, Integer> getClosest(List<GeoPoint> waypoints, GeoPoint location) {
        GeoPoint result = null;
        int index = -1;
        double minDist = location.distanceToAsDouble(waypoints.get(0));
        for (int i = 1; i < waypoints.size(); i++) {
            var waypoint = waypoints.get(i);
            double dist = location.distanceToAsDouble(waypoint);
            if (dist < minDist && dist < 30) {
                result = waypoint;
                index = i;
                minDist = dist;
            }
        }
        return index == -1 ? null : Pair.create(result, index);
    }

    public static String getEnvironment(List<Pair<GeoPoint, String>> points, Location location) {
        String environment = null;
        var start = System.nanoTime();
        var point = new GeoPoint(location);
        for (int i = 0; i < points.size(); i++) {
            if (points.get(i).first.distanceToAsDouble(point) < 40) {
                environment = points.get(i).second;
                break;
            }
        }
        var end = System.nanoTime();
//        Log.d(TAG, "getEnvironment: " + (end - start) / 1000000.0);
        return environment;
    }

    public static String inst2template(RoadNode mNode) {
        String instr = mNode.mInstructions.trim();
        if (!instr.isEmpty()) {
            instr = instr.substring(0, 1).toLowerCase(Locale.ROOT) + instr.substring(1);
        }
        String pre = "In ";
        if (instr.contains("continue")) {
            pre = "For ";
        }
        return pre + "{dist}" + " meter(s), " + instr;
    }

    public static String inst2text(RoadNode mNode) {
        if (mNode.mInstructions.contains("Waypoint")) {
            return "";
        }
        return mNode.mInstructions;
    }

    public static Step getClosest(@NonNull Location location, Route route) {
        var start = System.nanoTime();
        Step step = null;
        if (route == null) {
            Log.d(TAG, "getClosest: route is null");
            return null;
        }
        var current = new GeoPoint(location);
        for (int i = 0, size = route.steps.size(); i < size; i++) {
            Step tmp = route.steps.get(i);
            var isClose = tmp.toGeoPoint().distanceToAsDouble(current) <= 16+location.getSpeed();
            var highOrder = tmp.order > route.currentOrder;
            var noSkipping = tmp.order - route.currentOrder < 6;
            if (isClose && highOrder && noSkipping) {
                step = tmp;
                route.currentOrder = step.order;
                break;
            }
        }
        var end = System.nanoTime();
        //Log.d(TAG, "getClosest: " + (end - start) / 1000000.0);
        return step;
    }

    public static Location getCurrentLocation(Context context) {
        AtomicReference<Location> location = new AtomicReference<>();
        try {
            var locationManager = context.getSystemService(LocationManager.class);
            var locationClient = LocationServices.getFusedLocationProviderClient(context);
            locationClient.getLastLocation().addOnSuccessListener(location::set);
            if (location.get() == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                location.set(locationManager.getLastKnownLocation(LocationManager.FUSED_PROVIDER));
            if (location.get() == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                locationManager.getCurrentLocation(LocationManager.FUSED_PROVIDER, null, context.getMainExecutor(), location::set);
        } catch (SecurityException e) {
            Log.e(TAG, "getCurrentLocation: ", e);
        }
        return location.get();
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    public static Location getCurrentLocationAsync(Context context, ExecutorService executor) {
        if (noLocationPermission(context)) {
            Log.w(TAG, "getCurrentLocationAsync: No location permission");
            return null;
        }
        try {
            var future = new CompletableFuture<Location>();
            var locationManager = context.getSystemService(LocationManager.class);
            if (locationManager != null) {
                locationManager.getCurrentLocation(LocationManager.FUSED_PROVIDER, new CancellationSignal(), executor, future::complete);
            }
            return future.get();
        } catch (SecurityException | ExecutionException | InterruptedException e) {
            Log.d(TAG, e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public static int calculateCalories() {
        return 0;
    }

    public static String to2Dstring(float n) {return String.format("%.2f", n);}
    public static String to2Dstring(double n) {return String.format("%.2f", n);}
    public static String to2Dstring(String n) {
        float x = Float.parseFloat(n);
        return String.format("%.2f", x);
    }

    public static double normalize(double value, double min, double max) {
        return ((value - min) / (max - min));
    }

    public static class ElevationEntry implements Serializable {
        public float altitude;
        public String environment;

        public ElevationEntry(float altitude, String environment) {
            this.altitude = altitude;
            this.environment = environment;
        }
    }

    public static double getRouteDistance(Route route) {
        double distance = 0d;
        for (int i=0; i<route.steps.size(); i++) {
            distance+=route.steps.get(i).length;
        }
        return distance;
    }

    public static String stat2text(RunStat stat) {
        if (stat==null) {
            Log.e(TAG, "stat is null");
            return "";
        }
        String msg = "Distance: %d m\nDuration: %d s\nAverage Speed: %.2f m/s";
        int dist = (int) Math.round(stat.distance);
        int duration = stat.duration(Instant.now().getEpochSecond());
        return String.format(msg, dist, duration, stat.averageSpeed());
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    public static void requestAllTimeLocationPermission(Context context) {
        if (context.checkSelfPermission(ACCESS_FINE_LOCATION) != PERMISSION_GRANTED
                || context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PERMISSION_GRANTED) {
            var label = context.getPackageManager().getBackgroundPermissionOptionLabel();
            var builder = new AlertDialog.Builder(context);
            String message = String.format("This app needs access to your precise location in order to provide you with real-time navigation. " +
                    "\n\nYou need to grant '%s' location permission and enable precise location.", label);
            builder.setTitle("Precise Location Permission.")
                   .setMessage(message)
                   .setIcon(R.drawable.ic_baseline_my_location_24)
                   .setPositiveButton("OK", (dialog, which) -> {
                       var intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                       var uri = Uri.fromParts("package", context.getPackageName(), null);
                       intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                       intent.setData(uri);
                       context.startActivity(intent);
                   }).setNegativeButton("Cancel", (dialog, which) -> {
                       System.exit(0);
                   })
                   .create().show();
        }
    }

    public static void checkLocationEnabledOrQuit(Activity context, boolean cancelable) {
        if (isLocationEnabled()) return;
        new AlertDialog.Builder(context)
            .setTitle("Location service is disabled!")
            .setMessage("Please enable location services to generate routes around you.")
            .setIcon(R.drawable.ic_menu_mylocation)
            .setCancelable(cancelable)
            .setPositiveButton("OK", (dialog, which) -> {
                if (isLocationEnabled()) return;
                checkLocationEnabledOrQuit(context, cancelable);
            })
            .create().show();
    }

    public static boolean isLocationEnabled() {
        var lm = App.getInstance().getSystemService(LocationManager.class);
        return LocationManagerCompat.isLocationEnabled(lm);
    }

    public static String compress(String text) {
        var data = text.getBytes(StandardCharsets.UTF_8);
        var deflater = new Deflater();
        deflater.setLevel(Deflater.BEST_COMPRESSION);
        deflater.setInput(data);
        var outputStream = new ByteArrayOutputStream(data.length);
        deflater.finish();
        var buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        var output = outputStream.toByteArray();
        return Base64.getEncoder().encodeToString(output);
    }

    public static String decompress(String base64){
        var data = Base64.getDecoder().decode(base64);
        var inflater = new Inflater();
        inflater.setInput(data);
        var outputStream = new ByteArrayOutputStream(data.length);
        var buffer = new byte[1024];
        while (!inflater.finished()) {
            int count = 0;
            try {
                count = inflater.inflate(buffer);
            } catch (DataFormatException e) {
                e.printStackTrace();
            }
            outputStream.write(buffer, 0, count);
        }
        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputStream.toString();
    }

    public static String sha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

}
