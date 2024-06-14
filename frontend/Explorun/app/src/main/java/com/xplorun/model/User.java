package com.xplorun.model;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToMany;
import io.objectbox.relation.ToOne;

@Entity
public class User {
    @Id(assignable = true)
    public long id;
    public String username;
    public double lastSpeed;
    public double lastDist;//last distance to next step

    public ToMany<Route> routes;
    public ToOne<Route> activeRoute;
    public ToMany<Route> completedRoutes;
    public ToOne<Settings> settings;

    public User() {
    }

    public User(long id, String username) {
        this.id = id;
        this.username = username;
    }


    public Settings settings() {
        if (settings.getTarget()==null) {
            settings.setTarget(new Settings(1L));
        }
        return settings.getTarget();
    }
}
