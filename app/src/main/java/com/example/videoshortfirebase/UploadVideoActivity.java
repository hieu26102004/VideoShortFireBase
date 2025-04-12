package com.example.videoshortfirebase;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.*;

public class UploadVideoActivity extends AppCompatActivity {
    private SupabaseConfig supabaseConfig;

    private static final int VIDEO_PICK_CODE = 101;
    private Uri selectedVideoUri;
    private EditText edtTitle, edtDescription;
    private String SUPABASE_PROJECT = SupabaseConfig.SUPABASE_URL;
    private String SUPABASE_BUCKET = "videos";
    private String API_KEY = SupabaseConfig.SUPABASE_API_KEY;
    String userId,accessToken;

    private SessionManager session;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_video);
        session = new SessionManager(this);
        accessToken = session.getAccessToken();
        userId = session.getUserId();
        if (accessToken == null || accessToken.isEmpty()) {
            Toast.makeText(this, "Token bị thiếu hoặc sai", Toast.LENGTH_SHORT).show();
            Log.d("AuthDebug", "accessToken = " + accessToken);

            return;
        }
        Button btnSelect = findViewById(R.id.btnSelectVideo);
        Button btnUpload = findViewById(R.id.btnUpload);
        edtTitle = findViewById(R.id.edtTitle);
        edtDescription = findViewById(R.id.edtDescription);

        btnSelect.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_PICK);
            i.setType("video/*");
            startActivityForResult(i, VIDEO_PICK_CODE);
        });

        btnUpload.setOnClickListener(v -> {
            if (selectedVideoUri != null) {
                uploadVideoToSupabase(selectedVideoUri);
            } else {
                Toast.makeText(this, "Chưa chọn video", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VIDEO_PICK_CODE && resultCode == RESULT_OK && data != null) {
            selectedVideoUri = data.getData();
        }
    }

    private void uploadVideoToSupabase(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            byte[] bytes = readAllBytes(is);

            String fileName = "video_" + System.currentTimeMillis() + ".mp4";

            RequestBody requestBody = RequestBody.create(bytes, MediaType.parse("video/mp4"));
            Request request = new Request.Builder()
                    .url(SUPABASE_PROJECT + "/storage/v1/object/" + SUPABASE_BUCKET + "/" + fileName)
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("Content-Type", "video/mp4")
                    .put(requestBody)
                    .build();

            OkHttpClient client = new OkHttpClient();
            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Tải video thất bại", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String publicUrl = SUPABASE_PROJECT + "/storage/v1/object/public/" + SUPABASE_BUCKET + "/" + fileName;
                        saveVideoInfo(publicUrl);
                    } else {
                        String errorBody = response.body().string();
                        Log.e("UploadError", "Upload failed: " + errorBody);

                        // Xử lý khi token hết hạn
                        if (errorBody.contains("jwt")) {
                            session.refreshAccessToken(new SessionManager.RefreshCallback() {
                                @Override
                                public void onSuccess(String newAccessToken) {
                                    // Lưu lại access token mới và gọi lại phương thức uploadVideoToSupabase với token mới
                                    Log.d("RefreshAccessToken", "newAccessToken = " + newAccessToken);
                                    accessToken = newAccessToken;

                                    uploadVideoToSupabase(uri); // Gọi lại hàm upload với token mới
                                }

                                @Override
                                public void onError(String error) {
                                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show());
                                    session.logout();
                                    startActivity(new Intent(UploadVideoActivity.this, LoginActivity.class));
                                    finish();
                                }
                            });
                        } else {
                            runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Lỗi tải lên: " + errorBody, Toast.LENGTH_SHORT).show());
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveVideoInfo(String videoUrl) {
        JSONObject json = new JSONObject();
        try {
            json.put("title", edtTitle.getText().toString());
            json.put("description", edtDescription.getText().toString());
            json.put("video_url", videoUrl);
            json.put("user_id", userId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(SUPABASE_PROJECT + "/rest/v1/videos")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Lưu thông tin thất bại", Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(Call call, Response response) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Tải lên thành công", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[8192];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }
}
