package com.awsomefox.sprocket.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

@Entity
public class TrackEntity {

    @PrimaryKey
    @NotNull
    public final String ratingKey;
    public String libraryId;
    public String key;
    public String parentKey;
    public String title;
    public String albumTitle;
    public String artistTitle;
    public String thumb;
    public String source;
    public String uri;
    public int index;
    public boolean recent;
    public long duration;
    public long viewCount;
    public long viewOffset;
    public long queueItemId;

    public TrackEntity(@NotNull String ratingKey, String libraryId, String key, String parentKey,
                       String title, String albumTitle, String artistTitle, String thumb,
                       String source, String uri, int index, boolean recent, long duration,
                       long viewCount, long viewOffset, long queueItemId) {
        this.ratingKey = ratingKey;
        this.libraryId = libraryId;
        this.key = key;
        this.parentKey = parentKey;
        this.title = title;
        this.albumTitle = albumTitle;
        this.artistTitle = artistTitle;
        this.thumb = thumb;
        this.source = source;
        this.uri = uri;
        this.index = index;
        this.recent = recent;
        this.duration = duration;
        this.viewCount = viewCount;
        this.viewOffset = viewOffset;
        this.queueItemId = queueItemId;
    }
}