package com.xplorun.tree;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.xplorun.R;
import com.xplorun.model.DB;
import com.xplorun.model.Tree;

import java.util.Random;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PlantTreeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
@AndroidEntryPoint
public class PlantTreeFragment extends Fragment {

    @Inject
    DB db;

    public PlantTreeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * <p>
     *
     * @return A new instance of fragment PlantTreeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PlantTreeFragment newInstance() {
        PlantTreeFragment fragment = new PlantTreeFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the root for this fragment
        var root = inflater.inflate(R.layout.fragment_plant_tree, container, false);
        WebView tree = root.findViewById(R.id.tree);
        tree.getSettings().setJavaScriptEnabled(true);
        var params = new TreeParams();
//        params.seed = db.activeTree().seed;
//        params.progress = db.activeTree().progress;
        tree.addJavascriptInterface(params, "treeParams");
        final var fileUrl = "file:///android_asset/tree/index.html";
        tree.loadUrl(fileUrl);

        var plantTree = root.findViewById(R.id.plant_tree_button);
        var step = root.findViewById(R.id.step);
        ProgressBar progress = root.findViewById(R.id.progressBar);

        tree.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (fileUrl.equals(url)) {
                    if (db.activeTree() != null) {
                        params.seed = db.activeTree().seed;
                        params.oldProgress = db.activeTree().progress;
                        tree.loadUrl("javascript:restoreTree()");
                        progress.setProgress((int) (100 * params.oldProgress / params.maxLevel), true);
                    } else {
                        newTree(params);
                    }
                }
            }
        });

        step.setOnClickListener(v -> {
            tree.loadUrl("javascript:step()");
            setProgress(params, progress);
        });

        plantTree.setOnClickListener(v -> {
            newTree(params);
            progress.setProgress(0, true);
            tree.loadUrl("javascript:plantTree()");
        });

        return root;
    }

    private void setProgress(TreeParams params, ProgressBar progress) {
        progress.setProgress(getProgress(params), true);
        var activeTree = db.activeTree();
        activeTree.progress = params.progress;
        db.update(activeTree);
    }

    private void newTree(TreeParams params) {
        params.seed = new Random().nextInt(10000);
        var dbTree = new Tree(Tree.SINGLE_ID, params.seed, 0);
        db.update(dbTree);
    }

    private int getProgress(TreeParams params) {
        return (int) (params.progress / params.maxLevel * 100);
    }

    static class TreeParams {
        int seed = 80;//new Random().nextInt(100);
        int tree_size = 100;
        int maxLevel = 9;
        double rotationAngle = Math.PI / 2 / 4;
        double lengthRand = 1;
        double branchProb = 1;
        double rotationRand = 0.2;
        double leafProb = 0.6;
        double progress = 0;
        double oldProgress;

        @JavascriptInterface
        public void setProgress(double progress) {
            this.progress = progress;
        }

        @JavascriptInterface
        public double getOldProgress() {
            return oldProgress;
        }

        @JavascriptInterface
        public int getSeed() {
            return seed;
        }

        @JavascriptInterface
        public int getTreeSize() {
            return tree_size;
        }

        @JavascriptInterface
        public int getMaxLevel() {
            return maxLevel;
        }

        @JavascriptInterface
        public double getRotationAngle() {
            return rotationAngle;
        }

        @JavascriptInterface
        public double getLengthRand() {
            return lengthRand;
        }

        @JavascriptInterface
        public double getBranchProb() {
            return branchProb;
        }

        @JavascriptInterface
        public double getRotationRand() {
            return rotationRand;
        }

        @JavascriptInterface
        public double getLeafProb() {
            return leafProb;
        }

        @JavascriptInterface
        public String toString() { return "treeParams"; }
    }
}
