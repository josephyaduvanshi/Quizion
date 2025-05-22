package com.shaivites.quizion.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.shaivites.quizion.MainActivity;
import com.shaivites.quizion.R;
import com.shaivites.quizion.utils.PreferenceHelper; // If needed for streak directly

import java.util.Locale;

public class QuizSummaryActivity extends AppCompatActivity {

    private TextView summaryTitleText, summaryTopicText, summaryScoreText,
            summaryCorrectAnswersText, summaryXpGainedText, summaryStreakText;
    private MaterialButton doneButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_summary);

        summaryTitleText = findViewById(R.id.summary_title_text);
        summaryTopicText = findViewById(R.id.summary_topic_text);
        summaryScoreText = findViewById(R.id.summary_score_text);
        summaryCorrectAnswersText = findViewById(R.id.summary_correct_answers_text);
        summaryXpGainedText = findViewById(R.id.summary_xp_gained_text);
        summaryStreakText = findViewById(R.id.summary_streak_text);
        doneButton = findViewById(R.id.summary_done_button);

        Intent intent = getIntent();
        String topic = intent.getStringExtra("QUIZ_TOPIC");
        int score = intent.getIntExtra("QUIZ_SCORE", 0);
        int totalQuestions = intent.getIntExtra("TOTAL_QUESTIONS", 0);
        int correctAnswers = score / 10; // Assuming 10 points per question
        int xpGained = intent.getIntExtra("XP_GAINED", 0);
        int currentStreak = PreferenceHelper.getStreak(this); // Get updated streak

        summaryTopicText.setText(String.format(Locale.getDefault(), "Topic: %s", topic));
        summaryScoreText.setText(String.format(Locale.getDefault(), "Your Score: %d / %d", score, totalQuestions * 10));
        summaryCorrectAnswersText.setText(String.format(Locale.getDefault(), "Correct Answers: %d out of %d", correctAnswers, totalQuestions));
        summaryXpGainedText.setText(String.format(Locale.getDefault(), "XP Gained: +%d XP", xpGained));
        summaryStreakText.setText(String.format(Locale.getDefault(), "Current Streak: %d days 🔥", currentStreak));

        if (correctAnswers >= totalQuestions / 2) { // Example: if more than half correct
            summaryTitleText.setText("Great Job!");
            // You could also change Lottie animation here if you have different ones
        } else {
            summaryTitleText.setText("Quiz Completed!");
        }


        doneButton.setOnClickListener(v -> {
            // Navigate back to MainActivity (HomeFragment)
            Intent mainIntent = new Intent(QuizSummaryActivity.this, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(mainIntent);
            finish(); // Close this summary activity
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Ensure pressing back also leads to MainActivity cleanly
        Intent mainIntent = new Intent(QuizSummaryActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(mainIntent);
        finish();
    }
}