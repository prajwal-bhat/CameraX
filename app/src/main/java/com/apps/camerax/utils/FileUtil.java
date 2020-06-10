package com.apps.camerax.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class FileUtil {
    private static final String TAG = "FileUtil";


    private static final String mimeType = "image/jpeg";
    private static final String FOLDER_NAME = "CamVault";
    private static final int IMAGE_QUALITY = 100;
    private static final String IMAGE_NAME = "IMG_";
    private static final String IMAGE_EXTENSION = ".jpg";
    private static final String FOLDER_PATH = Environment.DIRECTORY_PICTURES + File.separator+ FOLDER_NAME;
    private static final String HIDDEN_FOLDER = "my_files";

    ArrayList<String> filePaths = new ArrayList<>();// list of file paths


    public static void saveBitmap(Context context, Bitmap bitmap) throws IOException {
        OutputStream fos;
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = IMAGE_NAME + timeStamp + IMAGE_EXTENSION;


        //Store in the regular media directory
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, imageFileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, FOLDER_PATH);
            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            fos = resolver.openOutputStream(Objects.requireNonNull(imageUri));
        }else {
            String imagesDir = Environment.getExternalStoragePublicDirectory(FOLDER_PATH).toString();
            File image = new File(imagesDir, imageFileName);
            fos = new FileOutputStream(image);
        }
        bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, fos);
        Objects.requireNonNull(fos).close();
    }


    public void getFromSdcard(Context context) {
        Log.d(TAG, "getFromSdcard: ");
        File hiddenFile = new File(String.valueOf(context.getExternalFilesDir(HIDDEN_FOLDER)));

        File[] listFile;
        if (hiddenFile.isDirectory()) {
            Log.d(TAG, "getFromSdcard: dir ");
            listFile = hiddenFile.listFiles();
            for (int i = 0; i < listFile.length; i++) {

                filePaths.add(listFile[i].getAbsolutePath());
            }
        }
        Log.d(TAG, "no of files: "+ filePaths.size());
        for(String path: filePaths){
            System.out.println(path);
        }
    }
}
