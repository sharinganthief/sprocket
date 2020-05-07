/*
 * Copyright (C) 2017 Simon Norberg
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
package com.awsomefox.sprocket.playback;

import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.awsomefox.sprocket.AndroidClock;
import com.awsomefox.sprocket.data.model.Track;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import okhttp3.HttpUrl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PlaybackManagerTest {

  @Mock
  QueueManager mockQueueManager;
  @Mock
  PlaybackManager.PlaybackServiceCallback mockServiceCallback;
  @Mock
  Playback mockPlayback;
  @Mock
  AndroidClock mockAndroidClock;
  private PlaybackManager playbackManager;
  private MediaSessionCompat.Callback mediaSessionCallback;

  @Before
  public void setup() {
    playbackManager = new PlaybackManager(mockQueueManager, mockServiceCallback, mockAndroidClock,
            mockPlayback);
    mediaSessionCallback = playbackManager.getMediaSessionCallback();
  }

  @Test
  public void onPlayEvent() {
    Track track = createTrack();
    when(mockQueueManager.currentTrack()).thenReturn(track);

    mediaSessionCallback.onPlay();

    verify(mockServiceCallback, times(1)).onPlaybackStart();
  }

  @Test
  public void onSkipToQueueItemEvent() {
    Track track = createTrack();
    when(mockQueueManager.currentTrack()).thenReturn(track);

    mediaSessionCallback.onSkipToQueueItem(100);

    verify(mockQueueManager, times(1)).setQueuePosition(100);
  }

  @Test
  public void onPauseEventWhenPlaying() {
    when(mockPlayback.isPlaying()).thenReturn(true);

    mediaSessionCallback.onPause();

    verify(mockServiceCallback, times(1)).onPlaybackPause();
  }

  @Test
  public void onPauseEventWhenNotPlaying() {
    when(mockPlayback.isPlaying()).thenReturn(false);

    mediaSessionCallback.onPause();

    verifyNoInteractions(mockServiceCallback);
  }

  @Test
  public void onSkipToNextEvent() {
    mediaSessionCallback.onSkipToNext();
    verify(mockQueueManager, times(1)).next();
  }

  @Test
  public void onSkipToPreviousEventShortProgress() {
    when(mockPlayback.getCurrentStreamPosition()).thenReturn(1500L);

    mediaSessionCallback.onSkipToPrevious();

    verify(mockQueueManager, times(1)).previous();
  }

  @Test
  public void onStopEvent() {
    mediaSessionCallback.onStop();
    verify(mockServiceCallback, times(1)).onPlaybackStop();
  }

  @Test
  public void onPlaybackStatusChanged() {
    when(mockPlayback.getState())
            .thenReturn(PlaybackStateCompat.STATE_PLAYING)
            .thenReturn(PlaybackStateCompat.STATE_STOPPED);

    playbackManager.onPlaybackStatusChanged();

    verify(mockServiceCallback, times(1)).onNotificationRequired();

    playbackManager.onPlaybackStatusChanged();
  }

  @Test
  public void onCompletionShouldPlayNext() {
    when(mockQueueManager.hasNext()).thenReturn(true);
    playbackManager.onCompletion();
    verify(mockQueueManager, times(1)).next();
  }

  @Test
  public void onCompletionShouldEndPlayback() {
    when(mockQueueManager.hasNext()).thenReturn(false);
    playbackManager.onCompletion();
    verify(mockQueueManager, never()).next();
  }

  @Test
  public void setCurrentTrack() {
    Track currentTrack = createTrack();
    playbackManager.setCurrentTrack(currentTrack);
    verify(mockQueueManager, times(1)).setCurrentTrack(currentTrack);
  }

  @Test
  public void switchPlayback() {
    Playback oldPlayback = playbackManager.getPlayback();
    assertThat(oldPlayback, sameInstance(mockPlayback));

    Playback newExpectedPlayback = mock(Playback.class);
    playbackManager.switchToPlayback(newExpectedPlayback, true);

    Playback newActualPlayback = playbackManager.getPlayback();
    assertThat(newActualPlayback, sameInstance(newExpectedPlayback));
  }

  private Track createTrack() {
    return Track.builder()
            .queueItemId(100)
            .libraryId("libraryId")
            .key("key")
            .ratingKey("ratingKey")
            .parentKey("parentKey")
            .title("title")
            .albumTitle("albumTitle")
            .artistTitle("artistTitle")
            .index(200)
            .duration(300)
            .thumb("thumb")
            .source("source")
            .uri(HttpUrl.parse("https://plex.tv"))
            .recent(true)
            .viewCount(0)
            .viewOffset(100L)
            .build();
  }
}
