package com.example.videoshortfirebase;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class AuthService {
    private static final String SUPABASE_URL = "https://oqdoljigjwouthaorjev.supabase.co";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9xZG9samlnandvdXRoYW9yamV2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDQzMzM2NjcsImV4cCI6MjA1OTkwOTY2N30.89WAShh2B0wCxoi5QQDR4COB2YV9vtpmPkBdGcg0wBg";
    private final Context context;
    private final RequestQueue queue;

    public AuthService(Context context) {
        this.context = context;
        this.queue = Volley.newRequestQueue(context);
    }

    public void signup(String email, String password, final Callback callback) {
        String url = SUPABASE_URL + "/auth/v1/signup";
        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
            body.put("password", password);
        } catch (JSONException e) {
            callback.onError(e.toString());
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
                response -> callback.onSuccess(response),
                error -> callback.onError(error.toString())
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("apikey", API_KEY);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        queue.add(request);
    }

    public void login(String email, String password, final Callback callback) {
        String url = SUPABASE_URL + "/auth/v1/token?grant_type=password";
        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
            body.put("password", password);
        } catch (JSONException e) {
            callback.onError(e.toString());
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
                response -> callback.onSuccess(response),
                error -> callback.onError(error.toString())
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("apikey", API_KEY);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        queue.add(request);
    }

    public interface Callback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }
}
