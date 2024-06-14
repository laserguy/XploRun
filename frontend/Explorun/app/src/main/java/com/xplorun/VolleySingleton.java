package com.xplorun;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class VolleySingleton {

    private static final String TAG = "log_VolleySingleton";
    private String URL = Params.EXPLORUN_URL;

    private static VolleySingleton instance;
    private RequestQueue requestQueue;
    private static Context ctx;

    private VolleySingleton(Context context) {
        ctx = context;
        requestQueue = getRequestQueue();
    }

    public static synchronized VolleySingleton getInstance(Context context) {
        if (instance == null) {
            instance = new VolleySingleton(context);
        }
        return instance;
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            requestQueue = Volley.newRequestQueue(ctx.getApplicationContext());
        }
        return requestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    public boolean login(String username, String password, final VolleyListener volleyListener){
        String endpoint = URL + "/api/users/login";

        JSONObject jsonParams = new JSONObject();
        try {
            jsonParams.put("username", username);
            jsonParams.put("password", password);

        } catch (JSONException e) {
            Log.d(TAG, e.getMessage());
            e.printStackTrace();
        }

        Log.d(TAG, jsonParams.toString());

        CustomJsonObjectRequest jsonObjectRequest = new CustomJsonObjectRequest(
                Request.Method.POST, endpoint, jsonParams, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, response.toString());
                        volleyListener.onResponse(response);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "Error " + error.getMessage());
                        volleyListener.onError(error.getMessage());
                    }
                }
                );

        // 10 seconds timeout policy
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        this.addToRequestQueue(jsonObjectRequest);
        return true;
    }

    public boolean register(String username, String password, final VolleyListener volleyListener) {
        String endpoint = URL + "/api/users/register";

        JSONObject jsonParams = new JSONObject();
        try {
            jsonParams.put("username", username);
            jsonParams.put("password", password);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        CustomJsonObjectRequest jsonObjectRequest = new CustomJsonObjectRequest(
                Request.Method.POST, endpoint, jsonParams, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, response.toString());
                        volleyListener.onResponse(response);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "Error " + error.getMessage());
                        volleyListener.onError(error.getMessage());
                    }
                }
                );

        // 10 seconds timeout policy
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        this.addToRequestQueue(jsonObjectRequest);
        return true;
    }

    public boolean setPreferences(JSONObject preferences, final VolleyListener volleyListener) {
        String endpoint = URL + "/api/users/set_preferences";
        CustomJsonObjectRequest jsonObjectRequest = new CustomJsonObjectRequest(
                Request.Method.POST, endpoint, preferences, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.d(TAG, response.toString());
                    volleyListener.onResponse(response);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, "Error " + error.getMessage());
                    volleyListener.onError(error.getMessage());
                }
            }
            );

        // 10 seconds timeout policy
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        this.addToRequestQueue(jsonObjectRequest);
        return true;

    }

    public boolean getRoutes(Location location, final VolleyListener volleyListener) {
        String endpoint = URL + "/api/users/getRoutes";
        String latlong = location.getLatitude() + "," + location.getLongitude();

        // Build uri
        String uri = endpoint + String.format("?user_id=%1$s&location=%2$s",
                UserSingleton.getInstance().getUser_id(), latlong);

        // Debugging logs
        Log.d(TAG, "URI: " + uri);

        CustomJsonObjectRequest jsonObjectRequest = new CustomJsonObjectRequest(
                Request.Method.GET, uri, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                volleyListener.onResponse(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "Error " + error.getMessage());
                volleyListener.onError(error.getMessage());
            }
        }
        );

        // 10 minutes timeout policy
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                600000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        this.addToRequestQueue(jsonObjectRequest);
        return true;
    }

    public boolean getCustomRoutes(Location location, JSONObject jsonParams, final VolleyListener volleyListener) {
        String endpoint = URL + "/api/users/getCustomRoutes";
        String latlong = location.getLatitude() + "," + location.getLongitude();

        // Build uri
        String uri = endpoint + String.format("?location=%1$s", latlong);

        // Debugging logs
        Log.d(TAG, "URI: " + uri);
        Log.d(TAG, "JSON params: " + jsonParams);

        CustomJsonObjectRequest jsonObjectRequest = new CustomJsonObjectRequest(
                Request.Method.POST, uri, jsonParams, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                volleyListener.onResponse(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "Error " + error.getMessage());
                volleyListener.onError(error.getMessage());
            }
        }
        );

        // 10 minutes timeout policy
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                600000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        this.addToRequestQueue(jsonObjectRequest);
        return true;
    }

    public boolean sendFeedback(JSONObject featuresJson, Boolean liked, final VolleyListener volleyListener) {
        String endpoint = URL + "/api/users/feedback";
        if (!liked) return false;

        CustomJsonObjectRequest jsonObjectRequest = new CustomJsonObjectRequest(
                Request.Method.POST, endpoint, featuresJson, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, response.toString());
                volleyListener.onResponse(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "Error getCustomRoutes " + error.getMessage());
                volleyListener.onError(error.getMessage());
            }
        }
        );

        // 10 seconds timeout policy
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        this.addToRequestQueue(jsonObjectRequest);
        return true;
    }

    public boolean dispatchRunInfo(JSONObject runStatsJson, final VolleyListener volleyListener) {
        String endpoint = URL + "/api/users/dispatch_run_info";

        CustomJsonObjectRequest jsonObjectRequest = new CustomJsonObjectRequest(
                Request.Method.POST, endpoint, runStatsJson, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, response.toString());
                volleyListener.onResponse(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "Error dispatchRunInfo " + error.getMessage());
                volleyListener.onError(error.getMessage());
            }
        }
        );

        // 10 seconds timeout policy
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        this.addToRequestQueue(jsonObjectRequest);
        return true;
    }

    public boolean getOverallStats(String userID, final VolleyListener volleyListener) {
        String endpoint = URL + "/api/users/get_overall_stats";

        String uri = endpoint + String.format("?user_id=%1$s",
                UserSingleton.getInstance().getUser_id());

        CustomJsonObjectRequest jsonObjectRequest = new CustomJsonObjectRequest(
                Request.Method.GET, uri, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                volleyListener.onResponse(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "Error " + error.getMessage());
                volleyListener.onError(error.getMessage());
            }
        }
        );

        // 10 minutes timeout policy
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                600000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        this.addToRequestQueue(jsonObjectRequest);
        return true;
    }

    public boolean getProfileStats(String userID, final VolleyListener volleyListener) {
        String endpoint = URL + "/api/users/get_profile_stats";

        String uri = endpoint + String.format("?user_id=%1$s",
                UserSingleton.getInstance().getUser_id());

        CustomJsonObjectRequest jsonObjectRequest = new CustomJsonObjectRequest(
                Request.Method.GET, uri, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                volleyListener.onResponse(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "Error " + error.getMessage());
                volleyListener.onError(error.getMessage());
            }
        }
        );

        // 10 minutes timeout policy
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                600000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        this.addToRequestQueue(jsonObjectRequest);
        return true;
    }

    public void postShare(String userId, String data, VolleyListener volleyListener){
        String sha = Utils.sha256(data);
        String putEnd = Params.SHARE_PUT_EXPLORUN_URL + "/" + sha;
        String getEnd = Params.SHARE_GET_EXPLORUN_URL + "/" + sha;
        JSONObject jsonParams = new JSONObject();
        try {
            jsonParams.put("data", data);
            jsonParams.put("userId", userId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        var req = new JsonObjectRequest(Request.Method.PUT, putEnd, jsonParams,
                response -> {
                    try {
                        volleyListener.onResponse(new JSONObject().put("url", getEnd));
                    } catch (JSONException e) {
                        volleyListener.onError(e.getMessage());
                    }
                },
                (VolleyError error) -> Log.d(TAG, "Error share ", error));

        // 10 seconds timeout policy
        req.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        this.addToRequestQueue(req);
    }

    public void getShare(String url, VolleyListener volleyListener){
        var jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET, url, null,
                (Response.Listener<JSONObject>) response -> {
                    Log.d(TAG, response.toString());
                    volleyListener.onResponse(response);
                },
                error -> {
                    Log.e(TAG, "Error share ", error);
                    volleyListener.onError("Error share");
                });
        jsonObjectRequest.setShouldCache(false);
        // 10 seconds timeout policy
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        this.addToRequestQueue(jsonObjectRequest);
    }

    // Override user-agent, to skip localtunnel.me startup page
    public class CustomJsonObjectRequest  extends JsonObjectRequest
    {
        public CustomJsonObjectRequest(int method, String url, JSONObject jsonRequest,Response.Listener listener, Response.ErrorListener errorListener)
        {
            super(method, url, jsonRequest, listener, errorListener);
        }

        @Override
        public Map getHeaders() throws AuthFailureError {
            Map headers = new HashMap();
            headers.put("User-agent", "localhost");

            return headers;
        }
    }

}
