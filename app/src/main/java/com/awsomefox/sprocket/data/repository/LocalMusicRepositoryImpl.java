package com.awsomefox.sprocket.data.repository;

import androidx.annotation.NonNull;

import com.awsomefox.sprocket.data.local.LibraryEntity;
import com.awsomefox.sprocket.data.local.LocalDB;
import com.awsomefox.sprocket.data.local.TrackDAO;
import com.awsomefox.sprocket.data.local.TrackEntity;
import com.awsomefox.sprocket.data.model.Author;
import com.awsomefox.sprocket.data.model.Book;
import com.awsomefox.sprocket.data.model.Library;
import com.awsomefox.sprocket.data.model.MediaType;
import com.awsomefox.sprocket.data.model.PlexItem;
import com.awsomefox.sprocket.data.model.Track;
import com.awsomefox.sprocket.util.Pair;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import okhttp3.HttpUrl;
import timber.log.Timber;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class LocalMusicRepositoryImpl implements MusicRepository {

    LocalDB localDB;

    LocalMusicRepositoryImpl(LocalDB localDB) {
        this.localDB = localDB;
    }

    @Override
    public Observable<PlexItem> sections(HttpUrl url) {
        return localDB.libraryDAO().getAllLibraries()
                .flatMap(libraryEntityToItem())
                .timeout(500, MILLISECONDS)
                .onErrorResumeNext(Observable.empty());
    }

    @Override
    public Single<List<PlexItem>> browseLibrary(Library lib) {
        return chaptersInProgress(lib);
    }

    @Override
    public Single<List<PlexItem>> browseMediaType(MediaType mediaType, int page, Integer pageSize) {
        return null;
    }

    @Override
    public Single<List<PlexItem>> artistItems(Author artist) {
        return null;
    }

    @Override
    public Single<List<PlexItem>> albumItems(Book album) {
        return localDB.trackDAO().getTracksForAlbum(album.libraryId(), album.title())
                .map(trackEntityToItem(false));
    }

    @Override
    public Single<List<PlexItem>> chaptersInProgress(Library lib) {
        return localDB.trackDAO().getTracksInProgress(lib.key()).map(trackEntityToItem(true));
    }

    @Override
    public Single<List<PlexItem>> booksInProgress(Library lib) {
        return null;
    }

    @Override
    public Single<Pair<List<Track>, Long>> createPlayQueue(Track track) {
        return localDB.trackDAO().getTracksForAlbum(track.libraryId(), track.albumTitle())
                .map(trackEntityToTrack())
                .map(tracks -> new Pair<>(tracks, track.queueItemId()));
    }

    @Override
    public Completable scrobble(Track track) {
        TrackEntity update = Stream.of(track)
                .map(trackToTrackEntity()).collect(Collectors.toList()).get(0);
        update.viewOffset = 0;
        update.viewCount++;
        return localDB.trackDAO().updateTrack(update);
    }

    @Override
    public Completable unscrobble(Track track) {
        TrackEntity update = Stream.of(track)
                .map(trackToTrackEntity()).collect(Collectors.toList()).get(0);
        update.viewOffset = 0;
        update.viewCount = 0;
        return localDB.trackDAO().updateTrack(update);
    }

    @Override
    public Completable addTracks(List<Track> tracks) {
        return localDB.trackDAO().addTracks(tracks.stream().map(trackToTrackEntity()).collect(Collectors.toList()));
    }

    @Override
    public Completable addLibraries(List<Library> libraries) {
        return localDB.libraryDAO().addLibraries(libraries.stream().map(libraryToLibraryEntity()).collect(Collectors.toList()));
    }

    @NonNull
    private java.util.function.Function<Track, TrackEntity> trackToTrackEntity() {
        return track -> new TrackEntity(track.ratingKey(), track.libraryId(), track.key(), track.parentKey(),
                track.title(), track.albumTitle(), track.artistTitle(), track.thumb(),
                track.source(), track.uri().toString(), track.index(), track.recent(), track.duration(),
                track.viewCount(), track.viewOffset(), track.queueItemId());
    }

    @NonNull
    private java.util.function.Function<Library, LibraryEntity> libraryToLibraryEntity() {
        return library -> new LibraryEntity(library.key(), library.uuid(), library.name(), library.uri().toString());
    }

    @NonNull
    private Function<List<TrackEntity>, List<PlexItem>> trackEntityToItem(boolean markRecent) {
        return tracks -> {
            List<PlexItem> out = new ArrayList<>();
            for (TrackEntity track : tracks) {
                out.add(Track.builder()
                        .queueItemId(track.queueItemId)
                        .libraryId(track.libraryId)
                        .key(track.key)
                        .ratingKey(track.ratingKey)
                        .parentKey(track.parentKey)
                        .title(track.title)
                        .albumTitle(track.albumTitle)
                        .artistTitle(track.artistTitle)
                        .index(track.index)
                        .duration(track.duration)
                        .viewOffset(track.viewOffset)
                        .viewCount(track.viewCount)
                        .thumb(track.thumb)
                        .source(track.source)
                        .uri(HttpUrl.parse(track.uri))
                        .recent(markRecent || track.recent)
                        .build());

            }
            return out;
        };
    }

    @NonNull
    private Function<LibraryEntity, Observable<PlexItem>> libraryEntityToItem() {
        return library -> Observable.just(Library.builder()
                    .key(library.key)
                    .name(library.name)
                    .uri(HttpUrl.parse(library.uri))
                    .uuid(library.uuid)
                    .build());
    }

    @NonNull
    private Function<List<TrackEntity>, List<Track>> trackEntityToTrack() {
        return tracks -> {
            List<Track> out = new ArrayList<>();
            for (TrackEntity track : tracks) {
                out.add(Track.builder()
                        .queueItemId(track.queueItemId)
                        .libraryId(track.libraryId)
                        .key(track.key)
                        .ratingKey(track.ratingKey)
                        .parentKey(track.parentKey)
                        .title(track.title)
                        .albumTitle(track.albumTitle)
                        .artistTitle(track.artistTitle)
                        .index(track.index)
                        .duration(track.duration)
                        .viewOffset(track.viewOffset)
                        .viewCount(track.viewCount)
                        .thumb(track.thumb)
                        .source(track.source)
                        .uri(HttpUrl.parse(track.uri))
                        .recent(track.recent)
                        .build());

            }
            return out;
        };
    }

}
