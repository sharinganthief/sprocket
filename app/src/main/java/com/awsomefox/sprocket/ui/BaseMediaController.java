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
package com.awsomefox.sprocket.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.awsomefox.sprocket.R;
import com.awsomefox.sprocket.data.model.Track;
import com.awsomefox.sprocket.data.repository.MusicRepository;
import com.awsomefox.sprocket.playback.MediaController;
import com.awsomefox.sprocket.playback.ContextManager;
import com.awsomefox.sprocket.ui.adapter.ClickableViewHolder;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;

import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.schedulers.Schedulers;

abstract class BaseMediaController extends BaseController {
    @Inject
    MediaController mediaController;
    @Inject
    MusicRepository musicRepository;
    @Inject
    ContextManager contextManager;

    SharedPreferences preferences;

    BaseMediaController(Bundle args) {
        super(args);
    }

    @NonNull
    protected View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        View view = super.onCreateView(inflater, container);
        preferences = Objects.requireNonNull(getActivity()).getSharedPreferences("playback-state",
                Context.MODE_PRIVATE);
        return view;
    }

    private void makeToastOnUIThread(String text) {
        Objects.requireNonNull(getActivity()).runOnUiThread(() ->
                Toast.makeText(Objects.requireNonNull(getActivity()), text,
                        Toast.LENGTH_SHORT).show());
    }

    void markUstarted(Track plexItem, ImageView iv) {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                Objects.requireNonNull(getActivity()));
        builder.setTitle(R.string.mark_unstarted)
                .setPositiveButton(R.string.yes, (dialog, id) -> {
                    musicRepository.unscrobble(plexItem)
                            .subscribeOn(Schedulers.io())
                            .subscribe(new DisposableCompletableObserver() {
                                @Override
                                public void onComplete() {
                                    iv.setImageResource(R.drawable.no_listen);
                                    iv.setTag(ClickableViewHolder.NONE);
                                    makeToastOnUIThread("Marked unstarted");
                                }

                                @Override
                                public void onError(Throwable e) {
                                    makeToastOnUIThread("Error marking unstarted");
                                }
                            });
                    dialog.dismiss();
                }).setNegativeButton(R.string.no, (dialog, id) -> dialog.dismiss());
        builder.create().show();
    }

    void markFinished(Track plexItem, ImageView iv) {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                Objects.requireNonNull(getActivity()));
        builder.setTitle(R.string.mark_finshed)
                .setPositiveButton(R.string.yes, (dialog, id) -> {
                    musicRepository.scrobble(plexItem)
                            .subscribeOn(Schedulers.io())
                            .subscribe(new DisposableCompletableObserver() {
                                @Override
                                public void onComplete() {
                                    iv.setImageResource(R.drawable.full_listen);
                                    iv.setTag(ClickableViewHolder.FULL);
                                    makeToastOnUIThread("Marked finished");
                                }

                                @Override
                                public void onError(Throwable e) {
                                    makeToastOnUIThread("Error marking finished");
                                }
                            });
                    dialog.dismiss();
                }).setNegativeButton(R.string.no, (dialog, id) -> dialog.dismiss());
        builder.create().show();
    }

    void updateSpeed(float speed) {
        contextManager.setSpeed(speed);
        mediaController.setSpeed(contextManager.getSpeed());
    }
}
