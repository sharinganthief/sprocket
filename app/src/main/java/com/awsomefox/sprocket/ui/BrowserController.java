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

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.awsomefox.sprocket.R;
import com.awsomefox.sprocket.SprocketApp;
import com.awsomefox.sprocket.data.Key;
import com.awsomefox.sprocket.data.ServerManager;
import com.awsomefox.sprocket.data.model.Author;
import com.awsomefox.sprocket.data.model.Book;
import com.awsomefox.sprocket.data.model.Library;
import com.awsomefox.sprocket.data.model.MediaType;
import com.awsomefox.sprocket.data.model.PlexItem;
import com.awsomefox.sprocket.data.model.Track;
import com.awsomefox.sprocket.ui.adapter.MusicAdapter;
import com.awsomefox.sprocket.ui.widget.DividerItemDecoration;
import com.awsomefox.sprocket.ui.widget.EndScrollListener;
import com.awsomefox.sprocket.util.Rx;
import com.awsomefox.sprocket.util.Views;
import com.bluelinelabs.conductor.Router;
import com.bluelinelabs.conductor.RouterTransaction;
import com.google.android.gms.cast.framework.CastButtonFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.OnClick;
import timber.log.Timber;

import static com.bluelinelabs.conductor.rxlifecycle2.ControllerEvent.DETACH;

public class BrowserController extends BaseMediaController implements
        MusicAdapter.OnPlexItemClickListener, EndScrollListener.EndListener,
        AdapterView.OnItemSelectedListener {

    private static final int PAGE_SIZE = 50;
    public static final String LIBRARY_PREFERENCE = "com.awsomefox.sprocket.selectedLibrary";
    private final MusicAdapter adapter;
    @BindView(R.id.toolbar_libs_spinner)
    Spinner toolbarSpinner;
    @BindView(R.id.content_loading)
    ContentLoadingProgressBar contentLoading;
    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;
    @BindView(R.id.miniplayer_container)
    FrameLayout miniplayerContainer;
    @BindDrawable(R.drawable.item_divider)
    Drawable itemDivider;
    @BindView(R.id.swipe_view)
    SwipeRefreshLayout swipeRefreshLayout;
    @Inject
    ServerManager serverManager;
    @Inject
    Rx rx;
    private EndScrollListener endScrollListener;
    private List<Library> libs = Collections.emptyList();
    private Library currentLib;
    private MediaType mediaType;
    private int currentPage = -1;
    private boolean isLoading;
    private boolean serverRefreshed;

    public BrowserController(Bundle args) {
        super(args);
        adapter = new MusicAdapter(this);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.controller_browser;
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

        PlexItem plexItem = getArgs().getParcelable(Key.PLEX_ITEM);
        if (plexItem instanceof MediaType) {
            mediaType = (MediaType) plexItem;
        }

        ActionBar actionBar = null;
        if (getActivity() != null) {
            actionBar = ((SprocketActivity) getActivity()).getSupportActionBar();
        }
        if (actionBar != null) {
            setHasOptionsMenu(true);
            if (mediaType == null) {
                actionBar.setDisplayHomeAsUpEnabled(false);
                actionBar.setDisplayShowTitleEnabled(false);
            } else {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setDisplayShowTitleEnabled(true);
                actionBar.setTitle(mediaType.title());
            }
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new DividerItemDecoration(itemDivider));

        contentLoading.hide();
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (mediaType == null) {
                browseLibrary(currentLib, true);
            } else {
                browseMediaType();
            }
        });
        return view;
    }

    @Override
    protected void onAttach(@NonNull View view) {
        super.onAttach(view);
        recyclerView.setAdapter(adapter);
        if (mediaType == null) {
            if (!serverRefreshed) {
                serverRefreshed = true;
                serverManager.refresh();
            }
            observeLibs();
        } else {
            startEndlessScrolling();
            if (currentPage < 0) {
                currentPage = 0;
                browseMediaType();
            }
        }
        observePlayback();

    }

    @Override
    protected void onDetach(@NonNull View view) {
        super.onDetach(view);
        recyclerView.clearOnScrollListeners();
        recyclerView.setAdapter(null);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main, menu);
        CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu,
                R.id.media_route_menu_item);
    }

    @Override
    public void endReached() {
        if (mediaType != null) {
            browseMediaType();
        }
    }

    @Override
    public void onPlexItemClicked(PlexItem plexItem) {
        if (plexItem instanceof MediaType) {
            goToMediaType((MediaType) plexItem);
        } else if (plexItem instanceof Author) {
            goToDetails(plexItem);
        } else if (plexItem instanceof Book) {
            goToDetails(plexItem);
        } else if (plexItem instanceof Track) {
            playTrack((Track) plexItem);
        }
    }

    @Override
    public void onPlexItemMarkFinished(PlexItem plexItem, ImageView iv) {
        markFinished((Track) plexItem, iv);
    }

    @Override
    public void onPlexItemMarkUnstarted(PlexItem plexItem, ImageView iv) {
        markUstarted((Track) plexItem, iv);
    }

    @OnClick(R.id.miniplayer_container)
    void onMiniplayerClicked() {
        getRouter().pushController(RouterTransaction.with(new PlayerController(null)));
    }

    private void observeLibs() {
        disposables.add(serverManager.libs()
                .compose(bindUntilEvent(DETACH))
                .compose(rx.flowableSchedulers())
                .subscribe(libs -> {
                    String persistedLibrary =
                            preferences.getString(LIBRARY_PREFERENCE, "1234");
                    BrowserController.this.libs = libs;

                    ArrayList<String> libNames = new ArrayList<>();
                    int currentPosition = 0;
                    for (int i = 0; i < libs.size(); ++i) {
                        Library lib = libs.get(i);
                        libNames.add(lib.name());
                        if (lib.equals(currentLib) || lib.uuid().equals(persistedLibrary)) {
                            currentPosition = i;
                        }
                    }

                    if (getActivity() != null) {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                                R.layout.item_spinner, libNames);
                        toolbarSpinner.setAdapter(adapter);
                        toolbarSpinner.setSelection(currentPosition);
                        toolbarSpinner.setOnItemSelectedListener(this);
                        Views.visible(toolbarSpinner);
                    }
                }, Rx::onError));
    }

    private void browseLibrary(Library lib, boolean refresh) {
        if (lib.equals(currentLib) && !refresh) {
            return;
        }
        currentLib = lib;
        swipeRefreshLayout.setRefreshing(true);
        disposables.add(musicRepository.browseLibrary(lib)
                .compose(bindUntilEvent(DETACH))
                .compose(rx.singleSchedulers())
                .subscribe(items -> {
                    adapter.set(items);
                    swipeRefreshLayout.setRefreshing(false);
                }, Rx::onError));
    }

    private void browseMediaType() {
        if (isLoading) {
            return;
        }
        swipeRefreshLayout.setRefreshing(true);
        isLoading = true;
        disposables.add(musicRepository.browseMediaType(mediaType, PAGE_SIZE * currentPage, 50)
                .compose(bindUntilEvent(DETACH))
                .compose(rx.singleSchedulers())
                .subscribe(items -> {
                    if (items.isEmpty()) {
                        stopEndlessScrolling();
                    } else {
                        adapter.addAll(items);
                    }
                    swipeRefreshLayout.setRefreshing(false);
                    currentPage++; // Only increment page if current page was loaded successfully
                    isLoading = false;
                }, Rx::onError));
    }

    private void startEndlessScrolling() {
        endScrollListener = new EndScrollListener((LinearLayoutManager)
                recyclerView.getLayoutManager(), this);
        recyclerView.addOnScrollListener(endScrollListener);
    }

    private void stopEndlessScrolling() {
        recyclerView.removeOnScrollListener(endScrollListener);
    }

    private void observePlayback() {
        disposables.add(mediaController.state()
                .compose(bindUntilEvent(DETACH))
                .compose(rx.flowableSchedulers())
                .subscribe(state -> {
                    switch (state) {
                        case PlaybackStateCompat.STATE_ERROR:
                        case PlaybackStateCompat.STATE_NONE:
                        case PlaybackStateCompat.STATE_STOPPED:
                            for (Router router : getChildRouters()) {
                                removeChildRouter(router);
                            }
                            break;
                        default:
                            Router miniplayerRouter = getChildRouter(miniplayerContainer);
                            if (!miniplayerRouter.hasRootController()) {
                                miniplayerRouter.setRoot(RouterTransaction.with(
                                        new MiniPlayerController(null)));
                            }
                    }
                }, Rx::onError));
    }

    private void goToMediaType(MediaType mediaType) {
        Bundle args = new Bundle();
        args.putParcelable(Key.PLEX_ITEM, mediaType);
        getRouter().pushController(RouterTransaction.with(new BrowserController(args)));
    }

    private void goToDetails(PlexItem plexItem) {
        Bundle args = new Bundle();
        args.putParcelable(Key.PLEX_ITEM, plexItem);
        getRouter().pushController(RouterTransaction.with(new DetailController(args)));
    }

    private void playTrack(Track track) {
        Timber.d("playTrack %s", track);
        disposables.add(musicRepository.createPlayQueue(track)
                .compose(bindUntilEvent(DETACH))
                .compose(rx.singleSchedulers())
                .subscribe(pair -> {
                    queueManager.setQueue(pair.first, pair.second, 0L);
                    updateSpeed(preferences.getFloat(PlayerController.SPEED, 1.0f));
                    mediaController.play();
                }, Rx::onError));
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position < libs.size()) {
            preferences.edit().putString(LIBRARY_PREFERENCE, libs.get(position).uuid()).apply();
            browseLibrary(libs.get(position), false);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }
}
