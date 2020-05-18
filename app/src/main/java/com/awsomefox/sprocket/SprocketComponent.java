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
package com.awsomefox.sprocket;

import com.awsomefox.sprocket.data.DataModule;
import com.awsomefox.sprocket.data.local.LocalDBModule;
import com.awsomefox.sprocket.data.repository.RepositoryModule;
import com.awsomefox.sprocket.playback.PlaybackModule;
import com.awsomefox.sprocket.util.Rx;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
    AndroidModule.class,
    DataModule.class,
    PlaybackModule.class,
    Rx.RxModule.class,
    SprocketModule.class,
    LocalDBModule.class,
    RepositoryModule.class
}) interface SprocketComponent extends AppComponent {
}
