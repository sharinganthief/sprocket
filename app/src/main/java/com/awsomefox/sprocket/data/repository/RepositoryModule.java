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

import com.awsomefox.sprocket.data.api.MediaService;
import com.awsomefox.sprocket.data.local.LocalDB;
import com.awsomefox.sprocket.data.local.TrackDAO;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class RepositoryModule {

    @Provides
    @Singleton
    @Named("remote")
    MusicRepository provideRemoteMusicRepository(MediaService media) {
        return new RemoteMusicRepositoryImpl(media);
    }

    @Provides
    @Singleton
    @Named("local")
    MusicRepository provideLocalMusicRepository(LocalDB localDB) {
        return new LocalMusicRepositoryImpl(localDB);
    }

    @Provides
    @Singleton
    MusicRepository provideMusicRepository(
            @Named("remote") MusicRepository remoteRepository,
            @Named("local") MusicRepository localRepository) {
        return new MusicRepositoryImpl(remoteRepository, localRepository);
    }
}
