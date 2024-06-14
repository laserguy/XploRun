package com.xplorun;

import org.json.JSONObject;

public interface VolleyListener{

    void onError(String message);

    void onResponse(JSONObject response);

}
