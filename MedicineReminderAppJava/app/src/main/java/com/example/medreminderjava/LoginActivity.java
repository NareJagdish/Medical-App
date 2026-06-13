package com.example.medreminderjava;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.medreminderjava.data.DatabaseHelper;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPrefs;
    private EditText etMobile, etPassword;
    private TextInputLayout tilMobile, tilPassword;

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

        setContentView(R.layout.activity_login);

        dbHelper = DatabaseHelper.getInstance(this);

        etMobile = findViewById(R.id.etMobile);
        etPassword = findViewById(R.id.etPassword);
        tilMobile = findViewById(R.id.tilMobile);
        tilPassword = findViewById(R.id.tilPassword);

        setupTextWatchers();

        findViewById(R.id.btnLogin).setOnClickListener(v -> loginUser());
        findViewById(R.id.tvRegisterLink).setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private void setupTextWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilMobile.setError(null);
                tilPassword.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        etMobile.addTextChangedListener(watcher);
        etPassword.addTextChangedListener(watcher);
    }

    private void loginUser() {
        String mobile = etMobile.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        boolean isValid = true;
        if (mobile.isEmpty()) {
            tilMobile.setError("Mobile number required");
            isValid = false;
        } else if (mobile.length() != 10) {
            tilMobile.setError("Enter valid 10-digit number");
            isValid = false;
        }

        if (password.isEmpty()) {
            tilPassword.setError("Password required");
            isValid = false;
        }

        if (!isValid) return;

        if (dbHelper.checkUser(mobile, password)) {
            sharedPrefs.edit()
                    .putBoolean(KEY_IS_LOGGED_IN, true)
                    .putString(KEY_LOGGED_IN_MOBILE, mobile)
                    .apply();
            
            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
            startMainActivity();
        } else {
            tilMobile.setError("Invalid credentials");
            tilPassword.setError("Invalid credentials");
            Toast.makeText(this, "Invalid Mobile or Password", Toast.LENGTH_SHORT).show();
        }
    }

    private void startMainActivity() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}
