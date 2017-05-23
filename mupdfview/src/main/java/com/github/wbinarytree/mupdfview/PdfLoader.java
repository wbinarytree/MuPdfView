package com.github.wbinarytree.mupdfview;

import android.support.v7.widget.RecyclerView;

/**
 * Created by yaoda on 05/05/17.
 */

abstract class PdfLoader {
    OnLoadListener listener;
    ThumbnailViewAdapter adapter;

    abstract int getPageCount();

    abstract int getCurrentPage();

    abstract void loadPdf(String Path);

    abstract void onPageViewSizeChanged(int w, int h);

    abstract void nextPage();

    abstract void previousPage();

    abstract void gotoPage(int num);

    abstract void setThumbnailView(RecyclerView view);

    abstract void finish();

    abstract void search(String words);

    void setAdapter(ThumbnailViewAdapter adapter) {
        this.adapter = adapter;
    }

    void setLoadListener(OnLoadListener loadListener) {
        this.listener = loadListener;
    }

    public abstract void setDoublePage(boolean doublePage);

    public abstract String getCurrentPath();

    public abstract void loadPdfWithPage(String path, int num);
}
