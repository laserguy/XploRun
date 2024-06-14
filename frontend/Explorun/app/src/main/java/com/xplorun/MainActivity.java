package com.xplorun;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationBarView;
import com.xplorun.tree.PlantTreeFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends FragmentActivity {

    private static final String TAG = "log_MainActivity";

    ActivityResultLauncher<Intent> mActivityLauncher;
    public String lastFragmentTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_loading);

        /* Activity launcher:
        1. Get location
        2. Login
        3. Load routes
        4. Display main activity UI
         */

        mActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        switch (result.getResultCode()) {
                            case Params.LOCATION_FETCHED:
                                startLoginActivity();
                                break;
                            case Params.LOGIN_FINISHED:
                                loadUI();
                                break;
                            case Params.RUN_COMPLETED_FINISHED:
                                ((StatisticsFragment) Objects.requireNonNull(getSupportFragmentManager()
                                        .findFragmentByTag("statistics_fragment"))).loadUI();
                                ((UserFragment) Objects.requireNonNull(getSupportFragmentManager()
                                        .findFragmentByTag("user_fragment"))).loadUI();
                        }

                    }
                });
        Utils.checkLocationEnabledOrQuit(this, false);
        startGetLocationActivity();
    }

    private void handleSharedRoute(Intent intent) {
        if (!Intent.ACTION_VIEW.equals(intent.getAction())) {
            return;
        }
        var url= intent.getData().toString();
        Log.d(TAG, "Share url: "+url);
        VolleySingleton.getInstance(this).getShare(url, new VolleyListener() {
            @Override
            public void onError(String message) {
                finishActivity(Params.ERROR_LOADING_ROUTES);
            }

            @Override
            public void onResponse(JSONObject response) {
                var data = Utils.decompress(response.optString("data"));
                try {
                    var json = new JSONObject(data);
                    RoutesSingleton.getInstance().clearCustomRoutes();
                    RoutesSingleton.getInstance().addToCustomRoutes(0, json);
                    var run = (RunFragment) getFragment("run_fragment");
                    run.displayCustomRoutes();
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing shared route: ", e);
                }
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleSharedRoute(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void startLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        mActivityLauncher.launch(intent);
    }

    public void startGetLocationActivity () {
        Intent intent = new Intent(this, GetLocationActivity.class);
        mActivityLauncher.launch(intent);
    }

    public void loadUI() {
        setContentView(R.layout.activity_main);
        NavigationBarView navigationBarView = findViewById(R.id.bottom_navigation);
        navigationBarView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_run:
                        switchFragments("run_fragment");
                        break;
                    case R.id.menu_statistics:
                        switchFragments("statistics_fragment");
                        break;
                    case R.id.menu_user:
                        switchFragments("user_fragment");
                        break;
                    case R.id.plant_tree:
                        switchFragments("plant_tree_fragment");
                        break;
                }
                return true;
            }
        });

        getFragment("run_fragment");
        lastFragmentTag = "run_fragment";
        handleSharedRoute(getIntent());
    }

    public void switchFragments(String nextTag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction().setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out
        );

        fragmentTransaction.hide(getFragment(lastFragmentTag)).commitNow();
        fragmentTransaction.show(getFragment(nextTag)).commitNow();
        lastFragmentTag = nextTag;
    }

    public Fragment getFragment(String tag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        Fragment fragment = fragmentManager.findFragmentByTag(tag);

        if (fragment == null) {
            switch (tag) {
                case "run_fragment":
                    fragmentTransaction.add(R.id.fragment_container_main, RunFragment.newInstance(), "run_fragment");
                    break;
                case "statistics_fragment":
                    fragmentTransaction.add(R.id.fragment_container_main, StatisticsFragment.newInstance(), "statistics_fragment");
                    break;
                case "user_fragment":
                    fragmentTransaction.add(R.id.fragment_container_main, UserFragment.newInstance(), "user_fragment");
                    break;
                case "plant_tree_fragment":
                    fragmentTransaction.add(R.id.fragment_container_main, PlantTreeFragment.newInstance(), "plant_tree_fragment");
                    break;
            }
            fragmentTransaction.commitNow();
            fragment = fragmentManager.findFragmentByTag(tag);
        }

        return fragment;

    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Exit")
                .setMessage("Are you sure you want to exit?")
                .setCancelable(true)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }

                })
                .setNegativeButton("No", null)
                .show();
    }

}
