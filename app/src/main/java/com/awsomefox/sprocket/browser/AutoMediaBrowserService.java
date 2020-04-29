///*
//	This file is part of Subsonic.
//
//	Subsonic is free software: you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation, either version 3 of the License, or
//	(at your option) any later version.
//
//	Subsonic is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//	GNU General Public License for more details.
//
//	You should have received a copy of the GNU General Public License
//	along with Subsonic. If not, see <http://www.gnu.org/licenses/>.
//
//	Copyright 2015 (C) Scott Jackson
//*/
//package com.awsomefox.sprocket.browser;
//
//import android.annotation.SuppressLint;
//import android.annotation.TargetApi;
//import android.content.Intent;
//import android.graphics.BitmapFactory;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Handler;
//import android.support.annotation.Nullable;
//import android.support.v4.media.MediaBrowserCompat;
//import android.support.v4.media.MediaBrowserServiceCompat;
//import android.support.v4.media.MediaDescriptionCompat;
//
//import androidx.annotation.Nullable;
//import androidx.media.MediaBrowserServiceCompat;
//
//import com.awsomefox.sprocket.R;
//import com.awsomefox.sprocket.data.model.MediaType;
//import com.awsomefox.sprocket.data.model.PlexItem;
//import com.awsomefox.sprocket.data.repository.MusicRepository;
//import com.awsomefox.sprocket.util.Rx;
//import com.google.android.exoplayer2.offline.DownloadService;
//
//import org.jetbrains.annotations.NotNull;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import javax.inject.Inject;
//
//import io.reactivex.disposables.CompositeDisposable;
//
//import static com.awsomefox.sprocket.data.Type.ARTIST;
//import static com.bluelinelabs.conductor.rxlifecycle2.ControllerEvent.DETACH;
//
//
//public class AutoMediaBrowserService extends MediaBrowserServiceCompat {
//	private static final String TAG = AutoMediaBrowserService.class.getSimpleName();
//	private static final String BROWSER_ROOT = "root";
//	private static final String BROWSER_BOOK_LISTS = "books";
//	private static final String BROWSER_AUTHOR_LIST = "authors";
//	private static final String BROWSER_BOOKMARKS = "bookmarks"; //TODO implement
//	private static final String BOOKS_FROM_AUTHOR_PREFIX = "books-by-author-";
//	private static final String CHAPTERS_FROM_BOOK_PREFIX = "chapters-in-book";
//
//	CompositeDisposable disposables;
//
//	@Inject
//	MusicRepository musicRepository;
//
//	@Override
//	public void onCreate() {
//		super.onCreate();
//		disposables = new CompositeDisposable();
//	}
//
//	@Nullable
//	@Override
//	public MediaBrowserServiceCompat.BrowserRoot onGetRoot(@NotNull String clientPackageName, int clientUid, Bundle rootHints) {
//		return new BrowserRoot(BROWSER_ROOT, null);
//	}
//
//	@Override
//	public void onLoadChildren(String parentId, Result<List<MediaBrowserCompat.MediaItem>> result) {
//		if(BROWSER_ROOT.equals(parentId)) {
//			getRootFolders(result);
//		} else if(BROWSER_BOOK_LISTS.equals(parentId)) {
//			//get books
//		} else if(BROWSER_AUTHOR_LIST.equals(parentId)) {
//			//get authors
//		}  else if(BROWSER_BOOKMARKS.equals(parentId)) {
//			//get chapters
//		} else {
//			// No idea what it is, send empty result
//			result.sendResult(new ArrayList<>());
//		}
//	}
//
//	private void getRootFolders(Result<List<MediaBrowserCompat.MediaItem>> result) {
//		List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
//
//
//		MediaDescriptionCompat.Builder rootLists = new MediaDescriptionCompat.Builder();
//
//		// Add authors to root
//		rootLists.setTitle(getString(R.string.authors))
//				.setMediaId(BROWSER_AUTHOR_LIST)
//				.setIconBitmap( BitmapFactory.decodeResource(getApplicationContext().getResources(),
//					R.drawable.authors));
//		mediaItems.add(new MediaBrowserCompat.MediaItem(rootLists.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
//
//		// Add Books to root
//		rootLists.setTitle(getString(R.string.books))
//				.setMediaId(BROWSER_BOOK_LISTS)
//				.setIconBitmap( BitmapFactory.decodeResource(getApplicationContext().getResources(),
//						R.drawable.books));
//		mediaItems.add(new MediaBrowserCompat.MediaItem(rootLists.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
//
//		// Add Bookmarks to root
//		rootLists.setTitle(getString(R.string.books))
//				.setMediaId(BROWSER_BOOK_LISTS)
//				.setIconBitmap( BitmapFactory.decodeResource(getApplicationContext().getResources(),
//						R.drawable.bookmarks));
//		mediaItems.add(new MediaBrowserCompat.MediaItem(rootLists.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
//
//		result.sendResult(mediaItems);
//	}
//
//	@SuppressLint("CheckResult")
//	private void getBookLists(Result<List<MediaBrowserCompat.MediaItem>> result) {
//		int recievedAmount = 50;
//		int page = 0;
////		while (recievedAmount == 50) {
////			List<PlexItem> results = musicRepository.browseMediaType(MediaType.builder().type(ARTIST).build(), page * 50);
//			musicRepository.browseMediaType(MediaType.builder().type(ARTIST).build(), page * 50)
//					.subscribe(items -> {
////						recievedAmount = items.size();
//						addPlexItemsToBrowserResult(result, items);
//					});
////		}
//		musicRepository.browseMediaType(ARTIST)
//		List<Integer> albums = new ArrayList<>();
//		albums.add(R.string.main_albums_newest);
//		albums.add(R.string.main_albums_random);
//		albums.add(R.string.main_albums_starred);
//		albums.add(R.string.main_albums_recent);
//
//		List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
//
//		for(Integer id: albums) {
//			MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
//					.setTitle(downloadService.getResources().getString(id))
//					.setMediaId(ALBUM_TYPE_PREFIX + id)
//					.build();
//
//			mediaItems.add(new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
//		}
//
//		result.sendResult(mediaItems);
//	}
//
//	private void addPlexItemsToBrowserResult(Result<List<MediaBrowserCompat.MediaItem>> result, List<MediaType> items) {
//		for (PlexItem item: items) {
//			MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
//					.setTitle(item)
//					.setMediaId(ALBUM_TYPE_PREFIX + id)
//					.build();
//		}
//	}
//
//	private void getBookList(final Result<List<MediaBrowserCompat.MediaItem>> result, final int id) {
//		new SilentServiceTask<MusicDirectory>(downloadService) {
//			@Override
//			protected MusicDirectory doInBackground(MusicService musicService) throws Throwable {
//				String albumListType;
//				switch(id) {
//					case R.string.main_albums_newest:
//						albumListType = "newest";
//						break;
//					case R.string.main_albums_random:
//						albumListType = "random";
//						break;
//					case R.string.main_albums_starred:
//						albumListType = "starred";
//						break;
//					case R.string.main_albums_recent:
//						albumListType = "recent";
//						break;
//					default:
//						albumListType = "newest";
//				}
//
//				return musicService.getBookList(albumListType, 20, 0, true, downloadService, null);
//			}
//
//			@Override
//			protected void done(MusicDirectory albumSet) {
//				List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
//
//				for(MusicDirectory.Entry album: albumSet.getChildren(true, false)) {
//					MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
//							.setTitle(album.getBookDisplay())
//							.setSubtitle(album.getAuthor())
//							.setMediaId(MUSIC_DIRECTORY_PREFIX + album.getId())
//							.build();
//
//					mediaItems.add(new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
//				}
//
//				result.sendResult(mediaItems);
//			}
//		}.execute();
//
//		result.detach();
//	}
//
//	private void getLibrary(final Result<List<MediaBrowserCompat.MediaItem>> result) {
//		new SilentServiceTask<List<MusicFolder>>(downloadService) {
//			@Override
//			protected List<MusicFolder> doInBackground(MusicService musicService) throws Throwable {
//				return musicService.getMusicFolders(false, downloadService, null);
//			}
//
//			@Override
//			protected void done(List<MusicFolder> folders) {
//				List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
//
//				for(MusicFolder folder: folders) {
//					MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
//							.setTitle(folder.getName())
//							.setMediaId(MUSIC_FOLDER_PREFIX + folder.getId())
//							.build();
//
//					mediaItems.add(new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
//				}
//
//				result.sendResult(mediaItems);
//			}
//		}.execute();
//
//		result.detach();
//	}
//	private void getIndexes(final Result<List<MediaBrowserCompat.MediaItem>> result, final String musicFolderId) {
//		new SilentServiceTask<Indexes>(downloadService) {
//			@Override
//			protected Indexes doInBackground(MusicService musicService) throws Throwable {
//				return musicService.getIndexes(musicFolderId, false, downloadService, null);
//			}
//
//			@Override
//			protected void done(Indexes indexes) {
//				List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
//
//				// music directories
//				for(Author artist : indexes.getAuthors()) {
//					MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
//							.setTitle(artist.getName())
//							.setMediaId(MUSIC_DIRECTORY_CONTENTS_PREFIX + artist.getId())
//							.build();
//
//					mediaItems.add(new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
//				}
//
//				// music files
//				for(MusicDirectory.Entry entry: indexes.getEntries()) {
//					MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
//							.setTitle(entry.getTitle())
//							.setMediaId(MUSIC_DIRECTORY_PREFIX + entry.getId())
//							.build();
//
//					mediaItems.add(new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
//				}
//
//				result.sendResult(mediaItems);
//			}
//		}.execute();
//
//		result.detach();
//	}
//
//	private void getMusicDirectory(final Result<List<MediaBrowserCompat.MediaItem>> result, final String musicDirectoryId) {
//		new SilentServiceTask<MusicDirectory>(downloadService) {
//			@Override
//			protected MusicDirectory doInBackground(MusicService musicService) throws Throwable {
//				return musicService.getMusicDirectory(musicDirectoryId, "", false, downloadService, null);
//			}
//
//			@Override
//			protected void done(MusicDirectory directory) {
//				List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
//
//				addPlayOptions(mediaItems, musicDirectoryId, Constants.INTENT_EXTRA_NAME_ID);
//
//				for(MusicDirectory.Entry entry : directory.getChildren()) {
//					MediaDescriptionCompat description;
//					if (entry.isDirectory()) {
//						// browse deeper
//						description = new MediaDescriptionCompat.Builder()
//								.setTitle(entry.getTitle())
//								.setSubtitle("Chapter "+ entry.getTrack())
//								.setMediaId(MUSIC_DIRECTORY_CONTENTS_PREFIX + entry.getId())
//								.build();
//					} else {
//						// playback options for a single item
//						description = new MediaDescriptionCompat.Builder()
//								.setTitle(entry.getTitle())
//								.setSubtitle("Chapter "+ entry.getTrack())
//								.setMediaId(MUSIC_DIRECTORY_PREFIX + entry.getId())
//								.build();
//					}
//
//					mediaItems.add(new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
//				}
//				result.sendResult(mediaItems);
//			}
//		}.execute();
//
//		result.detach();
//	}
//
//	private void getBookmarks(final Result<List<MediaBrowserCompat.MediaItem>> result) {
//		new SilentServiceTask<MusicDirectory>(downloadService) {
//			@Override
//			protected MusicDirectory doInBackground(MusicService musicService) throws Throwable {
//				return musicService.getBookmarks(false, downloadService, null);
//			}
//
//			@Override
//			protected void done(MusicDirectory bookmarkList) {
//				List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
//
//				for(MusicDirectory.Entry entry: bookmarkList.getChildren(false, true)) {
//					Bundle extras = new Bundle();
//					extras.putString(Constants.INTENT_EXTRA_NAME_PARENT_ID, entry.getParent());
//
//					MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
//							.setTitle(entry.getTitle())
//							.setSubtitle("Chapter "+ entry.getTrack() + " - " + Util.formatDuration(entry.getBookmark().getPosition() / 1000))
//							.setMediaId(MUSIC_DIRECTORY_PREFIX + entry.getId())
//							.setExtras(extras)
//							.build();
//
//					mediaItems.add(new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE));
//				}
//
//				result.sendResult(mediaItems);
//			}
//		}.execute();
//
//		result.detach();
//	}
//	private void addPlayOptions(List<MediaBrowserCompat.MediaItem> mediaItems, String id, String idConstant) {
//		Bundle playAllExtras = new Bundle();
//		playAllExtras.putString(idConstant, id);
//
//		MediaDescriptionCompat.Builder playAll = new MediaDescriptionCompat.Builder();
//		playAll.setTitle(downloadService.getString(R.string.menu_play))
//				.setMediaId("play-" + id)
//				.setExtras(playAllExtras);
//		mediaItems.add(new MediaBrowserCompat.MediaItem(playAll.build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE));
//	}
//
//	private void getPlayOptions(Result<List<MediaBrowserCompat.MediaItem>> result, String id, String idConstant) {
//		List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
//
//		addPlayOptions(mediaItems, id, idConstant);
//
//		result.sendResult(mediaItems);
//	}
//
//	public void getDownloadService() {
//		if(DownloadService.getInstance() == null) {
//			startService(new Intent(this, DownloadService.class));
//		}
//
//		waitForDownloadService();
//	}
//	public void waitForDownloadService() {
//		downloadService = DownloadService.getInstance();
//		if(downloadService == null) {
//			handler.postDelayed(new Runnable() {
//				@Override
//				public void run() {
//					waitForDownloadService();
//				}
//			}, 100);
//		} else {
//			RemoteControlClientLP remoteControlClient = (RemoteControlClientLP) downloadService.getRemoteControlClient();
//			setSessionToken(remoteControlClient.getMediaSession().getSessionToken());
//		}
//	}
//}
