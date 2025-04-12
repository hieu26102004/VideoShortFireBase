package com.example.videoshortfirebase;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private SessionManager session;
    private SupabaseConfig supabaseConfig;
    private ActivityMainBinding binding;
    private ViewPager2 vpager;
    private List<VideoItem> videoList = new ArrayList<>();
    private String SUPABASE_URL = SupabaseConfig.SUPABASE_URL;
    private String API_KEY = SupabaseConfig.SUPABASE_API_KEY;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        session = new SessionManager(this);

        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);
        vpager = findViewById(R.id.vpager);
        fetchVideos();
        findViewById(R.id.imPerson).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, UploadVideoActivity.class));
        });
    }

    private void fetchVideos() {
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/videos?select=title,description,video_url")
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
}