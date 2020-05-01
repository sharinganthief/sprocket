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

import com.awsomefox.sprocket.data.model.Author;
import com.awsomefox.sprocket.data.model.Book;
import com.awsomefox.sprocket.data.model.Library;
import com.awsomefox.sprocket.data.model.MediaType;
import com.awsomefox.sprocket.data.model.PlexItem;
import com.awsomefox.sprocket.data.model.Track;
import com.awsomefox.sprocket.util.Pair;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import okhttp3.HttpUrl;

public interface MusicRepository {
    Single<List<PlexItem>> browseLibrary(Library lib);

    Single<List<PlexItem>> browseMediaType(MediaType mediaType, int offset);

    Single<List<PlexItem>> artistItems(Author artist);

    Single<List<PlexItem>> albumItems(Book album);

    Single<Pair<List<Track>, Long>> createPlayQueue(Track track);

    Completable scrobble(HttpUrl url, String ratingKey);

    Completable unscrobble(HttpUrl url, String ratingKey);
}
