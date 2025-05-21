package com.shaivites.quizion.models;

import java.util.List;

/**
 * Data model representing a single multiple-choice quiz question.
 * Designed to match the JSON structure requested from the Gemini API.
 */
public class QuizQuestion {

    // Fields must match the JSON keys requested in the prompt
    private String question;
    private List<String> options;
    private int correctAnswerIndex;

    // --- Getters ---
    // Gson (or other JSON libraries) uses getters or public fields for deserialization

    public String getQuestion() {
        return question;
    }

    public List<String> getOptions() {
        return options;
    }

    public int getCorrectAnswerIndex() {
        return correctAnswerIndex;
    }

    // --- Optional: Setters (if needed for manual creation/modification) ---

    public void setQuestion(String question) {
        this.question = question;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public void setCorrectAnswerIndex(int correctAnswerIndex) {
        this.correctAnswerIndex = correctAnswerIndex;
    }

    // --- Optional: toString() for debugging ---

    @Override
    public String toString() {
        return "QuizQuestion{" +
                "question='" + question + '\'' +
                ", options=" + options +
                ", correctAnswerIndex=" + correctAnswerIndex +
                '}';
    }
}
