package com.example.videoshortfirebase;
import android.os.Handler;
import android.os.Looper;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VideoRepository {
    public interface VideoCallback {
        void onSuccess(List<VideoItem> videoList);
        void onError(String error);
    }

    public static void fetchVideos(VideoCallback callback) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://oqdoljigjwouthaorjev.supabase.co/rest/v1/videos?select=*")
                .addHeader("apikey", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9xZG9samlnandvdXRoYW9yamV2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDQzMzM2NjcsImV4cCI6MjA1OTkwOTY2N30.89WAShh2B0wCxoi5QQDR4COB2YV9vtpmPkBdGcg0wBg")
                .addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9xZG9samlnandvdXRoYW9yamV2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDQzMzM2NjcsImV4cCI6MjA1OTkwOTY2N30.89WAShh2B0wCxoi5QQDR4COB2YV9vtpmPkBdGcg0wBg")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                List<VideoItem> videos = new ArrayList<>();
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    try {
                        JSONArray array = new JSONArray(json);
//                        for (int i = 0; i < array.length(); i++) {
//                            JSONObject obj = array.getJSONObject(i);
//                            videos.add(new VideoItem(
//                                    obj.getString("video_url"),
//                                    obj.getString("title"),
//                                    obj.getString("description")
//                            ));
//                        }
                        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(videos));
                    } catch (Exception e) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onError("Parse error"));
                    }
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Error: " + response.code()));
                }
            }
        });
    }
}
