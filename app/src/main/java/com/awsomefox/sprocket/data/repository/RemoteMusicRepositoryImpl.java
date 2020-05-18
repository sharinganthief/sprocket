/*
 * Copyright (C) 2016 Simon Norberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.awsomefox.sprocket.data.repository;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.collection.SimpleArrayMap;

import com.awsomefox.sprocket.data.Type;
import com.awsomefox.sprocket.data.api.MediaService;
import com.awsomefox.sprocket.data.api.model.Directory;
import com.awsomefox.sprocket.data.api.model.MediaContainer;
import com.awsomefox.sprocket.data.api.model.Song;
import com.awsomefox.sprocket.data.local.TrackDAO;
import com.awsomefox.sprocket.data.local.TrackEntity;
import com.awsomefox.sprocket.data.model.Author;
import com.awsomefox.sprocket.data.model.Book;
import com.awsomefox.sprocket.data.model.Header;
import com.awsomefox.sprocket.data.model.Library;
import com.awsomefox.sprocket.data.model.MediaType;
import com.awsomefox.sprocket.data.model.PlexItem;
import com.awsomefox.sprocket.data.model.Track;
import com.awsomefox.sprocket.util.Pair;
import com.awsomefox.sprocket.util.Strings;
import com.awsomefox.sprocket.util.Urls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import okhttp3.HttpUrl;

import static java.util.concurrent.TimeUnit.SECONDS;

class RemoteMusicRepositoryImpl implements MusicRepository {

    private static final Function<MediaContainer, Observable<Directory>> DIRS = container -> {
        if (container.directories == null) {
            return Observable.fromIterable(Collections.emptyList());
        }
        return Observable.fromIterable(container.directories);
    };

    private static final Function<MediaContainer, Observable<Song>> TRACKS = container -> {
        if (container.tracks == null) {
            return Observable.fromIterable(Collections.emptyList());
        }
        return Observable.fromIterable(container.tracks);
    };

    private final MediaService media;

    RemoteMusicRepositoryImpl(MediaService media) {
        this.media = media;
    }

    @Override
    public Observable<PlexItem> sections(HttpUrl url) {
        return media.sections(url)
                .flatMap(container -> Observable.fromIterable(container.directories))
                .filter(section -> TextUtils.equals(section.type, "artist"))
                .map(libraryMapper(url))
                .timeout(1, SECONDS)
                .onErrorResumeNext(Observable.empty());
    }

    @Override
    public Single<List<PlexItem>> browseLibrary(Library lib) {
        return Observable.concat(chaptersInProgressImpl(lib),
                booksRecentlyListenedTo(lib)).toList();
//        return booksRecentlyListenedTo(lib).toList();
    }

    private Observable<PlexItem> chaptersInProgressImpl(Library lib) {
        return media.chaptersInProgress(lib.uri(), lib.key())
                .flatMap(TRACKS)
                .map(trackMapper(lib.key(), lib.uri(), true))
                .startWith(Header.builder().title("Chapters In Progress").build())
                .timeout(10, SECONDS)
                .onErrorResumeNext(Observable.empty());

    }

    @Override
    public Single<List<PlexItem>> chaptersInProgress(Library lib) {
        return chaptersInProgressImpl(lib).toList(); //, chaptersInProgressLocalImpl().toList());

    }

    private Observable<PlexItem> booksRecentlyListenedTo(Library lib) {
        return media.booksRecentlyListendTo(lib.uri(), lib.key())
                .flatMap(DIRS)
                .map(albumMapper(lib.key(), lib.uri()))
                .startWith(Header.builder().title("Recent Books").build())
                .timeout(10, SECONDS)
                .onErrorResumeNext(Observable.empty());
    }

    @Override
    public Single<List<PlexItem>> booksInProgress(Library lib) {
        return booksRecentlyListenedTo(lib).toList();
    }

    @Override
    public Single<List<PlexItem>> browseMediaType(MediaType mt, int page, Integer pageSize) {
        Single<List<PlexItem>> browseItems;

        if (mt.type() == Type.ARTIST) {
            browseItems = browseAuthors(mt, page, pageSize);
        } else if (mt.type() == Type.ALBUM) {
            browseItems = browseBooks(mt, page, pageSize);
        } else {
            browseItems = browseTracks(mt, page);
        }

        return Single.zip(browseHeaders(mt), browseItems, (headers, items) -> {
            List<PlexItem> plexItems = new ArrayList<>();

            for (int i = 0; i < items.size(); ++i) {
                // The headers need to be offset by the current offset!
                if (headers.containsKey(i + page)) {
                    plexItems.add(headers.get(i + page));
                }
                plexItems.add(items.get(i));
            }

            return plexItems;
        });
    }

    private Single<List<PlexItem>> browseAuthors(MediaType mt, int offset, Integer pageSize) {
        return media.browse(mt.uri(), mt.libraryKey(), mt.mediaKey(), offset, pageSize)
                .flatMap(DIRS)
                .map(artistMapper(mt.libraryKey(), mt.libraryId(), mt.uri()))
                .toList();
    }

    private Single<List<PlexItem>> browseBooks(MediaType mt, int page, Integer pageSize) {
        return media.browse(mt.uri(), mt.libraryKey(), mt.mediaKey(), page, pageSize)
                .flatMap(DIRS)
                .map(albumMapper(mt.libraryId(), mt.uri()))
                .toList();
    }

    private Single<List<PlexItem>> browseTracks(MediaType mt, int offset) {
        return media.browse(mt.uri(), mt.libraryKey(), mt.mediaKey(), offset, 50)
                .flatMap(TRACKS)
                .map(trackMapper(mt.libraryId(), mt.uri(), false))
                .toList();
    }

    private Single<SimpleArrayMap<Integer, PlexItem>> browseHeaders(MediaType mt) {
        return media.firstCharacter(mt.uri(), mt.libraryKey(), mt.mediaKey())
                .flatMap(DIRS)
                .toList()
                .map(dirs -> {
                    SimpleArrayMap<Integer, PlexItem> headers = new SimpleArrayMap<>();

                    int offset = 0;
                    for (int i = 0; i < dirs.size(); ++i) {
                        headers.put(offset, Header.builder().title(dirs.get(i).title).build());
                        offset += dirs.get(i).size;
                    }

                    return headers;
                });
    }

    @Override
    public Single<List<PlexItem>> artistItems(Author artist) {
        return Single.zip(popularTracks(artist), albums(artist), (tracks, albums) -> {
            List<PlexItem> items = new ArrayList<>();
            if (!tracks.isEmpty()) {
                items.add(Header.builder().title("Popular").build());
                items.addAll(tracks);
            }
            if (!albums.isEmpty()) {
                items.add(Header.builder().title("Books").build());
                items.addAll(albums);
            }
            return items;
        });
    }

    private Single<List<PlexItem>> popularTracks(Author artist) {
        return media.popularTracks(artist.uri(), artist.libraryKey(), artist.ratingKey())
                .flatMap(TRACKS)
                .map(trackMapper(artist.libraryId(), artist.uri(), false))
                .toList();
    }

    private Single<List<PlexItem>> albums(Author artist) {
        return media.albums(artist.uri(), artist.ratingKey())
                .flatMap(DIRS)
                .map(albumMapper(artist.libraryId(), artist.uri()))
                .toList();
    }

    @Override
    public Single<List<PlexItem>> albumItems(Book album) {
        return media.tracks(album.uri(), album.ratingKey())
                .flatMap(TRACKS)
                .map(trackMapper(album.libraryId(), album.uri(), false))
                .toList();
    }

    @Override
    public Single<Pair<List<Track>, Long>> createPlayQueue(Track track) {
        return media.playQueue(track.uri(), track.key(), track.parentKey(), track.libraryId())
                .flatMap(container -> Observable.just(container)
                        .flatMap(TRACKS)
                        .map(trackMapper(track.libraryId(), track.uri(), false))
                        .map(plexItem -> (Track) plexItem)
                        .toList()
                        .map(tracks -> new Pair<>(tracks, container.playQueueSelectedItemID)));
    }

    @Override
    public Completable scrobble(Track track) {
        return media.scrobble(track.uri(), track.ratingKey());
    }

    @Override
    public Completable unscrobble(Track track) {
        return media.unScrobble(track.uri(), track.ratingKey());
    }

    @Override
    public Completable addTracks(List<Track> tracks) {
        return null;
    }

    @Override
    public Completable addLibraries(List<Library> libraries) {
        return null;
    }

    @NonNull
    private Function<Directory, PlexItem> albumMapper(String libraryId, HttpUrl uri) {
        return dir -> Book.builder()
                .title(dir.title)
                .ratingKey(dir.ratingKey)
                .artistTitle(dir.parentTitle)
                .libraryId(libraryId)
                .thumb(Strings.isBlank(dir.thumb) ? null
                        : Urls.addPathToUrl(uri, dir.thumb).toString())
                .uri(uri)
                .build();
    }

    @NonNull
    private Function<Directory, PlexItem> artistMapper(String libKey, String libraryId, HttpUrl uri) {
        return dir -> Author.builder()
                .title(dir.title)
                .ratingKey(dir.ratingKey)
                .libraryKey(libKey)
                .libraryId(libraryId)
                .art(Urls.getTranscodeUrl(uri, dir.art))
                .thumb(Strings.isBlank(dir.thumb) ? null
                        : Urls.addPathToUrl(uri, dir.thumb).toString())
                .uri(uri)
                .build();
    }

    @NonNull
    private Function<Directory, PlexItem> libraryMapper(HttpUrl uri) {
        return section -> Library.builder()
                .uuid(section.uuid)
                .key(section.key)
                .name(section.title)
                .uri(uri)
                .build();
    }

    @NonNull
    private Function<Song, PlexItem> trackMapper(String libraryId, HttpUrl uri, boolean recent) {
        return track -> Track.builder()
                .queueItemId(track.playQueueItemID != null ? track.playQueueItemID : 0)
                .libraryId(libraryId)
                .key(track.key)
                .ratingKey(track.ratingKey)
                .parentKey(track.parentKey)
                .title(track.title)
                .albumTitle(track.parentTitle)
                .artistTitle(track.grandparentTitle)
                .index(track.index)
                .duration(track.duration)
                .viewOffset(track.viewOffset)
                .viewCount(track.viewCount)
                .thumb(Strings.isBlank(track.thumb) ? null : Urls.addPathToUrl(uri, track.thumb).toString())
                .source(Urls.addPathToUrl(uri, track.media.part.key).toString())
                .uri(uri)
                .recent(recent)
                .build();
    }
}
