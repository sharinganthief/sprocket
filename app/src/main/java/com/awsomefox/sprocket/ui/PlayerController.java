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

import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.media.session.PlaybackStateCompat.State;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.awsomefox.sprocket.R;
import com.awsomefox.sprocket.SprocketApp;
import com.awsomefox.sprocket.data.model.Track;
import com.awsomefox.sprocket.playback.MediaController;
import com.awsomefox.sprocket.playback.QueueManager;
import com.awsomefox.sprocket.ui.adapter.QueueAdapter;
import com.awsomefox.sprocket.ui.widget.DividerItemDecoration;
import com.awsomefox.sprocket.util.Rx;
import com.awsomefox.sprocket.util.Views;
import com.bumptech.glide.Glide;
import com.google.android.gms.cast.framework.CastButtonFactory;

import java.util.Objects;

import javax.inject.Inject;

import butterknife.BindDrawable;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.OnClick;

import static com.bluelinelabs.conductor.rxlifecycle2.ControllerEvent.DETACH;
import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

public class PlayerController extends BaseController implements QueueAdapter.OnTrackClickListener {

    private static final int[] PLAY = {-R.attr.state_pause};
    private static final int[] PAUSE = {R.attr.state_pause};
    private static final int[] QUEUE = {-R.attr.state_track};
    private static final int[] TRACK = {R.attr.state_track};
    private final QueueAdapter queueAdapter;
    @BindView(R.id.content_loading)
    ContentLoadingProgressBar contentLoading;
    @BindView(R.id.player_background_image)
    ImageView background;
    @BindView(R.id.player_queue)
    RecyclerView queueRecyclerView;
    @BindView(R.id.player_track_title)
    TextView trackTitle;
    @BindView(R.id.player_artist_title)
    TextView artistTitle;
    @BindView(R.id.player_seekbar)
    SeekBar seekBar;
    @BindView(R.id.player_elapsed_time)
    TextView elapsedTime;
    @BindView(R.id.player_total_time)
    TextView totalTime;
    @BindView(R.id.player_skip_forward)
    ImageView skipForwardButton;
    @BindView(R.id.player_skip_back)
    ImageView skipBackButton;
    @BindView(R.id.player_play_pause)
    ImageView playPauseButton;
    @BindDrawable(R.drawable.item_divider)
    Drawable itemDivider;
    @BindString(R.string.description_play)
    String descPlay;
    @BindString(R.string.description_pause)
    String descPause;
    @BindString(R.string.description_queue)
    String descQueue;
    @BindString(R.string.description_track)
    String descTrack;
    @Inject
    QueueManager queueManager;
    @Inject
    MediaController mediaController;
    @Inject
    Rx rx;
    private boolean isSeeking;
    private boolean isQueueVisible;
    private float speed;
    private Track currentTrack;

    public PlayerController(Bundle args) {
        super(args);
        queueAdapter = new QueueAdapter(this);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.controller_player;
    }

    @Override
    protected void injectDependencies() {
        if (getActivity() != null) {
            SprocketApp.get(getActivity()).component().inject(this);
        }
    }

    @NonNull
    @Override
    protected View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        View view = super.onCreateView(inflater, container);

        ActionBar actionBar = null;
        if (getActivity() != null) {
            actionBar = ((SprocketActivity) getActivity()).getSupportActionBar();
        }
        if (actionBar != null) {
            setHasOptionsMenu(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        queueRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        queueRecyclerView.setHasFixedSize(true);
        queueRecyclerView.addItemDecoration(new DividerItemDecoration(itemDivider));

        if (isQueueVisible) {
            Views.gone(background);
            Views.visible(queueRecyclerView);
        } else {
            Views.visible(background);
            Views.gone(queueRecyclerView);
        }

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                elapsedTime.setText(DateUtils.formatElapsedTime(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mediaController.seekTo(seekBar.getProgress() * 1000);
                isSeeking = false;
            }
        });

        return view;
    }

    @Override
    protected void onAttach(@NonNull View view) {
        super.onAttach(view);
        queueRecyclerView.setAdapter(queueAdapter);
        observePlaybackState();
    }

    @Override
    protected void onDetach(@NonNull View view) {
        super.onDetach(view);
        queueRecyclerView.setAdapter(null);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main, menu);
        inflater.inflate(R.menu.menu_player, menu);
        CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu,
                R.id.media_route_menu_item);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        handleQueueTrack(menu);
        handleSpeed(menu);
    }

    private void handleSpeed(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_change_speed);
        ImageView actionView = (ImageView) item.getActionView();
        actionView.setOnClickListener(view -> {
            LayoutInflater inflater = Objects.requireNonNull(getActivity()).getLayoutInflater();
            AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));
            builder.setView(inflater.inflate(R.layout.speed_control, null))
                    .setTitle("Set Playback Speed")
                    .setPositiveButton(R.string.cast_tracks_chooser_dialog_ok, (dialog, id) -> {
                        dialog.dismiss();
                    });
            AlertDialog dialog = builder.create();
            dialog.show();

            SeekBar speedBar = dialog.findViewById(R.id.speed_seek_bar);
            TextView speedValue = dialog.findViewById(R.id.speed_value);
            speedValue.setText(String.valueOf(mediaController.getSpeed()));
            if (speedBar != null) {
                speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar speedBar, int progress, boolean fromUser) {
                        float speed = (speedBar.getProgress() + 5) / 10f;
                        if (speedValue != null) {
                            mediaController.setSpeed(speed);
                            speedValue.setText(String.valueOf(speed));
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar speedBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar speedBar) {
                        float speed = (speedBar.getProgress() + 5) / 10f;
                        if (speedValue != null) {
                            mediaController.setSpeed(speed);
                            speedValue.setText(String.valueOf(speed));
                        }
                    }
                });
            }
        });

    }

    private void handleQueueTrack(@NonNull Menu menu) {
        MenuItem item = menu.findItem(R.id.action_queue_track);
        ImageView actionView = (ImageView) item.getActionView();
        actionView.setImageState(isQueueVisible ? TRACK : QUEUE, true);
        actionView.setContentDescription(isQueueVisible ? descTrack : descQueue);
        actionView.setOnClickListener(view -> {
            actionView.setImageState(isQueueVisible ? QUEUE : TRACK, true);
            actionView.setContentDescription(isQueueVisible ? descQueue : descTrack);
            if (isQueueVisible) {
                Views.visible(background);
                Views.gone(queueRecyclerView);
            } else {
                Views.gone(background);
                Views.visible(queueRecyclerView);
            }
            isQueueVisible = !isQueueVisible;
        });
    }

    @OnClick(R.id.player_skip_forward)
    void onClickForward() {
        mediaController.seekTo(mediaController.getPlaybackState().getPosition() + 30000);
    }

    @OnClick(R.id.player_skip_back)
    void onClickBack() {
        mediaController.seekTo(mediaController.getPlaybackState().getPosition() - 30000);
    }

    @OnClick(R.id.player_play_pause)
    void onClickPlayPause() {
        mediaController.playPause();
    }

    @OnClick(R.id.player_next)
    void onClickNext(ImageView nextButton) {
        ((Animatable) nextButton.getDrawable()).start();
        mediaController.next();
    }

    @OnClick(R.id.player_previous)
    void onClickPrevious(ImageView previousButton) {
        ((Animatable) previousButton.getDrawable()).start();
        mediaController.previous();
    }

    private void observePlaybackState() {
        disposables.add(mediaController.progress()
                .compose(bindUntilEvent(DETACH))
                .compose(rx.flowableSchedulers())
                .subscribe(progress -> {
                    if (!isSeeking) {
                        seekBar.setProgress((int) (progress / 1000));

                    }
                }, Rx::onError));

        disposables.add(mediaController.state()
                .compose(bindUntilEvent(DETACH))
                .compose(rx.flowableSchedulers())
                .subscribe(this::updatePlayButton, Rx::onError));

        disposables.add(queueManager.queue()
                .compose(bindUntilEvent(DETACH))
                .compose(rx.flowableSchedulers())
                .subscribe(pair -> {
                    queueAdapter.setQueue(pair.first, pair.second);
                    updateTrackInfo(pair.first.get(pair.second));
                }, Rx::onError));
    }

    private void updatePlayButton(@State int state) {
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
        contentLoading.hide();
        currentTrack = track;

        if (getActivity() != null) {
            Glide.with(getActivity())
                    .load(track.thumb())
                    .transition(withCrossFade())
                    .into(background);
        }

        seekBar.setMax((int) track.duration() / 1000);
        totalTime.setText(DateUtils.formatElapsedTime(track.duration() / 1000));

        trackTitle.setText(track.title());
        artistTitle.setText(track.artistTitle());
    }

    @Override
    public void onTrackClicked(Track track) {
        mediaController.playQueueItem(track.queueItemId());
    }
}
