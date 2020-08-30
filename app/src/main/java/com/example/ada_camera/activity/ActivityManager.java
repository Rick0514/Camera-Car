package com.example.ada_camera.activity;

import android.app.Activity;

import java.util.ArrayList;
import java.util.List;

public class ActivityManager {

    private List<Activity> activitieList = new ArrayList<>();

    private static final ActivityManager mActivityManager = new ActivityManager();
    private ActivityManager(){}

    public static ActivityManager getInstance(){
        return mActivityManager;
    }

    public void addActivity(Activity activity){
        activitieList.add(activity);
    }

    public void finishAllActivities(){
        for(Activity activity : activitieList){
            if (!activity.isFinishing()) {
                activity.finish();
            }
        }
    }

}
