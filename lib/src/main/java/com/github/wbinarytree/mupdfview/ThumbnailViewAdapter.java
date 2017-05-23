package com.github.wbinarytree.mupdfview;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;

/**
 * Created by yaoda on 10/05/17.
 */

public interface ThumbnailViewAdapter {
    void onBindView(View v, int position, Bitmap thumbnail);

    View onCreateViewHolder(Context context);
}
