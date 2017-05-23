package com.github.wbinarytree.mupdfview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.widget.RecyclerView;

/**
 * Created by yaoda on 05/05/17.
 * Use for EditableMode to show a Sample In UI preview
 */

class EditableModeLoader extends PdfLoader {

    private PageView pageView;

    EditableModeLoader(PageView pageView) {
        this.pageView = pageView;
    }

    @Override
    public int getPageCount() {
        return 1;
    }

    @Override
    int getCurrentPage() {
        return 0;
    }

    @Override
    void loadPdf(String Path) {

    }

    @Override
    void onPageViewSizeChanged(int w, int h) {
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.GRAY);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(50);
        paint.setTextAlign(Paint.Align.CENTER);
        int xPos = (canvas.getWidth() / 2);
        int yPos = (int) ((canvas.getHeight() / 2) - ((paint.descent() + paint.ascent()) / 2));
        canvas.drawText("SAMPLE PDF VIEW", xPos, yPos, paint);
        pageView.setBitmap(bmp, false, null);
    }

    @Override
    void nextPage() {

    }

    @Override
    void previousPage() {

    }

    @Override
    void gotoPage(int num) {

    }

    @Override
    void setThumbnailView(RecyclerView view) {

    }

    @Override
    void finish() {

    }

    @Override
    void search(String words) {

    }

    @Override
    public void setDoublePage(boolean doublePage) {

    }

    @Override
    public String getCurrentPath() {
        return null;
    }

    @Override
    public void loadPdfWithPage(String path, int num) {

    }
}
