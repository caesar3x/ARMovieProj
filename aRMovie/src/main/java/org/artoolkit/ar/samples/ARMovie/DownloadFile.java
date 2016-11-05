package org.artoolkit.ar.samples.ARMovie;

/**
 * Created by TUANWM on 9/2/2016.
 */

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class DownloadFile {

    static InputStream is = null;
    static JSONObject jObj = null;
    static String json = "";


    public DownloadFile() {

    }

    public Boolean downloadFromUrl(String url1, String outputFile, List<NameValuePair> params) {
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(url1);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return Boolean.FALSE;
            }

            // this will be useful to display download percentage
            // might be -1: server did not report the length
            int fileLength = connection.getContentLength();

            // download the file
            input = connection.getInputStream();
            output = new FileOutputStream(outputFile);

            byte data[] = new byte[4096];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                total += count;
                output.write(data, 0, count);
            }
        } catch (Exception e) {
            return Boolean.FALSE;
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException ignored) {
            }

            if (connection != null)
                connection.disconnect();
        }
        return Boolean.TRUE;

    }
    Boolean result;
    public Boolean download(String url, String outputFile, List<NameValuePair> params) {

        Params param = new Params(url, outputFile,params);
        Downloader myTask = new Downloader();
        try{
            result= myTask.execute(param).get();
        }catch (InterruptedException e) {
            e.printStackTrace();
        }catch (ExecutionException e){
            Log.i("Json2","Error!");
            e.printStackTrace();
        }
        return result;
    }


    private static class Params {
        String url;
        String outputFile;
        List<NameValuePair> params;


        Params(String url, String outputFile, List<NameValuePair> params) {
            this.url = url;
            this.outputFile = outputFile;
            this.params = params;

        }
    }

    private class Downloader extends AsyncTask<Params, String, Boolean> {

        @Override
        protected Boolean doInBackground(Params... args) {

            DownloadFile request = new DownloadFile();
            Boolean result = request.downloadFromUrl(args[0].url, args[0].outputFile,args[0].params);

            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {

            super.onPostExecute(result);

        }

    }
}
