package com.xplorun;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.bonuspack.R;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadLeg;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.bonuspack.utils.BonusPackHelper;
import org.osmdroid.bonuspack.utils.PolylineEncoder;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * get a route between a start and a destination point, going through a list of waypoints.
 * It uses OSRM, a free open source routing service based on OpenSteetMap data. <br>
 * <p>
 * It requests by default the OSRM demo site.
 * Use setService() to request an other (for instance your own) OSRM service. <br>
 *
 * @author M.Kergall
 * @see <a href="https://github.com/DennisOSRM/Project-OSRM/wiki/Server-api">OSRM</a>
 * @see <a href="https://github.com/Project-OSRM/osrm-backend/wiki/New-Server-api">V5 API</a>
 */
public class OSRMRoadManagerCustom extends RoadManager {
    static final String DEFAULT_SERVICE = "https://routing.openstreetmap.de/";
    public static final String MEAN_BY_CAR = "routed-car/route/v1/driving/";
    public static final String MEAN_BY_BIKE = "routed-bike/route/v1/driving/";
    public static final String MEAN_BY_FOOT = "routed-foot/route/v1/driving/";
    public static final String TAG = "OSRMCustom";
    private final Context mContext;
    protected String mServiceUrl;
    protected String mMeanUrl;
    protected String mUserAgent;
    private static final int CACHE_SIZE = 10;
    private static final Map<Integer, Road> CACHE = new LinkedHashMap<>(CACHE_SIZE * 10 / 7, 0.7f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, Road> eldest) {
            return size() > CACHE_SIZE;
        }
    };
    /**
     * mapping from OSRM StepManeuver types to MapQuest maneuver IDs:
     */
    static final HashMap<String, Integer> MANEUVERS;

    static {
        MANEUVERS = new HashMap<>();

        // Turns
        MANEUVERS.put("end of road", 1);            // No maneuver occurs here = continue
        MANEUVERS.put("turn-straight", 1);          // Continue straight.
        MANEUVERS.put("new name", 2);               // No maneuver occurs here. Road name changes.
        MANEUVERS.put("new name-slight right", 6);
        MANEUVERS.put("new name-slight left", 3);
        MANEUVERS.put("new name-right", 7);
        MANEUVERS.put("new name-left", 4);
        MANEUVERS.put("turn-slight left", 3);       // Make a slight left.
        MANEUVERS.put("turn-left", 4);              // Turn left.
        MANEUVERS.put("turn-sharp left", 5);        // Make a sharp left.
        MANEUVERS.put("turn-slight right", 6);      // Make a slight right.
        MANEUVERS.put("turn-right", 7);             // Turn right.
        MANEUVERS.put("turn-sharp right", 8);       // Make a sharp right.
        MANEUVERS.put("turn-uturn", 12);            // Make a u-turn
        MANEUVERS.put("end of road-left", 4);       // turn left
        MANEUVERS.put("end of road-right", 7);       // turn right
        MANEUVERS.put("end of road-slight left", 3); // turn left
        MANEUVERS.put("end of road-slight right", 6);// turn right

        // Ramps
        MANEUVERS.put("ramp-left", 17);
        MANEUVERS.put("ramp-sharp left", 17);
        MANEUVERS.put("ramp-slight left", 17);
        MANEUVERS.put("ramp-right", 18);
        MANEUVERS.put("ramp-sharp right", 18);
        MANEUVERS.put("ramp-slight right", 18);
        MANEUVERS.put("ramp-straight", 19);

        // Merges
        MANEUVERS.put("merge-left", 20);
        MANEUVERS.put("merge-sharp left", 20);
        MANEUVERS.put("merge-slight left", 20);
        MANEUVERS.put("merge-right", 21);
        MANEUVERS.put("merge-sharp right", 21);
        MANEUVERS.put("merge-slight right", 21);
        MANEUVERS.put("merge-straight", 22);

        // Roundabouts
        MANEUVERS.put("roundabout-1", 27); //Round-about, 1st exit
        MANEUVERS.put("roundabout-2", 28); //2nd exit, etc ...
        MANEUVERS.put("roundabout-3", 29);
        MANEUVERS.put("roundabout-4", 30);
        MANEUVERS.put("roundabout-5", 31);
        MANEUVERS.put("roundabout-6", 32);
        MANEUVERS.put("roundabout-7", 33);
        MANEUVERS.put("roundabout-8", 34);

        // Depart / arrive
        MANEUVERS.put("depart", 24); //"Head" => used by OSRM as the start node. Considered here as a "waypoint".
        MANEUVERS.put("arrive", 25); // Custom

        // Duplicated
        MANEUVERS.put("continue-left", 3);  // continue left = slight left
        MANEUVERS.put("continue-right", 6); // continue right = slight right
        MANEUVERS.put("continue-slight left", 3);  // continue left = slight left
        MANEUVERS.put("continue-slight right", 6); // continue right = slight right
        MANEUVERS.put("continue-uturn", 12); // continue uturn = uturn
        MANEUVERS.put("continue", 1);      // continue = straight
    }

    //From: Project-OSRM-Web / WebContent / localization / OSRM.Locale.en.js
    // driving directions
    // %s: road name
    // %d: direction => removed
    // <*>: will only be printed when there actually is a road name
    static final HashMap<Integer, Object> DIRECTIONS;

    static {
        DIRECTIONS = new HashMap<>();

        // Turns
        DIRECTIONS.put(1, R.string.osmbonuspack_directions_1);
        DIRECTIONS.put(2, R.string.osmbonuspack_directions_2);
        DIRECTIONS.put(3, R.string.osmbonuspack_directions_3);
        DIRECTIONS.put(4, R.string.osmbonuspack_directions_4);
        DIRECTIONS.put(5, R.string.osmbonuspack_directions_5);
        DIRECTIONS.put(6, R.string.osmbonuspack_directions_6);
        DIRECTIONS.put(7, R.string.osmbonuspack_directions_7);
        DIRECTIONS.put(8, R.string.osmbonuspack_directions_8);
        DIRECTIONS.put(12, R.string.osmbonuspack_directions_12);

        // Ramps
        DIRECTIONS.put(17, R.string.osmbonuspack_directions_17);
        DIRECTIONS.put(18, R.string.osmbonuspack_directions_18);
        DIRECTIONS.put(19, R.string.osmbonuspack_directions_19);

        // Merges
        DIRECTIONS.put(20, com.xplorun.R.string.osrm_custom_direction20);
        DIRECTIONS.put(21, com.xplorun.R.string.osrm_custom_direction21);
        DIRECTIONS.put(22, com.xplorun.R.string.osrm_custom_direction22);

        // Roundabouts
        DIRECTIONS.put(27, R.string.osmbonuspack_directions_27);
        DIRECTIONS.put(28, R.string.osmbonuspack_directions_28);
        DIRECTIONS.put(29, R.string.osmbonuspack_directions_29);
        DIRECTIONS.put(30, R.string.osmbonuspack_directions_30);
        DIRECTIONS.put(31, R.string.osmbonuspack_directions_31);
        DIRECTIONS.put(32, R.string.osmbonuspack_directions_32);
        DIRECTIONS.put(33, R.string.osmbonuspack_directions_33);
        DIRECTIONS.put(34, R.string.osmbonuspack_directions_34);

        // Depart arrive
        DIRECTIONS.put(24, com.xplorun.R.string.osrm_custom_direction24);
        DIRECTIONS.put(25, com.xplorun.R.string.osrm_custom_direction25);
    }

    public OSRMRoadManagerCustom(Context context, String userAgent) {
        super();
        mContext = context;
        mUserAgent = userAgent;
        mServiceUrl = DEFAULT_SERVICE;
        mMeanUrl = MEAN_BY_FOOT;
    }

    /**
     * allows to request on an other site than OSRM demo site
     */
    public void setService(String serviceUrl) {
        mServiceUrl = serviceUrl;
    }

    /**
     * to switch to another mean of transportation
     */
    public void setMean(String meanUrl) {
        mMeanUrl = meanUrl;
    }

    protected String getUrl(ArrayList<GeoPoint> waypoints, boolean getAlternate) {
        StringBuilder urlString = new StringBuilder(mServiceUrl + "/match/v1/foot/");
        for (int i = 0; i < waypoints.size(); i++) {
            GeoPoint p = waypoints.get(i);
            if (i > 0)
                urlString.append(';');
            urlString.append(geoPointAsLonLatString(p));
        }
        urlString.append("?steps=true&skip_waypoints=true&waypoints=0;")
                 .append(waypoints.size() - 1)
                 .append(mOptions);
        return urlString.toString();
    }

    protected Road[] defaultRoad(ArrayList<GeoPoint> waypoints) {
        Road[] roads = new Road[1];
        roads[0] = new Road(waypoints);
        return roads;
    }

    protected Road[] getRoads(ArrayList<GeoPoint> waypoints, boolean getAlternate) {
        String url = getUrl(waypoints, false);
        Log.d(BonusPackHelper.LOG_TAG, "OSRMRoadManager.getRoads:" + url);
        String jString = BonusPackHelper.requestStringFromUrl(url, mUserAgent);
        if (jString == null) {
            Log.e(BonusPackHelper.LOG_TAG, "OSRMRoadManager::getRoad: request failed.");
            Toast.makeText(mContext.getApplicationContext(), "Request to osrm server failed.",
                    Toast.LENGTH_LONG).show();
            System.exit(1);
        }

        try {
            JSONObject jObject = new JSONObject(jString);
            String jCode = jObject.getString("code");
            if (!"Ok".equals(jCode)) {
                Log.e(BonusPackHelper.LOG_TAG, "OSRMRoadManager::getRoad: error code=" + jCode);
                Road[] roads = defaultRoad(waypoints);
                if ("NoRoute".equals(jCode)) {
                    roads[0].mStatus = Road.STATUS_INVALID;
                }
                return roads;
            } else {
                JSONArray jRoutes = jObject.getJSONArray("matchings");
                Road[] roads = new Road[jRoutes.length()];
                for (int i = 0; i < jRoutes.length(); i++) {
                    Road road = new Road();
                    roads[i] = road;
                    road.mStatus = Road.STATUS_OK;
                    JSONObject jRoute = jRoutes.getJSONObject(i);
                    String route_geometry = jRoute.getString("geometry");
                    road.mRouteHigh = PolylineEncoder.decode(route_geometry, 10, false);
                    road.mBoundingBox = BoundingBox.fromGeoPoints(road.mRouteHigh);
                    road.mLength = jRoute.getDouble("distance") / 1000.0;
                    road.mDuration = jRoute.getDouble("duration");
                    //legs:
                    JSONArray jLegs = jRoute.getJSONArray("legs");
                    for (int l = 0; l < jLegs.length(); l++) {
                        //leg:
                        JSONObject jLeg = jLegs.getJSONObject(l);
                        RoadLeg leg = new RoadLeg();
                        road.mLegs.add(leg);
                        leg.mLength = jLeg.getDouble("distance");
                        leg.mDuration = jLeg.getDouble("duration");
                        //steps:
                        JSONArray jSteps = jLeg.getJSONArray("steps");
                        RoadNode lastNode = null;
                        String lastRoadName = "";
                        for (int s = 1; s < jSteps.length(); s++) { //skip first step, it's the start point
                            JSONObject jStep = jSteps.getJSONObject(s);
                            RoadNode node = new RoadNode();
                            node.mLength = jStep.getDouble("distance") / 1000.0;
                            node.mDuration = jStep.getDouble("duration");
                            JSONObject jStepManeuver = jStep.getJSONObject("maneuver");
                            JSONArray jLocation = jStepManeuver.getJSONArray("location");
                            node.mLocation = new GeoPoint(jLocation.getDouble(1), jLocation.getDouble(0));
                            String direction = jStepManeuver.getString("type");
                            switch (direction) {
                                case "end of road":
                                    var modifier = jStepManeuver.getString("modifier");
                                    if (!modifier.trim().isEmpty()) {
                                        direction = direction + '-' + modifier;
                                    }
                                    break;
                                case "turn":
                                case "ramp":
                                case "merge":
                                    modifier = jStepManeuver.getString("modifier");
                                    direction = direction + '-' + modifier;
                                    break;
                                case "new name":
                                    modifier = jStepManeuver.has("modifier") ? jStepManeuver.getString("modifier") : "";
                                    direction = direction + '-' + modifier;
                                    break;
                                case "fork":
                                    direction = "turn-" + jStepManeuver.getString("modifier");
                                    break;
                                case "rotary":
                                case "roundabout":
                                    direction = "roundabout-" + jStepManeuver.getInt("exit");
                                    break;
                                case "continue":
                                    modifier = jStepManeuver.has("modifier") ? jStepManeuver.getString("modifier") : "";
                                    direction = modifier.trim().isEmpty() ? "continue" : "continue-" + modifier;
                                    break;
                            }
                            node.mManeuverType = getManeuverCode(direction);
                            String roadName = jStep.optString("name", "");
                            node.mInstructions = buildInstructions(node.mManeuverType, roadName);
//                            Log.d("OSRMCustom", String.format("direction: %s -> %s", direction, node.mInstructions));
                            if (lastNode != null && node.mManeuverType == 2 && lastRoadName.equals(roadName)) {
                                //workaround for https://github.com/Project-OSRM/osrm-backend/issues/2273
                                //"new name", but identical to previous name:
                                //skip, but update values of last node:
                                lastNode.mDuration += node.mDuration;
                                lastNode.mLength += node.mLength;
                            } else {
                                road.mNodes.add(node);
                                lastNode = node;
                                lastRoadName = roadName;
                            }
                        } //steps
//                        close the loop
                        road.mNodes.get(road.mNodes.size() - 1).mLocation = road.mNodes.get(0).mLocation;
                    } //legs
                } //routes
                Log.d(BonusPackHelper.LOG_TAG, "OSRMRoadManager.getRoads - finished");
                return roads;
            } //if code is Ok
        } catch (JSONException e) {
            e.printStackTrace();
            return defaultRoad(waypoints);
        }
    }


    @Override
    public Road[] getRoads(ArrayList<GeoPoint> waypoints) {
        return getRoads(waypoints, true);
    }

    @Override
    public Road getRoad(ArrayList<GeoPoint> waypoints) {
        int key = waypoints.hashCode();
        if (CACHE.containsKey(key))
            return CACHE.get(key);
        Road[] roads = getRoads(waypoints, false);
        CACHE.put(key, roads[0]);
        return roads[0];
    }

    protected int getManeuverCode(String direction) {
        Integer code = MANEUVERS.get(direction);
        return Objects.requireNonNullElse(code, 0);
    }

    protected String buildInstructions(int maneuver, String roadName) {
        Integer resDirection = (Integer) DIRECTIONS.get(maneuver);
        if (resDirection == null)
            return "";
        String direction = mContext.getString(resDirection);
        String instructions = "";
        if (roadName.equals(""))
            //remove "<*>"
            instructions = direction.replaceFirst("\\[[^\\]]*\\]", "");
        else {
            direction = direction.replace("[", "");
            direction = direction.replace(']', ' ');
            instructions = String.format(direction, roadName);
        }
        return instructions;
    }
}
