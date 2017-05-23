package com.github.wbinarytree.mupdfview;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Scroller;
import com.artifex.mupdf.fitz.Link;
import com.artifex.mupdf.fitz.Rect;

import static com.github.wbinarytree.mupdfview.Utils.checkNotNull;

/**
 * Created by yaoda on 02/05/17.
 */
public class PageView extends FrameLayout {
    private final GestureDetector detector;
    private final ScaleGestureDetector scaleDetector;
    protected float viewScale;
    protected int bitmapW, bitmapH;
    protected int canvasW, canvasH;
    protected int scrollX, scrollY;
    protected com.artifex.mupdf.fitz.Link[] links;
    protected Scroller scroller;
    protected boolean error;
    protected Paint errorPaint;
    protected Path errorPath;
    protected Paint linkPaint;
    protected Paint searchPaint;
    protected boolean showLinks;
    private PdfLoader loader;
    private float MAX_SCALE = 5f;
    private float DOUBLE_CLICK_SCALE = 2f;
    private float MIN_SCALE = 1f;
    private Bitmap bitmap;
    private Bitmap previous, next;
    private float pX, pY;
    private boolean dragging;
    private RecyclerView thumbnailView;
    private ThumbnailViewAdapter adapter;
    private Rect[] currentSearch;
    private boolean showSearch;
    private boolean doublePage;
    private OnLoadListener listener;
    private float pxMax;
    private float pxMin;
    private boolean loading;
    private ProgressBar progressBar;
    private Paint bitmapPaint;

    public PageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        detector = new GestureDetector(context, new GestureListener(this));
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener(this));
        if (isInEditMode()) {
            loader = new EditableModeLoader(this);
        }

        init(context);
    }

    private void init(Context context) {
        progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyle);
        LayoutParams params =
            new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        progressBar.setVisibility(GONE);
        //progressBar(Color.WHITE);
        progressBar.getIndeterminateDrawable()
            .setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.MULTIPLY);
        progressBar.setLayoutParams(params);
        this.addView(progressBar);
        loading = false;
        scroller = new Scroller(context);
        viewScale = 1f;
        linkPaint = new Paint();
        linkPaint.setARGB(32, 0, 0, 255);
        searchPaint = new Paint();
        searchPaint.setARGB(32, 65, 105, 225);
        pX = 0;
        pY = 0;
        bitmapPaint = new Paint();
        bitmapPaint.setAntiAlias(true);
        bitmapPaint.setFilterBitmap(true);
        bitmapPaint.setDither(true);


        errorPaint = new Paint();
        errorPaint.setARGB(255, 255, 80, 80);
        errorPaint.setStrokeWidth(5);
        errorPaint.setStyle(Paint.Style.STROKE);
        errorPath = new Path();
        errorPath.moveTo(-100, -100);
        errorPath.lineTo(100, 100);
        errorPath.moveTo(100, -100);
        errorPath.lineTo(-100, 100);
        dragging = false;
        showSearch = false;
        listener = null;
    }

    /**
     * Load previous page for loaded Document.
     *
     * @throws NullPointerException if no document loaded
     */
    public void previousPage() {
        float v = (bitmapW - pX) / bitmapW;
        ValueAnimator animator = ValueAnimator.ofFloat(pX, bitmapW);
        animator.setDuration((long) (v * 200));
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                pX = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator.addListener(new DefaultListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (pX == bitmapW) {
                    checkNotNull(loader).previousPage();
                }
            }
        });
        animator.start();
    }

    public void setPrevious(Bitmap bitmap) {
        this.previous = bitmap;
    }

    public void setNext(Bitmap bitmap) {
        this.next = bitmap;
    }

    /**
     * Load next page for loaded Document.
     *
     * @throws NullPointerException if no document loaded
     */
    public void nextPage() {
        float v = (pX + bitmapW) / bitmapW;
        ValueAnimator animator = ValueAnimator.ofFloat(pX, -bitmapW);
        animator.setDuration((long) (v * 200));
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                pX = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator.addListener(new DefaultListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (pX == -bitmapW) {
                    checkNotNull(loader).nextPage();
                }
            }
        });
        animator.start();
    }

    /**
     * Get page count for loaded document.
     *
     * @return the count number as int value
     * @throws NullPointerException if no document loaded
     */
    public int getPageCount() {
        return checkNotNull(loader).getPageCount();
    }

    /**
     * Set up a thumbnail View associate with the Pager.
     *
     * @param view the ViewGroup holder for thumbnail.
     */
    public void setWithThumbnailView(@Nullable RecyclerView view) {
        this.thumbnailView = view;
        if (loader != null) {
            loader.setThumbnailView(this.thumbnailView);
        }
    }

    public void setWithThumbnailView(@Nullable RecyclerView view, ThumbnailViewAdapter adapter) {
        this.thumbnailView = view;
        this.adapter = adapter;
        if (loader != null) {
            loader.setThumbnailView(this.thumbnailView);
            loader.setAdapter(this.adapter);
        }
    }

    public void setThumbnailViewAdapter(ThumbnailViewAdapter adapter) {
        this.adapter = adapter;
        if (loader != null) {
            loader.setAdapter(adapter);
        }
    }

    public OnLoadListener getListener() {
        return listener;
    }

    public void setOnLoadListener(OnLoadListener listener) {
        this.listener = listener;
        if (loader != null) {
            loader.setLoadListener(listener);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP && dragging) {
            int i = canvasW / 6;
            scroller.forceFinished(true);
            boolean lastPage = !doublePage ? loader.getCurrentPage() == loader.getPageCount() - 1
                : loader.getCurrentPage() <= getPageCount() - 2;
            if (pX < -i && !lastPage) {
                this.nextPage();
            } else if (pX > i && loader.getCurrentPage() != 0) {
                this.previousPage();
            } else {
                ValueAnimator animator = ValueAnimator.ofFloat(pX, 0);
                animator.setDuration(200);
                animator.setInterpolator(new AccelerateInterpolator());
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        pX = (float) animation.getAnimatedValue();
                        invalidate();
                    }
                });
                animator.start();
            }
            dragging = false;
        }
        detector.onTouchEvent(event);
        scaleDetector.onTouchEvent(event);
        return true;
    }

    public void setDoublePageEnable(boolean enable) {
        doublePage = enable;
        if (loader != null) {
            loader.setDoublePage(doublePage);
        }
    }

    void setBitmap(Bitmap b, boolean wentBack, Link[] ls) {
        loading = false;
        error = false;
        links = ls;
        bitmap = b;
        bitmapW = (int) (bitmap.getWidth() * viewScale);
        bitmapH = (int) (bitmap.getHeight() * viewScale);
        scroller.forceFinished(true);
        scrollX = wentBack ? bitmapW - canvasW : 0;
        scrollY = wentBack ? bitmapH - canvasH : 0;
        pX = 0;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int x, y;

        if (drawLoading(canvas)) return;

        if (drawError(canvas)) return;

        if (scroller.computeScrollOffset()) {
            scrollX = scroller.getCurrX();
            scrollY = scroller.getCurrY();
            invalidate(); /* keep animating */
        }

        if (bitmapW <= canvasW) {
            scrollX = 0;
            x = (canvasW - bitmapW) / 2;
        } else {
            if (scrollX < 0) scrollX = 0;
            if (scrollX > bitmapW - canvasW) scrollX = bitmapW - canvasW;
            x = -scrollX;
        }

        if (bitmapH <= canvasH) {
            scrollY = 0;
            y = (canvasH - bitmapH) / 2;
        } else {
            if (scrollY < 0) scrollY = 0;
            if (scrollY > bitmapH - canvasH) scrollY = bitmapH - canvasH;
            y = -scrollY;
        }

        canvas.translate(x, y);
        canvas.scale(viewScale, viewScale);

        canvas.drawBitmap(bitmap, pX, 0, bitmapPaint);

        if (pX > 0 && previous != null) {
            canvas.drawBitmap(previous, pX - previous.getWidth(), 0, bitmapPaint);
        }

        if (pX < 0 && next != null) {
            canvas.drawBitmap(next, pX + bitmapW, 0, bitmapPaint);
        }

        if (showLinks && links != null) {
            for (Link link : links) {
                Rect b = link.bounds;
                canvas.drawRect(b.x0 + pX, b.y0, b.x1 + pX, b.y1, linkPaint);
            }
        }

        if (showSearch && currentSearch != null) {
            for (Rect b : currentSearch) {
                canvas.drawRect(b.x0 + pX, b.y0, b.x1 + pX, b.y1, searchPaint);
            }
        }
    }

    private boolean drawLoading(Canvas canvas) {
        if (loading) {
            progressBar.setVisibility(VISIBLE);
            return true;
        } else {
            progressBar.setVisibility(GONE);
            return false;
        }
    }

    private boolean drawError(Canvas canvas) {
        if (bitmap == null) {
            if (error) {
                // TODO: 03/05/17 draw error
                canvas.translate(canvasW / 2, canvasH / 2);
                canvas.drawPath(errorPath, errorPaint);
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        canvasW = w;
        canvasH = h;
        pxMax = canvasW / 3;
        pxMin = -pxMax;
        if (loader != null) {
            loader.onPageViewSizeChanged(w, h);
        }
    }

    private void scale(final float focusX, final float focusY, float scale) {
        if (bitmap != null) {
            scroller.forceFinished(true);
            final float previousScale = viewScale;
            viewScale = scale;
            if (viewScale < MIN_SCALE) viewScale = MIN_SCALE;
            if (viewScale > MAX_SCALE) viewScale = MAX_SCALE;

            bitmapW = (int) (bitmap.getWidth() * viewScale);
            bitmapH = (int) (bitmap.getHeight() * viewScale);
            float pageFocusX = (focusX + scrollX) / previousScale;
            float pageFocusY = (focusY + scrollY) / previousScale;
            scrollX = (int) (pageFocusX * viewScale - focusX);
            scrollY = (int) (pageFocusY * viewScale - focusY);
            pX = 0;
            pY = 0;
            invalidate();
        }
    }

    private void singleTap(MotionEvent e) {

        float x = e.getX();
        float y = e.getY();

        if (showLinks && links != null) {
            float dx = (bitmapW <= canvasW) ? (bitmapW - canvasW) / 2 : scrollX;
            float dy = (bitmapH <= canvasH) ? (bitmapH - canvasH) / 2 : scrollY;
            float mx = (x + dx) / viewScale;
            float my = (y + dy) / viewScale;
            for (Link link : links) {
                Rect b = link.bounds;
                if (mx >= b.x0 && mx <= b.x1 && my >= b.y0 && my <= b.y1) {
                    if (link.uri != null) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link.uri));
                        this.getContext().startActivity(intent);
                    } else if (link.page >= 0) {
                        loader.gotoPage(link.page);
                    }

                    break;
                }
            }
        }
        invalidate();
    }

    private void showLinks() {
        showLinks = !showLinks;
        invalidate();
    }

    private void dragPage(MotionEvent e1, MotionEvent e2, float dx, float dy) {
        if (bitmap != null) {
            dragging = true;
            pX = e2.getX() - e1.getX();
            if (canvasH != bitmapH) {
                scrollY += dy;
            }
            invalidate();
        }
    }

    private void scrollPage(MotionEvent e1, MotionEvent e2, float dx, float dy) {
        if (bitmap != null) {
            scrollX += (int) dx;
            scrollY += (int) dy;
            scroller.forceFinished(true);
            pX = 0;
            pY = 0;
            invalidate();
        }
    }

    // TODO: 03/05/17 determinate fling for zoom/page
    private void fling(MotionEvent e1, MotionEvent e2, float dx, float dy) {
        if (bitmap != null) {
            int maxX = bitmapW > canvasW ? bitmapW - canvasW : 0;
            int maxY = bitmapH > canvasH ? bitmapH - canvasH : 0;
            scroller.forceFinished(true);
            scroller.fling(scrollX, scrollY, (int) -dx, (int) -dy, 0, maxX, 0, maxY);
            invalidate();
        }
    }

    public void onDestroy() {
        if (loader != null) loader.finish();
        if (bitmap != null) bitmap.recycle();
    }

    public void setError() {
        if (bitmap != null) bitmap.recycle();
        loading = false;
        error = true;
        links = null;
        bitmap = null;
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    private void toggleThumbView() {
        if (thumbnailView != null) {
            thumbnailView.setVisibility(thumbnailView.getVisibility() == VISIBLE ? GONE : VISIBLE);
        }
    }

    void setCurrentSearch(Rect[] currentSearch) {
        this.currentSearch = currentSearch;
        showSearch = true;
        invalidate();
    }

    public void search(String words) {
        checkNotNull(loader).search(words);
    }

    @Nullable
    public Integer getCurrentPage() {
        if (loader != null) {
            return loader.getCurrentPage();
        } else {
            return null;
        }
    }

    public String getCurrentPath() {
        return loader.getCurrentPath();
    }

    public void gotoPage(int currentPage) {
        loader.gotoPage(currentPage);
    }

    /**
     * Load pdf with given path
     *
     * @param path the pdf path wait for loading
     * @param num the number of Page want to load, 0 by default
     */

    public void loadPdfWithPage(String path, int num) {
        loading = true;
        invalidate();
        if (loader == null) {
            DisplayMetrics metrics = new DisplayMetrics();
            ((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(metrics);
            loader = new DefaultPdfLoader(metrics.densityDpi, this);
            loader.onPageViewSizeChanged(canvasW, canvasH);
        }
        loader.setThumbnailView(thumbnailView);
        loader.setAdapter(adapter);
        loader.setDoublePage(doublePage);
        loader.loadPdfWithPage(path, num);
    }

    public void loadPdf(String path) {
        this.loadPdfWithPage(path, 0);
    }

    private static class GestureListener
        implements GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
        private PageView pageView;
        private ValueAnimator animator;

        private GestureListener(PageView pageView) {
            this.pageView = pageView;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            pageView.toggleThumbView();
            return true;
        }

        @Override
        public boolean onDoubleTap(final MotionEvent e) {
            final float viewScale =
                pageView.viewScale == pageView.MIN_SCALE ? pageView.DOUBLE_CLICK_SCALE
                    : pageView.MIN_SCALE;

            if (animator != null && animator.isRunning()) {
                animator.cancel();
            }
            animator = ValueAnimator.ofFloat(pageView.viewScale, viewScale);
            animator.setDuration(200);
            animator.setInterpolator(new LinearOutSlowInInterpolator());
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float animatedValue = (float) animation.getAnimatedValue();
                    pageView.scale(e.getX(), e.getY(), animatedValue);
                }
            });
            animator.start();
            return true;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            pageView.scroller.forceFinished(true);
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            pageView.singleTap(e);
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
            if (pageView.viewScale == pageView.MIN_SCALE) {
                pageView.dragPage(e1, e2, dx, dy);
                return true;
            } else {
                pageView.scrollPage(e1, e2, dx, dy);
                return true;
            }
        }

        @Override
        public void onLongPress(MotionEvent e) {
            pageView.showLinks();
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float dx, float dy) {
            pageView.fling(e1, e2, dx, dy);
            return true;
        }
    }

    private static class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {
        private PageView pageView;

        private ScaleListener(PageView pageView) {
            this.pageView = pageView;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {

            float scale = detector.getScaleFactor() * pageView.viewScale;
            scale = scale > pageView.MAX_SCALE ? pageView.MAX_SCALE : scale;
            pageView.scale(detector.getFocusX(), detector.getFocusY(), scale);
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {

        }
    }
}
