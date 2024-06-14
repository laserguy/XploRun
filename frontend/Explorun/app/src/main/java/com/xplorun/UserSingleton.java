package com.xplorun;

import android.util.Log;

import java.util.ArrayList;

public class UserSingleton {

    private static final String TAG = "log_UserSingleton";

    public static UserSingleton instance;

    private String username, user_id, gender;
    Integer age, height, weight;
    private ArrayList<Badge> badges = new ArrayList<>();

    private UserSingleton() { }

    public static synchronized UserSingleton getInstance() {
        if (instance == null) {
            instance = new UserSingleton();
        }
        return instance;
    }

    public void setUserData(String username, String user_id, String gender, int age, int height, int weight) {
        this.username = username;
        this.user_id = user_id;
        this.gender = gender;
        this.age = age;
        this.height = height;
        this.weight = weight;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }


    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public ArrayList<Badge> getBadges() {
        return badges;
    }

    public void addBadge(Badge badge) {
        for (Badge existingBadge : badges) {
            if (badge.badgeID == existingBadge.badgeID) {
                Log.d(TAG, "Badge already exists.");
                return;
            }
        }
        badges.add(badge);
    }

    public void clearBadges() {
        badges.clear();
    }

    public static class Badge {
        public int badgeID;
        public long expiryDate;

        public Badge(int badgeID, long expiryDate) {
            this.badgeID = badgeID;
            this.expiryDate = expiryDate;
        }
    }
}
