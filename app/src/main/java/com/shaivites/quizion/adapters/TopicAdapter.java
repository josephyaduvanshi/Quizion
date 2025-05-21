package com.shaivites.quizion.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
// import android.widget.ImageView; // Icon view removed from layout
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.shaivites.quizion.R;
import com.shaivites.quizion.models.TopicItem; // Use TopicItem model
import java.util.List;

/**
 * RecyclerView Adapter for displaying TopicItem data in a grid.
 */
public class TopicAdapter extends RecyclerView.Adapter<TopicAdapter.TopicViewHolder> {

    private final Context context;
    private final List<TopicItem> topicList; // Use List<TopicItem>
    private final OnTopicClickListener listener;

    /**
     * Interface definition for a callback to be invoked when a topic item is clicked.
     */
    public interface OnTopicClickListener {
        void onTopicClick(TopicItem topicItem);
    }

    /**
     * Constructor for TopicAdapter.
     * @param context The application context.
     * @param topicList The list of TopicItem data to display.
     * @param listener The listener for click events.
     */
    public TopicAdapter(Context context, List<TopicItem> topicList, OnTopicClickListener listener) {
        this.context = context;
        this.topicList = topicList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TopicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the specific layout for topic items (item_topic.xml)
        View view = LayoutInflater.from(context).inflate(R.layout.item_topic, parent, false);
        return new TopicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TopicViewHolder holder, int position) {
        // Get the data model based on position
        TopicItem currentTopic = topicList.get(position);

        // Set the topic title text
        holder.topicTitle.setText(currentTopic.getTitle());
        // Icon setting is removed as the ImageView is removed from item_topic.xml

        // Set the background color of the card using the color resource ID from the item
        holder.topicCard.setCardBackgroundColor(ContextCompat.getColor(context, currentTopic.getColorResId()));

        // Set the click listener for the entire item view
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                // Pass the clicked item to the listener
                listener.onTopicClick(currentTopic);
            }
        });
    }

    @Override
    public int getItemCount() {
        // Return the size of your dataset (invoked by the layout manager)
        return topicList.size();
    }

    /**
     * ViewHolder class specific to item_topic.xml layout.
     * Holds references to the views within each item.
     */
    public static class TopicViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView topicCard; // Reference to the card view for background color setting
        TextView topicTitle;        // Reference to the topic title TextView
        // ImageView topicIcon; // Icon view is removed

        public TopicViewHolder(@NonNull View itemView) {
            super(itemView);
            // Find views by their ID within the item layout
            topicCard = itemView.findViewById(R.id.topic_card);
            topicTitle = itemView.findViewById(R.id.topic_title);
            // topicIcon = itemView.findViewById(R.id.topic_icon); // Icon view is removed
        }
    }
}
