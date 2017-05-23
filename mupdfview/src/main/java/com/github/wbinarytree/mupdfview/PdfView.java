package com.github.wbinarytree.mupdfview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class PdfView extends FrameLayout {
    private PageView pageView;
    private Context context;
    private RecyclerView thumbnailView;

    public PdfView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        LayoutInflater inflater =
            (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.pdf_view_default, this);
        pageView = (PageView) findViewById(R.id.pdf_view_page);
        thumbnailView = (RecyclerView) findViewById(R.id.thumbnails);

        if (thumbnailView != null) {
            ThumbnailViewAdapter adapter = new DefaultAdapter();
            pageView.setWithThumbnailView(thumbnailView, adapter);
            thumbnailView.setLayoutManager(
                new LinearLayoutManager(this.context, LinearLayoutManager.HORIZONTAL, false));
            ((SimpleItemAnimator) thumbnailView.getItemAnimator()).setSupportsChangeAnimations(
                false);
        }
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PdfView);
        if (a == null) {
            return;
        }
        boolean doublePage = a.getBoolean(R.styleable.PdfView_doublePage, false);
        this.enableDoublePage(doublePage);
        boolean showThumbnail = a.getBoolean(R.styleable.PdfView_showThumbnail, true);
        this.showThumbnails(showThumbnail);
        a.recycle();
    }

    public void showThumbnails(boolean show) {
        if (!show) {
            thumbnailView.setVisibility(GONE);
            pageView.setWithThumbnailView(null);
        } else {
            pageView.setWithThumbnailView(thumbnailView);
        }
    }

    public void setOnLoadListener(OnLoadListener listener) {
        this.pageView.setOnLoadListener(listener);
    }

    public void setThumbnailViewAdapter(ThumbnailViewAdapter adapter) {
        this.pageView.setThumbnailViewAdapter(adapter);
    }

    public void onDestroy() {
        pageView.onDestroy();
    }

    public void enableDoublePage(boolean enable) {
        this.pageView.setDoublePageEnable(enable);
    }

    public void setLayoutManager(RecyclerView.LayoutManager manager) {
        Utils.checkNotNull(manager);
        this.thumbnailView.setLayoutManager(manager);
    }

    @Override
    public void setBackgroundColor(int color) {
        pageView.setBackgroundColor(color);
    }

    public void loadPdf(String path) {
        this.pageView.loadPdf(path);
    }

    public void loadPdfWithPage(String path, int num) {
        this.pageView.loadPdfWithPage(path, num);
    }

    public void search(String words) {
        this.pageView.search(words);
    }

    @Nullable
    public Integer getCurrentPage() {
        return pageView.getCurrentPage();
    }

    public String getCurrentPath() {
        return pageView.getCurrentPath();
    }

    public void gotoPage(int currentPage) {
        pageView.gotoPage(currentPage);
    }

    private static class DefaultAdapter implements ThumbnailViewAdapter {

        @Override
        public void onBindView(View v, int position, Bitmap thumbnail) {
            ImageView img = (ImageView) v.findViewById(R.id.thumbnails_img);
            img.setImageBitmap(thumbnail);
        }

        @Override
        public View onCreateViewHolder(Context context) {
            LayoutInflater inflater = LayoutInflater.from(context);
            return inflater.inflate(R.layout.pdf_view_item_thumbnail_default, null);
        }
    }
}
