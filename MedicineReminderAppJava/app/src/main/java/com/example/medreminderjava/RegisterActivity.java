package com.example.medreminderjava;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.medreminderjava.data.DatabaseHelper;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private EditText etName, etLocation, etAge, etBloodGroup, etMobile, etPassword, etAltMobile;
    private TextInputLayout tilName, tilMobile, tilPassword, tilAltMobile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        dbHelper = DatabaseHelper.getInstance(this);

        etName = findViewById(R.id.etName);
        etLocation = findViewById(R.id.etLocation);
        etAge = findViewById(R.id.etAge);
        etBloodGroup = findViewById(R.id.etBloodGroup);
        etMobile = findViewById(R.id.etMobile);
        etPassword = findViewById(R.id.etPassword);
        etAltMobile = findViewById(R.id.etAltMobile);

        tilName = findViewById(R.id.tilName);
        tilMobile = findViewById(R.id.tilMobile);
        tilPassword = findViewById(R.id.tilPassword);
        tilAltMobile = findViewById(R.id.tilAltMobile);

        setupTextWatchers();

        findViewById(R.id.btnRegister).setOnClickListener(v -> registerUser());
        findViewById(R.id.tvLoginLink).setOnClickListener(v -> {
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
        etName.addTextChangedListener(watcher);
        etMobile.addTextChangedListener(watcher);
        etPassword.addTextChangedListener(watcher);
        etAltMobile.addTextChangedListener(watcher);
    }

    private void clearErrors() {
        tilName.setError(null);
        tilMobile.setError(null);
        tilPassword.setError(null);
        tilAltMobile.setError(null);
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String age = etAge.getText().toString().trim();
        String bloodGroup = etBloodGroup.getText().toString().trim();
        String mobile = etMobile.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String altMobile = etAltMobile.getText().toString().trim();

        boolean isValid = true;

        if (name.isEmpty()) {
            tilName.setError("Name is required");
            isValid = false;
        } else if (name.matches(".*\\d.*")) {
            tilName.setError("Name should not contain digits");
            isValid = false;
        }

        if (mobile.isEmpty()) {
            tilMobile.setError("Mobile number is required");
            isValid = false;
        } else if (mobile.length() != 10) {
            tilMobile.setError("Mobile number must be 10 digits");
            isValid = false;
        }

        if (password.isEmpty()) {
            tilPassword.setError("Password is required");
            isValid = false;
        }

        if (!altMobile.isEmpty() && altMobile.length() != 10) {
            tilAltMobile.setError("Alternate mobile must be 10 digits");
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
