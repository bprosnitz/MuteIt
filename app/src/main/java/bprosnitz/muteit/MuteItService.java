package bprosnitz.muteit;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.prefs.Preferences;

public class MuteItService extends AccessibilityService {
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes =  AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        setServiceInfo(info);
    }

    String lastMatch;
    Set<String> matches;
    private Set<String> matchStrings() {
        SharedPreferences prefs =  PreferenceManager.getDefaultSharedPreferences(this);
        String full = prefs.getString("muted_apps", "");
        if (lastMatch == full) {
            return  matches;
        }
        matches = new HashSet<String>();
        String[] parts = full.split("\n");
        for (String part : parts) {
            matches.add(part);
        }
        lastMatch = full;
        return matches;
    }

    private boolean activityMatches(String packageName) {
        return matchStrings().contains(packageName);
    }

    String lastMutedPackage;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            ComponentName componentName = new ComponentName(
                    event.getPackageName().toString(),
                    event.getClassName().toString()
            );

            try {
                ActivityInfo activityInfo = getPackageManager().getActivityInfo(componentName, 0);
                if (componentName.getPackageName() != lastMutedPackage) {
                    if (lastMutedPackage != null) {
                        Log.i("MuteIt", "Unmuting package: " + lastMutedPackage);
                        unmute();
                    }
                    lastMutedPackage = null;
                    if (activityInfo != null && activityMatches(componentName.getPackageName())) {
                        lastMutedPackage = componentName.getPackageName();
                        Log.i("MuteIt", "Muting package: " + componentName.getPackageName());
                        mute();
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
    }

    int musicVolume;
    int notificationVolume;
    int systemVolume;
    public void mute() {
        AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        musicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        notificationVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
        systemVolume = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0);
        audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0);
    }
    public void unmute() {
        AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, musicVolume, 0);
        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, notificationVolume, 0);
        audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, systemVolume, 0);
    }

    @Override
    public void onInterrupt() {

    }
}
