package com.example.videoshortfirebase;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import org.json.JSONException;
import org.json.JSONObject;


public class RegisterActivity extends AppCompatActivity {
    private EditText emailInput, passwordInput;
    private Button registerBtn, gotoLogin;
    private AuthService authService;
    private SessionManager session;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authService = new AuthService(this);
        session = new SessionManager(this);

        emailInput = findViewById(R.id.email);
        passwordInput = findViewById(R.id.password);
        registerBtn = findViewById(R.id.registerBtn);
        gotoLogin = findViewById(R.id.gotoLogin);

        registerBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString();
            String password = passwordInput.getText().toString();

            authService.signup(email, password, new AuthService.Callback() {
                @Override
                public void onSuccess(JSONObject response) {
                    // Chuyển đến màn hình đăng nhập sau khi insert thành công
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
                @Override
                public void onError(String error) {
                    Log.e("RegisterActivity", "Register failed: " + error);
                    Toast.makeText(RegisterActivity.this, "Register thất bại: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        gotoLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });
    }
}
