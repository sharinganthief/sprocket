package com.awsomefox.sprocket.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.awsomefox.sprocket.data.model.Library;

import org.jetbrains.annotations.NotNull;

import okhttp3.HttpUrl;

@Entity
public class LibraryEntity {

    @PrimaryKey
    @NotNull
    public final String key;
    public String uuid;
    public String name;
    public String uri;

    public LibraryEntity(@NotNull String key, String uuid, String name, String uri) {
        this.key = key;
        this.uuid = uuid;
        this.name = name;
        this.uri = uri;
    }
}