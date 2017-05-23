package com.github.wbinarytree.sample;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.io.File;

public class MainActivity extends AppCompatActivity {

    private File[] files;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)) {

            } else {
                ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.READ_CONTACTS }, 10);
            }
        }


        File topDirectory =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        files = topDirectory.listFiles();
        ArrayAdapter<File> arrayAdapter =
            new ArrayAdapter<File>(this, android.R.layout.simple_list_item_1, files) {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView,
                    @NonNull ViewGroup parent) {
                    TextView view = (TextView) super.getView(position, convertView, parent);
                    final File item = getItem(position);
                    assert item != null;
                    String path = item.getAbsolutePath();
                    view.setText(path);
                    return view;
                }
            };

        AlertDialog.Builder builder = new AlertDialog.Builder(this).setAdapter(arrayAdapter,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String path = files[which].getPath();
                    getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_pdf, MuPdfFragment.newInstance(path))
                        .commit();
                    dialog.dismiss();
                }
            });
        final AlertDialog alertDialog = builder.create();
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.show();
            }
        });
    }
}
