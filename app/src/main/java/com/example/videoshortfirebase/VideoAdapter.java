package com.example.videoshortfirebase;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {
    private List<VideoItem> videoList;
    private Context context;

    public VideoAdapter(Context context, List<VideoItem> videoList) {
        this.context = context;
        this.videoList = videoList;
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

        holder.progressBar.setVisibility(View.VISIBLE);
        holder.videoView.setVideoURI(Uri.parse(item.videoUrl));

        holder.videoView.setOnPreparedListener(mp -> {
            holder.progressBar.setVisibility(View.GONE);
            mp.setLooping(true);
            holder.videoView.start();
        });
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    public class VideoViewHolder extends RecyclerView.ViewHolder {
        VideoView videoView;
        TextView title, description;
        ProgressBar progressBar;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            videoView = itemView.findViewById(R.id.videoView);
            title = itemView.findViewById(R.id.textVideoTitle);
            description = itemView.findViewById(R.id.textVideoDescription);
            progressBar = itemView.findViewById(R.id.videoProgressBar);
        }
    }
}
