package com.awsomefox.sprocket.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;


@Dao
public interface LibraryDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable addLibraries(List<LibraryEntity> libraryEntities);

    @Query("select * from libraryentity")
    Observable<LibraryEntity> getAllLibraries();

//    @Query("select * from trackentity where libraryId = :libraryId and albumTitle = :albumTitle")
//    Single<List<TrackEntity>> getTracksForAlbum(String libraryId, String albumTitle);

    //    @Query("select * from user where id = :userId")
//    public List<User> getUser(long userId);
//
    @Update(onConflict = OnConflictStrategy.REPLACE)
    Completable updateLibraries(LibraryEntity libraryEntity);
//
//    @Query("delete from user")
//    void removeAllUsers();
}
