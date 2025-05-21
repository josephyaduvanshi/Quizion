package com.shaivites.quizion.models;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;

/**
 * Data model for items displayed in the Topics RecyclerView.
 */
public class TopicItem {
    private String title;
    @DrawableRes private int iconResId; // Keep field even if not used, pass 0
    @ColorRes private int colorResId;   // Resource ID for the background color

    /**
     * Constructor for TopicItem.
     * @param title The name of the topic.
     * @param iconResId The drawable resource ID for the icon (use 0 if no icon).
     * @param colorResId The color resource ID for the card background.
     */
    public TopicItem(String title, @DrawableRes int iconResId, @ColorRes int colorResId) {
        this.title = title;
        this.iconResId = iconResId; // We'll pass 0 for now
        this.colorResId = colorResId;
    }

    // --- Getters ---

    public String getTitle() {
        return title;
    }

    @DrawableRes
    public int getIconResId() {
        return iconResId;
    }

    @ColorRes
    public int getColorResId() {
        return colorResId;
    }

    // --- Optional Setters ---

    public void setTitle(String title) {
        this.title = title;
    }

    public void setIconResId(@DrawableRes int iconResId) {
        this.iconResId = iconResId;
    }

    public void setColorResId(@ColorRes int colorResId) {
        this.colorResId = colorResId;
    }
}
