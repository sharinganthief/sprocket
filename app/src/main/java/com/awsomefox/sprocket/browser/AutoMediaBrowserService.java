
package com.awsomefox.sprocket.browser;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.text.format.DateUtils;

import androidx.annotation.Nullable;
import androidx.media.MediaBrowserServiceCompat;

import com.awsomefox.sprocket.R;
import com.awsomefox.sprocket.data.Type;
import com.awsomefox.sprocket.data.model.Author;
import com.awsomefox.sprocket.data.model.Book;
import com.awsomefox.sprocket.data.model.Library;
import com.awsomefox.sprocket.data.model.MediaType;
import com.awsomefox.sprocket.data.model.PlexItem;
import com.awsomefox.sprocket.data.model.Track;
import com.awsomefox.sprocket.playback.MusicService;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static com.awsomefox.sprocket.ui.PlayerController.BUNDLE_TRACK_KEY;
import static com.awsomefox.sprocket.ui.PlayerController.BUNDLE_TRACK_LIBRARY_ID;
import static com.awsomefox.sprocket.ui.PlayerController.BUNDLE_TRACK_PARENT_KEY;
import static com.awsomefox.sprocket.ui.PlayerController.BUNDLE_TRACK_URI;
import static com.awsomefox.sprocket.ui.PlayerController.SPEED;

public class AutoMediaBrowserService extends MediaBrowserServiceCompat {
    /**
     * Bundle extra indicating the presentation hint for browsable media items.
     */
    public static final String CONTENT_STYLE_BROWSABLE_HINT =
            "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT";
    /**
     * Specifies the corresponding items should be presented as lists.
     */
    public static final int CONTENT_STYLE_LIST_ITEM_HINT_VALUE = 1;
    /**
     * Specifies that the corresponding items should be presented as grids.
     */
    public static final int CONTENT_STYLE_GRID_ITEM_HINT_VALUE = 2;
    private static final String BROWSER_ROOT = "root";
    private static final String LIBRARY_ROOT_PREFIX = "library-root-";
    private static final String BROWSER_BOOK_LISTS_PREFIX = "books-";
    private static final String BROWSER_AUTHOR_LIST_PREFIX = "authors-";
    private static final String BROWSER_CHAPTERS_IN_PROGRESS = "chapters-";
    private static final String BROWSER_BOOKS_IN_PROGRESS = "bookinprog-";
    private static final String BOOKS_FROM_AUTHOR_PREFIX = "booksbyauthor-";
    private static final String CHAPTERS_FROM_BOOK_PREFIX = "chaptersinbook-";
    CompositeDisposable disposables;

    private Map<String, Library> currentLibraries;
    private Map<String, Author> currentAuthors;
    private Map<String, Book> currentBooks;

    private MusicService musicService;
    private Handler handler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        disposables = new CompositeDisposable();
        createMusicService();

        currentLibraries = new HashMap<>();
        currentAuthors = new HashMap<>();
        currentBooks = new HashMap<>();
    }

    public void createMusicService() {
        if (MusicService.getInstance() == null) {
            startService(new Intent(this, MusicService.class));
            musicService = MusicService.getInstance();
        }

        waitForMusicService();
    }

    public void waitForMusicService() {
        musicService = MusicService.getInstance();
        if (musicService == null) {
            handler.postDelayed(this::waitForMusicService, 100);
        } else {
            setSessionToken(musicService.session.getSessionToken());
        }
    }

    @Nullable
    @Override
    public MediaBrowserServiceCompat.BrowserRoot onGetRoot(@NotNull String clientPackageName,
                                                           int clientUid, Bundle rootHints) {
        return new BrowserRoot(BROWSER_ROOT, null);
    }

    @Override
    public void onLoadChildren(@NotNull String parentId,
                               @NotNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        Timber.d("Getting folders with parent id:%s", parentId);
        if (BROWSER_ROOT.equals(parentId)) {
            getRootFolders(result);
        } else if (parentId.startsWith(LIBRARY_ROOT_PREFIX)) {
            getLibraryRootFolders(result, parentId.substring(LIBRARY_ROOT_PREFIX.length()));
        } else if (parentId.startsWith(BROWSER_BOOK_LISTS_PREFIX)) {
            //get books
            getBookLists(result, parentId.substring(BROWSER_BOOK_LISTS_PREFIX.length()));
        } else if (parentId.startsWith(BROWSER_AUTHOR_LIST_PREFIX)) {
            //get authors
            getAuthorLists(result, parentId.substring(BROWSER_AUTHOR_LIST_PREFIX.length()));
        } else if (parentId.startsWith(BROWSER_CHAPTERS_IN_PROGRESS)) {
            //get chapters
            getChapterInProgressLists(result,
                    parentId.substring(BROWSER_CHAPTERS_IN_PROGRESS.length()));
        } else if (parentId.startsWith(BROWSER_BOOKS_IN_PROGRESS)) {
            getBooksInProgressLists(result, parentId.substring(BROWSER_BOOKS_IN_PROGRESS.length()));
            //get books in progress
        } else if (parentId.startsWith(BOOKS_FROM_AUTHOR_PREFIX)) {
            getBooksFromAuthor(result, parentId.substring(BOOKS_FROM_AUTHOR_PREFIX.length()));
            //get books in progress
        } else if (parentId.startsWith(CHAPTERS_FROM_BOOK_PREFIX)) {
            getChaptersFromBook(result, parentId.substring(CHAPTERS_FROM_BOOK_PREFIX.length()));
            //get books in progress
        } else {
            // No idea what it is, send empty result
            result.sendResult(new ArrayList<>());
        }
    }

    @SuppressLint("CheckResult")
    private void getRootFolders(Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.detach();
        Timber.d("Getting root libraries.");
        Timber.d(musicService.serverManager.toString());
        disposables.add(musicService.serverManager.libs()
                .compose(musicService.rx.flowableSchedulers())
                .subscribe(items -> {
                    Timber.d(items.toString());
                    List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
                    for (PlexItem item : items) {
                        Library library = (Library) item;
                        currentLibraries.put(library.uuid(), library);
                        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                                .setTitle(library.name())
                                .setMediaId(LIBRARY_ROOT_PREFIX + library.uuid())
                                .build();
                        mediaItems.add(new MediaBrowserCompat.MediaItem(description,
                                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));

                    }
                    result.sendResult(mediaItems);
                }, Timber::e));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposables.dispose();
    }

    private void getLibraryRootFolders(Result<List<MediaBrowserCompat.MediaItem>> result,
                                       String libraryId) {
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

        MediaDescriptionCompat.Builder rootLists = new MediaDescriptionCompat.Builder();

        Bundle authorStyle = new Bundle();
        authorStyle.putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_LIST_ITEM_HINT_VALUE);

        Bundle bookStyle = new Bundle();
        bookStyle.putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_GRID_ITEM_HINT_VALUE);

        // Add authors to root
        rootLists.setTitle(getString(R.string.authors))
                .setMediaId(BROWSER_AUTHOR_LIST_PREFIX + libraryId)
                .setIconUri(Uri.parse("android.resource://com.awsomefox.sprocket/"
                        + R.drawable.authors))
                .setExtras(authorStyle);

        mediaItems.add(new MediaBrowserCompat.MediaItem(rootLists.build(),
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));

        // Add Books to root
        rootLists.setTitle(getString(R.string.books))
                .setMediaId(BROWSER_BOOK_LISTS_PREFIX + libraryId)
                .setIconUri(Uri.parse("android.resource://com.awsomefox.sprocket/"
                        + R.drawable.books))
                .setExtras(bookStyle);
        mediaItems.add(new MediaBrowserCompat.MediaItem(rootLists.build(),
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));

        // Add chapters in progress to root
        rootLists.setTitle("Chapters in progress")
                .setMediaId(BROWSER_CHAPTERS_IN_PROGRESS + libraryId)
                .setIconUri(Uri.parse("android.resource://com.awsomefox.sprocket/"
                        + R.drawable.bookmarks));
        mediaItems.add(new MediaBrowserCompat.MediaItem(rootLists.build(),
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));

        // Add books in pprogress to root
        rootLists.setTitle("Books in progress")
                .setMediaId(BROWSER_BOOKS_IN_PROGRESS + libraryId)
                .setIconUri(Uri.parse("android.resource://com.awsomefox.sprocket/"
                        + R.drawable.bookmarks))
                .setExtras(bookStyle);
        mediaItems.add(new MediaBrowserCompat.MediaItem(rootLists.build(),
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));

        result.sendResult(mediaItems);
    }

    private void getBookLists(Result<List<MediaBrowserCompat.MediaItem>> result, String libraryId) {
        result.detach();
        Library lib = currentLibraries.get(libraryId);
        MediaType type = MediaType.builder()
                .title("Books")
                .type(Type.ALBUM)
                .mediaKey("9")
                .libraryKey(Objects.requireNonNull(lib).key())
                .libraryId(lib.uuid())
                .uri(lib.uri())
                .build();
        disposables.add(musicService.musicRepository.browseMediaType(type, 0, null)
                .subscribeOn(Schedulers.io())
                .subscribe(items -> addPlexItemsToBrowserResult(result, items)));
    }

    private void getAuthorLists(Result<List<MediaBrowserCompat.MediaItem>> result,
                                String libraryId) {
        result.detach();
        Library lib = currentLibraries.get(libraryId);
        MediaType type = MediaType.builder()
                .title("Author")
                .type(Type.ARTIST)
                .mediaKey("8")
                .libraryKey(Objects.requireNonNull(lib).key())
                .libraryId(lib.uuid())
                .uri(lib.uri())
                .build();
        disposables.add(musicService.musicRepository.browseMediaType(type, 0, null)
                .subscribeOn(Schedulers.io())
                .subscribe(items -> addPlexItemsToBrowserResult(result, items)));
    }

    private void getChapterInProgressLists(Result<List<MediaBrowserCompat.MediaItem>> result,
                                           String libraryId) {
        result.detach();
        Library lib = currentLibraries.get(libraryId);
        disposables.add(musicService.musicRepository.chaptersInProgress(lib)
                .subscribeOn(Schedulers.io())
                .subscribe(items -> addPlexItemsToBrowserResult(result, items)));
    }

    private void getBooksInProgressLists(Result<List<MediaBrowserCompat.MediaItem>> result,
                                         String libraryId) {
        result.detach();
        Library lib = currentLibraries.get(libraryId);
        disposables.add(musicService.musicRepository.booksInProgress(lib)
                .subscribeOn(Schedulers.io())
                .subscribe(items -> addPlexItemsToBrowserResult(result, items)));
    }

    private void getChaptersFromBook(Result<List<MediaBrowserCompat.MediaItem>> result,
                                     String bookId) {
        result.detach();
        Book book = currentBooks.get(bookId);
        disposables.add(musicService.musicRepository.albumItems(book)
                .subscribeOn(Schedulers.io())
                .subscribe(items -> addPlexItemsToBrowserResult(result, items)));
    }

    private void getBooksFromAuthor(Result<List<MediaBrowserCompat.MediaItem>> result,
                                    String authorId) {
        result.detach();
        Author author = currentAuthors.get(authorId);
        disposables.add(musicService.musicRepository.artistItems(author)
                .subscribeOn(Schedulers.io())
                .subscribe(items -> addPlexItemsToBrowserResult(result, items)));

    }

    private void addPlexItemsToBrowserResult(Result<List<MediaBrowserCompat.MediaItem>> result,
                                             List<PlexItem> items) {
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        for (PlexItem item : items) {
            if (item instanceof Book) {
                Book book = (Book) item;
                currentBooks.put(book.ratingKey(), book);
                MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                        .setTitle(book.title())
                        .setSubtitle(book.artistTitle())
                        .setMediaId(CHAPTERS_FROM_BOOK_PREFIX + book.ratingKey())
                        .setIconUri(Uri.parse(Uri.decode(book.thumb())))
                        .build();
                mediaItems.add(new MediaBrowserCompat.MediaItem(description,
                        MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
            } else if (item instanceof Author) {
                Bundle bookStyle = new Bundle();
                bookStyle.putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_GRID_ITEM_HINT_VALUE);
                Author author = (Author) item;
                currentAuthors.put(author.ratingKey(), author);
                MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                        .setTitle(author.title())
                        .setMediaId(BOOKS_FROM_AUTHOR_PREFIX + author.ratingKey())
                        .setExtras(bookStyle)
                        .setIconUri(Uri.parse(Uri.decode(author.thumb())))
                        .build();
                mediaItems.add(new MediaBrowserCompat.MediaItem(description,
                        MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
            } else if (item instanceof Track) {
                Track track = (Track) item;
                Bundle extras = new Bundle();
                extras.putString(BUNDLE_TRACK_URI, track.uri().toString());
                extras.putString(BUNDLE_TRACK_KEY, track.key());
                extras.putString(BUNDLE_TRACK_PARENT_KEY, track.parentKey());
                extras.putString(BUNDLE_TRACK_LIBRARY_ID, track.libraryId());
                extras.putFloat(SPEED,
                        musicService.queueManager.getSpeed());
                MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                        .setTitle(track.title())
                        .setSubtitle(track.artistTitle())
                        .setDescription(DateUtils.formatElapsedTime(track.viewOffset() / 1000)
                                + "/" + DateUtils.formatElapsedTime(track.duration() / 1000))
                        .setMediaId(track.ratingKey())
                        .setIconUri(Uri.parse(Uri.decode(track.thumb())))
                        .setExtras(extras)
                        .build();
                mediaItems.add(new MediaBrowserCompat.MediaItem(description,
                        MediaBrowserCompat.MediaItem.FLAG_PLAYABLE));
            }

        }
        result.sendResult(mediaItems);
    }
}
