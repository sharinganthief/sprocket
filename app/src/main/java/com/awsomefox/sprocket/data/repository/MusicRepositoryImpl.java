package com.awsomefox.sprocket.data.repository;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import com.awsomefox.sprocket.data.Type;
import com.awsomefox.sprocket.data.model.Author;
import com.awsomefox.sprocket.data.model.Book;
import com.awsomefox.sprocket.data.model.Header;
import com.awsomefox.sprocket.data.model.Library;
import com.awsomefox.sprocket.data.model.MediaType;
import com.awsomefox.sprocket.data.model.PlexItem;
import com.awsomefox.sprocket.data.model.Track;
import com.awsomefox.sprocket.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.BiFunction;
import io.reactivex.schedulers.Schedulers;
import okhttp3.HttpUrl;

public class MusicRepositoryImpl implements MusicRepository {

    private MusicRepository remoteMusicRepository;
    private MusicRepository localMusicRepository;

    MusicRepositoryImpl(MusicRepository remoteMusicRepository, MusicRepository localMusicRepository) {
        this.remoteMusicRepository = remoteMusicRepository;
        this.localMusicRepository = localMusicRepository;
    }

    @SuppressLint("CheckResult")
    @Override
    public Observable<PlexItem> sections(HttpUrl url) {
        Observable<PlexItem> remote = remoteMusicRepository.sections(url);
        Observable<PlexItem> merged = Observable.concat(remote,
                localMusicRepository.sections(url)).distinct(plexItem -> {
            Library bary = (Library) plexItem;
            return bary.key();
        });
        //TODO: auto update the local libraries or only show libraries with downloaded stuff?
//        remote.toList().subscribeOn(Schedulers.io())
//                .subscribe(items -> {
//                    List<Library> libs = new ArrayList<>();
//                    for (PlexItem item : items) {
//                        libs.add((Library) item);
//                    }
//                    addLibraries(libs).subscribeOn(Schedulers.io()).subscribe();
//                });
        return merged;
    }

    @Override
    public Single<List<PlexItem>> browseLibrary(Library lib) {
        return Single.zip(Single.zip(mediaTypes(lib), chaptersInProgressImpl(lib),
                mergeRemoteAndLocal()), remoteMusicRepository.booksInProgress(lib),
                mergeRemoteAndLocal());
//                booksRecentlyListenedTo(lib)).toList();
//        return Single.zip(Single.zip(mediaTypes(lib), remoteMusicRepository.browseLibrary(lib),
//                mergeRemoteAndLocal()), localMusicRepository.browseLibrary(lib),
//                mergeRemoteAndLocal());
    }

    private Single<List<PlexItem>> chaptersInProgressImpl(Library lib) {
        PlexItem header = Header.builder().title("Chapters In Progress").build();
        return Single.zip(Single.just(Collections.singletonList(header)), chaptersInProgress(lib), mergeRemoteAndLocal());
    }

    @Override
    public Single<List<PlexItem>> browseMediaType(MediaType mediaType, int page, Integer pageSize) {
        return remoteMusicRepository.browseMediaType(mediaType, page, pageSize);
    }

    private Single<List<PlexItem>> mediaTypes(Library lib) {
        return Observable.fromArray(Header.builder().title("Browse Library").build(),
                MediaType.builder()
                        .title("Authors")
                        .type(Type.ARTIST)
                        .mediaKey("8")
                        .libraryKey(lib.key())
                        .libraryId(lib.uuid())
                        .uri(lib.uri())
                        .build(),
                MediaType.builder()
                        .title("Books")
                        .type(Type.ALBUM)
                        .mediaKey("9")
                        .libraryKey(lib.key())
                        .libraryId(lib.uuid())
                        .uri(lib.uri())
                        .build()).toList();
    }

    @Override
    public Single<List<PlexItem>> artistItems(Author artist) {
        return remoteMusicRepository.artistItems(artist);
    }

    @Override
    public Single<List<PlexItem>> albumItems(Book album) {
        return remoteMusicRepository.albumItems(album);
    }

    @Override
    public Single<List<PlexItem>> chaptersInProgress(Library lib) {
        return Single.zip(remoteMusicRepository.chaptersInProgress(lib), localMusicRepository.chaptersInProgress(lib),
                mergeRemoteAndLocal());
//        return localMusicRepository.chaptersInProgress(lib);
    }

    @Override
    public Single<List<PlexItem>> booksInProgress(Library lib) {
        return remoteMusicRepository.booksInProgress(lib);
    }

    @Override
    public Single<Pair<List<Track>, Long>> createPlayQueue(Track track) {
        return remoteMusicRepository.createPlayQueue(track);
    }

    @Override
    public Completable scrobble(Track track) {
        return Completable.mergeArray(remoteMusicRepository.scrobble(track),
                localMusicRepository.scrobble(track));
    }

    @Override
    public Completable unscrobble(Track track) {
        return Completable.mergeArray(remoteMusicRepository.unscrobble(track),
                localMusicRepository.unscrobble(track));
    }

    @Override
    public Completable addTracks(List<Track> tracks) {
        return localMusicRepository.addTracks(tracks);
    }

    @Override
    public Completable addLibraries(List<Library> libraries) {
        return localMusicRepository.addLibraries(libraries);
    }

    @NonNull
    private BiFunction<List<PlexItem>, List<PlexItem>, List<PlexItem>> mergeRemoteAndLocal() {
        return (remote, local) -> {
            Map<String, PlexItem> noDup = new LinkedHashMap<>();
            for (PlexItem item : remote) {
                if (item instanceof MediaType) {
                    MediaType mediaType = (MediaType) item;
                    noDup.put(mediaType.mediaKey(), mediaType);
                } else if (item instanceof Author) {
                    Author author = (Author) item;
                    noDup.put(author.ratingKey(), author);
                } else if (item instanceof Book) {
                    Book book = (Book) item;
                    noDup.put(book.ratingKey(), book);
                } else if (item instanceof Track) {
                    Track track = (Track) item;
                    noDup.put(track.ratingKey(), track);
                } else if (item instanceof Header) {
                    Header header = (Header) item;
                    noDup.put(header.title(), header);
                } else if (item instanceof Library) {
                    Library library = (Library) item;
                    noDup.put(library.uuid(), library);
                }
            }
            for (PlexItem item : local) {
                //TODO add some checks here on when to use the plex version versus local
                if (item instanceof MediaType) {
                    MediaType mediaType = (MediaType) item;
                    noDup.put(mediaType.mediaKey(), mediaType);
                } else if (item instanceof Author) {
                    Author author = (Author) item;
                    noDup.put(author.ratingKey(), author);
                } else if (item instanceof Book) {
                    Book book = (Book) item;
                    noDup.put(book.ratingKey(), book);
                } else if (item instanceof Track) {
                    Track track = (Track) item;
                    noDup.put(track.ratingKey(), track);
                } else if (item instanceof Header) {
                    Header header = (Header) item;
                    noDup.put(header.title(), header);
                } else if (item instanceof Library) {
                    Library library = (Library) item;
                    noDup.put(library.uuid(), library);
                }
            }
            return new ArrayList<>(noDup.values());
        };
    }
}
