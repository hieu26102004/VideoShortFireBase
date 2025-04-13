package com.example.videoshortfirebase;

public class VideoItem {
    public String videoUrl;
    public String title;
    public String description;
    public String videoId;
    public String userId;
    public VideoItem() {
    }

    public VideoItem(String videoUrl, String title, String description, String videoId, String userId) {
        this.videoUrl = videoUrl;
        this.title = title;
        this.description = description;
        this.videoId = videoId;
        this.userId = userId;
    }
}

