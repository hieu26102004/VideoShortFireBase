package com.example.videoshortfirebase;

public class VideoItem {
    public String videoUrl;
    public String title;
    public String description;
    public VideoItem(){

    }
    public VideoItem(String videoUrl, String title, String description) {
        this.videoUrl = videoUrl;
        this.title = title;
        this.description = description;
    }
}
