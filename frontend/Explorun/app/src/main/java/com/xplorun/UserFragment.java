package com.xplorun;

import static android.content.Context.MODE_PRIVATE;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xplorun.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link UserFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class UserFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "log_UserFragment";
    View root;

    public UserFragment() {
        // Required empty public constructor
    }

    public static UserFragment newInstance() {
        UserFragment fragment = new UserFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        root = inflater.inflate(R.layout.fragment_user, container, false);
        ((SwipeRefreshLayout)root).setOnRefreshListener(this);

        loadUI();

        return root;
    }

    public void loadUI() {
        ((SwipeRefreshLayout)root).setRefreshing(true);
        displayUserDetails();
        loadProfileStats();
        ((SwipeRefreshLayout)root).setRefreshing(false);
        root.findViewById(R.id.button_logout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = getActivity().getSharedPreferences("UserData", MODE_PRIVATE);
                prefs.edit().clear().commit();
                PackageManager packageManager = getContext().getPackageManager();
                Intent intent = packageManager.getLaunchIntentForPackage(getContext().getPackageName());
                ComponentName componentName = intent.getComponent();
                Intent mainIntent = Intent.makeRestartActivityTask(componentName);
                getContext().startActivity(mainIntent);
                Runtime.getRuntime().exit(0);
            }
        });

    }

    public void displayUserDetails() {
        TextView textUsername, textGender, textAge, textHeight, textWeight;

        textUsername = root.findViewById(R.id.text_user_name);
        textGender = root.findViewById(R.id.text_gender);
        textAge = root.findViewById(R.id.text_age);
        textHeight = root.findViewById(R.id.text_height);
        textWeight = root.findViewById(R.id.text_weight);

        textUsername.setText(UserSingleton.getInstance().getUsername());
        textGender.setText(UserSingleton.getInstance().getGender());
        textAge.setText(String.valueOf(UserSingleton.getInstance().getAge()));
        textHeight.setText(String.valueOf(UserSingleton.getInstance().getHeight())+" cm");
        textWeight.setText(String.valueOf(UserSingleton.getInstance().getWeight())+" kg");
    }

    public void loadProfileStats() {
        VolleySingleton.getInstance(getContext()).getProfileStats(UserSingleton.getInstance().getUser_id(), new VolleyListener() {
            @Override
            public void onError(String message) {
                Log.d(TAG, "Error in getProfileStats: "+message);
                Toast.makeText(getContext(), "Something went wrong loading your profile stats.",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(JSONObject response) {
                try {
                    // Stats
                    JSONObject profileStatsJson = response.getJSONObject("week_stats");
                    ProfileStat profileStat = new ProfileStat(
                            profileStatsJson.getDouble("week_distance"),
                            profileStatsJson.getDouble("week_kcal"),
                            profileStatsJson.getDouble("week_xp")
                    );

                    displayProfileStats(profileStat);

                    // Weekly runs
                    JSONObject weekRunsJson = response.getJSONObject("week_runs");
                    ArrayList<WeekRun> weekRuns = new ArrayList<>();

                    try {
                        for (int i=0;i<weekRunsJson.length();i++) {
                            String key = weekRunsJson.names().getString(i);
                            JSONObject runJson = weekRunsJson.getJSONObject(key);
                            weekRuns.add(new WeekRun(
                                    new SimpleDateFormat("yyyy-MM-dd").parse(key), runJson.getDouble("distance"), runJson.getDouble("kcal"), runJson.getDouble("runs"), runJson.getDouble("xp")
                            ));
                        }

                        if (weekRunsJson.length() > 0) displayCharts(weekRuns);
                        else {
                            root.findViewById(R.id.chart_weekly_cals).setVisibility(View.GONE);
                            root.findViewById(R.id.chart_weekly_distance).setVisibility(View.GONE);
                        }
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage());
                    }

                    // Badges
                    try {
                        JSONObject badgesJson = response.getJSONObject("badges");
                        UserSingleton.getInstance().clearBadges();
                        for (int i=0;i<badgesJson.length();i++) {
                            JSONObject badgeJson = badgesJson.getJSONObject(String.valueOf(i));
                            UserSingleton.getInstance().addBadge(
                                    new UserSingleton.Badge(badgeJson.getInt("badge_id"), badgeJson.getLong("expire_ts"))
                            );
                        }

                        if (badgesJson.length() > 0) displayBadges();
                        else (root.findViewById(R.id.text_badges)).setVisibility(View.GONE);
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage());
                    }

                } catch (JSONException e) {
                    Log.d(TAG, "Error parsing getOverallStats json: "+e.getMessage());
                }
            }
        });
    }

    public void displayProfileStats(ProfileStat profileStat) {
        ((TextView)root.findViewById(R.id.text_weekly_distance)).setText(
                String.format("%s km", Utils.to2Dstring(profileStat.week_distance/1000))
        );
        ((TextView)root.findViewById(R.id.text_weekly_cals)).setText(
                String.format("%s kcal", Utils.to2Dstring(profileStat.kcal))
        );
        ((TextView)root.findViewById(R.id.text_weekly_xp)).setText(
                String.format("%s", Utils.to2Dstring(profileStat.xp))
        );
    }

    public void displayCharts(ArrayList<WeekRun> weekRuns) {
        List<String> weekdays = Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
        List<BarEntry> entriesDistance = new ArrayList<>();
        List<BarEntry> entriesCals = new ArrayList<>();

        // Set list to end at the current day of the week
        String currentDay = LocalDate.now().getDayOfWeek().name().toLowerCase();
        currentDay = currentDay.substring(0,1).toUpperCase()+currentDay.substring(1).toLowerCase();
        int distanceToRotate = weekdays.size() - weekdays.indexOf(currentDay)-1;
        Collections.rotate(weekdays, distanceToRotate);

        for (String weekday : weekdays) {
            float dayDistance = 0;
            float dayCals = 0;

            for (WeekRun weekRun : weekRuns) {
                if (new SimpleDateFormat("EEEE").format(weekRun.date).equals(weekday)) {
                    dayDistance += weekRun.distance;
                    dayCals += weekRun.kcal;
                }
            }
            entriesDistance.add(new BarEntry(weekdays.indexOf(weekday), dayDistance/1000));
            entriesCals.add(new BarEntry(weekdays.indexOf(weekday), dayCals));
        }

        // Distance chart
        BarDataSet barDataSetDistance = new BarDataSet(entriesDistance, "Distance [km]");
        barDataSetDistance.setColor(getContext().getColor(R.color.green));
        BarData barDataDistance = new BarData(barDataSetDistance);
        barDataDistance.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0f) return "";
                return super.getFormattedValue(value);
            }
        });
        
        BarChart chartDistance = root.findViewById(R.id.chart_weekly_distance);
        chartDistance.setData(barDataDistance);
        chartDistance.getDescription().setEnabled(false);
        chartDistance.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        chartDistance.getAxisRight().setEnabled(false);
        chartDistance.getXAxis().setDrawGridLines(false);
        chartDistance.getXAxis().setDrawAxisLine(false);
        chartDistance.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chartDistance.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return weekdays.get((int)value).substring(0, 3);
            }
        });
        
        chartDistance.invalidate();
        
        // Calories chart
        BarDataSet barDataSetCalories = new BarDataSet(entriesCals, "Calories [kcal]");
        barDataSetCalories.setColor(getContext().getColor(R.color.red));

        BarData barDataCals = new BarData(barDataSetCalories);
        barDataSetCalories.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0f) return "";
                return super.getFormattedValue(value);
            }
        });

        BarChart chartCalories = root.findViewById(R.id.chart_weekly_cals);
        chartCalories.setData(barDataCals);
        chartCalories.getDescription().setEnabled(false);
        chartCalories.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        chartCalories.getAxisRight().setEnabled(false);
        chartCalories.getXAxis().setDrawGridLines(false);
        chartCalories.getXAxis().setDrawAxisLine(false);
        chartCalories.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chartCalories.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return weekdays.get((int)value).substring(0, 3);
            }
        });

        chartCalories.invalidate();

    }

    public void displayBadges() {
        LinearLayout badgesLayout = root.findViewById(R.id.layout_badges);
        LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        badgesLayout.removeAllViews();

        ArrayList<UserSingleton.Badge> badges = UserSingleton.getInstance().getBadges();

        for (UserSingleton.Badge badge : badges) {
            View badgeEntry = inflater.inflate(R.layout.layout_badge, null);

            String title, description;

            switch (badge.badgeID) {
                case 1:
                    title = getString(R.string.nature);
                    description = getString(R.string.nature_badge_desc);
                    break;
                case 2:
                    title = getString(R.string.forrest_gump);
                    description = getString(R.string.forrest_gump_badge_desc);
                    break;
                case 3:
                    title = getString(R.string.high_intensity);
                    description = getString(R.string.high_intensity_badge_desc);
                    break;
                case 4:
                    title = getString(R.string.mountaineer);
                    description = getString(R.string.mountaineer_badge_desc);
                    break;
                case 5:
                    title = getString(R.string.lazy);
                    description = getString(R.string.lazy_badge_desc);
                    break;
                case 6:
                    title = getString(R.string.surrender);
                    description = getString(R.string.surrender_badge_desc);
                    break;
                case 7:
                    title = getString(R.string.crawler);
                    description = getString(R.string.crawler_badge_desc);
                    break;
                case 8:
                    title = getString(R.string.challenge_completed);
                    description = getString(R.string.challenge_completed_badge_desc);
                    break;
                default:
                    title = "";
                    description = "";
            }

            ((TextView)badgeEntry.findViewById(R.id.text_badge_title)).setText(title);
            ((TextView)badgeEntry.findViewById(R.id.text_badge_description)).setText(description);

            String imageName = "badge_" + badge.badgeID;
            int imageResId = getResources().getIdentifier(imageName, "drawable", getActivity().getPackageName());
            ((ImageView)badgeEntry.findViewById(R.id.image_badge)).setImageResource(imageResId);
            badgesLayout.addView(badgeEntry);
        }

    }

    @Override
    public void onRefresh() {
        loadUI();
    }


    class ProfileStat {
        public double week_distance, kcal, xp;

        public ProfileStat(double week_distance, double kcal, double xp) {
            this.week_distance = week_distance;
            this.kcal = kcal;
            this.xp = xp;
        }
    }

    class WeekRun {
        public Date date;
        public double distance, kcal, runs, xp;

        public WeekRun(Date date, double distance, double kcal, double runs, double xp) {
            this.date = date;
            this.distance = distance;
            this.kcal = kcal;
            this.runs = runs;
            this.xp = xp;
        }
    }
}
