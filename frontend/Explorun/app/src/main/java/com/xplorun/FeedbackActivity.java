package com.xplorun;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.xplorun.R;
import com.google.android.material.button.MaterialButton;

import org.json.JSONException;
import org.json.JSONObject;

// Activity to user SharedPreferences, check for active run and ask if one exists, ask for feedback

public class FeedbackActivity extends AppCompatActivity {

    private static final String TAG = "log_FeedbackActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        loadUI();
    }

    public void loadUI() {
        MaterialButton likeButton = findViewById(R.id.button_like);
        likeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleLike();
            }
        });

        MaterialButton dislikeButton = findViewById(R.id.button_dislike);
        dislikeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (RoutesSingleton.getInstance().getRoutesCount() == 0) {
                    endFeedback();
                }
                else cancelFeedback();
            }
        });

        MaterialButton closeButton = findViewById(R.id.button_cancel_feedback);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (RoutesSingleton.getInstance().getRoutesCount() == 0) {
                    endFeedback();
                }
                else cancelFeedback();
            }
        });
    }

    public void handleLike() {
        // Create json for feedback endpoint
        JSONObject activeRouteJSON = RoutesSingleton.getInstance().getActiveRoute();
        try {
            activeRouteJSON.put("user_id", UserSingleton.getInstance().getUser_id());
        } catch (JSONException e) {
            Log.d(TAG, e.getMessage());
        }

        VolleySingleton.getInstance(getApplicationContext()).sendFeedback(activeRouteJSON, true, new VolleyListener() {
            @Override
            public void onError(String message) {
                Log.d(TAG, message);
            }

            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, response.toString());
                Toast.makeText(getApplicationContext(), "Thanks for rating! Suggested routes are recalculated based on your feedback.",
                        Toast.LENGTH_LONG).show();
                endFeedback();
            }
        });
    }

    public void cancelFeedback() {
        RoutesSingleton.getInstance().setActiveRoute(null);
        finish();
    }

    public void endFeedback() {
        RoutesSingleton.getInstance().setActiveRoute(null);
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        return;
    }
}
