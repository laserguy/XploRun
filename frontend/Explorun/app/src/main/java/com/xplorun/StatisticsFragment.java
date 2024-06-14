package com.xplorun;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xplorun.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link StatisticsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StatisticsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "log_StatisticsFragment";

    View root;

    public StatisticsFragment() {
        // Required empty public constructor
    }

    public static StatisticsFragment newInstance() {
        StatisticsFragment fragment = new StatisticsFragment();
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
        root = inflater.inflate(R.layout.fragment_statistics, container, false);
        ((SwipeRefreshLayout)root).setOnRefreshListener(this);

        loadUI();

        return root;
    }

    public void loadUI() {
        ((SwipeRefreshLayout)root).setRefreshing(true);
        loadStats();
        ((SwipeRefreshLayout)root).setRefreshing(false);
    }

    public void loadStats() {
        VolleySingleton.getInstance(getActivity().getApplicationContext()).getOverallStats(UserSingleton.getInstance().getUser_id(), new VolleyListener() {
            @Override
            public void onError(String message) {
                Log.d(TAG, "Error in getOverallStats: "+message);
                Toast.makeText(getContext(), "Something went wrong loading your overall stats.",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject overallStatsJson = response.getJSONObject("overall_stats");

                    OverallStat overallStat = new OverallStat(
                        overallStatsJson.getDouble("best_pace"),
                        overallStatsJson.getDouble("distance"),
                        overallStatsJson.getDouble("kcal"),
                        overallStatsJson.getDouble("longest_distance"),
                        overallStatsJson.getDouble("nature_D"),
                        overallStatsJson.getDouble("urb_D"),
                        overallStatsJson.getDouble("xp")
                    );

                    displayOverallStats(overallStat);

                } catch (JSONException e) {
                    Log.d(TAG, "Error parsing getOverallStats json: "+e.getMessage());
                    Toast.makeText(getContext(), "Something went wrong loading your overall stats.",
                            Toast.LENGTH_SHORT).show();
                }

                try {
                    JSONObject runsJson = response.getJSONObject("runs");
                    ArrayList<RunStat> runStatsHistory = new ArrayList<>();
                    for (int i=0; i<runsJson.length(); i++) {
                        JSONObject runStatJson = runsJson.getJSONObject(String.valueOf(i));
                        runStatsHistory.add(new RunStat(
                                runStatJson.getDouble("avg_speed"),
                                runStatJson.getLong("date"),
                                runStatJson.getDouble("distance"),
                                runStatJson.getDouble("elevation"),
                                runStatJson.getDouble("kcal"),
                                runStatJson.getDouble("max_speed"),
                                runStatJson.getDouble("nature_D"),
                                runStatJson.getDouble("time_taken"),
                                runStatJson.getDouble("urb_D"),
                                runStatJson.getDouble("xp")
                        ));

                        displayRunHistory(runStatsHistory);
                    }
                } catch (JSONException e) {
                    Log.d(TAG, "Error parsing run history: "+e.getMessage());
                }
            }
        });
    }

    public void displayOverallStats(OverallStat overallStat) {
        String toDisplay = "<b>"+Utils.to2Dstring(overallStat.bestPace)+"</b> km/h";
        ((TextView) root.findViewById(R.id.text_best_pace)).setText(Html.fromHtml(toDisplay));
        toDisplay = "<b>"+Utils.to2Dstring(overallStat.distance/1000)+"</b> km";
        ((TextView) root.findViewById(R.id.text_total_distance)).setText(Html.fromHtml(toDisplay));
        toDisplay = "<b>"+Utils.to2Dstring(overallStat.kcal)+"</b> kcal";
        ((TextView) root.findViewById(R.id.text_total_calories)).setText(Html.fromHtml(toDisplay));
        toDisplay = "<b>"+Utils.to2Dstring(overallStat.longestDistance/1000)+"</b> km";
        ((TextView) root.findViewById(R.id.text_longest_distance)).setText(Html.fromHtml(toDisplay));
        toDisplay = "<b>"+Utils.to2Dstring(overallStat.natureD/1000)+"</b> km";
        ((TextView) root.findViewById(R.id.text_nature_D)).setText(Html.fromHtml(toDisplay));
        toDisplay = "<b>"+Utils.to2Dstring(overallStat.urbanD/1000)+"</b> km";
        ((TextView) root.findViewById(R.id.text_urban_D)).setText(Html.fromHtml(toDisplay));
        toDisplay = "<b>"+Utils.to2Dstring(overallStat.xp)+"</b>";
        ((TextView) root.findViewById(R.id.text_xp)).setText(Html.fromHtml(toDisplay));
    }

    public void displayRunHistory(ArrayList<RunStat> runStats) {

        LinearLayout runHistoryLayout = root.findViewById(R.id.layout_run_history);
        LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        runHistoryLayout.removeAllViews();
        for (int i=0; i<runStats.size(); i++) {
            View runHistoryEntryView = inflater.inflate(R.layout.layout_run_in_history, null);
            RunStat runStat = runStats.get(i);

            ((TextView)runHistoryEntryView.findViewById(R.id.text_run_title)).setText(
                    ZonedDateTime.ofInstant(Instant.ofEpochSecond(runStat.date), ZoneOffset.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
            );
            ((TextView)runHistoryEntryView.findViewById(R.id.text_distance)).setText(
                    String.format("%s km", Utils.to2Dstring(runStat.distance/1000))
            );
            ((TextView)runHistoryEntryView.findViewById(R.id.text_kcal)).setText(
                    String.format("%s kcal", Utils.to2Dstring(runStat.kcal))
            );
            ((TextView)runHistoryEntryView.findViewById(R.id.text_time_taken)).setText(
                    String.format("%s", LocalTime.ofSecondOfDay((long)runStat.timeTaken).toString())
            );
            runHistoryLayout.addView(runHistoryEntryView);
        }

    }

    @Override
    public void onRefresh() {
        loadUI();
    }

    class OverallStat {
        public double bestPace, distance, kcal, longestDistance, natureD, urbanD, xp;

        public OverallStat(double bestPace, double distance, double kcal, double longestDistance, double natureD, double urbanD, double xp) {
            this.bestPace = bestPace;
            this.distance = distance;
            this.kcal = kcal;
            this.longestDistance = longestDistance;
            this.natureD = natureD;
            this.urbanD = urbanD;
            this.xp = xp;
        }
    }

    class RunStat {
        public double averageSpeed, distance, elevation, kcal, maxSpeed, natureD, timeTaken, urbanD, xp;
        public long date;

        public RunStat(double averageSpeed, long date, double distance, double elevation, double kcal, double maxSpeed, double natureD, double timeTaken, double urbanD, double xp) {
            this.averageSpeed = averageSpeed;
            this.distance = distance;
            this.elevation = elevation;
            this.kcal = kcal;
            this.maxSpeed = maxSpeed;
            this.natureD = natureD;
            this.timeTaken = timeTaken;
            this.urbanD = urbanD;
            this.xp = xp;
            this.date = date;
        }
    }

}
