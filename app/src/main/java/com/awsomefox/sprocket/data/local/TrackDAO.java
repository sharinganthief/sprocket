package com.awsomefox.sprocket.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;


@Dao
public interface TrackDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable addTracks(List<TrackEntity> tracks);

    @Query("select * from trackentity")
    Single<List<TrackEntity>> getAllTracks();

    @Query("select * from trackentity where viewOffset > 0 and libraryId = :key")
    Single<List<TrackEntity>> getTracksInProgress(String key);

    @Query("select * from trackentity where libraryId = :libraryId and albumTitle = :albumTitle")
    Single<List<TrackEntity>> getTracksForAlbum(String libraryId, String albumTitle);

//    @Query("select * from user where id = :userId")
//    public List<User> getUser(long userId);
//
    @Update(onConflict = OnConflictStrategy.REPLACE)
    Completable updateTrack(TrackEntity trackEntity);
//
//    @Query("delete from user")
//    void removeAllUsers();
}
