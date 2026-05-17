package com.netflix.contentservice.dto;

import java.time.LocalDateTime;

import com.netflix.contentservice.model.Genre;
import com.netflix.contentservice.model.VideoStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieResponse {

    private String id;

    private String title;

    private String description;

    private Genre genre;

    private String director;

    private String cast;

    private int releaseYear;

    private double rating;

    private String thumbnailUrl;

    private int durationInMinutes;

    // HLS master playlist URL for streaming
    private String hlsMasterPlaylistUrl;

    // status of video processing
    private VideoStatus status;

    private LocalDateTime createdAt;
}
