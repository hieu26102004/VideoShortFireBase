package com.example.videoshortfirebase;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {
    private List<VideoItem> videoList;
    private Context context;
    static SupabaseConfig supabaseConfig;
    private static String SUPABASE_URL = SupabaseConfig.SUPABASE_URL;
    private static String API_KEY = SupabaseConfig.SUPABASE_API_KEY;
    private String accessToken;
    private String userId;
    private OkHttpClient client = new OkHttpClient();
    private SessionManager sessionManager;


    public VideoAdapter(Context context, List<VideoItem> videoList) {
        this.context = context;
        this.videoList = videoList;
        this.sessionManager = new SessionManager(context);
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.single_video_row, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        VideoItem item = videoList.get(position);
        holder.title.setText(item.title);
        holder.description.setText(item.description);
        Log.d("VideoDebug", "Video URL: " + item.videoUrl);
        if (item.videoUrl != null) {
            holder.videoView.setVideoURI(Uri.parse(item.videoUrl));
        } else {
            Log.e("VideoDebug", "Video URL is null");
        }

        holder.progressBar.setVisibility(View.VISIBLE);
        holder.videoView.setVideoURI(Uri.parse(item.videoUrl));

        holder.videoView.setOnPreparedListener(mp -> {
            holder.progressBar.setVisibility(View.GONE);
            mp.setLooping(true);
            holder.videoView.start();
        });
        holder.likeButton.setOnClickListener(v -> {
            sendReaction(item.videoId, "like", holder);
        });

        holder.dislikeButton.setOnClickListener(v -> {
            sendReaction(item.videoId, "dislike", holder);
        });

        loadReactionState(item.videoId, holder);
        loadUploaderInfo(item.userId, holder);
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    public class VideoViewHolder extends RecyclerView.ViewHolder {
        VideoView videoView;
        TextView title, description,likeCount,dislikeCount;
        ProgressBar progressBar;
        ImageView likeButton, dislikeButton;
        ImageView uploaderAvatar;
        TextView uploaderEmail;


        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            videoView = itemView.findViewById(R.id.videoView);
            title = itemView.findViewById(R.id.textVideoTitle);
            description = itemView.findViewById(R.id.textVideoDescription);
            progressBar = itemView.findViewById(R.id.videoProgressBar);
            likeButton = itemView.findViewById(R.id.favorites);
            dislikeButton = itemView.findViewById(R.id.dislike);
            likeCount = itemView.findViewById(R.id.likeCount);
            dislikeCount = itemView.findViewById(R.id.dislikeCount);
            uploaderAvatar = itemView.findViewById(R.id.uploaderAvatar);
            uploaderEmail = itemView.findViewById(R.id.uploaderEmail);

        }
    }
    private void loadUploaderInfo(String userId, VideoViewHolder holder) {
        Log.d("UserID", "ID: " + userId);
        // Sửa URL để truy vấn bảng users_custom thay vì profiles
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/users_custom?id=eq." + userId)  // Truy vấn bảng users_custom
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("UploaderInfo", "Failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                try {
                    JSONArray arr = new JSONArray(body);
                    if (arr.length() > 0) {
                        JSONObject user = arr.getJSONObject(0);
                        String email = user.optString("email", "No email");
                        String avatarUrl = user.optString("avatar_url", null);
                        Log.d("UserID", "Email: " + email + ", Avatar URL: " + avatarUrl);
                        // Cập nhật UI trên thread chính
                        ((Activity) context).runOnUiThread(() -> {
                            holder.uploaderEmail.setText(email);
                            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                // Dùng Glide để load ảnh
                                Glide.with(context)
                                        .load(avatarUrl)
                                        .placeholder(R.drawable.ic_avatar_placeholder)
                                        .circleCrop()
                                        .into(holder.uploaderAvatar);
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private void sendReaction(String videoId, String reaction, VideoViewHolder holder) {
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/video_reactions?video_id=eq." + videoId + "&user_id=eq." + sessionManager.getUserId())
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.e("SendReaction", "Request failed: " + e.getMessage());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                try {
                    JSONArray arr = new JSONArray(body);
                    boolean hasReaction = arr.length() > 0;

                    // Nếu đã có phản ứng, xóa phản ứng
                    if (hasReaction) {
                        JSONObject reactionData = arr.getJSONObject(0);
                        String currentReaction = reactionData.getString("reaction");

                        if (currentReaction.equals(reaction)) {
                            // Hủy phản ứng nếu người dùng nhấn lại nút đã tương tác
                            deleteReaction(videoId, reaction, holder);
                        } else {
                            // Nếu phản ứng khác, thay đổi phản ứng
                            updateReaction(videoId, reaction, holder);
                        }
                    } else {
                        // Nếu chưa có phản ứng, thêm phản ứng mới
                        addReaction(videoId, reaction, holder);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void addReaction(String videoId, String reaction, VideoViewHolder holder) {
        JSONObject json = new JSONObject();
        try {
            json.put("user_id", sessionManager.getUserId());
            json.put("video_id", videoId);
            json.put("reaction", reaction);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/video_reactions?on_conflict=video_id,user_id")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.e("AddReaction", "Request failed: " + e.getMessage());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    ((Activity) context).runOnUiThread(() -> {
                        loadReactionState(videoId, holder);
                    });
                } else {
                    Log.e("AddReaction", "Error: " + response.code());
                }
            }
        });
    }

    private void updateReaction(String videoId, String reaction, VideoViewHolder holder) {
        deleteReaction(videoId, reaction, holder);
        addReaction(videoId, reaction, holder);
    }

    private void deleteReaction(String videoId, String reaction, VideoViewHolder holder) {
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/video_reactions?video_id=eq." + videoId + "&user_id=eq." + sessionManager.getUserId())
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.e("DeleteReaction", "Request failed: " + e.getMessage());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    ((Activity) context).runOnUiThread(() -> {
                        loadReactionState(videoId, holder);
                    });
                } else {
                    Log.e("DeleteReaction", "Error: " + response.code());
                }
            }
        });
    }


    private void loadReactionState(String videoId, VideoViewHolder holder) {
        // Lấy trạng thái phản ứng của người dùng cho video này (xem họ đã "like" hoặc "dislike" chưa)
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/video_reactions?video_id=eq." + videoId)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("Reaction", "Lỗi khi tải trạng thái: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                try {
                    JSONArray arr = new JSONArray(body);
                    ((Activity) context).runOnUiThread(() -> {
                        try {
                            int likeCount = 0;
                            int dislikeCount = 0;

                            if (arr.length() > 0) {
                                // Đếm số lượt like và dislike đúng cách
                                for (int i = 0; i < arr.length(); i++) {
                                    String reaction = arr.getJSONObject(i).getString("reaction");
                                    if (reaction.equals("like")) {
                                        likeCount++;
                                    } else if (reaction.equals("dislike")) {
                                        dislikeCount++;
                                    }
                                }
                            }

                            // Cập nhật UI với số lượt like/dislike
                            holder.likeCount.setText(likeCount + " Likes");
                            holder.dislikeCount.setText(dislikeCount + " Dislikes");

                            // Thay đổi màu nút theo phản ứng của người dùng
                            boolean hasLiked = false;
                            boolean hasDisliked = false;
                            if (arr.length() > 0) {
                                String userReaction = arr.getJSONObject(0).getString("reaction");
                                if (userReaction.equals("like")) {
                                    hasLiked = true;
                                    holder.likeButton.setColorFilter(Color.RED);
                                    holder.dislikeButton.setColorFilter(Color.GRAY);
                                } else if (userReaction.equals("dislike")) {
                                    hasDisliked = true;
                                    holder.likeButton.setColorFilter(Color.GRAY);
                                    holder.dislikeButton.setColorFilter(Color.BLUE);
                                }
                            } else {
                                holder.likeButton.setColorFilter(Color.GRAY);
                                holder.dislikeButton.setColorFilter(Color.GRAY);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void retrySendReaction(String videoId, String reaction, VideoViewHolder holder) {
        sendReaction(videoId, reaction, holder);
    }

}
