package com.shaivites.quizion.adapters;

import android.content.Context; // Added context
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView; // Assuming item_tool uses a card
import com.shaivites.quizion.R;
import com.shaivites.quizion.models.ToolItem;
import java.util.List;

/**
 * RecyclerView Adapter for displaying ToolItem data in the HomeFragment's Tools section.
 */
public class HomeToolsAdapter extends RecyclerView.Adapter<HomeToolsAdapter.ToolViewHolder> {

    private final Context context; // Added context field
    private final List<ToolItem> toolItems;
    private final OnToolClickListener listener; // Added listener field

    /**
     * Interface definition for a callback to be invoked when a tool item is clicked.
     */
    public interface OnToolClickListener {
        void onToolClick(ToolItem toolItem);
    }

    /**
     * Constructor for HomeToolsAdapter.
     * @param context The application context.
     * @param toolItems The list of ToolItem data to display.
     * @param listener The listener for click events.
     */
    // Modified constructor to accept Context and Listener
    public HomeToolsAdapter(Context context, List<ToolItem> toolItems, OnToolClickListener listener) {
        this.context = context;
        this.toolItems = toolItems;
        this.listener = listener;
    }

    // Constructor matching user's original instantiation (less ideal, no click handling outside)
    // Kept for reference, but the constructor above is preferred for Fragment interaction
    // public HomeToolsAdapter(List<ToolItem> toolItems) {
    //     this.toolItems = toolItems;
    //     this.listener = null; // No listener provided
    //     this.context = null; // No context provided
    // }


    @NonNull
    @Override
    public ToolViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for a single tool item
        // Ensure R.layout.item_tool exists and matches the expected views
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tool, parent, false);
        return new ToolViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ToolViewHolder holder, int position) {
        ToolItem currentTool = toolItems.get(position);

        holder.toolTitle.setText(currentTool.getTitle());
        // Assuming item_tool.xml has a description TextView with id tool_description
        if (holder.toolDescription != null && currentTool.getDescription() != null) {
            holder.toolDescription.setText(currentTool.getDescription());
            holder.toolDescription.setVisibility(View.VISIBLE);
        } else if (holder.toolDescription != null) {
            holder.toolDescription.setVisibility(View.GONE);
        }

        // Assuming item_tool.xml has an icon ImageView with id tool_icon
        if (holder.toolIcon != null && currentTool.getIconResId() != 0) {
            holder.toolIcon.setImageResource(currentTool.getIconResId());
            holder.toolIcon.setVisibility(View.VISIBLE);
        } else if (holder.toolIcon != null) {
            holder.toolIcon.setVisibility(View.GONE); // Hide if no icon
        }


        // --- Set OnClickListener on the item view ---
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onToolClick(currentTool); // Notify the listener
            }
            // Avoid handling navigation directly inside the adapter if possible
        });
    }

    @Override
    public int getItemCount() {
        return toolItems.size();
    }

    /**
     * ViewHolder class specific to item_tool.xml layout.
     * Holds references to the views within each tool item.
     */
    public static class ToolViewHolder extends RecyclerView.ViewHolder {
        // Assuming item_tool.xml has these views with these IDs
        ImageView toolIcon;
        TextView toolTitle;
        TextView toolDescription; // Optional description view

        public ToolViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ensure these IDs match your item_tool.xml
            toolIcon = itemView.findViewById(R.id.toolIcon);
            toolTitle = itemView.findViewById(R.id.toolTitle);
            toolDescription = itemView.findViewById(R.id.toolDescription); // May be null if not in layout
        }
    }
}
