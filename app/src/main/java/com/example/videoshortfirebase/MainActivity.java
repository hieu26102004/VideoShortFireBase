package com.example.videoshortfirebase;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.videoshortfirebase.databinding.ActivityMainBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private SessionManager session;
    private SupabaseConfig supabaseConfig;
    private ActivityMainBinding binding;
    private ViewPager2 vpager;
    private List<VideoItem> videoList = new ArrayList<>();
    private final String SUPABASE_URL = SupabaseConfig.SUPABASE_URL;
    private final String API_KEY = SupabaseConfig.SUPABASE_API_KEY;
    String userId,accessToken;
    ImageView uploaderAvatar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        session = new SessionManager(this);


        accessToken = session.getAccessToken();
        userId = session.getUserId();
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);
        vpager = findViewById(R.id.vpager);
        uploaderAvatar = findViewById(R.id.uploaderAvatar);
        fetchVideos();

        findViewById(R.id.imPerson).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, UploadVideoActivity.class));
        });
        findViewById(R.id.logout).setOnClickListener(v -> {
            logout();
        });
        findViewById(R.id.uploaderAvatar).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, UploadAvatarActivity.class));
        });

        loadAvatar();
    }
    private void sendReaction(String videoId, String reaction) {
        JSONObject json = new JSONObject();
        try {
            json.put("user_id", userId);
            json.put("video_id", videoId);
            json.put("reaction", reaction);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/video_reactions?on_conflict=video_id")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Gửi phản ứng thất bại", Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Phản ứng đã được ghi nhận", Toast.LENGTH_SHORT).show());
                } else {
                    Log.e("ReactionError", response.message());
                }
            }
        });
    }

    private void fetchVideos() {
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/videos?select=title,description,video_url,id,user_id")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .build();

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    Log.d("VideoDebug", "Fetched JSON: " + json);  // 👈 Thêm dòng này

                    try {
                        JSONArray arr = new JSONArray(json);
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            VideoItem item = new VideoItem();
                            item.title = obj.getString("title");
                            item.description = obj.getString("description");
                            item.videoUrl = obj.getString("video_url");
                            item.videoId = obj.getString("id");
                            item.userId = obj.getString("user_id");
                            videoList.add(item);
                        }

                        runOnUiThread(() -> {
                            vpager.setAdapter(new VideoAdapter(MainActivity.this, videoList));
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
    private void logout() {
        // Xóa dữ liệu phiên làm việc
        session.logout();
        // Chuyển hướng người dùng đến màn hình đăng nhập
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // Kết thúc Activity hiện tại
    }
    private void loadAvatar() {
        String avatarUrl = SUPABASE_URL + "/storage/v1/object/public/avatars/" + userId + "/avatar.jpg";

        runOnUiThread(() -> {
            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .error(R.drawable.ic_avatar_placeholder)
                    .into(uploaderAvatar);
        });
    }
}