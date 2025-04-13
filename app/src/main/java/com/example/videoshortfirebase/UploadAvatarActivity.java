package com.example.videoshortfirebase;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadAvatarActivity extends AppCompatActivity {
    static SupabaseConfig supabaseConfig;

    private static final int PICK_AVATAR_REQUEST = 1;
    private Uri avatarUri;
    private Button btnSelectAvatar, btnUploadAvatar;
    private ImageView imgAvatar;
    SessionManager sessionManager;
    private static final String SUPABASE_URL = supabaseConfig.SUPABASE_URL;
    private static final String API_KEY = supabaseConfig.SUPABASE_API_KEY;

    String userId, accessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_avatar);
        sessionManager = new SessionManager(this);

        accessToken = sessionManager.getAccessToken();
        userId = sessionManager.getUserId();

        imgAvatar = findViewById(R.id.imgAvatar);
        btnSelectAvatar = findViewById(R.id.btnSelectAvatar);
        btnUploadAvatar = findViewById(R.id.btnUploadAvatar);

        btnSelectAvatar.setOnClickListener(v -> {
            // Intent chọn ảnh từ thư viện
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_AVATAR_REQUEST);
        });

        btnUploadAvatar.setOnClickListener(v -> {
            if (avatarUri != null) {
                uploadAvatarToSupabase(avatarUri);
            } else {
                Toast.makeText(this, "Vui lòng chọn ảnh trước", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_AVATAR_REQUEST && resultCode == RESULT_OK && data != null) {
            avatarUri = data.getData();
            imgAvatar.setImageURI(avatarUri);
        }
    }

    private void uploadAvatarToSupabase(Uri fileUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            inputStream.close();

            OkHttpClient client = new OkHttpClient();

            // Tải ảnh lên Supabase Storage
            RequestBody body = RequestBody.create(bytes, MediaType.parse("image/*"));
            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/storage/v1/object/avatars/" + userId + "/avatar.jpg")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("Content-Type", "image/*")
                    .addHeader("x-upsert", "true") // Ghi đè nếu tồn tại
                    .put(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Log.d("SUPABASE", "Avatar uploaded");
                        // URL của ảnh vừa upload
                        String avatarUrl = SUPABASE_URL + "/storage/v1/object/avatars/" + userId + "/avatar.jpg";

                        // Cập nhật URL avatar vào bảng user_custom
                        updateUserCustomTableWithAvatar(avatarUrl);

                        runOnUiThread(() -> Toast.makeText(UploadAvatarActivity.this, "Upload thành công", Toast.LENGTH_SHORT).show());
                    } else {
                        String errorBody = response.body().string();
                        Log.e("SUPABASE", "Upload failed: " + response.code() + errorBody);
                        // Xử lý khi token hết hạn
                        if (errorBody.contains("jwt")) {
                            sessionManager.refreshAccessToken(new SessionManager.RefreshCallback() {
                                @Override
                                public void onSuccess(String newAccessToken) {
                                    // Lưu lại access token mới và gọi lại phương thức uploadAvatarToSupabase với token mới
                                    Log.d("RefreshAccessToken", "newAccessToken = " + newAccessToken);
                                    accessToken = newAccessToken;

                                    uploadAvatarToSupabase(fileUri); // Gọi lại hàm upload với token mới
                                }

                                @Override
                                public void onError(String error) {
                                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show());
                                    sessionManager.logout();
                                    startActivity(new Intent(UploadAvatarActivity.this, LoginActivity.class));
                                    finish();
                                }
                            });
                        } else {
                            runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Lỗi tải lên: " + errorBody, Toast.LENGTH_SHORT).show());
                        }
                    }
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateUserCustomTableWithAvatar(String avatarUrl) {
        // Gửi yêu cầu PATCH để cập nhật avatar_url trong bảng user_custom
        OkHttpClient client = new OkHttpClient();
        JSONObject body = new JSONObject();
        try {
            body.put("avatar_url", avatarUrl);  // Lưu URL ảnh vào bảng user_custom

            RequestBody requestBody = RequestBody.create(body.toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/users_custom?id=eq." + userId)  // Cập nhật users custom bằng userId
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("apikey", API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .patch(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Log.d("SUPABASE", "User custom updated with avatar URL");
                    } else {
                        String errorBody = response.body().string();
                        Log.e("SUPABASE", "Failed to update user_custom: " + response.code() + errorBody);
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
