package com.shaivites.quizion;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.shaivites.quizion.activities.OnboardingActivity;
import com.shaivites.quizion.fragments.HomeFragment;
import com.shaivites.quizion.fragments.ProfileFragment;
import com.shaivites.quizion.fragments.SettingsFragment;
import com.shaivites.quizion.utils.PreferenceHelper;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!PreferenceHelper.isUserLoggedIn(this)) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        loadFragment(new HomeFragment());

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            if (item.getItemId() == R.id.nav_home) selectedFragment = new HomeFragment();
            else if (item.getItemId() == R.id.nav_profile) selectedFragment = new ProfileFragment();
            else if (item.getItemId() == R.id.nav_settings) selectedFragment = new SettingsFragment();
            return loadFragment(selectedFragment);
        });
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }
}