package com.xplorun;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextSwitcher;

import androidx.fragment.app.Fragment;

import com.xplorun.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

public class LoadingTipsFragment extends Fragment {

    private static final String TAG="log_LoadingTipsFragment";
    private ArrayList<JSONObject> runningTipsArrayList = new ArrayList<>();
    int tipsCount = 0;
    int currentTipIndex = 0;

    Timer timer;
    TimerTask timerTask;

    public LoadingTipsFragment() {
        // Required empty public constructor
    }

    public static LoadingTipsFragment newInstance() {
        LoadingTipsFragment fragment = new LoadingTipsFragment();
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

        View v = inflater.inflate(R.layout.fragment_loading_tips, container, false);

        loadTips();
        shuffleTips();

        TextSwitcher textSwitcherTipTitle = v.findViewById(R.id.text_switcher_tip_title);
        TextSwitcher textSwitcherTipText = v.findViewById(R.id.text_switcher_tip_text);
        TextSwitcher textSwitcherTipSource = v.findViewById(R.id.text_switcher_tip_source);

        timer = new Timer();
        timerTask = new TimerTask() {
            public void run() {
                getActivity().runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        try {
                            textSwitcherTipTitle.setText(runningTipsArrayList.get(currentTipIndex).getString("title"));
                            textSwitcherTipText.setText(runningTipsArrayList.get(currentTipIndex).getString("tip"));
                            if (runningTipsArrayList.get(currentTipIndex).getInt("id") <= 11) textSwitcherTipSource.setText("https://www.healthdirect.gov.au/running-tips");
                            else textSwitcherTipSource.setText("");

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        currentTipIndex = (currentTipIndex + 1) % tipsCount;
                    }
                });

            }};

        timer.schedule(timerTask, 0, 10000);

        return v;
    }

    public void loadTips() {
        JSONObject allTipsJson = null;
        Writer writer = new StringWriter();
        char[] buffer = new char[1024];
        try {
            InputStream is = getContext().getAssets().open("running_tips.json");
            Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
            is.close();
            allTipsJson = new JSONObject(writer.toString());
        } catch (IOException | JSONException e) {
            Log.d(TAG, e.getMessage());
        }

        JSONObject tipJson = null;
        tipsCount = 0;
        while (true) {
            try {
                tipJson = allTipsJson.getJSONObject(String.valueOf(tipsCount));
                this.runningTipsArrayList.add(tipJson);
                tipsCount++;
            } catch (JSONException e) {
                // Expected
                return;
            } catch (Exception e) {
                // Unexpected
                return;
            }
        }
    }

    public void shuffleTips() {
        Collections.shuffle(runningTipsArrayList);
    }

    @Override
    public void onStop() {
        super.onStop();
        if(timerTask != null) timerTask.cancel();
    }
}
