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

import com.awsomefox.sprocket.data.model.Track;
import com.awsomefox.sprocket.util.Pair;

import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.subscribers.TestSubscriber;
import okhttp3.HttpUrl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class QueueManagerTest {

  private QueueManager queueManager;
  private List<Track> queue;

  @Before public void setup() {
      queueManager = new QueueManager();
    queue = Arrays.asList(
        createTrack(100),
        createTrack(200),
        createTrack(300),
        createTrack(400),
        createTrack(500));
      queueManager.setQueue(new ArrayList<>(queue), 1000, 0L);
  }

  @Test public void currentQueue() {
    TestSubscriber<Pair<List<Track>, Integer>> test = queueManager.queue().take(1).test();
    test.awaitTerminalEvent();

    List<Track> actualQueue = test.values().get(0).first;
    int actualPosition = test.values().get(0).second;

    assertThat(actualQueue, IsIterableContainingInOrder.contains(
        queue.get(0), queue.get(1), queue.get(2), queue.get(3), queue.get(4)));
    assertThat(actualPosition, is(0));
  }

  @Test public void currentTrack() {
    Track actualTrack = queueManager.currentTrack();
    assertThat(actualTrack, is(queue.get(0)));
  }

  @Test public void setQueuePosition() {
    queueManager.setQueuePosition(queue.get(3).queueItemId());
    assertThat(queueManager.currentTrack(), is(queue.get(3)));

    TestSubscriber<Pair<List<Track>, Integer>> test = queueManager.queue().take(1).test();
    test.awaitTerminalEvent();

    int actualPosition = test.values().get(0).second;
    assertThat(actualPosition, is(3));
  }

  @Test public void setExistingTrack() {
    queueManager.setCurrentTrack(queue.get(3));
    assertThat(queueManager.currentTrack(), is(queue.get(3)));
  }

  @Test public void setNewTrack() {
    Track expectedTrack = createTrack(20);
    queueManager.setCurrentTrack(expectedTrack);
    assertThat(queueManager.currentTrack(), is(expectedTrack));
  }

  private Track createTrack(int index) {
    return Track.builder()
        .queueItemId(index * 10)
        .libraryId("libraryId")
        .key("key")
        .ratingKey("ratingKey")
        .parentKey("parentKey")
        .title("title")
        .albumTitle("albumTitle")
        .artistTitle("artistTitle")
        .index(index)
        .duration(30000)
        .thumb("thumb")
        .source("source")
        .uri(HttpUrl.parse("https://plex.tv"))
        .build();
  }
}
