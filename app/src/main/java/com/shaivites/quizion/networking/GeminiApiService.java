package com.shaivites.quizion.networking;

import android.annotation.SuppressLint;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable; // Added for Nullable return

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import com.shaivites.quizion.BuildConfig; // Still needed for API Key

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service class to interact with the Google Gemini REST API using HttpURLConnection.
 */
public class GeminiApiService {

    private static final String TAG = "GeminiApiServiceREST";
    private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();
    private final String apiKey;
    private final Gson gson = new Gson();

    // Gemini API Endpoint details
    // *** CORRECTED BASE_URL - Removed markdown formatting ***
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String MODEL_NAME = "gemini-1.5-flash-latest"; // Or "gemini-pro"
    private static final String METHOD = ":generateContent";

    /**
     * Callback interface for handling asynchronous results from the Gemini API.
     */
    public interface GeminiCallback {
        void onSuccess(String generatedJsonText); // Returns the raw JSON array string
        void onError(Throwable throwable);
    }

    public GeminiApiService() {
        // Load API Key from BuildConfig
        try {
            this.apiKey = BuildConfig.GEMINI_API_KEY;
            if (this.apiKey == null || this.apiKey.isEmpty()) {
                Log.e(TAG, "API Key is null or empty in BuildConfig. Check local.properties and rebuild.");
                throw new IllegalStateException("Gemini API Key not found or invalid in BuildConfig.");
            }
        } catch (Exception | Error e) {
            Log.e(TAG, "CRITICAL: Error accessing BuildConfig.GEMINI_API_KEY.", e);
            throw new IllegalStateException("Could not access Gemini API Key. Build configuration error.", e);
        }
        Log.i(TAG, "GeminiApiService (REST) initialized.");
    }

    /**
     * Generates quiz questions asynchronously using the Gemini REST API.
     */
    public void generateQuizQuestions(String topic, String difficulty, int numberOfQuestions, @NonNull GeminiCallback callback) {

        backgroundExecutor.execute(() -> {
            HttpURLConnection urlConnection = null;
            try {
                // 1. Construct URL (Now uses corrected BASE_URL)
                URL url = new URL(BASE_URL + MODEL_NAME + METHOD + "?key=" + apiKey);
                Log.d(TAG, "Request URL: " + url.toString()); // Log the final URL

                // 2. Create Connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("Accept", "application/json");
                urlConnection.setDoOutput(true);
                urlConnection.setConnectTimeout(15000); // 15 seconds
                urlConnection.setReadTimeout(30000); // 30 seconds

                // 3. Construct JSON Request Body
                String jsonRequestBody = buildRequestBody(topic, difficulty, numberOfQuestions);
                Log.d(TAG, "Request Body: " + jsonRequestBody);

                // 4. Write Request Body
                try (OutputStream os = urlConnection.getOutputStream()) {
                    byte[] input = jsonRequestBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                // 5. Get Response Code
                int responseCode = urlConnection.getResponseCode();
                Log.i(TAG, "Gemini API Response Code: " + responseCode);

                // 6. Read Response / Error Stream
                InputStream inputStream = (responseCode >= 200 && responseCode < 300) ?
                        urlConnection.getInputStream() : urlConnection.getErrorStream();

                StringBuilder response = new StringBuilder();
                // Ensure inputStream is not null before reading
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
                Log.d(TAG, "Response Body: " + responseBody);


                // 7. Process Response
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String extractedJson = extractGeneratedJson(responseBody); // Extract content
                    if (extractedJson != null) {
                        callback.onSuccess(extractedJson); // Pass cleaned JSON string
                    } else {
                        // Log the body if extraction failed
                        Log.e(TAG, "Failed to extract valid content from successful API response. Body: " + responseBody);
                        callback.onError(new Exception("Failed to extract valid content from API response."));
                    }
                } else {
                    // Handle HTTP error
                    String errorMsg = parseError(responseBody); // Attempt to parse specific error
                    Log.e(TAG, "Gemini API Error: " + responseCode + " - " + errorMsg);
                    callback.onError(new Exception("Gemini API Error: " + responseCode + ". " + errorMsg));
                }

            } catch (Exception e) { // Catch MalformedURLException here too
                Log.e(TAG, "Error during Gemini REST API call", e);
                callback.onError(e);
            } finally {
                // 8. Disconnect
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        });
    }

    /**
     * Builds the JSON request body string for the Gemini API call.
     */
    private String buildRequestBody(String topic, String difficulty, int numberOfQuestions) {
        // Using Gson to easily build the nested JSON structure
        JsonObject root = new JsonObject();
        JsonArray contentsArray = new JsonArray();
        JsonObject contentObject = new JsonObject();
        JsonArray partsArray = new JsonArray();
        JsonObject partObject = new JsonObject();

        @SuppressLint("DefaultLocale") String prompt = String.format(
                "Generate exactly %d multiple-choice quiz questions about the topic '%s'. " +
                        "The difficulty level should be '%s'. " +
                        "Format the output STRICTLY as a valid JSON array where each object has the following keys: " +
                        "\"question\" (string: the question text), " +
                        "\"options\" (JSON array of 4 strings: the answer choices), and " +
                        "\"correctAnswerIndex\" (integer: the 0-based index of the correct answer within the \"options\" array). " +
                        "Do not include any introductory text, explanations, markdown formatting, or anything else outside the single JSON array structure. Only output the JSON array.",
                numberOfQuestions, topic, difficulty
        );
        partObject.addProperty("text", prompt);
        partsArray.add(partObject);
        contentObject.add("parts", partsArray);
        contentsArray.add(contentObject);
        root.add("contents", contentsArray);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.7);
        generationConfig.addProperty("topK", 1);
        generationConfig.addProperty("topP", 1.0);
        generationConfig.addProperty("maxOutputTokens", 2048);
        root.add("generationConfig", generationConfig);

        JsonArray safetySettingsArray = new JsonArray();
        JsonObject safetySetting = new JsonObject();
        safetySetting.addProperty("category", "HARM_CATEGORY_HARASSMENT");
        safetySetting.addProperty("threshold", "BLOCK_MEDIUM_AND_ABOVE");
        safetySettingsArray.add(safetySetting);
        root.add("safetySettings", safetySettingsArray);

        return gson.toJson(root);
    }

    /**
     * Parses the successful JSON response to extract the generated text content,
     * removing potential markdown code fences.
     */
    @Nullable
    private String extractGeneratedJson(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            Log.w(TAG, "Attempted to extract JSON from empty response body.");
            return null;
        }
        try {
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();

            if (jsonResponse.has("promptFeedback")) {
                JsonObject feedback = jsonResponse.getAsJsonObject("promptFeedback");
                if (feedback.has("blockReason")) {
                    String reason = feedback.get("blockReason").getAsString();
                    Log.w(TAG, "Content blocked by API. Reason: " + reason);
                    return null;
                }
            }

            if (jsonResponse.has("candidates")) {
                JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
                    if (firstCandidate.has("content")) {
                        JsonObject content = firstCandidate.getAsJsonObject("content");
                        if (content.has("parts")) {
                            JsonArray parts = content.getAsJsonArray("parts");
                            if (parts != null && !parts.isEmpty()) {
                                JsonObject firstPart = parts.get(0).getAsJsonObject();
                                if (firstPart.has("text")) {
                                    String rawText = firstPart.get("text").getAsString();

                                    // Attempt to remove markdown fences
                                    String cleanedJson = rawText.trim();
                                    // Handle potential ```json ... ``` format
                                    if (cleanedJson.startsWith("```json")) {
                                        cleanedJson = cleanedJson.substring(7).trim(); // Remove ```json and leading/trailing whitespace
                                    } else if (cleanedJson.startsWith("```")) {
                                        cleanedJson = cleanedJson.substring(3).trim(); // Remove ``` and leading/trailing whitespace
                                    }
                                    // Remove trailing ``` if present
                                    if (cleanedJson.endsWith("```")) {
                                        cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3).trim();
                                    }

                                    // Check if it looks like a JSON array
                                    if (cleanedJson.startsWith("[") && cleanedJson.endsWith("]")) {
                                        Log.d(TAG, "Cleaned JSON extracted: " + cleanedJson);
                                        return cleanedJson;
                                    } else {
                                        Log.w(TAG, "Extracted text doesn't look like a JSON array after cleaning: " + cleanedJson);
                                        return null;
                                    }
                                }
                            }
                        }
                    }
                    if (firstCandidate.has("finishReason")) {
                        String reason = firstCandidate.get("finishReason").getAsString();
                        if (!reason.equals("STOP")) {
                            Log.w(TAG, "Candidate finish reason was not STOP: " + reason);
                        }
                    }
                }
            }
            Log.w(TAG, "Could not find generated text in the expected JSON structure.");
            return null;
        } catch (JsonSyntaxException | IllegalStateException | ClassCastException e) {
            Log.e(TAG, "Error parsing Gemini JSON response in extractGeneratedJson", e);
            return null;
        }
    }

    /**
     * Attempts to parse an error message from the API's error response body.
     */
    private String parseError(String errorBody) {
        if (errorBody == null || errorBody.isEmpty()) return "Unknown error";
        try {
            JsonObject jsonError = JsonParser.parseString(errorBody).getAsJsonObject();
            if (jsonError.has("error")) {
                JsonObject errorObj = jsonError.getAsJsonObject("error");
                if (errorObj.has("message")) {
                    return errorObj.get("message").getAsString();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not parse error JSON: " + errorBody);
        }
        return errorBody; // Return raw body if parsing fails
    }
}
