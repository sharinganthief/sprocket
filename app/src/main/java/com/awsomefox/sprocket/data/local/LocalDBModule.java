package com.awsomefox.sprocket.data.local;

import android.content.Context;

import androidx.room.Room;
import androidx.room.RoomDatabase;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class LocalDBModule {

    @Singleton
    @Provides
    public LocalDB provideLocalDB(Context context) {
        return Room.databaseBuilder(context,
                LocalDB.class, LocalDB.DB_NAME)
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build();
    }

    @Provides @Singleton TrackDAO provideTrackDao(LocalDB localDB) {
        return localDB.trackDAO();
    }
    @Provides @Singleton LibraryDAO provideLibraryDao(LocalDB localDB) {
        return localDB.libraryDAO();
    }
    @Provides @Singleton AuthorDAO provideAuthorDao(LocalDB localDB) {
        return localDB.authorDAO();
    }
    @Provides @Singleton BookDAO provideBookDao(LocalDB localDB) {
        return localDB.bookDAO();
    }
}