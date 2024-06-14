package com.xplorun;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.TimeUtils;

import com.xplorun.R;
import com.xplorun.model.DB;
import com.xplorun.model.Route;
import com.xplorun.model.RunStat;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.xplorun.model.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class RunCompletedActivity extends AppCompatActivity {

    private static final String TAG = "log_RunCompletedActivity";

    // UI elements
    LineChart analysisChart;
    MaterialButton closeButton;
    MaterialButtonToggleGroup feedbackToggle;
    TextView textElevation, textDistance, textTime, textCalories, textAvgSpeed, textTopSpeed;

    // Route
    Route route;
    RunStat stat;

    // Chart
    LineData lineData = new LineData();
    List<Utils.ElevationEntry> elevationHist = new ArrayList<Utils.ElevationEntry>();

    @Inject
    DB db;
    private TimeUtils DurationFormatUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run_completed);

        analysisChart = findViewById(R.id.chart_run_analysis);
        closeButton = findViewById(R.id.button_close);
        feedbackToggle = findViewById(R.id.toggle_feedback);
        textElevation = findViewById(R.id.text_elevation);
        textDistance = findViewById(R.id.text_distance);
        textTime = findViewById(R.id.text_time);
        textCalories = findViewById(R.id.text_calories);
        textAvgSpeed = findViewById(R.id.text_avg_speed);
        textTopSpeed = findViewById(R.id.text_top_speed);

        closeButton.setOnClickListener(view -> closeButtonPressed());

        route = db.activeRoute();
        stat = route.overallStat.getTarget();
        long runDuration = stat.endTime-stat.startTime;
        int kcalBurned = (int) (stat.distance*(float)UserSingleton.getInstance().getWeight() * 1.036f / 1000);

        // Gather and dispatch stats
        JSONObject runStatsJson = new JSONObject();
        try {
            runStatsJson.put("run_finished", getIntent().getBooleanExtra("run_finished",true));
            runStatsJson.put("user_id", UserSingleton.getInstance().getUser_id());
            runStatsJson.put("distance", stat.distance);
            runStatsJson.put("elevation", stat.elevation);
            runStatsJson.put("avg_speed", stat.avgSpeed);
            runStatsJson.put("time_taken", runDuration);
            runStatsJson.put("max_speed", stat.topSpeed);
            runStatsJson.put("nature_D", stat.natureDistance);
            runStatsJson.put("urb_D", stat.urbanDistance);
            runStatsJson.put("kcal", kcalBurned);

            dispatchRunStats(runStatsJson);
        }
        catch (JSONException e) {Log.d(TAG, "Error creating stats json: "+e.getMessage());}
        catch (Exception e) {Log.d(TAG, "Error dispatch stats: "+e.getMessage());}

        // Display stats
        textElevation.setText(Utils.to2Dstring(stat.elevation)+" m");
        textDistance.setText(Utils.to2Dstring(stat.distance)+" m");
        textTime.setText(LocalTime.ofSecondOfDay(Math.toIntExact(runDuration)).toString());
        textCalories.setText(Utils.to2Dstring(kcalBurned)+" kcal");
        textAvgSpeed.setText(Utils.to2Dstring(stat.avgSpeed)+" m/s");
        textTopSpeed.setText(Utils.to2Dstring(stat.topSpeed)+" m/s");
        textCalories.setText(Utils.to2Dstring(kcalBurned)+" kcals");

        try {
            elevationHist = (List<Utils.ElevationEntry>) getIntent().getSerializableExtra("elevation_hist");

            // Fill in possible missing environment value
            String lastEnvironment = "OTHER";
            for (Utils.ElevationEntry entry : elevationHist) {
                if (entry.environment != null) {
                    lastEnvironment = entry.environment;
                    break;
                }
            }
            for (Utils.ElevationEntry entry : elevationHist) {
                if (entry.environment != null) {
                    lastEnvironment = entry.environment;
                }
                else {
                    entry.environment = lastEnvironment;
                }
            }

            List<Integer> indexes = new ArrayList<Integer>();
            int index = 0;

            Log.d(TAG, "elevation hist size: "+elevationHist.size());
            while (index<elevationHist.size()-1) {
                indexes.add(index);
                if (elevationHist.get(index).environment == elevationHist.get(index+1).environment) {
                    index++;
                    continue;
                }
                else {
                    indexes.add(index+1);
                    Log.d(TAG, "Adding datapoints: "+indexes.toString());
                    Log.d(TAG, "Current index: "+index);
                    addDataPoints(indexes);
                    indexes.clear();
                    index++;
                }
            }

            if (indexes.size() > 2) addDataPoints(indexes);
            Log.d(TAG, "Adding datapoints finished on index: "+index);

            AnalysisChartYAxisFormatter formatter = new AnalysisChartYAxisFormatter();
            analysisChart.getLegend().setCustom(Arrays.asList(
                    new LegendEntry("Nature", Legend.LegendForm.DEFAULT, Float.NaN, Float.NaN, null, getColor(R.color.analysis_chart_nature)),
                    new LegendEntry("Urban", Legend.LegendForm.DEFAULT, Float.NaN, Float.NaN, null, getColor(R.color.analysis_chart_urban)),
                    new LegendEntry("Other", Legend.LegendForm.DEFAULT, Float.NaN, Float.NaN, null, getColor(R.color.analysis_chart_other))
                    ));
            analysisChart.setData(lineData);
            analysisChart.setDrawMarkers(false);
            analysisChart.animateY(500);
            analysisChart.getDescription().setEnabled(false);
            analysisChart.getAxisRight().setEnabled(false);
            analysisChart.getXAxis().setEnabled(false);
            analysisChart.getAxisLeft().setValueFormatter(formatter);
            analysisChart.invalidate();
        } catch (Exception e) {
            findViewById(R.id.chart_run_analysis).setVisibility(View.GONE);
            Log.d(TAG, "Error creating chart. "+e.getMessage());
        }
    }

    public void closeButtonPressed() {
        // Handle rating toggle
        switch (feedbackToggle.getCheckedButtonId()) {
            case R.id.button_like:
                handleLike();
                break;
            default:
                break;
        }
        Log.d(TAG, String.valueOf(feedbackToggle.getCheckedButtonId()));
        finishThisActivity();
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
                Toast.makeText(getApplicationContext(), "Thanks for rating! Suggested routes will be recalculated based on your feedback.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    public void addDataPoints(List<Integer> toConnect) {
        List<Entry> entries = new ArrayList<Entry>();
        LineDataSet lineDataSet;

        for (int i=0; i<toConnect.size(); i++) {
            entries.add(new Entry(toConnect.get(i), elevationHist.get(toConnect.get(i)).altitude));
        }

        switch (elevationHist.get((toConnect.get(0))).environment) {
            case "URBAN":
                lineDataSet = new LineDataSet(entries, "URBAN");
                lineDataSet.setFillColor(getColor(R.color.analysis_chart_urban));
                break;
            case "NATURE":
                lineDataSet = new LineDataSet(entries, "NATURE");
                lineDataSet.setFillColor(getColor(R.color.analysis_chart_nature));
                break;
            default:
                lineDataSet = new LineDataSet(entries, "OTHER");
                lineDataSet.setFillColor(getColor(R.color.analysis_chart_other));
        }

        lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        lineDataSet.setDrawFilled(true);
        lineData.addDataSet(lineDataSet);
    }

    public void dispatchRunStats (JSONObject runStatsJson) {
        VolleySingleton.getInstance(getApplicationContext()).dispatchRunInfo(runStatsJson, new VolleyListener() {
            @Override
            public void onError(String message) {
                Log.d(TAG, "Error dispatching stats: "+message);
            }

            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, "Run stats dispatched");

                try {
                    boolean newBadge=false;
                    for (int i=0; i<response.length(); i++) {
                        JSONObject badgeJson = response.getJSONObject(String.valueOf(i));
                        UserSingleton.Badge badge = new UserSingleton.Badge(badgeJson.getInt("badge_id"), badgeJson.getLong("expire_ts"));
                        UserSingleton.getInstance().addBadge(badge);
                        if (UserSingleton.getInstance().getBadges().contains(badge)) newBadge=true;
                    }
                    if (newBadge) {
                        Toast.makeText(getApplicationContext(), "Congrats, you got a new badge! Check it out in your profile.",
                                Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    Log.d(TAG, "Error parsing badges: "+e.getMessage());
                }
            }
        });
    }

    public class AnalysisChartYAxisFormatter extends ValueFormatter {

        @Override
        public String getAxisLabel(float value, AxisBase axis) {
            return Math.round(value)+" m";
        }

        @Override
        public String getFormattedValue(float value) {
            return Math.round(value)+" m";
        }

    }

    public void finishThisActivity() {
        setResult(Params.RUN_COMPLETED_FINISHED);
        finish();
    }

    @Override
    public void onBackPressed() {
        finishThisActivity();
    }
}
