package com.github.wbinarytree.mupdfview;

import android.graphics.Bitmap;
import com.artifex.mupdf.fitz.Link;

import static com.github.wbinarytree.mupdfview.Utils.bitmapMerge;
import static com.github.wbinarytree.mupdfview.Utils.concat;

/**
 * Created by yaoda on 17/05/17.
 */
public class PdfPage {
    final Bitmap bitmap;
    final Link links[];

    public PdfPage(Bitmap bitmap, Link[] links) {
        this.bitmap = bitmap;
        this.links = links;
    }

    public static PdfPage empty() {
        return new PdfPage(null, null);
    }

    public static PdfPage merge(PdfPage page1, PdfPage page2) {
        Link[] links = concat(page1.links, page2.links);
        Bitmap bitmap = bitmapMerge(page1.bitmap, page2.bitmap);
        return new PdfPage(bitmap, links);
    }
}
