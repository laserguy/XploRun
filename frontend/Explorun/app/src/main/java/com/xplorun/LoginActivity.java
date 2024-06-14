package com.xplorun;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.xplorun.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "log_LoginActivity";

    // UI elements
    TextInputEditText edit_userName, edit_password, edit_confirm_password;
    TextInputLayout textInputLayoutConfirmPassword;
    Button loginButton, signupButton;

    ActivityResultLauncher activityResultLauncher;

    // Shared prefs
    SharedPreferences prefs;
    SharedPreferences.Editor editor;


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (Params.SKIP_LOGIN) {
            UserSingleton.getInstance()
                    .setUserData("user17", "21", "M", 25, 175, 82);
            finishActivity(Params.LOGIN_FINISHED);
        }

        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        switch (result.getResultCode()) {
                            case Params.USER_FEATURES_SET:
                                setPreferences(result.getData());
                                break;
                            case Params.CANCELLED:
                                resetUI();
                                break;
                        }

                    }
                });

        prefs = getSharedPreferences("UserData", MODE_PRIVATE);
        editor = prefs.edit();

        if (tryCachedLogin()) finishActivity(Params.LOGIN_FINISHED);
        else loadUI();
    }

    public boolean tryCachedLogin() {
        List<String> userLoginParams = new ArrayList<>(Arrays
                .asList("username", "user_id", "gender", "age", "height", "weight")
        );

        for (String s : userLoginParams) if (!prefs.contains(s)) {
            Log.d(TAG, s+ "not found in prefs");
            return false;
        }

        UserSingleton.getInstance().setUserData(
                prefs.getString("username", ""),
                prefs.getString("user_id", ""),
                prefs.getString("gender", ""),
                prefs.getInt("age", 0),
                prefs.getInt("height", 0),
                prefs.getInt("weight", 0)
        );
        return true;
    }


    public void loadUI() {
        edit_userName = findViewById(R.id.edit_username);
        edit_password = findViewById(R.id.edit_password);
        edit_confirm_password = findViewById(R.id.edit_confirm_password);
        textInputLayoutConfirmPassword = findViewById(R.id.text_input_layout_confirm_password);

        loginButton = findViewById(R.id.button_login);
        loginButton.setOnClickListener(view -> startLogin());

        signupButton = findViewById(R.id.button_signup);
        signupButton.setOnClickListener(view -> startSignup());
    }

    public void startLogin() {
        // Login process
        String userName = edit_userName.getText().toString();
        String password = edit_password.getText().toString();
        if (userName.isEmpty() || password.isEmpty()) return;

        // Load user data from VolleySingleton
        VolleySingleton.getInstance(getApplicationContext())
                .login(userName, password, new VolleyListener() {
                    @Override
                    public void onError(String message) {
                        // Error connecting to server or wrong credentials
                        Log.d(TAG, "Error " + message);
                        Toast.makeText(getApplicationContext(), "Something went wrong :(",
                                Toast.LENGTH_LONG).show();
                    }
                    @Override
                    public void onResponse(JSONObject response) {
                        handleLogin(userName, response);
                    }
                });
    }

    public void handleLogin(String userName, JSONObject response) {
        try {
            Log.d(TAG, response.toString());
            if (response.getString("id").equals("e")) {
                // Error connecting to database, connection to server exists
                Toast.makeText(getApplicationContext(), "Something went wrong :(",
                        Toast.LENGTH_LONG).show();
                return;
            }

            // Credentials ok
            UserSingleton.getInstance().setUsername(userName);
            editor.putString("username", userName);
            UserSingleton.getInstance().setUser_id(response.getString("id"));
            editor.putString("user_id", response.getString("id"));
            editor.commit();

            // Set preferences if needed
            if (response.getString("firstlogin").equals("0")) {
                Toast.makeText(getApplicationContext(), "Let's set your preferences!",
                        Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(getApplicationContext(), SetFeaturesActivity.class);
                intent.putExtra("action", "set_user_preferences");
                activityResultLauncher.launch(intent);
            }
            else {
                UserSingleton.getInstance().setGender(response.getString("sex"));
                editor.putString("gender", response.getString("sex"));
                UserSingleton.getInstance().setAge(response.getInt("age"));
                editor.putInt("age", response.getInt("age"));
                UserSingleton.getInstance().setHeight(response.getInt("height"));
                editor.putInt("height", response.getInt("height"));
                UserSingleton.getInstance().setWeight(response.getInt("weight"));
                editor.putInt("weight", response.getInt("weight"));
                editor.commit();

                Toast.makeText(getApplicationContext(), "Successfully logged in!",
                        Toast.LENGTH_LONG).show();
                finishActivity(Params.LOGIN_FINISHED);
            }
        } catch (JSONException e) {
            Log.d(TAG, e.getMessage());
            Toast.makeText(getApplicationContext(), "Something went wrong :(",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void startSignup() {
            edit_confirm_password.setVisibility(View.VISIBLE);
            textInputLayoutConfirmPassword.setVisibility(View.VISIBLE);
            loginButton.setEnabled(false);
            signupButton.setText("Continue");

            // Signup process
            String userName = edit_userName.getText().toString();
            String password = edit_password.getText().toString();
            String confirm_password = edit_confirm_password.getText().toString();

            if (password.isEmpty() || userName.isEmpty() || confirm_password.isEmpty()) return;
            if (!password.equals(confirm_password)) {
                Toast.makeText(getApplicationContext(), "Passwords don't match!",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Load user data from VolleySingleton
            VolleySingleton.getInstance(getApplicationContext())
                    .register(userName, password, new VolleyListener() {
                        @Override
                        public void onError(String message) {
                            Log.d(TAG, "Error " + message);
                            Toast.makeText(getApplicationContext(), "Something went wrong :(",
                                    Toast.LENGTH_LONG).show();
                            resetUI();
                        }

                        @Override
                        public void onResponse(JSONObject response) {
                            handleSignup(response);
                        }
                    });
    }

    public void handleSignup(JSONObject response) {
        Log.d(TAG, response.toString());
        try {
            if(response.getString("id") != null) {
                Toast.makeText(getApplicationContext(), "Successfully registered! Let's set your preferences.",
                        Toast.LENGTH_LONG).show();
                startLogin();
            }
        }
        catch (JSONException e) {
            Log.d(TAG, e.getMessage());
            Toast.makeText(getApplicationContext(), "Something went wrong :(",
                    Toast.LENGTH_LONG).show();
            resetUI();
        }
    }

    public void setPreferences(Intent data) {
        String preferencesString = data.getStringExtra("preferences");
        JSONObject preferencesJSON = null;
        try {
            preferencesJSON = new JSONObject(preferencesString);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d(TAG, preferencesJSON.toString());

        VolleySingleton.getInstance(getApplicationContext()).setPreferences(preferencesJSON, new VolleyListener() {
            @Override
            public void onError(String message) {
                Log.d(TAG, "Error setting preferences! " + message);
                finishActivity(Params.ERROR_SETTING_USER_PREFERENCES);
            }

            @Override
            public void onResponse(JSONObject response) {
                Toast.makeText(getApplicationContext(), "Preferences set!",
                        Toast.LENGTH_SHORT).show();
                finishActivity(Params.LOGIN_FINISHED);
            }
        });

    }

    public void resetUI() {
        edit_userName.setText("");
        edit_password.setText("");
        edit_confirm_password.setText("");
        edit_confirm_password.setVisibility(View.GONE);
        textInputLayoutConfirmPassword.setVisibility(View.GONE);
        loginButton.setEnabled(true);
    }

    public void finishActivity(int resultCode) {
        setResult(resultCode);
        finish();
    }

    @Override
    public void onBackPressed() {
        resetUI();
    }

}
