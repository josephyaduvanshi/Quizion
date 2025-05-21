package com.shaivites.quizion.models;

public class CardItem {

    private final String title;
    private final String description;

    public CardItem(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
