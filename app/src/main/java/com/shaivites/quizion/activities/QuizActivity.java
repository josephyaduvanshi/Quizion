package com.shaivites.quizion.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.shaivites.quizion.R;
import com.shaivites.quizion.models.QuizQuestion;
import com.shaivites.quizion.networking.GeminiApiService;
import com.shaivites.quizion.utils.PreferenceHelper; // Import PreferenceHelper

import java.lang.reflect.Type;
import java.text.SimpleDateFormat; // For streak logic
import java.util.ArrayList;
import java.util.Calendar; // For streak logic
import java.util.Date; // For streak logic
import java.util.List;
import java.util.Locale;
import java.util.Map; // For getAllTopicStats
import java.util.concurrent.TimeUnit;

public class QuizActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "QuizActivity";

    private LinearProgressIndicator progressIndicator;
    private ImageButton buttonExit;
    private TextView textViewScore;
    private TextView textViewTimer;
    private TextView textViewQuestionNumber;
    private TextView textViewQuestion;
    private MaterialButton buttonOption1, buttonOption2, buttonOption3, buttonOption4;
    private MaterialButton buttonSubmitNext;
    private MaterialCardView cardQuestion;
    private ProgressBar progressBarLoading;
    private Group groupQuizContent;

    private String quizMode = "QUICK";
    private String topicTitle = "General Knowledge";
    private int currentQuestionIndex = 0;
    private int score = 0;
    private int totalQuestions = 0;
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis;
    private static final long DEFAULT_TIME_PER_QUESTION = 30000; // 30 seconds

    private List<QuizQuestion> questionList = new ArrayList<>();
    private MaterialButton selectedOptionButton = null;
    private boolean answerSubmitted = false;

    private GeminiApiService geminiApiService;
    private Gson gson;
    private Handler mainThreadHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        try {
            geminiApiService = new GeminiApiService();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Failed to initialize GeminiApiService", e);
            Toast.makeText(this, "Error initializing quiz service: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        gson = new Gson();
        mainThreadHandler = new Handler(Looper.getMainLooper());

        Intent intent = getIntent();
        quizMode = intent.getStringExtra("QUIZ_MODE") != null ? intent.getStringExtra("QUIZ_MODE") : "QUICK";
        topicTitle = intent.getStringExtra("TOPIC_TITLE") != null ? intent.getStringExtra("TOPIC_TITLE") : "General Knowledge";
        Log.i(TAG, "Starting Quiz - Mode: " + quizMode + ", Topic: " + topicTitle);

        findViews();
        setupListeners();
        loadQuestions();
    }

    private void findViews() {
        progressIndicator = findViewById(R.id.quiz_progress_indicator);
        buttonExit = findViewById(R.id.button_exit_quiz);
        textViewScore = findViewById(R.id.text_view_score);
        textViewTimer = findViewById(R.id.text_view_timer);
        cardQuestion = findViewById(R.id.card_question);
        textViewQuestionNumber = findViewById(R.id.text_view_question_number);
        textViewQuestion = findViewById(R.id.text_view_question);
        buttonOption1 = findViewById(R.id.button_option_1);
        buttonOption2 = findViewById(R.id.button_option_2);
        buttonOption3 = findViewById(R.id.button_option_3);
        buttonOption4 = findViewById(R.id.button_option_4);
        buttonSubmitNext = findViewById(R.id.button_submit_answer);
        progressBarLoading = findViewById(R.id.progress_bar_loading);
        groupQuizContent = findViewById(R.id.group_quiz_content);

        showLoading(true);
        updateScoreUI();
    }

    private void setupListeners() {
        buttonExit.setOnClickListener(v -> finish());
        buttonOption1.setOnClickListener(this);
        buttonOption2.setOnClickListener(this);
        buttonOption3.setOnClickListener(this);
        buttonOption4.setOnClickListener(this);
        buttonSubmitNext.setOnClickListener(this);
    }

    private void loadQuestions() {
        Log.i(TAG, "Requesting questions from Gemini API for topic: " + topicTitle);
        showLoading(true);
        int numberOfQuestions = 10;
        String difficulty = "Medium";

        Map<String, Map<String, String>> allUserStats = PreferenceHelper.getAllTopicStats(this);

        geminiApiService.generateQuizQuestions(topicTitle, difficulty, numberOfQuestions, allUserStats, new GeminiApiService.GeminiCallback() {
            @Override
            public void onSuccess(String generatedJson) {
                mainThreadHandler.post(() -> {
                    Log.d(TAG, "Received JSON from Gemini: " + generatedJson);
                    try {
                        Type questionListType = new TypeToken<ArrayList<QuizQuestion>>(){}.getType();
                        List<QuizQuestion> parsedQuestions = gson.fromJson(generatedJson, questionListType);

                        if (parsedQuestions != null && !parsedQuestions.isEmpty()) {
                            Log.i(TAG, "Successfully parsed " + parsedQuestions.size() + " questions.");
                            questionList = parsedQuestions;
                            totalQuestions = questionList.size();
                            progressIndicator.setMax(totalQuestions);
                            currentQuestionIndex = 0;
                            score = 0;
                            updateScoreUI();

                            showLoading(false);
                            displayQuestion();
                            updateProgressIndicator();
                        } else {
                            Log.w(TAG, "Parsed questions list is null or empty after JSON parsing.");
                            handleQuestionLoadError("Received empty or invalid question data.");
                        }
                    } catch (JsonSyntaxException e) {
                        Log.e(TAG, "Error parsing JSON response from Gemini", e);
                        handleQuestionLoadError("Failed to parse questions. Invalid format received.");
                    } catch (Exception e) {
                        Log.e(TAG, "Unexpected error processing questions after fetch", e);
                        handleQuestionLoadError("An unexpected error occurred while preparing questions.");
                    }
                });
            }

            @Override
            public void onError(Throwable throwable) {
                Log.e(TAG, "Error generating questions via Gemini API", throwable);
                mainThreadHandler.post(() -> {
                    showLoading(false);
                    handleQuestionLoadError("Error fetching questions: " + throwable.getMessage());
                });
            }
        });
    }

    private void handleQuestionLoadError(String message) {
        Toast.makeText(QuizActivity.this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    private void displayQuestion() {
        if (questionList == null || questionList.isEmpty()) {
            Log.e(TAG, "Attempted to display question, but question list is null or empty.");
            handleQuestionLoadError("No questions available to display.");
            return;
        }
        if (currentQuestionIndex >= questionList.size()) {
            Log.i(TAG, "Reached end of question list.");
            finishQuiz();
            return;
        }

        QuizQuestion currentQuestion = questionList.get(currentQuestionIndex);

        if (currentQuestion == null || currentQuestion.getQuestion() == null || currentQuestion.getOptions() == null || currentQuestion.getOptions().size() < 4) {
            Log.e(TAG, "Invalid question data encountered at index: " + currentQuestionIndex + ". Skipping question.");
            Toast.makeText(this, "Skipping invalid question data.", Toast.LENGTH_SHORT).show();
            currentQuestionIndex++;
            // Potentially try to display next or end quiz if too many are bad
            if (currentQuestionIndex < totalQuestions) {
                displayQuestion();
            } else {
                finishQuiz();
            }
            return;
        }

        textViewQuestionNumber.setText(String.format(Locale.getDefault(), "Question %d/%d", currentQuestionIndex + 1, totalQuestions));
        textViewQuestion.setText(currentQuestion.getQuestion());

        List<String> options = currentQuestion.getOptions();
        buttonOption1.setText(options.get(0));
        buttonOption2.setText(options.get(1));
        buttonOption3.setText(options.get(2));
        buttonOption4.setText(options.get(3));

        resetOptionButtonsAppearance();
        answerSubmitted = false;
        buttonSubmitNext.setText(R.string.submit);
        buttonSubmitNext.setEnabled(false);
        selectedOptionButton = null;

        startTimer(DEFAULT_TIME_PER_QUESTION);
    }

    private void resetOptionButtonsAppearance() {
        MaterialButton[] optionButtons = {buttonOption1, buttonOption2, buttonOption3, buttonOption4};
        // Rely on the custom style "QuizOptionButton" for default appearance
        for (MaterialButton btn : optionButtons) {
            btn.setEnabled(true);
            btn.setChecked(false);
            btn.setBackgroundTintList(null); // Use style's default
            btn.setIcon(null);
            // Explicitly set text and stroke if not perfectly handled by style on reset
            btn.setTextColor(ContextCompat.getColorStateList(this, R.color.md_theme_light_primary));
            btn.setStrokeColor(ContextCompat.getColorStateList(this, R.color.md_theme_light_primary));

        }
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();

        if (viewId == R.id.button_submit_answer) {
            if (answerSubmitted) {
                currentQuestionIndex++;
                if (currentQuestionIndex < totalQuestions) {
                    displayQuestion();
                    updateProgressIndicator();
                } else {
                    finishQuiz();
                }
            } else {
                if (selectedOptionButton != null) {
                    checkAnswer();
                } else {
                    Toast.makeText(this, "Please select an option.", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (viewId == R.id.button_option_1 || viewId == R.id.button_option_2 || viewId == R.id.button_option_3 || viewId == R.id.button_option_4) {
            if (!answerSubmitted) {
                handleOptionSelection((MaterialButton) v);
            }
        }
    }

    private void handleOptionSelection(MaterialButton clickedButton) {
        ColorStateList selectedBgColor = ContextCompat.getColorStateList(this, R.color.md_theme_light_primary);
        ColorStateList selectedTextColor = ContextCompat.getColorStateList(this, R.color.md_theme_light_onPrimary);
        // Default colors are now primarily handled by the style QuizOptionButton
        ColorStateList defaultTextColor = ContextCompat.getColorStateList(this, R.color.md_theme_light_primary);
        ColorStateList defaultStrokeColor = ContextCompat.getColorStateList(this, R.color.md_theme_light_primary);


        if (selectedOptionButton != null && selectedOptionButton != clickedButton) {
            selectedOptionButton.setChecked(false);
            selectedOptionButton.setBackgroundTintList(null); // Revert to styled
            selectedOptionButton.setTextColor(defaultTextColor);
            selectedOptionButton.setStrokeColor(defaultStrokeColor);
        }

        selectedOptionButton = clickedButton;
        selectedOptionButton.setChecked(true);

        selectedOptionButton.setBackgroundTintList(selectedBgColor);
        selectedOptionButton.setTextColor(selectedTextColor);
        // Optionally change stroke for selected state if desired
        // selectedOptionButton.setStrokeColor(null); // Or a different color

        buttonSubmitNext.setEnabled(true);
    }

    private void checkAnswer() {
        answerSubmitted = true;
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        buttonOption1.setEnabled(false);
        buttonOption2.setEnabled(false);
        buttonOption3.setEnabled(false);
        buttonOption4.setEnabled(false);

        QuizQuestion currentQuestion = questionList.get(currentQuestionIndex);
        int correctAnswerIndex = currentQuestion.getCorrectAnswerIndex();
        MaterialButton correctButton = getButtonByIndex(correctAnswerIndex);

        boolean isCorrect = false;
        int selectedIndex = getSelectedOptionIndex();

        if (selectedIndex == correctAnswerIndex) {
            isCorrect = true;
        }

        ColorStateList correctColor = ContextCompat.getColorStateList(this, android.R.color.holo_green_light);
        ColorStateList incorrectColor = ContextCompat.getColorStateList(this, android.R.color.holo_red_light);
        ColorStateList iconColor = ContextCompat.getColorStateList(this, R.color.white);

        if (isCorrect) {
            score += 10;
            PreferenceHelper.addXP(this, 10);
            if (selectedOptionButton != null) {
                selectedOptionButton.setBackgroundTintList(correctColor);
                selectedOptionButton.setIconResource(R.drawable.ic_check);
                selectedOptionButton.setIconTint(iconColor);
                selectedOptionButton.setTextColor(ContextCompat.getColor(this, R.color.white));
            }
        } else {
            if (selectedOptionButton != null) {
                selectedOptionButton.setBackgroundTintList(incorrectColor);
                selectedOptionButton.setIconResource(R.drawable.ic_close);
                selectedOptionButton.setIconTint(iconColor);
                selectedOptionButton.setTextColor(ContextCompat.getColor(this, R.color.white));
            }
            if (correctButton != null && correctButton != selectedOptionButton) {
                correctButton.setBackgroundTintList(correctColor);
                correctButton.setIconResource(R.drawable.ic_check);
                correctButton.setIconTint(iconColor);
                correctButton.setTextColor(ContextCompat.getColor(this, R.color.white));
            }
        }
        PreferenceHelper.updateTopicStats(this, topicTitle, isCorrect);
        updateScoreUI();

        buttonSubmitNext.setText(R.string.next_question);
        buttonSubmitNext.setEnabled(true);
    }

    private int getSelectedOptionIndex() {
        if (selectedOptionButton == buttonOption1) return 0;
        if (selectedOptionButton == buttonOption2) return 1;
        if (selectedOptionButton == buttonOption3) return 2;
        if (selectedOptionButton == buttonOption4) return 3;
        return -1;
    }

    private MaterialButton getButtonByIndex(int index) {
        switch (index) {
            case 0: return buttonOption1;
            case 1: return buttonOption2;
            case 2: return buttonOption3;
            case 3: return buttonOption4;
            default: Log.e(TAG, "Invalid correct answer index: " + index); return null;
        }
    }

    private void updateScoreUI() {
        if (textViewScore != null) {
            textViewScore.setText(String.format(Locale.getDefault(), "Score: %d", score));
        }
    }

    private void updateProgressIndicator() {
        if (progressIndicator != null) {
            // Progress is 1-based for display, index is 0-based
            int currentProgress = Math.min(currentQuestionIndex +1 , totalQuestions);
            if (totalQuestions > 0) { // Ensure no division by zero if totalQuestions isn't set yet
                progressIndicator.setProgressCompat(currentProgress, true);
            }
        }
    }


    private void startTimer(long duration) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        timeLeftInMillis = duration;
        updateTimerText();

        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerText();
            }

            @Override
            public void onFinish() {
                timeLeftInMillis = 0;
                updateTimerText();
                if (!answerSubmitted) {
                    Toast.makeText(QuizActivity.this, "Time's up!", Toast.LENGTH_SHORT).show();
                    selectedOptionButton = null; // No option was selected in time
                    checkAnswer(); // Process as unanswered or incorrect
                }
            }
        }.start();
    }

    private void updateTimerText() {
        if (textViewTimer != null) {
            String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(timeLeftInMillis),
                    TimeUnit.MILLISECONDS.toSeconds(timeLeftInMillis) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeLeftInMillis))
            );
            textViewTimer.setText(timeFormatted);

            if (timeLeftInMillis < 10000 && timeLeftInMillis > 0) {
                textViewTimer.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            } else {
                textViewTimer.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_primary));
            }
        }
    }

    private void finishQuiz() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        Log.i(TAG, "Quiz Finished! Final Score: " + score + "/" + (totalQuestions * 10));

        updateStreakAfterQuizCompletion();

        int xpGainedThisSession = score; // Or however you calculate XP gained
        int correctAnswersCount = score / 10; // If 10 points per question

        PreferenceHelper.saveLastQuizSummary(this, topicTitle, score, correctAnswersCount, totalQuestions);

        Intent summaryIntent = new Intent(this, QuizSummaryActivity.class);
        summaryIntent.putExtra("QUIZ_TOPIC", topicTitle);
        summaryIntent.putExtra("QUIZ_SCORE", score);
        summaryIntent.putExtra("TOTAL_QUESTIONS", totalQuestions);
        summaryIntent.putExtra("XP_GAINED", xpGainedThisSession);
        // Streak is read by SummaryActivity directly from Preferences
        startActivity(summaryIntent);
        finish();
    }

    private void updateStreakAfterQuizCompletion() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDateStr = sdf.format(new Date());
        String lastPlayedDateStr = PreferenceHelper.getLastPlayedDateForStreak(this);
        int currentStreak = PreferenceHelper.getStreak(this);

        if (lastPlayedDateStr.isEmpty()) { // First quiz ever or after a long break
            currentStreak = 1;
        } else if (!lastPlayedDateStr.equals(currentDateStr)) { // Played on a new day
            try {
                Date lastPlayedDate = sdf.parse(lastPlayedDateStr);
                Calendar cal = Calendar.getInstance();
                cal.setTime(lastPlayedDate);
                cal.add(Calendar.DATE, 1); // Day after last played
                String expectedNextDayForStreak = sdf.format(cal.getTime());

                if (expectedNextDayForStreak.equals(currentDateStr)) {
                    currentStreak++; // Continued streak
                } else {
                    currentStreak = 1; // Streak broken (gap was more than 1 day)
                }
            } catch (java.text.ParseException e) {
                Log.e(TAG, "Error parsing last played date for streak", e);
                currentStreak = 1; // Reset on error
            }
        }
        // If lastPlayedDateStr IS THE SAME as currentDateStr, streak doesn't increase further from this session.
        // It would have been maintained or incremented if this was the first quiz of *today*.

        PreferenceHelper.saveStreak(this, currentStreak);
        PreferenceHelper.saveLastPlayedDateForStreak(this, currentDateStr);
    }


    private void showLoading(boolean isLoading) {
        if (progressBarLoading != null && groupQuizContent != null) {
            progressBarLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            groupQuizContent.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        } else {
            Log.w(TAG, "Attempted to update loading state but views were null.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}