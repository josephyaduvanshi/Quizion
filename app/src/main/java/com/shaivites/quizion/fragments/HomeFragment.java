package com.shaivites.quizion.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.shaivites.quizion.R;
import com.shaivites.quizion.activities.QuizActivity; // Import QuizActivity
import com.shaivites.quizion.adapters.HomeToolsAdapter; // Adapter for Tools section
import com.shaivites.quizion.adapters.TopicAdapter; // Adapter for Topics section
import com.shaivites.quizion.models.ToolItem; // Model for Tools section
import com.shaivites.quizion.models.TopicItem; // Model for Topics section
import com.shaivites.quizion.utils.PreferenceHelper;
import com.shaivites.quizion.utils.DiceBearAvatarGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for the Home screen, displaying user info, tools, and topics.
 * Implements listeners for both Tool and Topic item clicks.
 */
// Implement BOTH listener interfaces
public class HomeFragment extends Fragment implements TopicAdapter.OnTopicClickListener, HomeToolsAdapter.OnToolClickListener {

    private static final String TAG = "HomeFragment";

    // Declare views
    private ImageView avatarImage;
    private TextView helloText;
    private TextView xpText;
    private RecyclerView toolsRecyclerView;
    private RecyclerView topicsRecyclerView;

    // Adapters and Data Lists
    private TopicAdapter topicAdapter;
    private HomeToolsAdapter toolsAdapter;
    private List<TopicItem> topicItems;
    private List<ToolItem> toolItems;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // --- Retrieve view references using findViewById ---
        avatarImage = view.findViewById(R.id.avatarImage);
        helloText = view.findViewById(R.id.helloText);
        xpText = view.findViewById(R.id.xpText);
        toolsRecyclerView = view.findViewById(R.id.toolsRecyclerView);
        topicsRecyclerView = view.findViewById(R.id.topicsRecyclerView);

        // --- Call setup methods ---
        setupUserInfo();
        setupToolsRecyclerView();   // Setup tools section
        setupTopicsRecyclerView(); // Setup topics section

        return view;
    }

    /**
     * Sets up the user greeting, avatar image, and XP text.
     */
    private void setupUserInfo() {
        String username = PreferenceHelper.getUsername(requireContext());
        if (username == null || username.trim().isEmpty()) {
            username = "Guest";
        }
        if (helloText != null) {
            helloText.setText("Hello, " + username + " 👋");
        } else {
            Log.e(TAG, "helloText view not found!");
        }

        if (xpText == null) {
            Log.e(TAG, "xpText view not found!");
        } // Else: it displays the static text from XML for now

        String avatarUrl = new DiceBearAvatarGenerator()
                .setSeed(username)
                .setSize(128)
                .buildUrl()
                .replace("/svg", "/png"); // Request PNG
        Log.d(TAG, "Avatar URL (requesting PNG): " + avatarUrl);

        if (avatarImage != null) {
            Glide.with(this)
                    .load(avatarUrl)
                    .apply(new RequestOptions()
                            .circleCrop()
                            .placeholder(R.drawable.ic_avatar_placeholder) // Use original placeholder ID
                            .error(R.drawable.ic_avatar_placeholder)       // Use original error ID
                            .diskCacheStrategy(DiskCacheStrategy.ALL))
                    .into(avatarImage);
        } else {
            Log.e(TAG, "avatarImage view not found!");
        }
    }

    /**
     * Sets up the RecyclerView for the "Tools" section using HomeToolsAdapter.
     * Passes 'this' as the click listener.
     */
    private void setupToolsRecyclerView() {
        if (toolsRecyclerView == null) {
            Log.e(TAG, "toolsRecyclerView view not found!");
            return;
        }

        toolsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        toolsRecyclerView.setHasFixedSize(true);

        toolItems = new ArrayList<>();
        // Assuming R.drawable.ic_quiz and R.drawable.ic_ai are your PNG files
        toolItems.add(new ToolItem(R.drawable.ic_quiz, "Quick Quiz", "Test your reflexes"));
        toolItems.add(new ToolItem(R.drawable.ic_ai, "AI Rumble", "Battle smart"));

        // Instantiate HomeToolsAdapter, passing context and 'this' as the listener
        toolsAdapter = new HomeToolsAdapter(requireContext(), toolItems, this);
        toolsRecyclerView.setAdapter(toolsAdapter);
    }

    /**
     * Sets up the RecyclerView for the "Topics" section using TopicAdapter.
     * Uses variable span grid layout. Passes 'this' as the click listener.
     */
    private void setupTopicsRecyclerView() {
        if (topicsRecyclerView == null) {
            Log.e(TAG, "topicsRecyclerView view not found!");
            return;
        }

        final int SPAN_COUNT = 2;
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), SPAN_COUNT);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return (position == 0) ? SPAN_COUNT : 1; // First item spans 2 columns
            }
        });

        topicsRecyclerView.setLayoutManager(gridLayoutManager);
        topicsRecyclerView.setHasFixedSize(true);

        topicItems = new ArrayList<>();
        // Populate topics data
        topicItems.add(new TopicItem("Math Problem", 0, R.color.topic_color_light_purple)); // Wide
        topicItems.add(new TopicItem("Quizzes & Tests", 0, R.color.topic_color_light_pink));  // Narrow
        topicItems.add(new TopicItem("Physics", 0, R.color.topic_color_light_green)); // Narrow
        topicItems.add(new TopicItem("Biology", 0, R.color.topic_color_light_teal));   // Narrow
        topicItems.add(new TopicItem("Chemistry", 0, R.color.topic_color_light_yellow)); // Narrow

        // Instantiate TopicAdapter, passing context and 'this' as the listener
        topicAdapter = new TopicAdapter(requireContext(), topicItems, this);
        topicsRecyclerView.setAdapter(topicAdapter);
    }

    /**
     * Handles clicks on items in the Topics RecyclerView.
     * @param topicItem The TopicItem that was clicked.
     */
    @Override
    public void onTopicClick(TopicItem topicItem) {
        Toast.makeText(getContext(), "Selected Topic: " + topicItem.getTitle(), Toast.LENGTH_SHORT).show();

        // Start QuizActivity, passing the selected topic and a generic mode
        Intent intent = new Intent(getActivity(), QuizActivity.class);
        intent.putExtra("TOPIC_TITLE", topicItem.getTitle());
        intent.putExtra("QUIZ_MODE", "TOPIC"); // Example mode for topic quizzes
        startActivity(intent);
    }

    /**
     * Handles clicks on items in the Tools RecyclerView.
     * @param toolItem The ToolItem that was clicked.
     */
    @Override
    public void onToolClick(ToolItem toolItem) {
        Toast.makeText(getContext(), "Selected Tool: " + toolItem.getTitle(), Toast.LENGTH_SHORT).show();

        // --- Check if "Quick Quiz" was clicked ---
        if (toolItem.getTitle() != null && toolItem.getTitle().equals("Quick Quiz")) {
            // Start QuizActivity for Quick Quiz mode
            Intent intent = new Intent(getActivity(), QuizActivity.class);
            intent.putExtra("QUIZ_MODE", "QUICK"); // Set the mode
            // Optionally set a default topic or let QuizActivity handle it
            intent.putExtra("TOPIC_TITLE", "General Knowledge");
            startActivity(intent);
        } else if (toolItem.getTitle() != null && toolItem.getTitle().equals("AI Rumble")) {
            // TODO: Handle AI Rumble click (maybe navigate to a setup screen or QuizActivity with "AI" mode)
            Intent intent = new Intent(getActivity(), QuizActivity.class);
            intent.putExtra("QUIZ_MODE", "AI");
            intent.putExtra("TOPIC_TITLE", "Random AI"); // Example
            startActivity(intent);
        }
        // Add more else-if blocks for other tools
    }

    // No need for onDestroyView if not using ViewBinding
}
