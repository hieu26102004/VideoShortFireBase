package com.example.videoshortfirebase;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {
    private EditText emailInput, passwordInput;
    private Button loginBtn, gotoSignup;
    private AuthService authService;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authService = new AuthService(this);
        session = new SessionManager(this);

        emailInput = findViewById(R.id.email);
        passwordInput = findViewById(R.id.password);
        loginBtn = findViewById(R.id.loginBtn);
        gotoSignup = findViewById(R.id.gotoSignup);

        loginBtn.setOnClickListener(v -> {

            android.util.Log.d("LoginActivity", "Login button clicked");

            String email = emailInput.getText().toString();
            String password = passwordInput.getText().toString();

            authService.login(email, password, new AuthService.Callback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        android.util.Log.d("LoginActivity", "Login successful");
                        android.util.Log.d("LoginActivity", "Response: " + response.toString());
                        String token = response.getString("access_token");
                        String refreshToken = response.getString("refresh_token");
                        String userId = response.getJSONObject("user").getString("id");
                        session.saveSession(token, refreshToken, userId);

                        // Chuyển đến MainActivity
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } catch (JSONException e) {
                        android.util.Log.e("LoginActivity", "Error parsing JSON", e);
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(@NonNull String error) {

                    android.util.Log.e("LoginActivity", "Login failed: " + error);


                    Toast.makeText(LoginActivity.this, "Login thất bại: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        gotoSignup.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }
}
