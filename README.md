# MuPdfView

Base on [MuPDF](https://mupdf.com/index.html) a custom view that simplify displaying Pdf files.

Basic usage:
-----------

add into your layout file:

```
    <com.github.wbinarytree.mupdfview.PdfView
        android:id="@+id/pdf_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:showThumbnail="true"
        app:doublePage="false"
        />
```

in your java code:

```
   PdfView pdfView = (PdfView) v.findViewById(R.id.pdf_view);
   pdfView.loadPdf(path);
   
    //You need call onDestroy to clear cache when you don't use this view anymore
    @Override
    public void onDestroy() {
        super.onDestroy();
        pdfView.onDestroy();
    }
```

Thumbnails are based on [RecyclerView](https://developer.android.com/reference/android/support/v7/widget/RecyclerView.html).
So you can register your own LayoutManager simply by calling 

```
    pdfView.setLayoutManager(yourLayoutManager);
```

For custom Thumbnails, you need to rewrite following class and inflate your view inside of `onCreateViewHolder(context)`.
Bind your view inside `onBindView(view,position,thumnbail)`

```
public interface ThumbnailViewAdapter {
    void onBindView(View v, int position, Bitmap thumbnail);

    View onCreateViewHolder(Context context);
}
```

For example, the default Adapter: 

```
    private static class DefaultAdapter implements ThumbnailViewAdapter {

        @Override
        public void onBindView(View v, int position, Bitmap thumbnail) {
            ImageView img = (ImageView) v.findViewById(R.id.thumbnails_img);
            img.setImageBitmap(thumbnail);
        }

        @Override
        public View onCreateViewHolder(Context context) {
            LayoutInflater inflater = LayoutInflater.from(context);
            //Don't give your view a parent. Will will add a parent to it
            return inflater.inflate(R.layout.pdf_view_item_thumbnail_default, null);
        }
    }
```

License
--------
```
The MIT License

Copyright 2017 WBinaryTree

Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
and associated documentation files (the "Software"), to deal in the Software without restriction, 
including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do 
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial 
portions of the Software.
```

