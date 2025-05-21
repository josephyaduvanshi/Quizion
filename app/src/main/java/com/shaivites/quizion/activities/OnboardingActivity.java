package com.shaivites.quizion.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.shaivites.quizion.MainActivity;
import com.shaivites.quizion.R;
import com.shaivites.quizion.utils.TypeWriter;

import java.util.Random;

public class OnboardingActivity extends AppCompatActivity {

    private EditText usernameInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        TypeWriter welcomeText = findViewById(R.id.welcomeText);
        welcomeText.setCharacterDelay(50);
        welcomeText.animateText("Welcome to Quizion!");

        usernameInput = findViewById(R.id.usernameInput);
        Button continueBtn = findViewById(R.id.continueBtn);

        // Suggest auto-generated name
        String suggestedName = "Player#" + new Random().nextInt(9999);
        usernameInput.setText(suggestedName);

        continueBtn.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            if (username.isEmpty()) {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save username
            SharedPreferences prefs = getSharedPreferences("quizion_prefs", MODE_PRIVATE);
            ((SharedPreferences) prefs).edit().putString("username", username).apply();

            // Go to main screen
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}
