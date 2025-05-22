package com.shaivites.quizion.fragments;

import android.content.Context;
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
import com.shaivites.quizion.activities.QuizActivity;
import com.shaivites.quizion.adapters.HomeToolsAdapter;
import com.shaivites.quizion.adapters.TopicAdapter;
import com.shaivites.quizion.models.ToolItem;
import com.shaivites.quizion.models.TopicItem;
import com.shaivites.quizion.utils.PreferenceHelper;
import com.shaivites.quizion.utils.DiceBearAvatarGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment implements TopicAdapter.OnTopicClickListener, HomeToolsAdapter.OnToolClickListener {

    private static final String TAG = "HomeFragment";

    private ImageView avatarImage;
    private TextView helloText;
    private TextView xpText; // This will display XP, Level, and Streak
    private RecyclerView toolsRecyclerView;
    private RecyclerView topicsRecyclerView;

    private TopicAdapter topicAdapter;
    private HomeToolsAdapter toolsAdapter;
    private List<TopicItem> topicItems;
    private List<ToolItem> toolItems;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        avatarImage = view.findViewById(R.id.avatarImage);
        helloText = view.findViewById(R.id.helloText);
        xpText = view.findViewById(R.id.xpText);
        toolsRecyclerView = view.findViewById(R.id.toolsRecyclerView);
        topicsRecyclerView = view.findViewById(R.id.topicsRecyclerView);

        // Setup methods called here, but data display will be refreshed in onResume
        setupToolsRecyclerView();
        setupTopicsRecyclerView();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh user info every time the fragment is resumed
        setupUserInfo();
    }

    private void setupUserInfo() {
        Context context = getContext();
        if (context == null) return;

        String username = PreferenceHelper.getUsername(context);
        if (username == null || username.trim().isEmpty()) {
            username = "Guest"; // Default if somehow not set
        }
        if (helloText != null) {
            helloText.setText(String.format("Hello, %s 👋", username));
        }

        int userXP = PreferenceHelper.getXP(context);
        int userLevel = PreferenceHelper.getLevel(context); // Calculated
        int userStreak = PreferenceHelper.getStreak(context); // Also checks and resets if needed

        if (xpText != null) {
            String userInfo = String.format(Locale.getDefault(),
                    "XP: %d | Level: %d | Streak: %d 🔥",
                    userXP, userLevel, userStreak);
            xpText.setText(userInfo);
        }

        // Avatar setup using DiceBear
        String avatarUrl = new DiceBearAvatarGenerator()
                .setSeed(username) // Use username as seed for consistency
                .setSize(128)      // Define avatar size
                .setBackgroundColor("b6e3f4","c0aede","d1d4f9", "ffd5dc", "ffdfbf") // Example pastel colors
                .setBackgroundType("gradientLinear", "solid") // Add variety
                .setEyes("variant01", "variant02", "variant03", "variant09", "variant12") // More eye variety
                .setMouth("variant01", "variant02", "variant03", "variant05", "variant09") // More mouth variety
                .setShape("circle", "rounded", "square") // Different shapes
                .buildUrl()
                .replace("/svg", "/png"); // Ensure PNG for Glide

        Log.d(TAG, "Avatar URL (requesting PNG): " + avatarUrl);

        if (avatarImage != null) {
            Glide.with(this)
                    .load(avatarUrl)
                    .apply(new RequestOptions()
                            .circleCrop()
                            .placeholder(R.drawable.ic_avatar_placeholder)
                            .error(R.drawable.ic_avatar_placeholder)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)) // Cache avatar
                    .into(avatarImage);
        }
    }

    private void setupToolsRecyclerView() {
        if (toolsRecyclerView == null || getContext() == null) {
            Log.e(TAG, "toolsRecyclerView or context is null!");
            return;
        }

        toolsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        toolsRecyclerView.setHasFixedSize(true); // If item sizes don't change

        toolItems = new ArrayList<>();
        // Make sure these drawables exist (e.g., ic_quiz.png, ic_ai.png)
        toolItems.add(new ToolItem(R.drawable.ic_quiz, "Quick Quiz", "Test your knowledge"));
        toolItems.add(new ToolItem(R.drawable.ic_ai, "AI Challenge", "Battle an AI"));
        // Add more tools as needed

        toolsAdapter = new HomeToolsAdapter(requireContext(), toolItems, this);
        toolsRecyclerView.setAdapter(toolsAdapter);
    }

    private void setupTopicsRecyclerView() {
        if (topicsRecyclerView == null || getContext() == null) {
            Log.e(TAG, "topicsRecyclerView or context is null!");
            return;
        }

        final int SPAN_COUNT = 2;
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), SPAN_COUNT);
        // Example of making the first item span full width, others half
        // Adjust if your design differs
        // gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
        //     @Override
        //     public int getSpanSize(int position) {
        //         // Example: if you want the first item to be wider
        //         // return (position == 0 && topicItems.size() > 1) ? SPAN_COUNT : 1;
        //         return 1; // Default to all items being same width in grid
        //     }
        // });

        topicsRecyclerView.setLayoutManager(gridLayoutManager);
        topicsRecyclerView.setHasFixedSize(true);

        topicItems = new ArrayList<>();
        // Ensure these R.color values exist in your colors.xml
        topicItems.add(new TopicItem("General Knowledge", 0, R.color.topic_color_light_blue));
        topicItems.add(new TopicItem("Science", 0, R.color.topic_color_light_pink));
        topicItems.add(new TopicItem("History", 0, R.color.topic_color_light_green));
        topicItems.add(new TopicItem("Movies", 0, R.color.topic_color_light_orange));
        topicItems.add(new TopicItem("Technology", 0, R.color.topic_color_light_purple));
        topicItems.add(new TopicItem("Mathematics", 0, R.color.topic_color_light_teal));


        topicAdapter = new TopicAdapter(requireContext(), topicItems, this);
        topicsRecyclerView.setAdapter(topicAdapter);
    }

    @Override
    public void onTopicClick(TopicItem topicItem) {
        if (getContext() == null) return;
        Toast.makeText(getContext(), "Selected Topic: " + topicItem.getTitle(), Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(getActivity(), QuizActivity.class);
        intent.putExtra("TOPIC_TITLE", topicItem.getTitle());
        intent.putExtra("QUIZ_MODE", "TOPIC");
        startActivity(intent);
    }

    @Override
    public void onToolClick(ToolItem toolItem) {
        if (getContext() == null) return;
        Toast.makeText(getContext(), "Selected Tool: " + toolItem.getTitle(), Toast.LENGTH_SHORT).show();

        if (toolItem.getTitle() != null && toolItem.getTitle().equals("Quick Quiz")) {
            Intent intent = new Intent(getActivity(), QuizActivity.class);
            intent.putExtra("QUIZ_MODE", "QUICK");
            intent.putExtra("TOPIC_TITLE", "General Knowledge"); // Default for quick quiz
            startActivity(intent);
        } else if (toolItem.getTitle() != null && toolItem.getTitle().equals("AI Challenge")) {
            // For AI Challenge, you might have a specific topic or let user choose
            Intent intent = new Intent(getActivity(), QuizActivity.class);
            intent.putExtra("QUIZ_MODE", "AI_CHALLENGE"); // A new mode
            intent.putExtra("TOPIC_TITLE", "Mixed AI Questions"); // Example topic for AI
            startActivity(intent);
            // Toast.makeText(getContext(), "AI Challenge coming soon!", Toast.LENGTH_SHORT).show();
        }
    }
}