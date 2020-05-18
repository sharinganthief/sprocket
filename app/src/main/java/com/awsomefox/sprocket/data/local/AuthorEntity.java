package com.awsomefox.sprocket.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

@Entity
public class AuthorEntity {

    @PrimaryKey
    @NotNull
    public final String ratingKey;
    public String libraryId;
    public String libraryKey;
    public String name;
    public String art;
    public String thumb;
    public String uri;

    public AuthorEntity(@NotNull String ratingKey, String libraryId, String libraryKey, String name,
                        String art, String thumb, String uri) {
        this.ratingKey = ratingKey;
        this.libraryId = libraryId;
        this.libraryKey = libraryKey;
        this.name = name;
        this.art = art;
        this.thumb = thumb;
        this.uri = uri;
    }
}