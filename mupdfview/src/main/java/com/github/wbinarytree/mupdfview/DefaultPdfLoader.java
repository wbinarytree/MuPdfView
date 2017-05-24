package com.github.wbinarytree.mupdfview;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.SparseArray;
import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Link;
import com.artifex.mupdf.fitz.Matrix;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.Rect;
import com.artifex.mupdf.fitz.android.AndroidDrawDevice;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.wbinarytree.mupdfview.ViewWorker.*;

/**
 * Created by yaoda on 03/05/17.
 */

class DefaultPdfLoader extends PdfLoader {
    private static final int DEFAULT_BUFFER_SIZE = 6;
    private static final int MAX_COUNT = 10;
    private final ViewWorker worker;
    private float displayDPI;
    private PageView pageView;
    private String path;
    private Document doc;

    private RecyclerView thumbnailView;
    private boolean isReflowable;
    private int pageCount;
    private int thumbnailCount;
    private int currentPage;
    private int canvasW;
    private int canvasH;
    private float layoutW, layoutH, layoutEm;
    private boolean hasLoaded;
    private LruCache<Integer, PdfPage> pdfCache;
    private boolean wentBack = false;
    private boolean doublePage;

    DefaultPdfLoader(float displayDPI, PageView pageView) {
        this.displayDPI = displayDPI;
        this.pageView = pageView;
        worker = new ViewWorker();
        hasLoaded = false;
        pdfCache = new LruCache<Integer, PdfPage>(DEFAULT_BUFFER_SIZE) {
            @Override
            protected void entryRemoved(boolean evicted, Integer key, PdfPage oldValue,
                PdfPage newValue) {
                super.entryRemoved(evicted, key, oldValue, newValue);
                oldValue.bitmap.recycle();
            }
        };
    }

    @Override
    int getPageCount() {
        return pageCount;
    }

    @Override
    int getCurrentPage() {
        if (currentPage == -1) {
            return 0;
        } else {
            return currentPage;
        }
    }

    @Override
    void loadPdf(String path) {
        this.path = path;
        if (worker.isWorking()) {
            clearCache();
        } else {
            worker.start();
        }
        this.listener = pageView.getListener();
        openDocument();
    }

    private void openDocument() {
        worker.addTask(new Task() {
            boolean success = true;

            @Override
            public void work() {
                if (doc != null) {
                    doc.destroy();
                    doc = null;
                }
                try {
                    doc = Document.openDocument(path);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void run() {
                if (success) {
                    loadDocument();
                } else {
                    pageView.setError();
                }
            }
        });
    }

    private void loadDocument() {
        worker.addTask(new Task() {
            @Override
            public void work() {
                if (doc == null) return;
                try {
                    if (listener != null) {
                        listener.onStart(doc);
                    }
                    isReflowable = doc.isReflowable();
                    if (isReflowable) {
                        doc.layout(layoutW, layoutH, layoutEm);
                    }
                    pageCount = doc.countPages();
                    thumbnailCount = pageCount > MAX_COUNT ? MAX_COUNT : pageCount;
                } catch (Throwable x) {
                    doc = null;
                    pageCount = 1;
                    currentPage = 0;
                    thumbnailCount = pageCount;
                }
            }

            @Override
            public void run() {
                if (currentPage < 0 || currentPage >= pageCount) currentPage = 0;
                thumbnailCount += currentPage;
                thumbnailCount = thumbnailCount > pageCount ? pageCount : thumbnailCount;
                hasLoaded = true;
                loadPage();
                loadThumbnails();
                // TODO: 11/05/17 load outlines
            }
        });
    }

    private void loadThumbnails() {
        if (thumbnailView != null) {
            worker.addTask(new Task() {
                int start = pageCount > MAX_COUNT ? currentPage : 0;
                SparseArray<Bitmap> bitmaps = new SparseArray<>(pageCount);

                @Override
                public void work() {
                    for (int i = start ; i < thumbnailCount ; i++) {
                        Page page = doc.loadPage(i);
                        Matrix ctm = AndroidDrawDevice.fitPageWidth(page, canvasW / 12);
                        Bitmap bitmap = AndroidDrawDevice.drawPage(page, ctm);
                        bitmaps.put(i, bitmap);
                    }
                }

                @Override
                public void run() {
                    if (adapter != null && thumbnailView != null) {
                        InnerAdapter inner =
                            new InnerAdapter(adapter, bitmaps, DefaultPdfLoader.this, start,
                                thumbnailCount, currentPage);
                        thumbnailView.setAdapter(inner);
                        LazyLoadListener listener =
                            new LazyLoadListener(DefaultPdfLoader.this, pageCount);
                        thumbnailView.addOnScrollListener(listener);
                    }
                    bitmaps = null;
                }
            });
        }
    }

    @Override
    void onPageViewSizeChanged(int w, int h) {
        canvasW = w;
        canvasH = h;
        layoutW = canvasW * 72 / displayDPI;
        layoutH = canvasH * 72 / displayDPI;
    }

    @Override
    void setAdapter(ThumbnailViewAdapter adapter) {
        super.setAdapter(adapter);
        if (this.thumbnailView != null && this.thumbnailView.getAdapter() != null) {
            InnerAdapter current = (InnerAdapter) thumbnailView.getAdapter();
            current.setAdapter(adapter);
            current.notifyDataSetChanged();
        }
    }

    // TODO: 11/05/17
    private void relayoutDocument() {

    }

    private void loadPage() {
        final int pageNumber = currentPage;
        Task task = new LoadPageTask(pageNumber);
        worker.addTask(task);
    }

    @Override
    void nextPage() {
        Utils.checkNotNull(doc);
        if (doublePage) {
            if (currentPage < pageCount - 2) {
                pageView.setCurrentSearch(null);
                currentPage += 2;
                loadPage();
            }
        } else {
            if (currentPage < pageCount - 1) {
                pageView.setCurrentSearch(null);
                currentPage++;
                loadPage();
            }
        }
    }

    @Override
    void previousPage() {
        Utils.checkNotNull(doc);
        if (doublePage) {
            if (currentPage > 0) {
                pageView.setCurrentSearch(null);
                wentBack = false;
                currentPage -= 2;
                loadPage();
            }
        } else {
            if (currentPage > 0) {
                pageView.setCurrentSearch(null);
                wentBack = false;
                currentPage--;
                loadPage();
            }
        }
    }

    @Override
    void gotoPage(int p) {
        Utils.checkNotNull(doc);
        if (p >= 0 && p < pageCount && p != currentPage) {
            pageView.setCurrentSearch(null);
            currentPage = p;
            loadPage();
        }
    }

    @Override
    void setThumbnailView(RecyclerView view) {
        this.thumbnailView = view;
    }

    @Override
    void finish() {
        worker.stop();
        clearCache();
        thumbnailView = null;
        pageView = null;
    }

    private void clearCache() {
        if (worker.isWorking()) worker.restart();
        hasLoaded = false;
        if (pdfCache != null) pdfCache.evictAll();
        if (thumbnailView != null) {
            thumbnailView.setAdapter(null);
            thumbnailView.clearOnScrollListeners();
        }
    }

    @Override
    void search(String words) {
        if (doc == null) return;
        if (words == null) {
            pageView.setCurrentSearch(null);
            return;
        }
        Rect[] search;
        try {
            Page page = doc.loadPage(currentPage);
            Matrix ctm = AndroidDrawDevice.fitPageWidth(page, canvasW);
            search = page.search(words);
            for (Rect rect : search) {
                rect.transform(ctm);
            }
        } catch (Exception e) {
            search = null;
        }
        pageView.setCurrentSearch(search);
    }

    @Override
    public void setDoublePage(boolean doublePage) {
        this.doublePage = doublePage;
        if (hasLoaded) {
            loadPage();
        }
    }

    @Override
    public String getCurrentPath() {
        return path;
    }

    @Override
    public void loadPdfWithPage(String path, int num) {
        this.currentPage = num;
        loadPdf(path);
    }

    private PdfPage loadPdfPage(int pageNumber) {
        int size = canvasW;
        return loadPdfPage(pageNumber, size);
    }

    private PdfPage loadPdfPage(int pageNumber, int size) {
        if (pageNumber > pageCount || pageNumber < 0) {
            return PdfPage.empty();
        }
        PdfPage pdfPage = pdfCache.get(pageNumber);
        if (pdfPage == null) {
            try {
                Page page = doc.loadPage(pageNumber);
                Matrix ctm = AndroidDrawDevice.fitPageWidth(page, size);
                Bitmap bitmap = AndroidDrawDevice.drawPage(page, ctm);
                Link[] links = page.getLinks();
                if (links != null) {
                    for (Link link : links)
                        link.bounds.transform(ctm);
                }
                pdfPage = new PdfPage(bitmap, links);
                pdfCache.put(pageNumber, pdfPage);
            } catch (RuntimeException e) {
                e.printStackTrace();
                pdfPage = PdfPage.empty();
            }
        }
        return pdfPage;
    }

    private void loadNextThumbnails(final AtomicBoolean loaded, final int end) {
        if (thumbnailView != null) {
            worker.addTask(new Task() {

                SparseArray<Bitmap> bitmaps = new SparseArray<>(MAX_COUNT);

                @Override
                public void work() {

                    for (int i = end ; i < end + MAX_COUNT ; i++) {
                        if (i >= pageCount) return;
                        Page page = doc.loadPage(i);
                        Matrix ctm = AndroidDrawDevice.fitPageWidth(page, canvasW / 12);
                        Bitmap bitmap = AndroidDrawDevice.drawPage(page, ctm);

                        bitmaps.put(i, bitmap);
                    }
                }

                @Override
                public void run() {
                    InnerAdapter adapter = (InnerAdapter) thumbnailView.getAdapter();
                    if (adapter == null) {
                        loaded.compareAndSet(false, true);
                        return;
                    }
                    adapter.addNext(bitmaps);
                    thumbnailCount += bitmaps.size();
                    loaded.compareAndSet(false, true);
                }
            });
        }
    }

    private void loadPreviousThumbnails(final AtomicBoolean loaded, final int start) {
        if (thumbnailView != null) {
            worker.addTask(new Task() {

                SparseArray<Bitmap> bitmaps = new SparseArray<>(MAX_COUNT);

                @Override
                public void work() {

                    int from = start - MAX_COUNT < 0 ? 0 : start - MAX_COUNT;
                    for (int i = from ; i < start ; i++) {
                        if (i >= pageCount) return;
                        Page page = doc.loadPage(i);
                        Matrix ctm = AndroidDrawDevice.fitPageWidth(page, canvasW / 12);
                        Bitmap bitmap = AndroidDrawDevice.drawPage(page, ctm);
                        bitmaps.put(i, bitmap);
                    }
                }

                @Override
                public void run() {
                    InnerAdapter adapter = (InnerAdapter) thumbnailView.getAdapter();
                    adapter.addPrevious(bitmaps);
                    thumbnailCount += bitmaps.size();
                    loaded.compareAndSet(false, true);
                }
            });
        }
    }

    private void loadPrevious() {
        final int pageNumber = currentPage - 1;
        if (pageNumber < 0) {
            pageView.setPrevious(null);
            return;
        }

        worker.addTask(new Task() {
            PdfPage pdfPage;

            @Override
            public void work() {
                if (doublePage) {
                    PdfPage left = loadPdfPage(pageNumber - 1, canvasW / 2);
                    PdfPage right = loadPdfPage(pageNumber, canvasW / 2);
                    pdfPage = PdfPage.merge(left, right);
                } else {
                    pdfPage = loadPdfPage(pageNumber);
                }
            }

            @Override
            public void run() {
                pageView.setPrevious(pdfPage.bitmap);
            }
        });
    }

    private void loadNext() {
        final int pageNumber = doublePage ? currentPage + 2 : currentPage + 1;
        if (pageNumber == pageCount) {
            pageView.setNext(null);
            return;
        }
        worker.addTask(new Task() {
            PdfPage pdfPage;

            @Override
            public void work() {
                if (doublePage) {
                    PdfPage left = loadPdfPage(pageNumber, canvasW / 2);
                    PdfPage right = loadPdfPage(pageNumber + 1, canvasW / 2);
                    pdfPage = PdfPage.merge(left, right);
                } else {
                    pdfPage = loadPdfPage(pageNumber);
                }
            }

            @Override
            public void run() {
                pageView.setNext(pdfPage.bitmap);
            }
        });
    }

    /**
     * Use for hold Bitmap and associated links
     */

    private static class LazyLoadListener extends RecyclerView.OnScrollListener {

        private final WeakReference<DefaultPdfLoader> loader;
        private final int pageCount;
        private RecyclerView.LayoutManager manager;
        private int first;
        private int last;
        private volatile AtomicBoolean loadedPre;
        private volatile AtomicBoolean loadedNext;
        private int start, end;
        private InnerAdapter adapter;


        LazyLoadListener(DefaultPdfLoader loader, int pageCount) {
            this.loader = new WeakReference<>(loader);
            this.pageCount = pageCount;
            loadedPre = new AtomicBoolean(true);
            loadedNext = new AtomicBoolean(true);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            manager = recyclerView.getLayoutManager();
            adapter = (InnerAdapter) recyclerView.getAdapter();
            start = adapter.getStart();
            end = adapter.getEnd();

            if (manager instanceof LinearLayoutManager) {
                last = ((LinearLayoutManager) manager).findLastCompletelyVisibleItemPosition();
                first = ((LinearLayoutManager) manager).findFirstCompletelyVisibleItemPosition();
            } else if (manager instanceof StaggeredGridLayoutManager) {
                int[] l =
                    ((StaggeredGridLayoutManager) manager).findLastCompletelyVisibleItemPositions(
                        null);
                last = l[l.length - 1];
                int[] f =
                    ((StaggeredGridLayoutManager) manager).findFirstCompletelyVisibleItemPositions(
                        null);
                first = f[f.length - 1];
            }

            if (loadedNext.get() && last < pageCount && end != pageCount && dx >= 0) {
                recyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        adapter.startLoading();
                    }
                });
                onLoadNext();
            }
            if (loadedPre.get() && first <= 1 && start != 0 && dx <= 0) {
                recyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        adapter.startLoading();
                    }
                });
                onLoadPrevious();
            }
        }

        private void onLoadPrevious() {
            loadedPre.compareAndSet(true, false);
            loader.get().loadPreviousThumbnails(loadedPre, start);
        }

        private void onLoadNext() {
            loadedNext.compareAndSet(true, false);
            loader.get().loadNextThumbnails(loadedNext, end);
        }
    }

    private class LoadPageTask extends Task {
        private final int pageNumber;
        PdfPage pdfPage;

        private LoadPageTask(int pageNumber) {
            this.pageNumber = pageNumber;
        }

        @Override
        public void work() {
            try {
                if (doublePage) {
                    loadDoublePage();
                } else {
                    loadSinglePage();
                }
            } catch (Exception e) {
                pdfPage = PdfPage.empty();
            }
        }

        private void loadSinglePage() {
            pdfPage = loadPdfPage(pageNumber);
        }

        private void loadDoublePage() {
            PdfPage left = loadPdfPage(pageNumber, canvasW / 2);
            PdfPage right = loadPdfPage(pageNumber + 1, canvasW / 2);
            pdfPage = PdfPage.merge(left, right);
        }

        @Override
        public void run() {
            if (pdfPage.bitmap != null) {
                pageView.setBitmap(pdfPage.bitmap, wentBack, pdfPage.links);
                if (listener != null) {
                    listener.onPageChange(currentPage);
                }
                if (thumbnailView != null) {
                    InnerAdapter adapter = (InnerAdapter) thumbnailView.getAdapter();
                    if (adapter != null) adapter.setSelected(currentPage == -1 ? 0 : currentPage);
                }
                loadPrevious();
                loadNext();
            } else {
                pageView.setError();
            }
            wentBack = false;
        }
    }
}
