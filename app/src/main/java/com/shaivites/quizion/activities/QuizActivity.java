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
import android.widget.ProgressBar; // Import ProgressBar
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group; // Import Group
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.gson.Gson; // Import Gson
import com.google.gson.JsonSyntaxException; // Import JsonSyntaxException
import com.google.gson.reflect.TypeToken; // Import TypeToken
import com.shaivites.quizion.R;
import com.shaivites.quizion.models.QuizQuestion; // Import your QuizQuestion model
import com.shaivites.quizion.networking.GeminiApiService; // Import GeminiApiService

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Activity to display and handle the quiz interface.
 * Fetches questions using GeminiApiService and manages quiz state.
 */
public class QuizActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "QuizActivity";

    // --- UI Elements ---
    private LinearProgressIndicator progressIndicator;
    private ImageButton buttonExit;
    private TextView textViewScore;
    private TextView textViewTimer;
    private TextView textViewQuestionNumber;
    private TextView textViewQuestion;
    private MaterialButton buttonOption1, buttonOption2, buttonOption3, buttonOption4;
    private MaterialButton buttonSubmitNext;
    private MaterialCardView cardQuestion;
    private ProgressBar progressBarLoading; // Loading indicator
    private Group groupQuizContent; // Group for main content visibility

    // --- Quiz State Variables ---
    private String quizMode = "QUICK"; // Default mode, can be overridden by Intent
    private String topicTitle = "General Knowledge"; // Default topic
    private int currentQuestionIndex = 0;
    private int score = 0;
    private int totalQuestions = 0; // Determined after fetching questions
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis;
    private static final long DEFAULT_TIME_PER_QUESTION = 30000; // 30 seconds

    private List<QuizQuestion> questionList = new ArrayList<>(); // Initialize the list
    private MaterialButton selectedOptionButton = null;
    private boolean answerSubmitted = false;

    // --- Services and Utilities ---
    private GeminiApiService geminiApiService;
    private Gson gson;
    private Handler mainThreadHandler; // To post results back to main thread

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        // Initialize services and utilities
        try {
            geminiApiService = new GeminiApiService(); // Initialize API service
        } catch (IllegalStateException e) {
            Log.e(TAG, "Failed to initialize GeminiApiService", e);
            Toast.makeText(this, "Error initializing quiz service: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        gson = new Gson();
        mainThreadHandler = new Handler(Looper.getMainLooper());

        // Retrieve data from Intent
        Intent intent = getIntent();
        quizMode = intent.getStringExtra("QUIZ_MODE") != null ? intent.getStringExtra("QUIZ_MODE") : "QUICK";
        topicTitle = intent.getStringExtra("TOPIC_TITLE") != null ? intent.getStringExtra("TOPIC_TITLE") : "General Knowledge";
        Log.i(TAG, "Starting Quiz - Mode: " + quizMode + ", Topic: " + topicTitle);

        // Initialize Views
        findViews();

        // Setup Listeners
        setupListeners();

        // Load questions from the API
        loadQuestions();
    }

    /**
     * Finds and assigns references to all the necessary UI elements from the layout.
     */
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

        // Set initial UI state (loading)
        showLoading(true); // Call helper method
        updateScore();     // Call helper method
    }

    /**
     * Sets OnClickListeners for interactive UI elements.
     */
    private void setupListeners() {
        buttonExit.setOnClickListener(v -> finish());
        buttonOption1.setOnClickListener(this);
        buttonOption2.setOnClickListener(this);
        buttonOption3.setOnClickListener(this);
        buttonOption4.setOnClickListener(this);
        buttonSubmitNext.setOnClickListener(this);
    }

    /**
     * Initiates the process of loading quiz questions from the Gemini API.
     */
    private void loadQuestions() {
        Log.i(TAG, "Requesting questions from Gemini API for topic: " + topicTitle);
        showLoading(true); // Show loading indicator
        int numberOfQuestions = 10; // Example: fetch 10 questions
        String difficulty = "Medium"; // Example: fetch medium difficulty

        geminiApiService.generateQuizQuestions(topicTitle, difficulty, numberOfQuestions, new GeminiApiService.GeminiCallback() {
            @Override
            public void onSuccess(String generatedJson) {
                // Process the successful response on the main thread
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
                            updateScore(); // Update score display

                            // Questions loaded, show the first question
                            showLoading(false); // Hide loading indicator
                            displayQuestion();
                            updateProgress(); // Update progress bar
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
                // Show error message on the main thread
                mainThreadHandler.post(() -> {
                    showLoading(false); // Hide loading indicator
                    handleQuestionLoadError("Error fetching questions: " + throwable.getMessage());
                });
            }
        });
    }

    /**
     * Handles errors during question loading, shows a message, and finishes the activity.
     * @param message The error message to display.
     */
    private void handleQuestionLoadError(String message) {
        Toast.makeText(QuizActivity.this, message, Toast.LENGTH_LONG).show();
        finish(); // Exit the quiz activity if questions can't be loaded
    }

    /**
     * Displays the current question and its options on the UI.
     */
    private void displayQuestion() {
        if (questionList == null || questionList.isEmpty()) {
            Log.e(TAG, "Attempted to display question, but question list is null or empty.");
            handleQuestionLoadError("No questions available to display.");
            return;
        }
        if (currentQuestionIndex >= questionList.size()) {
            Log.i(TAG, "Reached end of question list.");
            finishQuiz(); // Call helper method
            return;
        }

        QuizQuestion currentQuestion = questionList.get(currentQuestionIndex);

        if (currentQuestion == null || currentQuestion.getQuestion() == null || currentQuestion.getOptions() == null || currentQuestion.getOptions().size() < 4) {
            Log.e(TAG, "Invalid question data encountered at index: " + currentQuestionIndex + ". Skipping question.");
            Toast.makeText(this, "Skipping invalid question data.", Toast.LENGTH_SHORT).show();
            currentQuestionIndex++;
            displayQuestion();
            return;
        }

        textViewQuestionNumber.setText(String.format(Locale.getDefault(), "Question %d/%d", currentQuestionIndex + 1, totalQuestions));
        textViewQuestion.setText(currentQuestion.getQuestion());

        List<String> options = currentQuestion.getOptions();
        buttonOption1.setText(options.get(0));
        buttonOption2.setText(options.get(1));
        buttonOption3.setText(options.get(2));
        buttonOption4.setText(options.get(3));

        resetOptionButtons();
        answerSubmitted = false;
        buttonSubmitNext.setText(R.string.submit); // Use string resource
        buttonSubmitNext.setEnabled(false);
        selectedOptionButton = null;

        startTimer(DEFAULT_TIME_PER_QUESTION); // Call helper method
    }

    /**
     * Resets the appearance and state of all option buttons.
     */
    private void resetOptionButtons() {
        MaterialButton[] options = {buttonOption1, buttonOption2, buttonOption3, buttonOption4};
        ColorStateList defaultTextColor = ContextCompat.getColorStateList(this, R.color.md_theme_light_primary);
        ColorStateList defaultStrokeColor = ContextCompat.getColorStateList(this, R.color.md_theme_light_primary);

        for (MaterialButton btn : options) {
            btn.setEnabled(true);
            btn.setChecked(false);
            btn.setBackgroundTintList(null);
            btn.setStrokeColor(defaultStrokeColor);
            btn.setTextColor(defaultTextColor);
            btn.setIcon(null);
        }
    }

    /**
     * Handles clicks on the option buttons and the submit/next button.
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        int viewId = v.getId();

        if (viewId == R.id.button_submit_answer) {
            if (answerSubmitted) {
                currentQuestionIndex++;
                displayQuestion();
                updateProgress(); // Call helper method
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

    /**
     * Updates the visual state of option buttons when one is selected.
     * @param clickedButton The MaterialButton that was clicked.
     */
    private void handleOptionSelection(MaterialButton clickedButton) {
        ColorStateList selectedBgColor = ContextCompat.getColorStateList(this, R.color.md_theme_light_primary);
        ColorStateList selectedTextColor = ContextCompat.getColorStateList(this, R.color.md_theme_light_onPrimary);
        ColorStateList defaultTextColor = ContextCompat.getColorStateList(this, R.color.md_theme_light_primary);

        if (selectedOptionButton != null && selectedOptionButton != clickedButton) {
            selectedOptionButton.setChecked(false);
            selectedOptionButton.setBackgroundTintList(null);
            selectedOptionButton.setTextColor(defaultTextColor);
        }

        selectedOptionButton = clickedButton;
        selectedOptionButton.setChecked(true);
        selectedOptionButton.setBackgroundTintList(selectedBgColor);
        selectedOptionButton.setTextColor(selectedTextColor);

        buttonSubmitNext.setEnabled(true);
    }

    /**
     * Checks the selected answer against the correct answer, updates UI feedback and score.
     */
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
        MaterialButton correctButton = getButtonByIndex(correctAnswerIndex); // Call helper method

        boolean isCorrect = false;
        int selectedIndex = -1;

        if(selectedOptionButton == buttonOption1) selectedIndex = 0;
        else if(selectedOptionButton == buttonOption2) selectedIndex = 1;
        else if(selectedOptionButton == buttonOption3) selectedIndex = 2;
        else if(selectedOptionButton == buttonOption4) selectedIndex = 3;

        if (selectedIndex == correctAnswerIndex) {
            isCorrect = true;
        }

        ColorStateList correctColor = ContextCompat.getColorStateList(this, android.R.color.holo_green_light);
        ColorStateList incorrectColor = ContextCompat.getColorStateList(this, android.R.color.holo_red_light);
        ColorStateList iconColor = ContextCompat.getColorStateList(this, R.color.white);

        if (isCorrect) {
            score += 10;
            updateScore(); // Call helper method
            if (selectedOptionButton != null) {
                selectedOptionButton.setBackgroundTintList(correctColor);
                selectedOptionButton.setIconResource(R.drawable.ic_check); // Use check icon
                selectedOptionButton.setIconTint(iconColor);
                selectedOptionButton.setTextColor(ContextCompat.getColor(this, R.color.white));
            }
        } else {
            if (selectedOptionButton != null) {
                selectedOptionButton.setBackgroundTintList(incorrectColor);
                selectedOptionButton.setIconResource(R.drawable.ic_close); // Use close icon
                selectedOptionButton.setIconTint(iconColor);
                selectedOptionButton.setTextColor(ContextCompat.getColor(this, R.color.white));
            }
            // Highlight the correct answer if one exists and wasn't the selected one
            if (correctButton != null && correctButton != selectedOptionButton) {
                correctButton.setBackgroundTintList(correctColor);
                correctButton.setIconResource(R.drawable.ic_check); // Use check icon
                correctButton.setIconTint(iconColor);
                correctButton.setTextColor(ContextCompat.getColor(this, R.color.white));
            }
        }

        buttonSubmitNext.setText(R.string.next_question); // Use string resource
        buttonSubmitNext.setEnabled(true);
    }

    // --- START OF HELPER METHOD DEFINITIONS ---

    /**
     * Helper method to get the MaterialButton corresponding to a given answer index (0-3).
     */
    private MaterialButton getButtonByIndex(int index) {
        switch (index) {
            case 0: return buttonOption1;
            case 1: return buttonOption2;
            case 2: return buttonOption3;
            case 3: return buttonOption4;
            default:
                Log.e(TAG, "Invalid correct answer index received: " + index);
                return null;
        }
    }

    private void updateScore() {
        if (textViewScore != null) {
            textViewScore.setText(String.format(Locale.getDefault(), "Score: %d", score));
        }
    }

    private void updateProgress() {
        if (progressIndicator != null) {
            int progress = Math.min(currentQuestionIndex + 1, totalQuestions);
            progressIndicator.setProgressCompat(progress, true);
        }
    }

    /**
     * Starts the countdown timer for the current question.
     */
    private void startTimer(long duration) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        timeLeftInMillis = duration;
        updateTimerText(); // Display initial time

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
                    selectedOptionButton = null;
                    checkAnswer();
                }
            }
        }.start();
    }

    /**
     * Updates the timer TextView, formatting milliseconds into MM:SS.
     */
    private void updateTimerText() {
        if (textViewTimer != null) {
            String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(timeLeftInMillis),
                    TimeUnit.MILLISECONDS.toSeconds(timeLeftInMillis) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeLeftInMillis))
            );
            textViewTimer.setText(timeFormatted);

            if (timeLeftInMillis < 10000 && timeLeftInMillis > 0) { // Last 10 seconds
                textViewTimer.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            } else {
                textViewTimer.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_primary));
            }
        }
    }

    /**
     * Handles the logic when the quiz finishes.
     */
    private void finishQuiz() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        Log.i(TAG, "Quiz Finished! Final Score: " + score + "/" + (totalQuestions * 10));
        Toast.makeText(this, "Quiz Finished! Final Score: " + score, Toast.LENGTH_LONG).show();
        finish(); // Close the QuizActivity
    }

    /**
     * Shows or hides the loading indicator and the main quiz content group.
     */
    private void showLoading(boolean isLoading) {
        if (progressBarLoading != null && groupQuizContent != null) {
            progressBarLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            groupQuizContent.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        } else {
            Log.w(TAG, "Attempted to update loading state but views were null.");
        }
    }

    // --- END OF HELPER METHOD DEFINITIONS ---

    /**
     * Cleans up resources, specifically cancelling the timer when the activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
