package com.example.videoshortfirebase;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;
public class RegisterActivity extends AppCompatActivity {
    private EditText emailInput, passwordInput;
    private Button registerBtn, gotoLogin;
    private AuthService authService;
    private SessionManager session;

    @Override
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
                    try {
                        String token = response.getString("access_token");
                        String refreshToken = response.getString("refresh_token");
                        String userId = response.getJSONObject("user").getString("id");
                        session.saveSession(token, refreshToken, userId);


                        // Chuyển đến MainActivity
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(RegisterActivity.this, "Register thất bại: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        gotoLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });
    }
}
