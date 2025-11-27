/*
 * Copyright (C) 2021-2025 Halcyon Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.internal.util.halcyon;

import android.content.ContentResolver;
import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utility to manage user-selected app spoofing.
 */
public class UserSelectedSpoofUtils {

    // Key in Settings.Secure to store user-selected apps
    private static final String SPOOFED_APPS_KEY = "HALCYON_SPOOFED_APPS";

    /**
     * Return true if this app should be spoofed dynamically by the user.
     */
    public static boolean shouldSpoofApp(Context context, String packageName) {
        if (context == null || packageName == null) return false;
        Set<String> apps = getUserSelectedApps(context);
        return apps.contains(packageName);
    }

    /**
     * Return the spoof profile for the given app if it exists.
     * Example: "PixelXL", "ROG6", etc.
     */
    public static String getSpoofProfile(Context context, String packageName) {
        if (context == null || packageName == null) return null;
        Map<String, String> map = getUserSelectedAppsMap(context);
        return map.get(packageName);
    }

    /**
     * Get the full map of user-selected apps to their spoof profile.
     */
    public static Map<String, String> getUserSelectedAppsMap(Context context) {
        Map<String, String> map = new HashMap<>();
        if (context == null) return map;

        ContentResolver cr = context.getContentResolver();
        String raw = Settings.Secure.getString(cr, SPOOFED_APPS_KEY);
        if (raw == null || raw.isEmpty()) return map;

        try {
            JSONObject obj = new JSONObject(raw);
            JSONArray keys = obj.names();
            if (keys != null) {
                for (int i = 0; i < keys.length(); i++) {
                    String pkg = keys.getString(i);
                    String profile = obj.getString(pkg);
                    map.put(pkg, profile);
                }
            }
        } catch (JSONException e) {
            // Ignore invalid JSON
        }

        return map;
    }

    /**
     * Return the set of all user-selected packages.
     */
    public static Set<String> getUserSelectedApps(Context context) {
        return getUserSelectedAppsMap(context).keySet();
    }

    private enum Action {
        ADD, REMOVE, SET
    }

    private static void putAppForUser(Context context, String packageName, String profile, int userId, Action action) {
        if (context == null || userId < 0) return;

        Map<String, String> apps = getUserSelectedAppsMap(context);

        switch (action) {
            case ADD:
                if (packageName != null && profile != null) {
                    apps.put(packageName, profile);
                }
                break;
            case REMOVE:
                if (packageName != null) {
                    apps.remove(packageName);
                }
                break;
            case SET:
                // Do nothing, placeholder
                break;
        }

        JSONObject json = new JSONObject(apps);
        Settings.Secure.putStringForUser(context.getContentResolver(), SPOOFED_APPS_KEY, json.toString(), userId);
    }

    /** Add an app to user-selected spoof list */
    public static void addApp(Context context, String packageName, String profile, int userId) {
        putAppForUser(context, packageName, profile, userId, Action.ADD);
    }

    /** Remove an app from user-selected spoof list */
    public static void removeApp(Context context, String packageName, int userId) {
        putAppForUser(context, packageName, null, userId, Action.REMOVE);
    }

    /** Reset all user-selected spoof apps for a user */
    public static void setApps(Context context, int userId) {
        putAppForUser(context, null, null, userId, Action.SET);
    }
}
