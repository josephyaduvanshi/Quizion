package com.shaivites.quizion.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.shaivites.quizion.MainActivity;
import com.shaivites.quizion.activities.OnboardingActivity;
import com.shaivites.quizion.utils.PreferenceHelper;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (PreferenceHelper.isUserLoggedIn(this)) {
            startActivity(new Intent(this, MainActivity.class));
        } else {
            startActivity(new Intent(this, OnboardingActivity.class));
        }
        finish(); // close splash activity
    }
}
