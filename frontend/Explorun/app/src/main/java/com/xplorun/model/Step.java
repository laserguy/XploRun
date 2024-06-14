package com.xplorun.model;

import android.location.Location;

import org.osmdroid.util.GeoPoint;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Transient;
import io.objectbox.relation.ToOne;

@Entity
public class Step {
    @Id
    public long id;
    public String ttsInstruction;
    public String uiInstruction;
    public double longitude;
    public double latitude;
    public double altitude;
    public double duration; // seconds
    public double length; // meters
    public int maneuverType;
    public long lastSpoken; // milliseconds
    public int speakCount;
    public int order;

    public ToOne<Step> nextStep;

    @Transient
    private GeoPoint geoPoint;

    public Step() {
    }

    public Step(long id, String instruction, double longitude, double latitude, double altitude, double length, double duration, int maneuverType) {
        this.id = id;
        this.ttsInstruction = instruction;
        this.longitude = longitude;
        this.latitude = latitude;
        this.altitude = altitude;
        this.length = length;
        this.duration = duration;
        this.maneuverType = maneuverType;
    }

    public Step(long id, String instruction, GeoPoint point, double length, double duration, int maneuverType) {
        this(id, instruction, point.getLongitude(), point.getLatitude(), point.getAltitude(), length, duration, maneuverType);
    }

    public Step(String instruction, GeoPoint point, double length, double duration, int maneuverType, int order) {
        this(0, instruction, point, length, duration, maneuverType);
        this.order = order;
    }

    public Step(GeoPoint point, double length, double duration, int maneuverType, int order) {
        this(0, "", point, length, duration, maneuverType);
        this.order = order;
    }

    public Step nextStep() {
        return nextStep.getTarget();
    }

    public double distTo(Location location) {
        return toGeoPoint().distanceToAsDouble(new GeoPoint(location));
    }

    public void lastSpoken(long lastSpoken) {
        this.lastSpoken = lastSpoken;
        this.speakCount++;
    }

    public boolean recentlySpoke(long currentTime) {
        return currentTime - lastSpoken < Math.max(duration * 1000, 10000);
    }

    public GeoPoint toGeoPoint() {
        if (geoPoint == null) {
            geoPoint = new GeoPoint(latitude, longitude, altitude);
        }
        return geoPoint;
    }

    public static Step from(GeoPoint point) {
        Step step = new Step();
        step.altitude = point.getAltitude();
        step.latitude = point.getLatitude();
        step.longitude = point.getLongitude();
        return step;
    }

    public Location toLocation() {
        var loc = new Location("");
        loc.setLatitude(latitude);
        loc.setLongitude(longitude);
        loc.setAltitude(altitude);
        return loc;
    }
}
