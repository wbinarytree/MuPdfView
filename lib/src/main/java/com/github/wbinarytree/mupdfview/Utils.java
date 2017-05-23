package com.github.wbinarytree.mupdfview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import java.util.Arrays;

/**
 * Created by yaoda on 23/05/17.
 */

public class Utils {
    public static <T> T checkNotNull(T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }

    public static Bitmap bitmapMerge(Bitmap bmp1, Bitmap bmp2) {
        Bitmap bitmap;
        int mode;
        if (bmp1 == null && bmp2 == null) {
            mode = 1;
        } else if (bmp1 != null && bmp2 == null) {
            mode = 2;
        } else if (bmp1 == null) {
            mode = 3;
        } else {
            mode = 4;
        }
        Canvas canvas;
        switch (mode) {
            case 2:
                bitmap =
                    Bitmap.createBitmap(bmp1.getWidth() * 2, bmp1.getHeight(), bmp1.getConfig());
                canvas = new Canvas(bitmap);
                canvas.drawBitmap(bmp1, 0, 0, null);
                break;
            case 3:
                bitmap =
                    Bitmap.createBitmap(bmp2.getWidth() * 2, bmp2.getHeight(), bmp2.getConfig());
                canvas = new Canvas(bitmap);
                canvas.drawBitmap(bmp2, bmp2.getWidth(), 0, null);
                break;
            case 4:
                bitmap = Bitmap.createBitmap(bmp1.getWidth() + bmp2.getWidth(), bmp1.getHeight(),
                    bmp1.getConfig());
                canvas = new Canvas(bitmap);
                canvas.drawBitmap(bmp1, 0, 0, null);
                canvas.drawBitmap(bmp2, bmp1.getWidth(), 0, null);
                break;
            default:
                bitmap = null;
        }

        return bitmap;
    }

    public static <T> T[] concat(T[] first, T[] second) {
        if (first == null && second == null) {
            return null;
        }
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }

        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
