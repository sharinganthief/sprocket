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

import android.view.View;
import android.widget.ImageView;

/**
 * ClickableViewHolder adds a click listener to the default ViewHolder
 */
public abstract class ClickableViewHolder<T> extends BaseViewHolder<T>
        implements View.OnClickListener {

  private final ViewHolderListener listener;
    public static final String NONE = "none";
    static final String PARTIAL = "partial";
    public static final String FULL = "full";

  ClickableViewHolder(View view, ViewHolderListener listener) {
    super(view);
    this.listener = listener;
    view.setOnClickListener(this);
  }

  @Override public void onClick(View v) {
      final int position = getBindingAdapterPosition();
    if (position >= 0 && listener != null) {
        if (v.getTag() == null) {
            listener.onClick(position);
        } else {
            String tag = (String) v.getTag();
            ImageView iv = (ImageView) v;
            switch (tag) {
                case PARTIAL:
                case NONE:
                    listener.onMarkFinished(position, iv);
                    break;
                case FULL:
                    listener.onMarkUnstarted(position, iv);
                    break;
                default:
            }
        }
    }
  }

  interface ViewHolderListener {
    void onClick(int position);

      void onMarkFinished(int position, ImageView iv);

      void onMarkUnstarted(int position, ImageView iv);
  }
}
