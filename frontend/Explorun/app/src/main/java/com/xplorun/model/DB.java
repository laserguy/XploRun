package com.xplorun.model;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.xplorun.Utils;

import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadNode;

import java.util.ArrayList;
import java.util.List;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.relation.ToOne;

public class DB {
    private final BoxStore store;
    private final Box<User> userBox;
    private final Box<Route> routeBox;
    private final Box<Step> stepBox;
    private final Box<RunStat> statBox;
    private final Box<Tree> treeBox;
    private final long USER_ID = 1L;
    public static DB instance;
    private User user;

    public synchronized static DB get(Context context) {
        if (instance == null)
            instance = new DB(context);
        return instance;
    }

    private DB(Context context) {
        this.store = MyObjectBox.builder()
                                // TODO provide File object for backup purposes
                                //.initialDbFile(new File(path))
                                .androidContext(context)
                                .build();
        this.userBox = store.boxFor(User.class);
        this.routeBox = store.boxFor(Route.class);
        this.stepBox = store.boxFor(Step.class);
        this.statBox = store.boxFor(RunStat.class);
        this.treeBox = store.boxFor(Tree.class);
        if (userBox.isEmpty()) {
            var user = new User(USER_ID, "user");
            user.settings.setTarget(new Settings(1L));
            userBox.put(user);
        }
    }

    public void update(User user) {
        userBox.put(user);
    }

    public User user() {
        user = user == null ? userBox.get(USER_ID) : user;
        return user;
    }

    public void activeRoute(Route route) {
        User user = user();
        user.activeRoute.setTarget(route);
        if (route != null && !route.steps.isEmpty())
            route.nextStep(route.steps.get(0));
        else
            Log.e("DB", "route steps is empty");
        update(user);
    }

    public Route activeRoute() {
        return user().activeRoute.getTarget();
    }

    public List<Route> routes() {
        return user().routes;
    }

    public void update(Route route) {
        routeBox.put(route);
    }

    public void update(Step step) {
        stepBox.put(step);
    }

    public void addRoute(Route route) {
        user().routes.add(route);
    }

    public Route create(Road road) {
        var route = new Route();
        route.currentOrder = Integer.MIN_VALUE;
        route.road = road;
        route.started = false;
        List<RoadNode> mNodes = road.mNodes;//mergeRadius(road.mNodes, 13);
        for (int i = 0, size = mNodes.size(); i < size; i++) {
            RoadNode mNode = mNodes.get(i);
            var step = new Step(mNode.mLocation,
                    mNode.mLength * 1000,
                    mNode.mDuration, mNode.mManeuverType, i);
            step.ttsInstruction = Utils.inst2text(mNode);
            step.uiInstruction = Utils.inst2template(mNode);

            route.steps.add(step);
            if (i > 0)
                route.steps.get(i - 1).nextStep.setTarget(step);
        }
        routeBox.put(route);
        return route;
    }

    private List<RoadNode> mergeRadius(List<RoadNode> mNodes, double dist) {
        var result = new ArrayList<RoadNode>();
        var size = mNodes.size();
        for (int i = 0; i < mNodes.size() - 1; i++) {
            var location = mNodes.get(i).mLocation;
            if (!(location.distanceToAsDouble(mNodes.get(i + 1).mLocation) < dist)) continue;
            var start = i;
            var j = i + 1;
            while (j < size && location.distanceToAsDouble(mNodes.get(j).mLocation) < dist) {
                j++;
            }
            var end = j - 1;
            if (start != end) {
                Log.d("DB.merge", String.format("merging %d-%d", start, end));
                RoadNode node = merge(mNodes, start, end);
                result.add(node);
            } else result.add(mNodes.get(i));
            i = j - 1;
        }

        return result;
    }

    @NonNull
    private RoadNode merge(List<RoadNode> mNodes, int start, int end) {
        var node = new RoadNode();
        node.mLocation = mNodes.get(start).mLocation;
        node.mLength = mNodes.get(start).mLength + mNodes.get(end).mLength;
        node.mDuration = mNodes.get(start).mDuration + mNodes.get(end).mDuration;
        node.mManeuverType = mNodes.get(start).mManeuverType;
        node.mInstructions = String.format("%s \n and then \n %s",
                mNodes.get(start).mInstructions,
                mNodes.get(end).mInstructions);
        return node;
    }

    public void update(RunStat stat) {
        statBox.put(stat);
    }

    public Tree activeTree() {
        return treeBox.get(Tree.SINGLE_ID);
    }

    public void update(Tree tree) {
        try{
            treeBox.put(tree);
        } catch (IllegalArgumentException e) {
            treeBox.removeAll();
            treeBox.put(tree);
        }
    }
}
