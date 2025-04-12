package com.example.videoshortfirebase;

import android.content.Context;
import android.content.SharedPreferences;


import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SessionManager {
    private static final String PREF_NAME = "user_session";
    private static final String KEY_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveSession(String accessToken, String refreshToken, String userId) {
        editor.putString(KEY_TOKEN, accessToken);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.putString(KEY_USER_ID, userId);
        editor.apply();
    }

    public String getAccessToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }
    public interface RefreshCallback {
        void onSuccess(String newAccessToken);
        void onError(String error);
    }

    public void refreshAccessToken(final RefreshCallback callback) {
        String refreshToken = getRefreshToken();
        if (refreshToken == null) {
            callback.onError("No refresh token available.");
            return;
        }

        // Gọi API để refresh access token bằng refresh token
        OkHttpClient client = new OkHttpClient();
        RequestBody body = new FormBody.Builder()
                .add("refresh_token", refreshToken)
                .build();

        Request request = new Request.Builder()
                .url("https://your-supabase-url/auth/v1/token")
                .addHeader("apikey", "your-api-key")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonResponse = new JSONObject(response.body().string());
                        String newAccessToken = jsonResponse.getString("access_token");
                        String newRefreshToken = jsonResponse.getString("refresh_token");
                        saveSession(newAccessToken, getUserId(), newRefreshToken); // Lưu lại token mới
                        callback.onSuccess(newAccessToken);
                    } catch (JSONException e) {
                        callback.onError(e.getMessage());
                    }
                } else {
                    callback.onError("Failed to refresh token");
                }
            }
        });
    }
}

