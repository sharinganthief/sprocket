/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.awsomefox.sprocket;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.media.session.PlaybackStateCompat.State;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import com.awsomefox.sprocket.data.model.Track;
import com.awsomefox.sprocket.playback.MediaController;
import com.awsomefox.sprocket.playback.MusicService;
import com.awsomefox.sprocket.playback.ContextManager;
import com.awsomefox.sprocket.ui.SprocketActivity;
import com.awsomefox.sprocket.util.Rx;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.Locale;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_NONE;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_STOPPED;

public class MediaNotificationManager extends BroadcastReceiver {

    private static final String ACTION_PLAY = "com.awsomefox.sprocket.ACTION_PLAY";
    private static final String ACTION_PAUSE = "com.awsomefox.sprocket.ACTION_PAUSE";
    private static final String ACTION_SKIP_FORWARD = "com.awsomefox.sprocket.ACTION_SKIP_FORWARD";
    private static final String ACTION_SKIP_BACK = "com.awsomefox.sprocket.SKIP_BACK";
    private static final String ACTION_STOP_CAST = "com.awsomefox.sprocket.ACTION_STOP_CAST";
    private static final String CHANNEL_ID = "com.awsomefox.sprocket.CHANNEL_ID";
    private static final int NOTIFICATION_ID = 412;
    private static final int REQUEST_CODE = 100;

    private final MusicService musicService;
    private final MediaController mediaController;
    private final ContextManager contextManager;
    private final Rx rx;
    private final NotificationManager notificationManager;
    private final PendingIntent playIntent;
    private final PendingIntent pauseIntent;
    private final PendingIntent nextIntent;
    private final PendingIntent previousIntent;
    private final PendingIntent stopCastIntent;
    private final int iconWidth;
    private final int iconHeight;
    @State
    private int state;
    private Track currentTrack;
    private Disposable disposable;
    private Notification notification;

    public MediaNotificationManager(MusicService service, MediaController mediaController,
                                    ContextManager contextManager, Rx rx) {
        this.musicService = service;
        this.mediaController = mediaController;
        this.contextManager = contextManager;
        this.rx = rx;

        notificationManager = (NotificationManager) service.getSystemService(
                Context.NOTIFICATION_SERVICE);

        String p = service.getPackageName();
        playIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
                new Intent(ACTION_PLAY).setPackage(p), FLAG_CANCEL_CURRENT);
        pauseIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
                new Intent(ACTION_PAUSE).setPackage(p), FLAG_CANCEL_CURRENT);
        nextIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
                new Intent(ACTION_SKIP_FORWARD).setPackage(p), FLAG_CANCEL_CURRENT);
        previousIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
                new Intent(ACTION_SKIP_BACK).setPackage(p), FLAG_CANCEL_CURRENT);
        stopCastIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
                new Intent(ACTION_STOP_CAST).setPackage(p), FLAG_CANCEL_CURRENT);

        iconWidth = service.getResources().getDimensionPixelSize(
                android.R.dimen.notification_large_icon_width);
        iconHeight = service.getResources().getDimensionPixelSize(
                android.R.dimen.notification_large_icon_height);

        // Cancel all notifications to handle the case where the Service was killed and
        // restarted by the system.
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
    }

    /**
     * Posts the notification and starts tracking the session to keep it
     * updated. The notification will automatically be removed if the session is
     * destroyed before {@link #stopNotification} is called.
     */
    public void startNotification() {
        currentTrack = contextManager.currentTrack();
        updateNotification();
        if (notification != null) {
            observeSession();
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_SKIP_FORWARD);
            filter.addAction(ACTION_PAUSE);
            filter.addAction(ACTION_PLAY);
            filter.addAction(ACTION_SKIP_BACK);
            filter.addAction(ACTION_STOP_CAST);
            musicService.registerReceiver(this, filter);
            musicService.startForeground(NOTIFICATION_ID, notification);
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private void observeSession() {
        Rx.dispose(disposable);
        disposable = Flowable.combineLatest(contextManager.queue(), mediaController.state(),
                mediaController.progress(),
                (pair, state, prog) -> {
                    boolean stopNotification = false;

                    Track track = pair.first.get(pair.second);
                    if (!track.equals(currentTrack)) {
                        currentTrack = track;
                    }

                    MediaNotificationManager.this.state = state;
                    if (state == STATE_STOPPED || state == STATE_NONE) {
                        stopNotification = true;
                    }

                    return stopNotification;
                })
                .compose(rx.flowableSchedulers())
                .subscribe(shouldStop -> {
                    if (shouldStop) {
                        stopNotification();
                    } else {
                        updateNotification();
                    }
                }, Rx::onError);
    }

    /**
     * Removes the notification and stops tracking the session. If the session
     * was destroyed this has no effect.
     */
    public void stopNotification() {
        if (notification != null) {
            Rx.dispose(disposable);
            try {
                notificationManager.cancel(NOTIFICATION_ID);
                musicService.unregisterReceiver(this);
            } catch (IllegalArgumentException ignored) {
                // ignore if the receiver is not registered.
            }
            musicService.stopForeground(true);
            notification = null;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null) {
            return;
        }
        switch (intent.getAction()) {
            case ACTION_PAUSE:
            case ACTION_PLAY:
                mediaController.playPause();
                break;
            case ACTION_SKIP_FORWARD:
                mediaController.skipForward();
                break;
            case ACTION_SKIP_BACK:
                mediaController.skipBack();
                break;
            case ACTION_STOP_CAST:
                Intent stopCastIntent = new Intent(context, MusicService.class);
                stopCastIntent.setAction(MusicService.ACTION_STOP_CASTING);
                ContextCompat.startForegroundService(musicService, stopCastIntent);
                break;
            default:
        }
    }

    private Notification createNotification() {
        Timber.d("createNotification currentTrack %s", currentTrack);
        if (currentTrack == null) {
            return null;
        }

        // Notification channels are only supported on Android O+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(musicService, CHANNEL_ID);

        notificationBuilder.addAction(R.drawable.skip_back,
                musicService.getString(R.string.description_skip_back), previousIntent);

        addPlayPauseAction(notificationBuilder);

        notificationBuilder.addAction(R.drawable.skip_forward,
                musicService.getString(R.string.description_skip_forward), nextIntent);

        notificationBuilder
                .setStyle(new MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)
                        .setMediaSession(mediaController.getSessionToken()))
                .setDeleteIntent(stopCastIntent)
                .setSmallIcon(R.drawable.sprocket_logo)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setContentIntent(createContentIntent())
                .setContentTitle(String.format(Locale.US, "Chapter %d",
                        currentTrack.index()))
                .setContentText(currentTrack.albumTitle());

        String castName = mediaController.getCastName();
        if (castName != null) {
            String castInfo = musicService.getResources().getString(R.string.casting_to_device,
                    castName);
            notificationBuilder.setSubText(castInfo);
            notificationBuilder.addAction(R.drawable.ic_notification_close,
                    musicService.getString(R.string.stop_casting), stopCastIntent);
        }

        setNotificationPlaybackState(notificationBuilder);

        if (currentTrack.thumb() != null) {
            loadImage(currentTrack.thumb(), notificationBuilder);
        }

        return notificationBuilder.build();
    }

    private void updateNotification() {
        notification = createNotification();
        if (notification != null) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private void addPlayPauseAction(NotificationCompat.Builder builder) {
        if (state == PlaybackStateCompat.STATE_PLAYING
                || state == PlaybackStateCompat.STATE_BUFFERING) {
            builder.addAction(new NotificationCompat.Action(R.drawable.ic_notification_pause,
                    musicService.getString(R.string.label_pause), pauseIntent));
        } else {
            builder.addAction(new NotificationCompat.Action(R.drawable.ic_notification_play,
                    musicService.getString(R.string.label_play), playIntent));
        }
    }

    private PendingIntent createContentIntent() {
        Intent intent = new Intent(musicService, SprocketActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(musicService, REQUEST_CODE, intent, FLAG_CANCEL_CURRENT);
    }

    private void setNotificationPlaybackState(NotificationCompat.Builder builder) {
        PlaybackStateCompat playbackState = mediaController.getPlaybackState();
        if (playbackState == null || playbackState.getState() == STATE_STOPPED
                || playbackState.getState() == STATE_NONE
                || playbackState.getState() == PlaybackStateCompat.STATE_PAUSED) {
            musicService.stopForeground(false);
            return;
        }
        // Make sure that the notification can be dismissed by the user when we are not playing:
        builder.setOngoing(playbackState.getState() == PlaybackStateCompat.STATE_PLAYING);
    }

    private void loadImage(final String url, final NotificationCompat.Builder builder) {
        Glide.with(musicService)
                .asBitmap()
                .load(url)
                .apply(RequestOptions.overrideOf(iconWidth, iconHeight))
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(
                            @NonNull Bitmap resource,
                            Transition<? super Bitmap> transition
                    ) {
                        if (TextUtils.equals(currentTrack.thumb(), url)) {
                            builder.setLargeIcon(resource);
                            notificationManager.notify(NOTIFICATION_ID, builder.build());
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }

    /**
     * Creates Notification Channel. This is required in Android O+ to display notifications.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    musicService.getString(R.string.notification_channel),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(
                    musicService.getString(R.string.notification_channel_description));
            notificationManager.createNotificationChannel(channel);
        }
    }
}
