package com.example.medreminderjava;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.medreminderjava.data.DatabaseHelper;
import com.example.medreminderjava.databinding.ActivityRegisterBinding;

import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = DatabaseHelper.getInstance(this);

        setupTextWatchers();

        binding.btnRegister.setOnClickListener(v -> registerUser());
        binding.tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void setupTextWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearErrors();
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        binding.etName.addTextChangedListener(watcher);
        binding.etMobile.addTextChangedListener(watcher);
        binding.etPassword.addTextChangedListener(watcher);
        binding.etAltMobile.addTextChangedListener(watcher);
    }

    private void clearErrors() {
        binding.tilName.setError(null);
        binding.tilMobile.setError(null);
        binding.tilPassword.setError(null);
        binding.tilAltMobile.setError(null);
    }

    private void registerUser() {
        String name = Objects.requireNonNull(binding.etName.getText()).toString().trim();
        String location = Objects.requireNonNull(binding.etLocation.getText()).toString().trim();
        String age = Objects.requireNonNull(binding.etAge.getText()).toString().trim();
        String bloodGroup = Objects.requireNonNull(binding.etBloodGroup.getText()).toString().trim();
        String mobile = Objects.requireNonNull(binding.etMobile.getText()).toString().trim();
        String password = Objects.requireNonNull(binding.etPassword.getText()).toString().trim();
        String altMobile = Objects.requireNonNull(binding.etAltMobile.getText()).toString().trim();

        boolean isValid = true;

        if (name.isEmpty()) {
            binding.tilName.setError("Name is required");
            isValid = false;
        } else if (name.matches(".*\\d.*")) {
            binding.tilName.setError("Name should not contain digits");
            isValid = false;
        }

        if (mobile.isEmpty()) {
            binding.tilMobile.setError("Mobile number is required");
            isValid = false;
        } else if (mobile.length() != 10) {
            binding.tilMobile.setError("Mobile number must be 10 digits");
            isValid = false;
        }

        if (password.isEmpty()) {
            binding.tilPassword.setError("Password is required");
            isValid = false;
        }

        if (!altMobile.isEmpty() && altMobile.length() != 10) {
            binding.tilAltMobile.setError("Alternate mobile must be 10 digits");
            isValid = false;
        }

        if (!isValid) return;

        long result = dbHelper.registerUser(name, location, age, bloodGroup, mobile, password, altMobile);
        if (result != -1) {
            Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        } else {
            Toast.makeText(this, "Registration Failed. Mobile already exists?", Toast.LENGTH_SHORT).show();
        }
    }
}