package com.xplorun;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.xplorun.R;
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class RunFragment extends Fragment {

    private static final String TAG = "log_RunFragment";
    View root;

    Group groupRecommendationsMaps;
    LinearLayout idleRunLayout;

    ActivityResultLauncher activityResultLauncher;

    // UI vars
    private static int NUM_PAGES = 1;
    private ViewPager2 viewPager;
    private FragmentStateAdapter pagerAdapter;

    public static RunFragment newInstance() {
        RunFragment fragment = new RunFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    switch (result.getResultCode()) {
                        case Params.MAIN_ROUTES_LOADED:
                            displayRecommendedRoutes();
                            break;
                        case Params.CUSTOM_ROUTES_LOADED:
                            displayCustomRoutes();
                            break;
                        case Params.CUSTOM_ROUTES_FEATURES_SET:
                            startLoadRoutes("custom");
                            break;
                        case Params.CANCELLED:
                            break;
                        default:
                            Log.d(TAG, "Fail in RunFragment!");
                            System.exit(1);
                    }
                });

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        root = inflater.inflate(R.layout.fragment_run, container, false);

        groupRecommendationsMaps = root.findViewById(R.id.group_recommendations_maps);
        idleRunLayout = root.findViewById(R.id.layout_main_tip);

        groupRecommendationsMaps.setVisibility(View.GONE);
        idleRunLayout.setVisibility(View.VISIBLE);

        // Buttons
        Button loadRoutesButton = root.findViewById(R.id.button_load_recommended_routes);
        loadRoutesButton.setOnClickListener(view -> {
            startLoadRoutes("main");
        });
        Button customRouteButton = root.findViewById(R.id.button_custom_routes);
        customRouteButton.setOnClickListener(view ->  {
            Intent intent = new Intent(getContext(), SetFeaturesActivity.class);
            intent.putExtra("action", "set_custom_route_features");
            activityResultLauncher.launch(intent);
        });

        displayIdleMap();

        return root;
    }

    public void startLoadRoutes(String routesType) {
        Intent intent = new Intent(getContext(), LoadRoutesActivity.class);
        intent.putExtra("routesType", routesType);
        activityResultLauncher.launch(intent);
    }

    public void displayRecommendedRoutes() {
        idleRunLayout.setVisibility(View.GONE);
        groupRecommendationsMaps.setVisibility(View.VISIBLE);

        // Instantiate a ViewPager2 and a PagerAdapter.
        int routesCount = RoutesSingleton.getInstance().getRoutesCount();
        NUM_PAGES = routesCount == 0 ? NUM_PAGES : routesCount;

        viewPager = root.findViewById(R.id.pager_routes);
        viewPager.setOffscreenPageLimit(NUM_PAGES);
        pagerAdapter = new RoutesPagerAdapter(this, "main");
        viewPager.setPageTransformer(new ZoomOutPageTransformer());
        viewPager.setAdapter(pagerAdapter);

        WormDotsIndicator dotsIndicator = root.findViewById(R.id.dots_indicator);
        dotsIndicator.setViewPager2(viewPager);
    }

    public void displayCustomRoutes() {
        idleRunLayout.setVisibility(View.GONE);
        groupRecommendationsMaps.setVisibility(View.VISIBLE);

        // Instantiate a ViewPager2 and a PagerAdapter.
        NUM_PAGES = RoutesSingleton.getInstance().getCustomRoutesCount();

        viewPager = root.findViewById(R.id.pager_routes);
        viewPager.setOffscreenPageLimit(NUM_PAGES);
        pagerAdapter = new RoutesPagerAdapter(this, "custom");
        viewPager.setPageTransformer(new ZoomOutPageTransformer());
        viewPager.setAdapter(pagerAdapter);

        WormDotsIndicator dotsIndicator = root.findViewById(R.id.dots_indicator);
        dotsIndicator.setViewPager2(viewPager);
    }

    public void displayIdleMap() {
        ((TextView)root.findViewById(R.id.text_prompt)).setText(
                String.format("Ready for a run, %s?", UserSingleton.getInstance().getUsername())
        );

        try {
            Context ctx = getContext().getApplicationContext();
            Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
            Configuration.getInstance().setUserAgentValue("Explorun/1.0");

            // Map view
            MapView map = (MapView) root.findViewById(R.id.mapViewIdle);
            map.setTileProvider(new MapTileProviderBasic(getContext(), TileSourceFactory.MAPNIK));
            map.setMultiTouchControls(true);
            GpsMyLocationProvider myLocationProvider = new GpsMyLocationProvider(getActivity());

            // Thread policy
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

            var overlay = new MyLocationNewOverlay(myLocationProvider, map);
            overlay.enableMyLocation();
            overlay.enableFollowLocation();
            overlay.setDrawAccuracyEnabled(true);
            map.getOverlays().add(overlay);
            map.getController().setZoom(17d);
            map.invalidate();
        }
        catch (Exception e) {
            idleRunLayout.setVisibility(View.GONE);
            Log.d(TAG, "Error in idle map: "+e.getMessage());
        }
    }

    private static class RoutesPagerAdapter extends FragmentStateAdapter {

        private String routesType;

        public RoutesPagerAdapter(RunFragment fa, String routesType) {
            super(fa);
            this.routesType = routesType;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return new MapViewFragmentOSM(position, routesType);
        }

        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }

    }

}
