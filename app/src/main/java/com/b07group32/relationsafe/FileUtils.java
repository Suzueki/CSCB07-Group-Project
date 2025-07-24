package com.b07group32.relationsafe;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Log;

public class FileUtils
{
    //followed stack overflow for this
    //frankly I don't fully understand why this all works
    public static String getFileName(Context context, Uri uri)
    {
        String result = null;
        if (uri.getScheme().equals("content"))
        {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst())
                {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1)
                    {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null)
        {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1)
            {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public static String getFileExtension(Context context, Uri uri)
    {
        String fileName = getFileName(context, uri);
        if (fileName != null && fileName.contains("."))
        {
            return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }

        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType != null)
        {
            if (mimeType.startsWith("image/"))
            {
                return mimeType.substring(6);
            }
            else if (mimeType.equals("application/pdf"))
            {
                return "pdf";
            }
        }
        return "unknown";
    }

    public static long getFileSize(Context context, Uri uri)
    {
        try
        {
            ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
            if (pfd != null)
            {
                long size = pfd.getStatSize();
                pfd.close();
                return size;
            }
        }
        catch (Exception e)
        {
            Log.e("FileUtils", "Error getting file size", e);
        }
        return 0;
    }

    public static String getMimeType(Context context, Uri uri)
    {
        try
        {
            return context.getContentResolver().getType(uri);
        }
        catch (Exception e)
        {
            Log.e("FileUtils", "Error getting MIME type", e);
            return "application/octet-stream"; // Default binary type
        }
    }
}