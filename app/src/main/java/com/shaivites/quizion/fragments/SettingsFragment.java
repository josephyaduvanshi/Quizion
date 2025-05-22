package com.shaivites.quizion.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.shaivites.quizion.R;
import com.shaivites.quizion.activities.SplashActivity;
import com.shaivites.quizion.utils.PreferenceHelper;

public class SettingsFragment extends Fragment {

    private MaterialSwitch notificationSwitch;
    private MaterialButton resetProgressButton, logoutButton;
    private MaterialButton themeLightButton, themeDarkButton, themeSystemButton;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        notificationSwitch = view.findViewById(R.id.notificationSwitch);
        resetProgressButton = view.findViewById(R.id.button_reset_progress);
        logoutButton = view.findViewById(R.id.button_logout);
        themeLightButton = view.findViewById(R.id.button_theme_light);
        themeDarkButton = view.findViewById(R.id.button_theme_dark);
        themeSystemButton = view.findViewById(R.id.button_theme_system);

        if (getContext() != null) {
            notificationSwitch.setChecked(PreferenceHelper.getNotificationPreference(getContext()));
        }

        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (getContext() != null) {
                PreferenceHelper.saveNotificationPreference(getContext(), isChecked);
                Toast.makeText(getContext(), "Notification settings " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
            }
        });

        themeLightButton.setOnClickListener(v -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO));
        themeDarkButton.setOnClickListener(v -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES));
        themeSystemButton.setOnClickListener(v -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));


        resetProgressButton.setOnClickListener(v -> {
            if (getContext() == null) return;
            new AlertDialog.Builder(requireContext())
                    .setTitle("Reset Progress")
                    .setMessage("Are you sure you want to reset all your XP, levels, streaks, and topic stats? This action cannot be undone.")
                    .setPositiveButton("Reset", (dialog, which) -> {
                        PreferenceHelper.clearUserProgress(requireContext());
                        Toast.makeText(getContext(), "Progress Reset!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        });

        logoutButton.setOnClickListener(v -> {
            if (getContext() == null || getActivity() == null) return;
            new AlertDialog.Builder(requireContext())
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Logout", (dialog, which) -> {
                        PreferenceHelper.logoutUser(requireContext());
                        Toast.makeText(getContext(), "Logged out successfully.", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(getActivity(), SplashActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        if (getActivity() != null) {
                            getActivity().finishAffinity();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        });
        return view;
    }
}
