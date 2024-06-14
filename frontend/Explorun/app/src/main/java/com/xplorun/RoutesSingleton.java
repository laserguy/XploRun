package com.xplorun;

import org.json.JSONObject;

import java.util.ArrayList;

// List of json objects - routes
public class RoutesSingleton {

    private static final String TAG = "log_RoutesSingleton";

    private static ArrayList<JSONObject> arrayListMainRoutes = new ArrayList<>();

    private static ArrayList<JSONObject> arrayListCustomRoutes = new ArrayList<>();

    private JSONObject activeRouteJSON;

    private static JSONObject likedFeaturesJson;

    public static RoutesSingleton instance;

    private RoutesSingleton() { }

    public static synchronized RoutesSingleton getInstance() {
        if (instance == null) {
            instance = new RoutesSingleton();
        }
        return instance;
    }

    public void addToMainRoutes(int index, JSONObject routeJson) {
        arrayListMainRoutes.add(index, routeJson);
    }

    public void clearMainRoutes() {
        arrayListMainRoutes.clear();
    }

    public JSONObject getRouteAtIndex(int index) {
        return arrayListMainRoutes.get(index);
    }

    public int getRoutesCount() {
        return arrayListMainRoutes.size();
    }

    public void addToCustomRoutes(int index, JSONObject routeJson) {
        arrayListCustomRoutes.add(index, routeJson);
    }

    public void clearCustomRoutes() {
        arrayListCustomRoutes.clear();
    }

    public JSONObject getCustomRouteAtIndex(int index) {
        return arrayListCustomRoutes.get(index);
    }

    public int getCustomRoutesCount() {
        return arrayListCustomRoutes.size();
    }

    public void setActiveRoute(JSONObject activeRoute) {
        this.activeRouteJSON = activeRoute;
    }

    public JSONObject getActiveRoute() {
        return activeRouteJSON;
    }

    public JSONObject getLikedFeaturesJson() {
        return likedFeaturesJson;
    }

    public void setLikedFeaturesJson(JSONObject likedFeaturesJson) {
        RoutesSingleton.likedFeaturesJson = likedFeaturesJson;
    }

}
