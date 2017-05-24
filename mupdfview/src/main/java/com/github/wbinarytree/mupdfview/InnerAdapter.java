package com.github.wbinarytree.mupdfview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import static android.graphics.PorterDuff.Mode.MULTIPLY;
import static android.support.v7.appcompat.R.attr.progressBarStyle;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

class InnerAdapter extends RecyclerView.Adapter<InnerAdapter.ThumbnailHolder> {

    private static final int TYPE_BITMAP = 0x01;
    private static final int TYPE_LOADING = 0x02;

    private ThumbnailViewAdapter adapter;
    private PdfLoader loader;
    private SparseArray<Bitmap> bitmapArray;
    private int selected = 0;
    private boolean loading;
    private int start;
    private int end;
    private RecyclerView recyclerView;
    private int count;

    InnerAdapter(ThumbnailViewAdapter adapter, SparseArray<Bitmap> bitmaps, PdfLoader loader,
        int start, int end, int selected) {
        this.adapter = adapter;
        this.bitmapArray = bitmaps;
        this.loader = loader;
        this.start = start;
        this.end = end;
        this.selected = selected;
        count = bitmaps.size();
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        this.recyclerView = null;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == getItemCount() - 1 && loading) {
            return TYPE_LOADING;
        }
        return super.getItemViewType(position);
    }

    SparseArray<Bitmap> getBitmapArray() {
        return bitmapArray;
    }

    @Override
    public ThumbnailHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        ViewGroup root =
            (ViewGroup) LayoutInflater.from(context).inflate(R.layout.pdf_view_item_holder, null);

        ThumbnailHolder thumbnailHolder = new ThumbnailHolder(root);
        if (viewType == TYPE_LOADING) {
            ProgressBar progressBar = new ProgressBar(context, null, progressBarStyle);
            FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            params.gravity = Gravity.CENTER;
            progressBar.getIndeterminateDrawable().setColorFilter(Color.WHITE, MULTIPLY);
            progressBar.setLayoutParams(params);
            thumbnailHolder.v = progressBar;
            root.addView(thumbnailHolder.v);
        } else {
            thumbnailHolder.v = adapter.onCreateViewHolder(context);
            if (thumbnailHolder.v == null) {
                throw new IllegalStateException("Do not receive thumbnail view");
            }
            if (thumbnailHolder.v.getParent() != null) {
                throw new IllegalStateException("View already have a parent");
            }
            root.addView(thumbnailHolder.v);
        }
        return thumbnailHolder;
    }

    @Override
    public void onBindViewHolder(final ThumbnailHolder holder, final int position) {
        if (getItemViewType(position) == TYPE_LOADING && loading) {
            return;
        }
        final int index = start + position;
        final Bitmap thumbnail = bitmapArray.get(index);

        if (thumbnail == null) {
            holder.v.setVisibility(View.GONE);
        } else {
            holder.v.setVisibility(View.VISIBLE);
        }
        holder.v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loader.gotoPage(index);
            }
        });
        if (selected == index) {
            holder.v.setBackgroundColor(Color.BLUE);
        } else {
            holder.v.setBackgroundColor(Color.TRANSPARENT);
        }
        adapter.onBindView(holder.v, index, thumbnail);
    }

    public void setSelected(int num) {
        notifyItemChanged(selected - start);
        selected = num;
        notifyItemChanged(selected - start);
        if (recyclerView != null) {
            recyclerView.smoothScrollToPosition(selected - start < 0 ? 0 : selected - start);
        }
    }

    @Override
    public int getItemCount() {
        return count;
    }


    public void startLoading() {
        loading = true;
        count++;
        notifyItemInserted(count - 1);
    }

    public void hideLoading() {
        loading = false;
        count--;
        notifyItemRemoved(count - 1);
    }


    void addNext(SparseArray<Bitmap> bitmaps) {
        hideLoading();
        int size = bitmaps.size();
        for (int i = 0 ; i < size ; i++) {
            this.bitmapArray.put(bitmaps.keyAt(i), bitmaps.valueAt(i));
        }
        count += size;
        notifyItemRangeInserted(end, size);
        end += size;
    }

    void addPrevious(SparseArray<Bitmap> bitmaps) {
        hideLoading();
        int size = bitmaps.size();
        for (int i = 0 ; i < size ; i++) {
            this.bitmapArray.put(bitmaps.keyAt(i), bitmaps.valueAt(i));
        }

        count += size;
        notifyItemRangeInserted(0, size);
        start -= size;
    }

    public void setAdapter(ThumbnailViewAdapter adapter) {
        this.adapter = adapter;
    }


    static class ThumbnailHolder extends RecyclerView.ViewHolder {
        View v;

        ThumbnailHolder(View itemView) {
            super(itemView);
        }
    }
}
