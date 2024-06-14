package com.xplorun.model;

import android.util.Pair;

import com.xplorun.Utils;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

import io.objectbox.annotation.Convert;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Transient;
import io.objectbox.converter.PropertyConverter;
import io.objectbox.relation.ToMany;
import io.objectbox.relation.ToOne;

@Entity
public class Route {
    @Id
    public long id;
    public boolean started;
    public int currentOrder;
    public ToMany<Step> steps;
    public ToOne<Step> nextStep;
    public ToOne<RunStat> overallStat;

    @Convert(converter = RoadConverter.class, dbType = String.class)
    public Road road;

    @Convert(converter = JSONObjectConverter.class, dbType = String.class)
    public JSONObject json;

    @Transient
    public ArrayList<Pair<GeoPoint, String>> waypoints;

    @Transient
    static Gson gson = new Gson();

    public Route() {
    }

    public List<Pair<GeoPoint, String>> waypoints() {
        if (waypoints == null) {
            try {
                waypoints = Utils.getWaypointsEnv(json);
            } catch (JSONException e) {
                e.printStackTrace();
                waypoints = new ArrayList<>();
            }
        }
        return waypoints;
    }

    public void nextStep(Step nextStep) {
        this.nextStep.setTarget(nextStep);
    }

    public Step nextStep() {
        return nextStep.getTarget();
    }

    static class JSONObjectConverter implements PropertyConverter<JSONObject, String> {

        @Override
        public JSONObject convertToEntityProperty(String databaseValue) {
            return gson.fromJson(databaseValue, JSONObject.class);
        }

        @Override
        public String convertToDatabaseValue(JSONObject entityProperty) {
            return gson.toJson(entityProperty);
        }
    }

    static class RoadConverter implements PropertyConverter<Road, String> {
        @Override
        public Road convertToEntityProperty(String databaseValue) {
            return gson.fromJson(databaseValue, Road.class);
        }

        @Override
        public String convertToDatabaseValue(Road entityProperty) {
            return gson.toJson(entityProperty);
        }
    }
}
