package com.awsomefox.sprocket.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;


@Database(entities = {TrackEntity.class, BookEntity.class, LibraryEntity.class, AuthorEntity.class}, version = 17, exportSchema = false)
public abstract class LocalDB extends RoomDatabase {

    static String DB_NAME = "sprocketDB";

    public abstract TrackDAO trackDAO();
    public abstract BookDAO bookDAO();
    public abstract LibraryDAO libraryDAO();
    public abstract AuthorDAO authorDAO();
}