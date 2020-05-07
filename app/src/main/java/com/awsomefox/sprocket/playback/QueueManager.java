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
import com.jakewharton.rxrelay2.BehaviorRelay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;

public class QueueManager {

    private final BehaviorRelay<Pair<List<Track>, Integer>> queueRelay =
            BehaviorRelay.createDefault(new Pair<>(Collections.emptyList(), 0));
    private List<Track> queue = Collections.emptyList();
    private int position;
    private float speed = 1.0f;

    public QueueManager() {
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        if (speed > 2.5f) {
            this.speed = .5f;
        } else if (speed < .5f) {
            this.speed = 2.5f;
        } else {
            //fix weird rounding errors
            this.speed = (float) Math.round(speed * 100000f) / 100000f;
        }
    }

    public Flowable<Pair<List<Track>, Integer>> queue() {
        return queueRelay.toFlowable(BackpressureStrategy.LATEST);
    }

    public void setQueue(List<Track> queue, long queueItemId, long currentOffest) {
        this.queue = queue;
        setQueuePosition(queueItemId);
        //set the persisted offset
        if (currentOffest != 0L && queue.get(position).viewOffset() != 0L
                && currentOffest > queue.get(position).viewOffset()) {
            queue.set(position, queue.get(position).toBuilder().viewOffset(currentOffest).build());
        }
        notifyQueue();
    }

    public List<Track> getQueue() {
        return queue;
    }

    public List<Track> getUpNextQueue() {
        return queue.subList(position, queue.size());
    }

    public Track currentTrack() {
        return queue.get(position);
    }

    void setCurrentTrack(Track currentTrack) {
        if (queue.contains(currentTrack)) {
            setQueuePosition(currentTrack.queueItemId());
        } else {
            setQueue(Collections.singletonList(currentTrack), currentTrack.queueItemId(), 0L);
        }
    }

    void setQueuePosition(long queueItemId) {
        int newPosition = getPositionFromQueueItem(queueItemId);
        if (newPosition != position) {
            position = newPosition;
            notifyQueue();
        }
    }

    void next() {

        int newPosition = position;

        if ((newPosition + 1) >= queue.size()) {
            newPosition = Math.max(0, queue.size() - 1);
        } else {
            ++newPosition;
        }

        position = newPosition;
        notifyQueue();
    }

    void previous() {

        int newPosition = position;

        if ((newPosition - 1) < 0) {
            newPosition = 0;
        } else {
            --newPosition;
        }

        position = newPosition;
        notifyQueue();
    }

    boolean hasNext() {
        return (position + 1) < queue.size();
    }

    private int getPositionFromQueueItem(long id) {
        for (int position = 0; position < queue.size(); ++position) {
            if (queue.get(position).queueItemId() == id) {
                return position;
            }
        }
        return 0;
    }

    private void notifyQueue() {
        queueRelay.accept(new Pair<>(new ArrayList<>(queue), position));
    }
}
