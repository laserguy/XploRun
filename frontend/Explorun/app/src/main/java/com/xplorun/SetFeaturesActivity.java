package com.xplorun;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.xplorun.R;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

public class SetFeaturesActivity extends AppCompatActivity {
    private static final String TAG = "log_SignUpActivity";

    Slider sliderUniqueness, sliderLength, sliderElevation, sliderPedestrianFriendliness, sliderNature;
    TextInputEditText editAge, editHeight, editWeight;
    TextInputLayout menuGender;
    private final int DURATION = Toast.LENGTH_SHORT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_features);

        sliderUniqueness = findViewById(R.id.slider_uniqueness);
        sliderLength = findViewById(R.id.slider_length);
        sliderElevation = findViewById(R.id.slider_elevation);
        sliderPedestrianFriendliness = findViewById(R.id.slider_pedestrian_friendliness);
        sliderNature = findViewById(R.id.slider_nature);

        editAge = findViewById(R.id.edit_age);
        editHeight = findViewById(R.id.edit_height);
        editWeight = findViewById(R.id.edit_weight);
        menuGender = findViewById(R.id.menu_gender);

        if (getIntent().getStringExtra("action").equals("set_user_preferences")) {
            setUserPreferences();
        }
        else getCustomRouteFeatures();

    }

    public void setUserPreferences() {
        handleSliders();
        Button buttonPreferences = findViewById(R.id.button_set_preferences);
        buttonPreferences.setOnClickListener(view -> {

            String gender;
            Integer age, height, weight;

            try {
                gender = String.valueOf(menuGender.getEditText().getText());
                age = Integer.parseInt(String.valueOf(editAge.getText()));
                height = Integer.parseInt(String.valueOf(editHeight.getText()));
                weight = Integer.parseInt(String.valueOf(editWeight.getText()));
                if (gender.isEmpty() || age == 0 || height == 0 || weight == 0) throw new NumberFormatException();
            }
            catch (NumberFormatException e) {
                Toast.makeText(getApplicationContext(), "All fields are required!",
                        DURATION).show();
                return;
            }

            if (gender == "Male") gender = "M";
            else if (gender == "Female") gender = "F";
            else gender = "M";

            UserSingleton.getInstance().setGender(gender);
            UserSingleton.getInstance().setAge(age);
            UserSingleton.getInstance().setHeight(height);
            UserSingleton.getInstance().setWeight(weight);

            JSONObject jsonParams = new JSONObject();
            try {
                jsonParams.put("user_id", Integer.valueOf(UserSingleton.getInstance().getUser_id()));
                jsonParams.put("uniqueness", (float)sliderUniqueness.getValue());
                jsonParams.put("length", (int)sliderLength.getValue());
                jsonParams.put("elevation", (int)sliderElevation.getValue());
                jsonParams.put("ped_friend", (float)(sliderPedestrianFriendliness.getValue()));
                jsonParams.put("nature",(float)(sliderNature.getValue()));
                jsonParams.put("sex", gender);
                jsonParams.put("age", age);
                jsonParams.put("height", height);
                jsonParams.put("weight", weight);
                Log.d(TAG, jsonParams.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Intent intent = new Intent();
            intent.putExtra("preferences", jsonParams.toString());
            setResult(Params.USER_FEATURES_SET, intent);
            finish();
        });
    }

    private void getCustomRouteFeatures() {
        menuGender.setVisibility(View.GONE);
        editAge.setVisibility(View.GONE);
        editHeight.setVisibility(View.GONE);
        editWeight.setVisibility(View.GONE);
        findViewById(R.id.text_details).setVisibility(View.GONE);

        TextView textHeader = findViewById(R.id.text_header);
        textHeader.setText("Set features of the desired routes");
        handleSliders();
        Button buttonPreferences = findViewById(R.id.button_set_preferences);
        buttonPreferences.setText("Set features");

        buttonPreferences.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject jsonParams = new JSONObject();
                try {
                    jsonParams.put("uniqueness", sliderUniqueness.getValue());
                    jsonParams.put("length", (int)sliderLength.getValue());
                    jsonParams.put("elevation", (int)sliderElevation.getValue());
                    jsonParams.put("ped_friend", (sliderPedestrianFriendliness.getValue()));
                    jsonParams.put("nature",(sliderNature.getValue()));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                RoutesSingleton.getInstance().setLikedFeaturesJson(jsonParams);
                setResult(Params.CUSTOM_ROUTES_FEATURES_SET);
                finish();
            }
        });
    }

    @SuppressLint("RestrictedApi")
    public void handleSliders() {
        sliderUniqueness.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                Toast.makeText(getApplicationContext(), "Uniqueness determines whether part of the route might be repeated.",
                        DURATION).show();
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {

            }
        });

        sliderLength.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                Toast.makeText(getApplicationContext(), "Length determines the preferred length of your routes.",
                        DURATION).show();
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {

            }
        });

        sliderElevation.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                Toast.makeText(getApplicationContext(), "Elevation determines your preferred elevation for your routes.",
                        DURATION).show();
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {

            }
        });

        sliderPedestrianFriendliness.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                Toast.makeText(getApplicationContext(), "Pedestrian friendliness determines whether your routes will be absolutely pedestrian friendly.",
                        DURATION).show();
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {

            }
        });

        sliderNature.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                Toast.makeText(getApplicationContext(), "Nature cover determines how much of your routes will be in nature.",
                        DURATION).show();
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {

            }
        });

    }

    @Override
    public void onBackPressed() {
        setResult(Params.CANCELLED);
        finish();
    }

}
