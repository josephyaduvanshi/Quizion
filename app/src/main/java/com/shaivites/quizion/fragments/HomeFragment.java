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
import com.shashank.sony.fancytoastlib.FancyToast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment implements TopicAdapter.OnTopicClickListener, HomeToolsAdapter.OnToolClickListener {

    private static final String TAG = "HomeFragment";

    private ImageView avatarImage;
    private TextView helloText;
    private TextView xpLevelStreakText; // Matches ID: home_xp_level_streak_text
    private RecyclerView toolsRecyclerView;
    private RecyclerView topicsRecyclerView;
    private TextView toolsTitleText; // Matches ID: home_tools_title
    private TextView topicsTitleText; // Matches ID: home_topics_title


    private TopicAdapter topicAdapter;
    private HomeToolsAdapter toolsAdapter;
    private List<TopicItem> topicItems;
    private List<ToolItem> toolItems;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views based on fragment_home.xml IDs
        avatarImage = view.findViewById(R.id.home_avatar_image);
        helloText = view.findViewById(R.id.home_hello_text);
        xpLevelStreakText = view.findViewById(R.id.home_xp_level_streak_text); // Corrected ID
        toolsRecyclerView = view.findViewById(R.id.home_tools_recyclerview);
        topicsRecyclerView = view.findViewById(R.id.home_topics_recyclerview);
        toolsTitleText = view.findViewById(R.id.home_tools_title);
        topicsTitleText = view.findViewById(R.id.home_topics_title);


        setupToolsRecyclerView();
        setupTopicsRecyclerView();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        setupUserInfo();
    }

    private void setupUserInfo() {
        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "Context is null in setupUserInfo");
            return;
        }

        String username = PreferenceHelper.getUsername(context);
        if (username == null || username.trim().isEmpty()) {
            username = "Guest";
        }
        if (helloText != null) {
            helloText.setText(String.format("Hello, %s 👋", username));
        } else {
            Log.e(TAG, "helloText is null");
        }

        int userXP = PreferenceHelper.getXP(context);
        int userLevel = PreferenceHelper.getLevel(context);
        int userStreak = PreferenceHelper.getStreak(context);

        if (xpLevelStreakText != null) { // Corrected variable name
            String userInfo = String.format(Locale.getDefault(),
                    "XP: %d  |  Level: %d  |  Streak: %d 🔥",
                    userXP, userLevel, userStreak);
            xpLevelStreakText.setText(userInfo); // Corrected variable name
        } else {
            Log.e(TAG, "xpLevelStreakText is null");
        }

        String avatarUrl = new DiceBearAvatarGenerator()
                .setSeed(username)
                .setSize(128)
                .setBackgroundColor("b6e3f4","c0aede","d1d4f9", "ffd5dc", "ffdfbf")
                .setBackgroundType("gradientLinear", "solid")
                .setEyes("variant01", "variant02", "variant03", "variant09", "variant12")
                .setMouth("variant01", "variant02", "variant03", "variant05", "variant09")
                .setShape("circle", "rounded")
                .buildUrl()
                .replace("/svg", "/png");

        Log.d(TAG, "Avatar URL: " + avatarUrl);

        if (avatarImage != null) {
            Glide.with(this)
                    .load(avatarUrl)
                    .apply(new RequestOptions()
                            .circleCrop()
                            .placeholder(R.drawable.ic_avatar_placeholder)
                            .error(R.drawable.ic_avatar_placeholder)
                            .diskCacheStrategy(DiskCacheStrategy.ALL))
                    .into(avatarImage);
        } else {
            Log.e(TAG, "avatarImage is null");
        }
    }

    private void setupToolsRecyclerView() {
        Context context = getContext();
        if (toolsRecyclerView == null || context == null) {
            Log.e(TAG, "toolsRecyclerView or context is null in setupToolsRecyclerView!");
            return;
        }

        toolsRecyclerView.setLayoutManager(new GridLayoutManager(context, 2));
        toolsRecyclerView.setHasFixedSize(true);

        toolItems = new ArrayList<>();
        toolItems.add(new ToolItem(R.drawable.ic_quiz, "Quick Quiz", "Test your knowledge"));
        toolItems.add(new ToolItem(R.drawable.ic_ai, "AI Challenge", "Battle an AI"));

        toolsAdapter = new HomeToolsAdapter(context, toolItems, this);
        toolsRecyclerView.setAdapter(toolsAdapter);
    }

    private void setupTopicsRecyclerView() {
        Context context = getContext();
        if (topicsRecyclerView == null || context == null) {
            Log.e(TAG, "topicsRecyclerView or context is null in setupTopicsRecyclerView!");
            return;
        }

        final int SPAN_COUNT = 2;
        GridLayoutManager gridLayoutManager = new GridLayoutManager(context, SPAN_COUNT);
        topicsRecyclerView.setLayoutManager(gridLayoutManager);
        topicsRecyclerView.setHasFixedSize(true);

        topicItems = new ArrayList<>();
        topicItems.add(new TopicItem("General Knowledge", 0, R.color.topic_color_light_blue));
        topicItems.add(new TopicItem("Science", 0, R.color.topic_color_light_pink));
        topicItems.add(new TopicItem("History", 0, R.color.topic_color_light_green));
        topicItems.add(new TopicItem("Movies", 0, R.color.topic_color_light_orange));
        topicItems.add(new TopicItem("Technology", 0, R.color.topic_color_light_purple));
        topicItems.add(new TopicItem("Mathematics", 0, R.color.topic_color_light_teal));

        topicAdapter = new TopicAdapter(context, topicItems, this);
        topicsRecyclerView.setAdapter(topicAdapter);
    }

    @Override
    public void onTopicClick(TopicItem topicItem) {
        if (getContext() == null || getActivity() == null) return;
        FancyToast.makeText(getContext(), "Selected Topic: " + topicItem.getTitle(), FancyToast.LENGTH_SHORT,FancyToast.INFO,false).show();

        Intent intent = new Intent(getActivity(), QuizActivity.class);
        intent.putExtra("TOPIC_TITLE", topicItem.getTitle());
        intent.putExtra("QUIZ_MODE", "TOPIC");
        startActivity(intent);
    }

    @Override
    public void onToolClick(ToolItem toolItem) {
        if (getContext() == null || getActivity() == null) return;
        FancyToast.makeText(getContext(), "Selected Tool: " + toolItem.getTitle(), FancyToast.LENGTH_SHORT,FancyToast.INFO,false).show();

        if (toolItem.getTitle() != null) {
            if (toolItem.getTitle().equals("Quick Quiz")) {
                Intent intent = new Intent(getActivity(), QuizActivity.class);
                intent.putExtra("QUIZ_MODE", "QUICK");
                intent.putExtra("TOPIC_TITLE", "General Knowledge");
                startActivity(intent);
            } else if (toolItem.getTitle().equals("AI Challenge")) {
                Intent intent = new Intent(getActivity(), QuizActivity.class);
                intent.putExtra("QUIZ_MODE", "AI_CHALLENGE");
                intent.putExtra("TOPIC_TITLE", "Mixed AI Questions");
                startActivity(intent);
            }
        }
    }
}
