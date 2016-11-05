package org.artoolkit.ar.samples.ARMovie;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by TUANWM on 9/3/2016.
 */
public class Util {
    public static String convertToBase64(String imagePath)

    {

        Bitmap bm = BitmapFactory.decodeFile(imagePath);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);

        byte[] byteArrayImage = baos.toByteArray();

        String encodedImage = Base64.encodeToString(byteArrayImage, Base64.DEFAULT);

        return encodedImage;

    }
    public static void rotateImage(String path){
        File file = new File(path);
        ExifInterface exifInterface = null;
        try {
            exifInterface = new ExifInterface(file.getPath());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        if ( (orientation == ExifInterface.ORIENTATION_NORMAL) | (orientation == 0) ) {
            exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, ""+ ExifInterface.ORIENTATION_ROTATE_90);
        } else if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
            exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, ""+ ExifInterface.ORIENTATION_ROTATE_180);
        } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
            exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, ""+ ExifInterface.ORIENTATION_ROTATE_270);
        } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
            exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, ""+ ExifInterface.ORIENTATION_NORMAL);
        }
        try {
            exifInterface.saveAttributes();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    public static boolean writeFile(String url, String filePath){
        Log.i("Write file - url: ", url);
        Log.i("Write file - filePath: ", filePath);
        URL u;
        InputStream is = null;
        DataInputStream dis;
        String s;

        try{
            u = new URL(url);

            is = u.openStream();

            dis = new DataInputStream(new BufferedInputStream(is));

            //File targetFile = new File(filePath);
            //PrintWriter fileWriter = new PrintWriter(targetFile);
            while ((s = dis.readLine()) != null) {
                System.out.println(s);
                //fileWriter.println(s);
            }
            //fileWriter.flush();
            //fileWriter.close();
            return true;
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
            return false;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return false;
        } finally {
            try {
                if(is != null) {
                    is.close();
                }
            } catch (IOException ioe) {
                // just going to ignore this one
            }
        }

    }
}
