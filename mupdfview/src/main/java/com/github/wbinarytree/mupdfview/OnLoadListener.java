package com.github.wbinarytree.mupdfview;

import com.artifex.mupdf.fitz.Document;

/**
 * Created by yaoda on 12/05/17.
 */

public interface OnLoadListener {
    void onStart(Document document);

    void onPageChange(int number);
}
