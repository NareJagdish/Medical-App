package com.example.medreminderjava;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.medreminderjava.data.DatabaseHelper;
import com.example.medreminderjava.databinding.ActivityLoginBinding;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPrefs;

    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_LOGGED_IN_MOBILE = "logged_in_mobile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (sharedPrefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
            startMainActivity();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = DatabaseHelper.getInstance(this);

        setupTextWatchers();

        binding.btnLogin.setOnClickListener(v -> loginUser());
        binding.tvRegisterLink.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private void setupTextWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tilMobile.setError(null);
                binding.tilPassword.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        binding.etMobile.addTextChangedListener(watcher);
        binding.etPassword.addTextChangedListener(watcher);
    }

    private void loginUser() {
        String mobile = Objects.requireNonNull(binding.etMobile.getText()).toString().trim();
        String password = Objects.requireNonNull(binding.etPassword.getText()).toString().trim();

        boolean isValid = true;
        if (mobile.isEmpty()) {
            binding.tilMobile.setError("Mobile number required");
            isValid = false;
        } else if (mobile.length() != 10) {
            binding.tilMobile.setError("Enter valid 10-digit number");
            isValid = false;
        }

        if (password.isEmpty()) {
            binding.tilPassword.setError("Password required");
            isValid = false;
        }

        if (!isValid) return;

        android.util.Log.d("LoginActivity", "Attempting login for: [" + mobile + "] with password: [" + password + "]");
        if (dbHelper.checkUser(mobile, password)) {
            sharedPrefs.edit()
                    .putBoolean(KEY_IS_LOGGED_IN, true)
                    .putString(KEY_LOGGED_IN_MOBILE, mobile)
                    .apply();
            
            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
            startMainActivity();
        } else {
            binding.tilMobile.setError("Invalid credentials");
            binding.tilPassword.setError("Invalid credentials");
            Toast.makeText(this, "Invalid Mobile or Password", Toast.LENGTH_SHORT).show();
        }
    }

    private void startMainActivity() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}