package com.shaivites.quizion.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.shaivites.quizion.R;
import com.shaivites.quizion.utils.DiceBearAvatarGenerator;
import com.shaivites.quizion.utils.PreferenceHelper;
import java.util.Locale;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private ImageView avatarImage;
    private TextView usernameText, levelText, xpText, streakText;
    private ProgressBar xpProgressBar;
    private LinearLayout strengthsContainer;

    // Views for Last Quiz Summary
    private TextView lastQuizTopicText, lastQuizDateText, lastQuizScoreText;
    private MaterialCardView lastQuizCard;
    private TextView profileStrengthsTitle; // Added this


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize views
        avatarImage = view.findViewById(R.id.profile_avatar_image);
        usernameText = view.findViewById(R.id.profile_username_text);
        levelText = view.findViewById(R.id.profile_level_text);
        xpText = view.findViewById(R.id.profile_xp_text);
        streakText = view.findViewById(R.id.profile_streak_text);
        xpProgressBar = view.findViewById(R.id.profile_xp_progress_bar);
        strengthsContainer = view.findViewById(R.id.profile_strengths_container);
        profileStrengthsTitle = view.findViewById(R.id.profile_strengths_title); // Initialize this

        // Initialize Last Quiz Summary views
        lastQuizTopicText = view.findViewById(R.id.last_quiz_topic_text);
        lastQuizDateText = view.findViewById(R.id.last_quiz_date_text);
        lastQuizScoreText = view.findViewById(R.id.last_quiz_score_text);
        lastQuizCard = view.findViewById(R.id.profile_last_quiz_card);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data every time the fragment becomes visible
        loadProfileData();
    }

    private void loadProfileData() {
        Context context = getContext();
        if (context == null) return;

        String username = PreferenceHelper.getUsername(context);
        usernameText.setText(username);

        // Avatar
        String avatarUrl = new DiceBearAvatarGenerator()
                .setSeed(username)
                .setSize(256)
                .setBackgroundColor("b6e3f4","c0aede","d1d4f9", "ffd5dc", "ffdfbf")
                .setBackgroundType("gradientLinear", "solid")
                .setEyes("variant01", "variant02", "variant03", "variant09", "variant12")
                .setMouth("variant01", "variant02", "variant03", "variant05", "variant09")
                .setShape("circle", "rounded", "square")
                .buildUrl()
                .replace("/svg", "/png");
        Glide.with(this)
                .load(avatarUrl)
                .apply(new RequestOptions()
                        .circleCrop()
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .error(R.drawable.ic_avatar_placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.ALL))
                .into(avatarImage);

        // XP, Level, Streak
        int currentXP = PreferenceHelper.getXP(context);
        int currentLevel = PreferenceHelper.getLevel(context);
        int currentStreak = PreferenceHelper.getStreak(context);

        levelText.setText(String.format(Locale.getDefault(), "Level: %d", currentLevel));

        int xpForCurrentLevelStart = (currentLevel - 1) * 100;
        int xpInCurrentLevelProgress = currentXP - xpForCurrentLevelStart;
        int xpNeededForNextLevelTotal = 100;

        xpText.setText(String.format(Locale.getDefault(), "XP: %d / %d", xpInCurrentLevelProgress, xpNeededForNextLevelTotal));
        xpProgressBar.setMax(xpNeededForNextLevelTotal);
        xpProgressBar.setProgress(xpInCurrentLevelProgress);

        streakText.setText(String.format(Locale.getDefault(), "Current Streak: %d days 🔥", currentStreak));

        loadSubjectStrengths(context);
        loadLastQuizData(context);
    }

    private void loadSubjectStrengths(Context context) {
        strengthsContainer.removeAllViews();

        Map<String, Map<String, String>> allStats = PreferenceHelper.getAllTopicStats(context);

        if (allStats.isEmpty()) {
            if (profileStrengthsTitle != null) profileStrengthsTitle.setVisibility(View.GONE); // Hide title if no stats
            TextView noStatsText = new TextView(context);
            noStatsText.setText("Play some quizzes to see your strengths!");
            noStatsText.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
            noStatsText.setTextColor(ContextCompat.getColor(context, R.color.md_theme_light_onSurfaceVariant));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 16, 0, 0);
            noStatsText.setLayoutParams(params);
            strengthsContainer.addView(noStatsText);
            return;
        }

        if (profileStrengthsTitle != null) profileStrengthsTitle.setVisibility(View.VISIBLE); // Show title if stats exist

        for (Map.Entry<String, Map<String, String>> entry : allStats.entrySet()) {
            String topic = entry.getKey();
            int correct = 0;
            int total = 0;
            try {
                correct = Integer.parseInt(entry.getValue().get("correct"));
                total = Integer.parseInt(entry.getValue().get("total"));
            } catch (NumberFormatException e) {
                // Log error or handle gracefully
            }


            if (total == 0) continue;

            View strengthItemView = LayoutInflater.from(context).inflate(R.layout.item_subject_strength, strengthsContainer, false);
            TextView topicNameText = strengthItemView.findViewById(R.id.subject_name_text);
            LinearProgressIndicator strengthIndicator = strengthItemView.findViewById(R.id.subject_progress_indicator);
            TextView strengthPercentageText = strengthItemView.findViewById(R.id.subject_percentage_text);

            topicNameText.setText(topic);
            int percentage = (int) (((float) correct / total) * 100);
            strengthIndicator.setProgressCompat(percentage, true);
            strengthPercentageText.setText(String.format(Locale.getDefault(), "%d%%", percentage));

            strengthsContainer.addView(strengthItemView);
        }
    }

    private void loadLastQuizData(Context context) {
        Map<String, String> lastSummary = PreferenceHelper.getLastQuizSummary(context);
        String topic = lastSummary.get("topic");

        if (topic != null && !topic.equals("N/A") && !topic.isEmpty()) {
            lastQuizCard.setVisibility(View.VISIBLE);
            lastQuizTopicText.setText(String.format(Locale.getDefault(), "Topic: %s", topic));
            lastQuizDateText.setText(String.format(Locale.getDefault(), "Played on: %s", lastSummary.get("date")));
            int scoreValue = 0;
            int correct = 0;
            int totalQuestions = 0;
            try {
                scoreValue = Integer.parseInt(lastSummary.get("score"));
                correct = Integer.parseInt(lastSummary.get("correct"));
                totalQuestions = Integer.parseInt(lastSummary.get("total_questions"));
            } catch (NumberFormatException e) {
                // Log or handle
            }

            lastQuizScoreText.setText(String.format(Locale.getDefault(), "Score: %d/%d (%d XP)",
                    correct, totalQuestions, scoreValue));
        } else {
            lastQuizCard.setVisibility(View.GONE);
        }
    }
}
