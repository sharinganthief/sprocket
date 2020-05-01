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
package com.awsomefox.sprocket.ui.adapter;

import android.text.format.DateUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.awsomefox.sprocket.R;
import com.awsomefox.sprocket.data.model.Track;
import com.awsomefox.sprocket.util.Urls;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import butterknife.BindDimen;
import butterknife.BindString;
import butterknife.BindView;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

final class RecentViewHolder extends ClickableViewHolder<Track> {

    @BindView(R.id.track_title_recent)
    TextView title;
    @BindView(R.id.track_subtitle_recent)
    TextView subtitle;
    @BindView(R.id.track_duration_recent)
    TextView duration;
    @BindView(R.id.album_thumb)
    ImageView thumb;
    @BindView(R.id.listen_track)
    ImageView listened;
    @BindString(R.string.chapter_title)
    String chapter_title;
    @BindDimen(R.dimen.item_height)
    int height;

    RecentViewHolder(View view, ViewHolderListener listener) {
        super(view, listener);
        listened.setOnClickListener(this);
    }

    @Override
    void bindModel(@NonNull Track track) {
        subtitle.setText(String.format(chapter_title, track.index()));
        title.setText(track.albumTitle());
        if (track.viewOffset() != 0) {
            listened.setImageResource(R.drawable.partial_listen);
            listened.setTag(PARTIAL);
            String time = DateUtils.formatElapsedTime(track.viewOffset() / 1000)
                    + "/"
                    + DateUtils.formatElapsedTime(track.duration() / 1000);
            duration.setText(time);
        } else if (track.viewCount() != 0) {
            listened.setImageResource(R.drawable.full_listen);
            listened.setTag(FULL);
            duration.setText(DateUtils.formatElapsedTime(track.duration() / 1000));
        } else {
            listened.setImageResource(R.drawable.no_listen);
            listened.setTag(NONE);
            duration.setText(DateUtils.formatElapsedTime(track.duration() / 1000));
        }
        //noinspection SuspiciousNameCombination
        Glide.with(itemView.getContext())
                .load(Urls.addTranscodeParams(track.thumb(), height, height))
                .apply(RequestOptions.centerCropTransform())
                .transition(withCrossFade())
                .into(thumb);
    }
}
