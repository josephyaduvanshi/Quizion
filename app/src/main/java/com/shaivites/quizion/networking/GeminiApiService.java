package com.shaivites.quizion.networking;

import android.annotation.SuppressLint;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.shaivites.quizion.BuildConfig;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GeminiApiService {

    private static final String TAG = "GeminiApiServiceREST";
    private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();
    private final String apiKey;
    private final Gson gson = new Gson();

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String MODEL_NAME = "gemini-1.5-flash-latest";
    private static final String METHOD = ":generateContent";

    public interface GeminiCallback {
        void onSuccess(String generatedJsonText);
        void onError(Throwable throwable);
    }

    public GeminiApiService() {
        try {
            this.apiKey = BuildConfig.GEMINI_API_KEY;
            if (this.apiKey == null || this.apiKey.isEmpty() || "null".equals(this.apiKey) || "\"\"".equals(this.apiKey)) {
                Log.e(TAG, "API Key is invalid or not set in BuildConfig. Check local.properties (GEMINI_API_KEY=YOUR_API_KEY) and rebuild.");
                throw new IllegalStateException("Gemini API Key not found or invalid. Please ensure it is correctly set in your local.properties file and the project has been rebuilt.");
            }
        } catch (Exception | Error e) {
            Log.e(TAG, "CRITICAL: Error accessing BuildConfig.GEMINI_API_KEY.", e);
            throw new IllegalStateException("Could not access Gemini API Key. Build configuration error.", e);
        }
        Log.i(TAG, "GeminiApiService (REST) initialized.");
    }

    public void generateQuizQuestions(String topic, String difficulty, int numberOfQuestions,
                                      @Nullable Map<String, Map<String, String>> allTopicStats,
                                      @NonNull GeminiCallback callback) {
        backgroundExecutor.execute(() -> {
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(BASE_URL + MODEL_NAME + METHOD + "?key=" + apiKey);
                Log.d(TAG, "Request URL: " + url.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("Accept", "application/json");
                urlConnection.setDoOutput(true);
                urlConnection.setConnectTimeout(20000); // 20 seconds
                urlConnection.setReadTimeout(60000);    // 60 seconds

                String jsonRequestBody = buildRequestBody(topic, difficulty, numberOfQuestions, allTopicStats);
                Log.d(TAG, "Request Body: " + jsonRequestBody);

                try (OutputStream os = urlConnection.getOutputStream()) {
                    byte[] input = jsonRequestBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = urlConnection.getResponseCode();
                Log.i(TAG, "Gemini API Response Code: " + responseCode);

                InputStream inputStream = (responseCode >= 200 && responseCode < 300) ?
                        urlConnection.getInputStream() : urlConnection.getErrorStream();

                StringBuilder response = new StringBuilder();
                if (inputStream != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line.trim());
                        }
                    }
                } else {
                    Log.e(TAG, "InputStream was null for response code: " + responseCode);
                }
                String responseBody = response.toString();
                Log.d(TAG, "Raw Response Body: " + responseBody);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String extractedJson = extractGeneratedJson(responseBody);
                    if (extractedJson != null) {
                        callback.onSuccess(extractedJson);
                    } else {
                        Log.e(TAG, "Failed to extract valid content from successful API response. Body: " + responseBody);
                        callback.onError(new Exception("Failed to extract valid content from API response. The response might be blocked or in an unexpected format."));
                    }
                } else {
                    String errorMsg = parseError(responseBody);
                    Log.e(TAG, "Gemini API Error: " + responseCode + " - " + errorMsg);
                    callback.onError(new Exception("Gemini API Error: " + responseCode + ". " + errorMsg));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during Gemini REST API call", e);
                callback.onError(e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        });
    }

    private String buildRequestBody(String topic, String difficulty, int numberOfQuestions, @Nullable Map<String, Map<String, String>> allTopicStats) {
        JsonObject root = new JsonObject();
        JsonArray contentsArray = new JsonArray();
        JsonObject contentObject = new JsonObject();
        JsonArray partsArray = new JsonArray();
        JsonObject partObject = new JsonObject();

        StringBuilder performanceContextSb = new StringBuilder();
        if (allTopicStats != null && !allTopicStats.isEmpty()) {
            performanceContextSb.append(" User's current performance in various topics (correct answers:total questions): ");
            int count = 0;
            for (Map.Entry<String, Map<String, String>> entry : allTopicStats.entrySet()) {
                if (count >= 5) break; // Limit context to avoid overly long prompts
                String topicName = entry.getKey();
                Map<String, String> stats = entry.getValue();
                performanceContextSb.append(String.format(Locale.US, "%s (%s:%s), ", topicName, stats.get("correct"), stats.get("total")));
                count++;
            }
            if (performanceContextSb.length() > 2) { // Remove trailing comma and space
                performanceContextSb.setLength(performanceContextSb.length() - 2);
            }
            performanceContextSb.append(".");
        }
        String performanceContext = performanceContextSb.toString();

        @SuppressLint("DefaultLocale") String prompt = String.format(
                "Generate exactly %d multiple-choice quiz questions about the topic '%s'. " +
                        "The difficulty level should be '%s'.%s" + // Performance context inserted here
                        " If performance data is provided, try to tailor questions slightly, perhaps by focusing on sub-topics where the user has fewer attempts or lower accuracy, or by slightly adjusting difficulty if the user is performing very well or poorly on '%s'." +
                        " Format the output STRICTLY as a valid JSON array where each object has the following keys: " +
                        "\"question\" (string: the question text), " +
                        "\"options\" (JSON array of 4 strings: the answer choices), and " +
                        "\"correctAnswerIndex\" (integer: the 0-based index of the correct answer within the \"options\" array). " +
                        "Do not include any introductory text, explanations, markdown formatting, or anything else outside the single JSON array structure. Only output the JSON array.",
                numberOfQuestions, topic, difficulty, performanceContext, topic // topic repeated for tailoring instruction
        );

        partObject.addProperty("text", prompt);
        partsArray.add(partObject);
        contentObject.add("parts", partsArray);
        contentsArray.add(contentObject);
        root.add("contents", contentsArray);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.8); // Slightly higher for more variety
        generationConfig.addProperty("topK", 1);
        generationConfig.addProperty("topP", 0.95); // Adjusted topP
        generationConfig.addProperty("maxOutputTokens", 4096); // Increased for potentially longer JSON
        // Adding response_mime_type for explicit JSON output
        generationConfig.addProperty("response_mime_type", "application/json");

        root.add("generationConfig", generationConfig);


        // Safety settings (Adjust as needed, for quizzes, usually keep them moderately strict)
        JsonArray safetySettingsArray = new JsonArray();
        String[] harmCategories = {"HARM_CATEGORY_HARASSMENT", "HARM_CATEGORY_HATE_SPEECH", "HARM_CATEGORY_SEXUALLY_EXPLICIT", "HARM_CATEGORY_DANGEROUS_CONTENT"};
        for (String category : harmCategories) {
            JsonObject safetySetting = new JsonObject();
            safetySetting.addProperty("category", category);
            safetySetting.addProperty("threshold", "BLOCK_MEDIUM_AND_ABOVE"); // Or BLOCK_LOW_AND_ABOVE for stricter
            safetySettingsArray.add(safetySetting);
        }
        root.add("safetySettings", safetySettingsArray);


        return gson.toJson(root);
    }

    @Nullable
    private String extractGeneratedJson(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            Log.w(TAG, "Attempted to extract JSON from empty/null response body.");
            return null;
        }
        try {
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();

            if (jsonResponse.has("promptFeedback")) {
                JsonObject feedback = jsonResponse.getAsJsonObject("promptFeedback");
                if (feedback.has("blockReason")) {
                    String reason = feedback.get("blockReason").getAsString();
                    Log.w(TAG, "Content blocked by API. Reason: " + reason);
                    if (feedback.has("safetyRatings")) {
                        Log.w(TAG, "Safety Ratings: " + feedback.getAsJsonArray("safetyRatings").toString());
                    }
                    return null; // Blocked content
                }
            }

            if (jsonResponse.has("candidates")) {
                JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
                    if (firstCandidate.has("finishReason") && !"STOP".equals(firstCandidate.get("finishReason").getAsString())) {
                        String reason = firstCandidate.get("finishReason").getAsString();
                        Log.w(TAG, "Candidate finish reason was not STOP: " + reason + ". This might indicate an issue or blocked content.");
                        if ("SAFETY".equals(reason) && firstCandidate.has("safetyRatings")) {
                            Log.w(TAG, "Safety Ratings for non-STOP: " + firstCandidate.getAsJsonArray("safetyRatings").toString());
                        }
                        // If the finish reason is due to safety or other errors, we might not have valid content.
                        if (!"STOP".equals(reason) && !"MAX_TOKENS".equals(reason)) return null; // MAX_TOKENS might still have partial valid JSON
                    }

                    if (firstCandidate.has("content")) {
                        JsonObject content = firstCandidate.getAsJsonObject("content");
                        if (content.has("parts")) {
                            JsonArray parts = content.getAsJsonArray("parts");
                            if (parts != null && !parts.isEmpty()) {
                                JsonObject firstPart = parts.get(0).getAsJsonObject();
                                if (firstPart.has("text")) {
                                    String rawText = firstPart.get("text").getAsString();
                                    Log.d(TAG, "Raw text from Gemini part: " + rawText);

                                    // The API should directly return JSON if response_mime_type is set.
                                    // No need to strip markdown if API respects response_mime_type.
                                    if (rawText.trim().startsWith("[") && rawText.trim().endsWith("]")) {
                                        // Basic validation that it looks like a JSON array.
                                        // More robust validation might involve trying to parse it here.
                                        return rawText.trim();
                                    } else {
                                        Log.w(TAG, "Expected direct JSON array from Gemini due to response_mime_type, but received: " + rawText);
                                        // Fallback to try stripping markdown just in case
                                        String cleanedJson = rawText.trim();
                                        if (cleanedJson.startsWith("```json")) {
                                            cleanedJson = cleanedJson.substring(7).trim();
                                        } else if (cleanedJson.startsWith("```")) {
                                            cleanedJson = cleanedJson.substring(3).trim();
                                        }
                                        if (cleanedJson.endsWith("```")) {
                                            cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3).trim();
                                        }
                                        if (cleanedJson.startsWith("[") && cleanedJson.endsWith("]")) {
                                            return cleanedJson;
                                        }
                                        Log.e(TAG, "Fallback markdown stripping also failed to yield a JSON array.");
                                        return null;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Log.w(TAG, "Could not find generated text in the expected JSON structure. Full response: " + responseBody);
            return null;
        } catch (JsonSyntaxException | IllegalStateException | ClassCastException e) {
            Log.e(TAG, "Error parsing Gemini JSON response in extractGeneratedJson. Response body: " + responseBody, e);
            return null;
        }
    }

    private String parseError(String errorBody) {
        if (errorBody == null || errorBody.isEmpty()) return "Unknown error (empty error body)";
        try {
            JsonObject jsonError = JsonParser.parseString(errorBody).getAsJsonObject();
            if (jsonError.has("error")) {
                JsonObject errorObj = jsonError.getAsJsonObject("error");
                if (errorObj.has("message")) {
                    return errorObj.get("message").getAsString();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not parse error JSON: " + errorBody, e);
        }
        return errorBody;
    }
}