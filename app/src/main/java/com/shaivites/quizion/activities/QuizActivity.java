package com.shaivites.quizion.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import com.shaivites.quizion.utils.PreferenceHelper;
import com.shashank.sony.fancytoastlib.FancyToast;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    private ColorStateList defaultOptionTextColorStateList;
    private ColorStateList defaultOptionStrokeColorStateList;
    private ColorStateList selectedOptionBgColorStateList;
    private ColorStateList selectedOptionTextColorStateList;
    private ColorStateList correctOptionBgColorStateList;
    private ColorStateList incorrectOptionBgColorStateList;
    private ColorStateList feedbackIconColorStateList;
    private int defaultOptionStrokeWidthPx;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        initializeColorsAndDimens();

        try {
            geminiApiService = new GeminiApiService();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Failed to initialize GeminiApiService", e);
            FancyToast.makeText(this, "Error initializing quiz service: " + e.getMessage(), FancyToast.LENGTH_LONG,FancyToast.ERROR,false).show();
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

    private void initializeColorsAndDimens() {
        defaultOptionTextColorStateList = ContextCompat.getColorStateList(this, R.color.md_theme_light_primary);
        defaultOptionStrokeColorStateList = ContextCompat.getColorStateList(this, R.color.md_theme_light_primary);
        selectedOptionBgColorStateList = ContextCompat.getColorStateList(this, R.color.md_theme_light_primary);
        selectedOptionTextColorStateList = ContextCompat.getColorStateList(this, R.color.md_theme_light_onPrimary);
        correctOptionBgColorStateList = ContextCompat.getColorStateList(this, R.color.holo_green_light_quiz);
        incorrectOptionBgColorStateList = ContextCompat.getColorStateList(this, R.color.holo_red_light_quiz);
        feedbackIconColorStateList = ContextCompat.getColorStateList(this, R.color.white);

        try {
            defaultOptionStrokeWidthPx = getResources().getDimensionPixelSize(R.dimen.quiz_option_button_stroke_width_default);
        } catch (Exception e) {
            Log.w(TAG, "quiz_option_button_stroke_width_default not found in dimens.xml, defaulting to 1dp in code");
            defaultOptionStrokeWidthPx = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        }
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
                            if (progressIndicator != null) {
                                progressIndicator.setMax(totalQuestions);
                            }
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
        FancyToast.makeText(QuizActivity.this, message, FancyToast.LENGTH_LONG,FancyToast.ERROR,false).show();
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
            FancyToast.makeText(this, "Skipping invalid question data.", FancyToast.LENGTH_SHORT,FancyToast.INFO,false).show();
            currentQuestionIndex++;
            if (currentQuestionIndex < totalQuestions) {
                displayQuestion();
            } else {
                finishQuiz();
            }
            return;
        }

        if (textViewQuestionNumber != null) textViewQuestionNumber.setText(String.format(Locale.getDefault(), "Question %d/%d", currentQuestionIndex + 1, totalQuestions));
        if (textViewQuestion != null) textViewQuestion.setText(currentQuestion.getQuestion());

        List<String> options = currentQuestion.getOptions();
        if (buttonOption1 != null) buttonOption1.setText(options.get(0));
        if (buttonOption2 != null) buttonOption2.setText(options.get(1));
        if (buttonOption3 != null) buttonOption3.setText(options.get(2));
        if (buttonOption4 != null) buttonOption4.setText(options.get(3));

        resetOptionButtonsAppearance();
        answerSubmitted = false;
        if (buttonSubmitNext != null) {
            buttonSubmitNext.setText(R.string.submit);
            buttonSubmitNext.setEnabled(false);
        }
        selectedOptionButton = null;

        startTimer(DEFAULT_TIME_PER_QUESTION);
    }

    private void resetOptionButtonsAppearance() {
        MaterialButton[] optionButtons = {buttonOption1, buttonOption2, buttonOption3, buttonOption4};
        for (MaterialButton btn : optionButtons) {
            if (btn != null) {
                btn.setEnabled(true);
                btn.setChecked(false);
                btn.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.white)); // Key change: Clear programmatic tint to let style take over
                // The style QuizOptionButton has android:backgroundTint="@android:color/transparent"
                btn.setStrokeColor(defaultOptionStrokeColorStateList);
                btn.setStrokeWidth(defaultOptionStrokeWidthPx);
                btn.setTextColor(defaultOptionTextColorStateList);
                btn.setIcon(null);
            }
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
                    FancyToast.makeText(this, "Please select an option.", FancyToast.LENGTH_SHORT,FancyToast.ERROR,false).show();
                }
            }
        } else if (viewId == R.id.button_option_1 || viewId == R.id.button_option_2 || viewId == R.id.button_option_3 || viewId == R.id.button_option_4) {
            if (!answerSubmitted) {
                handleOptionSelection((MaterialButton) v);
            }
        }
    }

    private void handleOptionSelection(MaterialButton clickedButton) {
        if (selectedOptionButton != null && selectedOptionButton != clickedButton) {
            selectedOptionButton.setChecked(false);
            selectedOptionButton.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, android.R.color.white) // Revert to transparent background
            ); // Revert to styled (transparent background)
            selectedOptionButton.setStrokeColor(defaultOptionStrokeColorStateList);
            selectedOptionButton.setStrokeWidth(defaultOptionStrokeWidthPx);
            selectedOptionButton.setTextColor(defaultOptionTextColorStateList);
        }

        selectedOptionButton = clickedButton;
        selectedOptionButton.setChecked(true);

        // Apply selected state appearance (filled)
        selectedOptionButton.setBackgroundTintList(selectedOptionBgColorStateList);
        selectedOptionButton.setTextColor(selectedOptionTextColorStateList);
        selectedOptionButton.setStrokeWidth(0); // No stroke when filled

        if (buttonSubmitNext != null) buttonSubmitNext.setEnabled(true);
    }

    private void checkAnswer() {
        answerSubmitted = true;
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        MaterialButton[] optionButtons = {buttonOption1, buttonOption2, buttonOption3, buttonOption4};
        for (MaterialButton btn : optionButtons) {
            if (btn != null) btn.setEnabled(false);
        }

        QuizQuestion currentQuestion = questionList.get(currentQuestionIndex);
        int correctAnswerIndex = currentQuestion.getCorrectAnswerIndex();
        MaterialButton correctButton = getButtonByIndex(correctAnswerIndex);

        boolean isCorrect = false;
        int selectedIndex = getSelectedOptionIndex();

        if (selectedIndex == correctAnswerIndex) {
            isCorrect = true;
        }

        if (isCorrect) {
            score += 10;
            PreferenceHelper.addXP(this, 10);
            if (selectedOptionButton != null) {
                selectedOptionButton.setBackgroundTintList(correctOptionBgColorStateList);
                selectedOptionButton.setIconResource(R.drawable.ic_check);
                selectedOptionButton.setIconTint(feedbackIconColorStateList);
                selectedOptionButton.setTextColor(feedbackIconColorStateList);
                selectedOptionButton.setStrokeWidth(0);
            }
        } else {
            if (selectedOptionButton != null) {
                selectedOptionButton.setBackgroundTintList(incorrectOptionBgColorStateList);
                selectedOptionButton.setIconResource(R.drawable.ic_close);
                selectedOptionButton.setIconTint(feedbackIconColorStateList);
                selectedOptionButton.setTextColor(feedbackIconColorStateList);
                selectedOptionButton.setStrokeWidth(0);
            }
            if (correctButton != null && correctButton != selectedOptionButton) {
                correctButton.setBackgroundTintList(correctOptionBgColorStateList);
                correctButton.setIconResource(R.drawable.ic_check);
                correctButton.setIconTint(feedbackIconColorStateList);
                correctButton.setTextColor(feedbackIconColorStateList);
                correctButton.setStrokeWidth(0);
            }
        }
        PreferenceHelper.updateTopicStats(this, topicTitle, isCorrect);
        updateScoreUI();

        if (buttonSubmitNext != null) {
            buttonSubmitNext.setText(R.string.next_question);
            buttonSubmitNext.setEnabled(true);
        }
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
            int currentProgress = Math.min(currentQuestionIndex + 1 , totalQuestions);
            if (totalQuestions > 0) {
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
                    FancyToast.makeText(QuizActivity.this, "Time's up!", FancyToast.LENGTH_SHORT,FancyToast.ERROR,false).show();
                    selectedOptionButton = null;
                    checkAnswer();
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
                textViewTimer.setTextColor(ContextCompat.getColor(this, R.color.holo_red_dark_quiz));
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

        int xpGainedThisSession = score;
        int correctAnswersCount = score / 10;

        PreferenceHelper.saveLastQuizSummary(this, topicTitle, score, correctAnswersCount, totalQuestions);

        Intent summaryIntent = new Intent(this, QuizSummaryActivity.class);
        summaryIntent.putExtra("QUIZ_TOPIC", topicTitle);
        summaryIntent.putExtra("QUIZ_SCORE", score);
        summaryIntent.putExtra("TOTAL_QUESTIONS", totalQuestions);
        summaryIntent.putExtra("XP_GAINED", xpGainedThisSession);
        startActivity(summaryIntent);
        finish();
    }

    private void updateStreakAfterQuizCompletion() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDateStr = sdf.format(new Date());
        String lastPlayedDateStr = PreferenceHelper.getLastPlayedDateForStreak(this);
        int currentStreak = PreferenceHelper.getStreak(this);

        if (lastPlayedDateStr.isEmpty() || !lastPlayedDateStr.equals(currentDateStr)) {
            Calendar cal = Calendar.getInstance();
            try {
                if (!lastPlayedDateStr.isEmpty()) {
                    Date lastPlayedDateParsed = sdf.parse(lastPlayedDateStr);
                    if (lastPlayedDateParsed != null) {
                        cal.setTime(lastPlayedDateParsed);
                        cal.add(Calendar.DATE, 1);
                        String expectedYesterdayForStreakContinuation = sdf.format(cal.getTime());
                        if (expectedYesterdayForStreakContinuation.equals(currentDateStr)) {
                            currentStreak++;
                        } else {
                            currentStreak = 1;
                        }
                    } else {
                        currentStreak = 1;
                    }
                } else {
                    currentStreak = 1;
                }
            } catch (java.text.ParseException e) {
                Log.e(TAG, "Error parsing last played date for streak", e);
                currentStreak = 1;
            }
        }

        PreferenceHelper.saveStreak(this, currentStreak);
        PreferenceHelper.saveLastPlayedDateForStreak(this, currentDateStr);
    }

    private void showLoading(boolean isLoading) {
        if (progressBarLoading != null && groupQuizContent != null) {
            progressBarLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            groupQuizContent.setVisibility(isLoading ? View.GONE : View.VISIBLE);
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
