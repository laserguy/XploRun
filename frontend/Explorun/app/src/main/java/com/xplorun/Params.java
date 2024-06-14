package com.xplorun;

public class Params {

    // Debug vars
    public static final boolean SKIP_LOGIN=false;
    public static final boolean USE_SAMPLE_ROUTES=false;

    // Services URLs
    // public static final String EXPLORUN_URL="http://10.0.2.2:5000";
    public static final String EXPLORUN_URL="https://app.explorun.ml";
    public static final String OSRM_URL="https://osrm1.explorun.ml";
    // public static final String OSRM_URL="https://routing.openstreetmap.de/routed-foot";
    public static final String SHARE_GET_EXPLORUN_URL ="https://share.explorun.ml/r";
    public static final String SHARE_PUT_EXPLORUN_URL = "https://put.explorun.ml/r";

    // Options
    public static final int NUM_OF_ANALYSIS_CHART_SEGMENTS = 50;

    // Result codes for activities
    public static final int LOGIN_FINISHED = 100;
    public static final int LOCATION_FETCHED = 101;
    public static final int MAIN_ROUTES_LOADED = 102;
    public static final int CUSTOM_ROUTES_LOADED = 103;

    public static final int CUSTOM_ROUTES_FEATURES_SET = 104;
    public static final int USER_FEATURES_SET = 105;

    public static final int RUN_COMPLETED_FINISHED=106;

    public static final int CANCELLED = 200;
    public static final int ERROR_LOADING_ROUTES = 201;
    public static final int ERROR_SETTING_USER_PREFERENCES = 202;

    public static final String USER_AGENT = "Explorun/1.0";
}
