package com.awsomefox.sprocket.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

@Entity
public class BookEntity {

    @PrimaryKey
    @NotNull
    public final String ratingKey;
    public String libraryId;
    public String title;
    public String artistTitle;
    public String thumb;
    public String uri;

    public BookEntity(@NotNull String ratingKey, String libraryId, String title, String artistTitle,
                      String thumb, String uri) {
        this.ratingKey = ratingKey;
        this.libraryId = libraryId;
        this.title = title;
        this.artistTitle = artistTitle;
        this.thumb = thumb;
        this.uri = uri;
    }
}