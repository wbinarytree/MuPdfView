package com.github.wbinarytree.sample;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.github.wbinarytree.mupdfview.PdfView;

/**
 * Created by yaoda on 23/05/17.
 */

public class MuPdfFragment extends Fragment {
    public static final String TAG = "MuPdfFragment";
    private PdfView pdfView;

    public static MuPdfFragment newInstance(String path) {
        Bundle args = new Bundle();
        args.putString("path", path);
        MuPdfFragment fragment = new MuPdfFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }
        View v;
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            v = inflater.inflate(R.layout.fragment_pdf_landscape, container, false);
        } else {
            v = inflater.inflate(R.layout.fragment_pdf, container, false);
        }
        pdfView = (PdfView) v.findViewById(R.id.pdf_view);
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            String currentPath = savedInstanceState.getString("currentPath");
            final int currentPage = savedInstanceState.getInt("currentPage", -1);
            if (currentPath != null && currentPage != -1) {
                pdfView.loadPdfWithPage(currentPath, currentPage);
            }
        } else {
            Bundle arguments = getArguments();
            String path = arguments.getString("path");
            if (path != null) pdfView.loadPdf(path);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pdfView.getCurrentPage() != null) {
            outState.putString("currentPath", pdfView.getCurrentPath());
            outState.putInt("currentPage", pdfView.getCurrentPage());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        pdfView.onDestroy();
    }
}
