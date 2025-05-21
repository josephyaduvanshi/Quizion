package com.shaivites.quizion.utils;

import android.content.Context;

public class PreferenceHelper {
    private static final String PREF_NAME = "quizion_prefs";

    // Save username to SharedPreferences
    public static void saveUsername(Context context, String username) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString("username", username).apply();
    }

    // Get the saved username from SharedPreferences
    public static String getUsername(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString("username", "Player#0000");
    }

    // Check if user is already logged in
    public static boolean isUserLoggedIn(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .contains("username");
    }
}
