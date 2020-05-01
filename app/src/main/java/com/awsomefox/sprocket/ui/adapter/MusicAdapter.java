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

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.awsomefox.sprocket.R;
import com.awsomefox.sprocket.data.Type;
import com.awsomefox.sprocket.data.model.Author;
import com.awsomefox.sprocket.data.model.Book;
import com.awsomefox.sprocket.data.model.Header;
import com.awsomefox.sprocket.data.model.MediaType;
import com.awsomefox.sprocket.data.model.PlexItem;
import com.awsomefox.sprocket.data.model.Track;

import java.util.ArrayList;
import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
    implements ClickableViewHolder.ViewHolderListener {

  private final OnPlexItemClickListener listener;
  private List<PlexItem> items = new ArrayList<>();

  public MusicAdapter(OnPlexItemClickListener listener) {
    this.listener = listener;
  }

  @Override @NonNull
  public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    if (viewType == Type.ARTIST) {
      return new AuthorViewHolder(inflater.inflate(R.layout.item_artist, parent, false), this);
    } else if (viewType == Type.ALBUM) {
      return new BookViewHolder(inflater.inflate(R.layout.item_album, parent, false), this);
    } else if (viewType == Type.TRACK) {
      return new TrackViewHolder(inflater.inflate(R.layout.item_track, parent, false), this);
    } else if (viewType == Type.MEDIA_TYPE) {
      return new MediaTypeViewHolder(inflater.inflate(R.layout.item_media_type, parent, false),
          this);
    } else {
      return new HeaderViewHolder(inflater.inflate(R.layout.item_header, parent, false));
    }
  }

  @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    Object item = items.get(position);
    switch (getItemViewType(position)) {
      case Type.ARTIST:
        ((AuthorViewHolder) holder).bindModel((Author) item);
        break;
      case Type.ALBUM:
        ((BookViewHolder) holder).bindModel((Book) item);
        break;
      case Type.TRACK:
        ((TrackViewHolder) holder).bindModel((Track) item);
        break;
      case Type.MEDIA_TYPE:
        ((MediaTypeViewHolder) holder).bindModel((MediaType) item);
        break;
      case Type.HEADER:
        ((HeaderViewHolder) holder).bindModel((Header) item);
        break;
      default:
    }
  }

  @Override public int getItemCount() {
    return items.size();
  }

  @Override public int getItemViewType(int position) {
    PlexItem item = items.get(position);
    if (item instanceof Author) {
      return Type.ARTIST;
    } else if (item instanceof Book) {
      return Type.ALBUM;
    } else if (item instanceof Track) {
      return Type.TRACK;
    } else if (item instanceof MediaType) {
      return Type.MEDIA_TYPE;
    } else {
      return Type.HEADER;
    }
  }

  @Override public void onClick(int position) {
    listener.onPlexItemClicked(items.get(position));
  }

  public void addAll(List<PlexItem> items) {
    this.items.addAll(items);
    notifyDataSetChanged();
  }

  public void set(List<PlexItem> items) {
    this.items = items;
    notifyDataSetChanged();
  }

  public interface OnPlexItemClickListener {
    void onPlexItemClicked(PlexItem plexItem);
  }
}
