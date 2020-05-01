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

import com.awsomefox.sprocket.playback.MusicService;
import com.awsomefox.sprocket.ui.BrowserController;
import com.awsomefox.sprocket.ui.DetailController;
import com.awsomefox.sprocket.ui.SprocketActivity;
import com.awsomefox.sprocket.ui.LoginController;
import com.awsomefox.sprocket.ui.MiniPlayerController;
import com.awsomefox.sprocket.ui.PlayerController;

public interface AppComponent {
  void inject(BrowserController controller);
  void inject(DetailController controller);
  void inject(LoginController controller);
  void inject(MiniPlayerController controller);
  void inject(PlayerController controller);
  void inject(SprocketActivity activity);
  void inject(MusicService service);
}
