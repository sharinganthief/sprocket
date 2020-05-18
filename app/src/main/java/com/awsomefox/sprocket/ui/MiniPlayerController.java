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
package com.awsomefox.sprocket.ui;

import android.os.Bundle;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.media.session.PlaybackStateCompat.State;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.awsomefox.sprocket.R;
import com.awsomefox.sprocket.SprocketApp;
import com.awsomefox.sprocket.data.model.Track;
import com.awsomefox.sprocket.util.Rx;
import com.bumptech.glide.Glide;

import java.util.Objects;

import javax.inject.Inject;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.OnClick;
import timber.log.Timber;

import static com.bluelinelabs.conductor.rxlifecycle2.ControllerEvent.DETACH;
import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

public class MiniPlayerController extends BaseMediaController {

    private static final int[] PLAY = {-R.attr.state_pause};
    private static final int[] PAUSE = {R.attr.state_pause};

    @BindView(R.id.miniplayer_track_title)
    TextView trackTitle;
    @BindView(R.id.miniplayer_book_title)
    TextView bookTitle;
    @BindView(R.id.miniplayer_play_pause)
    ImageView playPauseButton;
    @BindView(R.id.album_thumb)
    ImageView albumThumb;
    @BindString(R.string.description_play)
    String descPlay;
    @BindString(R.string.description_pause)
    String descPause;
    @BindString(R.string.chapter_title)
    String chapterTitle;
    @Inject
    Rx rx;

    public MiniPlayerController(Bundle args) {
        super(args);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.controller_miniplayer;
    }

    @Override
    protected void injectDependencies() {
        if (getActivity() != null) {
            SprocketApp.get(getActivity()).component().inject(this);
        }
    }

    @Override
    protected void onAttach(@NonNull View view) {
        super.onAttach(view);
        observePlaybackState();
    }

    @OnClick(R.id.miniplayer_play_pause)
    void onClickPlayPause() {
        mediaController.playPause();
    }

    private void observePlaybackState() {
//        disposables.add(mediaController.progress()
//                .compose(bindUntilEvent(DETACH))
//                .compose(rx.flowableSchedulers())
//                .subscribe(this::persistProgress, Rx::onError));
        disposables.add(mediaController.state()
                .compose(bindUntilEvent(DETACH))
                .compose(rx.flowableSchedulers())
                .subscribe(this::updatePlayButton, Rx::onError));
        disposables.add(contextManager.queue()
                .compose(bindUntilEvent(DETACH))
                .compose(rx.flowableSchedulers())
                .subscribe(pair -> updateTrackInfo(pair.first.get(pair.second)), Rx::onError));
    }

    private void updatePlayButton(@State int state) {
        Timber.d("New state for minicontroller: " + state);
        if (state == PlaybackStateCompat.STATE_PLAYING
                || state == PlaybackStateCompat.STATE_BUFFERING) {
            playPauseButton.setImageState(PAUSE, true);
            playPauseButton.setContentDescription(descPause);
        } else {
            playPauseButton.setImageState(PLAY, true);
            playPauseButton.setContentDescription(descPlay);
        }
    }

    private void updateTrackInfo(@NonNull Track track) {
        trackTitle.setText(String.format(chapterTitle, track.index()));
        bookTitle.setText(track.albumTitle());
        Glide.with(Objects.requireNonNull(getActivity()))
                .load(track.thumb())
                .transition(withCrossFade())
                .into(albumThumb);
    }
}
