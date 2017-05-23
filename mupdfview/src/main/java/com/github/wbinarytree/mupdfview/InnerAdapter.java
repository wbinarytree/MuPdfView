package com.github.wbinarytree.mupdfview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

class InnerAdapter extends RecyclerView.Adapter<InnerAdapter.ThumbnailHolder> {

    private ThumbnailViewAdapter adapter;
    private PdfLoader loader;
    private SparseArray<Bitmap> bitmapArray;
    private int selected = 0;
    private int start;
    private int end;
    private RecyclerView recyclerView;

    InnerAdapter(ThumbnailViewAdapter adapter, SparseArray<Bitmap> bitmaps, PdfLoader loader) {
        this.adapter = adapter;
        this.bitmapArray = bitmaps;
        this.loader = loader;
        start = 0;
        end = bitmaps.size();
    }

    InnerAdapter(ThumbnailViewAdapter adapter, SparseArray<Bitmap> bitmaps, PdfLoader loader,
        int start, int end, int selected) {
        this.adapter = adapter;
        this.bitmapArray = bitmaps;
        this.loader = loader;
        this.start = start;
        this.end = end;
        this.selected = selected;
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

    SparseArray<Bitmap> getBitmapArray() {
        return bitmapArray;
    }

    @Override
    public ThumbnailHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        ViewGroup root =
            (ViewGroup) LayoutInflater.from(context).inflate(R.layout.pdf_view_item_holder, null);

        ThumbnailHolder thumbnailHolder = new ThumbnailHolder(root);
        thumbnailHolder.v = adapter.onCreateViewHolder(context);
        if (thumbnailHolder.v == null) {
            throw new IllegalStateException("Do not receive thumbnail view");
        }
        if (thumbnailHolder.v.getParent() != null) {
            throw new IllegalStateException("View already have a parent");
        }
        root.addView(thumbnailHolder.v);
        return thumbnailHolder;
    }

    @Override
    public void onBindViewHolder(final ThumbnailHolder holder, final int position) {
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
        return bitmapArray.size();
    }

    void addNext(SparseArray<Bitmap> bitmaps) {
        int size = bitmaps.size();
        for (int i = 0 ; i < size ; i++) {
            this.bitmapArray.put(bitmaps.keyAt(i), bitmaps.valueAt(i));
        }
        notifyItemRangeInserted(end, size);
        end += size;
    }

    void addPrevious(SparseArray<Bitmap> bitmaps) {
        int size = bitmaps.size();
        for (int i = 0 ; i < size ; i++) {
            this.bitmapArray.put(bitmaps.keyAt(i), bitmaps.valueAt(i));
        }
        notifyItemRangeInserted(0, size);
        start -= size;
    }

    static class ThumbnailHolder extends RecyclerView.ViewHolder {
        View v;

        ThumbnailHolder(View itemView) {
            super(itemView);
        }
    }
}
