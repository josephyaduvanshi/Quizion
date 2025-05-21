package com.shaivites.quizion.models;

public class ToolItem {
    private final int iconResId;
    private final String title;
    private final String description;

    public ToolItem(int iconResId, String title, String description) {
        this.iconResId = iconResId;
        this.title = title;
        this.description = description;
    }

    public int getIconResId() {
        return iconResId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
