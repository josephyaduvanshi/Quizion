package com.shaivites.quizion.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log; // Added for logging date parse errors

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PreferenceHelper {
    private static final String PREF_NAME = "quizion_prefs";
    private static final String USERNAME_KEY = "username";
    private static final String XP_KEY = "user_xp";
    private static final String LEVEL_KEY = "user_level"; // Though level is calculated, can be stored if needed
    private static final String STREAK_KEY = "user_streak";
    private static final String LAST_PLAYED_DATE_KEY = "last_played_date_for_streak"; // For streak logic (YYYY-MM-DD)
    private static final String TOPIC_STATS_PREFIX = "topic_stats_";

    // Keys for last quiz summary
    private static final String LAST_QUIZ_SCORE_KEY = "last_quiz_score";
    private static final String LAST_QUIZ_CORRECT_KEY = "last_quiz_correct";
    private static final String LAST_QUIZ_TOTAL_QUESTIONS_KEY = "last_quiz_total_questions";
    private static final String LAST_QUIZ_TOPIC_KEY = "last_quiz_topic";
    private static final String LAST_QUIZ_DATE_KEY_DISPLAY = "last_quiz_date_display"; // For display with time

    private static final String NOTIFICATION_PREF_KEY = "notifications_enabled";


    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void saveUsername(Context context, String username) {
        getPrefs(context).edit().putString(USERNAME_KEY, username).apply();
    }

    public static String getUsername(Context context) {
        return getPrefs(context).getString(USERNAME_KEY, "Player#0000");
    }

    public static boolean isUserLoggedIn(Context context) {
        return getPrefs(context).contains(USERNAME_KEY);
    }

    // --- XP Methods ---
    public static void saveXP(Context context, int xp) {
        getPrefs(context).edit().putInt(XP_KEY, xp).apply();
    }

    public static int getXP(Context context) {
        return getPrefs(context).getInt(XP_KEY, 0);
    }

    public static void addXP(Context context, int xpToAdd) {
        int currentXP = getXP(context);
        saveXP(context, currentXP + xpToAdd);
        // Level is calculated dynamically, so no explicit saveLevel call needed here
        // unless you change the design to store level.
    }

    // --- Level Methods ---
    public static int getLevel(Context context) {
        return calculateLevel(getXP(context)); // Calculate on the fly
    }

    public static int calculateLevel(int xp) {
        // Example: 100 XP for level 2, 200 for level 3, etc. (XP is cumulative)
        // Level 1: 0-99 XP
        // Level 2: 100-199 XP
        // Level N: (N-1)*100 to N*100 - 1
        if (xp < 0) return 1; // Should not happen
        return (xp / 100) + 1;
    }

    // --- Streak Methods ---
    public static void saveStreak(Context context, int streak) {
        getPrefs(context).edit().putInt(STREAK_KEY, streak).apply();
    }

    public static int getStreak(Context context) {
        // Auto-reset streak if last played was not yesterday or today
        checkAndResetStreakIfNeeded(context);
        return getPrefs(context).getInt(STREAK_KEY, 0);
    }

    public static void saveLastPlayedDateForStreak(Context context, String date) { // date as YYYY-MM-DD
        getPrefs(context).edit().putString(LAST_PLAYED_DATE_KEY, date).apply();
    }

    public static String getLastPlayedDateForStreak(Context context) {
        return getPrefs(context).getString(LAST_PLAYED_DATE_KEY, "");
    }

    private static void checkAndResetStreakIfNeeded(Context context) {
        String lastPlayedDateStr = getLastPlayedDateForStreak(context);
        if (lastPlayedDateStr.isEmpty()) {
            return; // No streak to check or reset yet
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date lastPlayed = sdf.parse(lastPlayedDateStr);
            Calendar lastPlayedCal = Calendar.getInstance();
            lastPlayedCal.setTime(lastPlayed);

            Calendar todayCal = Calendar.getInstance();

            // Normalize to start of day for comparison
            lastPlayedCal.set(Calendar.HOUR_OF_DAY, 0);
            lastPlayedCal.set(Calendar.MINUTE, 0);
            lastPlayedCal.set(Calendar.SECOND, 0);
            lastPlayedCal.set(Calendar.MILLISECOND, 0);

            todayCal.set(Calendar.HOUR_OF_DAY, 0);
            todayCal.set(Calendar.MINUTE, 0);
            todayCal.set(Calendar.SECOND, 0);
            todayCal.set(Calendar.MILLISECOND, 0);

            // Calculate yesterday
            Calendar yesterdayCal = (Calendar) todayCal.clone();
            yesterdayCal.add(Calendar.DATE, -1);

            // If last played was neither today nor yesterday, reset streak
            if (!lastPlayedCal.equals(todayCal) && !lastPlayedCal.equals(yesterdayCal)) {
                saveStreak(context, 0);
                // Optionally clear LAST_PLAYED_DATE_KEY too if streak is 0
                // saveLastPlayedDateForStreak(context, ""); // Or keep it to know last ever play
            }
        } catch (java.text.ParseException e) {
            Log.e("PreferenceHelper", "Error parsing date for streak check", e);
            saveStreak(context, 0); // Reset streak on parse error
        }
    }


    // --- Topic Performance Stats ---
    public static void saveTopicStats(Context context, String topicTitle, int correctAnswers, int totalQuestions) {
        String key = TOPIC_STATS_PREFIX + topicTitle.replaceAll("\\s+", "_").replaceAll("[^a-zA-Z0-9_]", ""); // Sanitize
        String stats = correctAnswers + ":" + totalQuestions;
        getPrefs(context).edit().putString(key, stats).apply();
    }

    public static Map<String, String> getTopicStats(Context context, String topicTitle) {
        String key = TOPIC_STATS_PREFIX + topicTitle.replaceAll("\\s+", "_").replaceAll("[^a-zA-Z0-9_]", "");
        String statsStr = getPrefs(context).getString(key, "0:0");
        String[] parts = statsStr.split(":");
        Map<String, String> statsMap = new HashMap<>();
        if (parts.length == 2) {
            statsMap.put("correct", parts[0]);
            statsMap.put("total", parts[1]);
        } else {
            statsMap.put("correct", "0");
            statsMap.put("total", "0");
        }
        return statsMap;
    }

    public static void updateTopicStats(Context context, String topicTitle, boolean wasCorrect) {
        Map<String, String> currentStats = getTopicStats(context, topicTitle);
        int correct = Integer.parseInt(currentStats.get("correct"));
        int total = Integer.parseInt(currentStats.get("total"));

        if (wasCorrect) {
            correct++;
        }
        total++;
        saveTopicStats(context, topicTitle, correct, total);
    }

    public static Map<String, Map<String, String>> getAllTopicStats(Context context) {
        Map<String, ?> allEntries = getPrefs(context).getAll();
        Map<String, Map<String, String>> allTopicStats = new HashMap<>();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getKey().startsWith(TOPIC_STATS_PREFIX)) {
                String topicName = entry.getKey().substring(TOPIC_STATS_PREFIX.length()).replaceAll("_", " ");
                String[] parts = ((String) entry.getValue()).split(":");
                Map<String, String> statsMap = new HashMap<>();
                if (parts.length == 2) {
                    statsMap.put("correct", parts[0]);
                    statsMap.put("total", parts[1]);
                } else {
                    statsMap.put("correct", "0");
                    statsMap.put("total", "0");
                }
                allTopicStats.put(topicName, statsMap);
            }
        }
        return allTopicStats;
    }

    // --- Last Quiz Summary ---
    public static void saveLastQuizSummary(Context context, String topic, int score, int correctAnswers, int totalQuestions) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putString(LAST_QUIZ_TOPIC_KEY, topic);
        editor.putInt(LAST_QUIZ_SCORE_KEY, score);
        editor.putInt(LAST_QUIZ_CORRECT_KEY, correctAnswers);
        editor.putInt(LAST_QUIZ_TOTAL_QUESTIONS_KEY, totalQuestions);

        SimpleDateFormat sdfDisplay = new SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault());
        editor.putString(LAST_QUIZ_DATE_KEY_DISPLAY, sdfDisplay.format(new Date()));
        editor.apply();
    }

    public static Map<String, String> getLastQuizSummary(Context context) {
        Map<String, String> summary = new HashMap<>();
        summary.put("topic", getPrefs(context).getString(LAST_QUIZ_TOPIC_KEY, "N/A"));
        summary.put("score", String.valueOf(getPrefs(context).getInt(LAST_QUIZ_SCORE_KEY, 0)));
        summary.put("correct", String.valueOf(getPrefs(context).getInt(LAST_QUIZ_CORRECT_KEY, 0)));
        summary.put("total_questions", String.valueOf(getPrefs(context).getInt(LAST_QUIZ_TOTAL_QUESTIONS_KEY, 0)));
        summary.put("date", getPrefs(context).getString(LAST_QUIZ_DATE_KEY_DISPLAY, "N/A"));
        return summary;
    }


    // --- Notification Preference ---
    public static void saveNotificationPreference(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(NOTIFICATION_PREF_KEY, enabled).apply();
    }

    public static boolean getNotificationPreference(Context context) {
        return getPrefs(context).getBoolean(NOTIFICATION_PREF_KEY, true); // Default to true
    }


    // --- Clear User Progress (for Settings) ---
    public static void clearUserProgress(Context context) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putInt(XP_KEY, 0);
        // editor.putInt(LEVEL_KEY, 1); // Level is calculated
        editor.putInt(STREAK_KEY, 0);
        editor.remove(LAST_PLAYED_DATE_KEY);

        editor.remove(LAST_QUIZ_TOPIC_KEY);
        editor.remove(LAST_QUIZ_SCORE_KEY);
        editor.remove(LAST_QUIZ_CORRECT_KEY);
        editor.remove(LAST_QUIZ_TOTAL_QUESTIONS_KEY);
        editor.remove(LAST_QUIZ_DATE_KEY_DISPLAY);

        Map<String, ?> allEntries = getPrefs(context).getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getKey().startsWith(TOPIC_STATS_PREFIX)) {
                editor.remove(entry.getKey());
            }
        }
        editor.apply();
    }

    // --- Logout (for Settings) ---
    public static void logoutUser(Context context) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.remove(USERNAME_KEY);
        // Also clear progress
        editor.putInt(XP_KEY, 0);
        // editor.putInt(LEVEL_KEY, 1);
        editor.putInt(STREAK_KEY, 0);
        editor.remove(LAST_PLAYED_DATE_KEY);

        editor.remove(LAST_QUIZ_TOPIC_KEY);
        editor.remove(LAST_QUIZ_SCORE_KEY);
        editor.remove(LAST_QUIZ_CORRECT_KEY);
        editor.remove(LAST_QUIZ_TOTAL_QUESTIONS_KEY);
        editor.remove(LAST_QUIZ_DATE_KEY_DISPLAY);

        Map<String, ?> allEntries = getPrefs(context).getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getKey().startsWith(TOPIC_STATS_PREFIX)) {
                editor.remove(entry.getKey());
            }
        }
        // Keep notification preference or clear it? User choice, for now, let's keep it.
        editor.apply();
    }
}