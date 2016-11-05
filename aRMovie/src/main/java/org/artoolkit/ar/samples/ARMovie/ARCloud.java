package org.artoolkit.ar.samples.ARMovie;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by TUANWM on 8/30/2016.
 */
public class ARCloud extends Activity {
    private CameraPreview mPreview;
    private CameraManager mCameraManager;
    private boolean mIsOn = true;
    private Button mButton;
    private String mIP;
    private int mPort = 8888;
    private int matchPoint = 0;

    private ImageView imageView;
    private Uri file;
    private String videoStr;

    List<NameValuePair> params;

    private static  final int FOCUS_AREA_SIZE= 300;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main);

        mButton = (Button)findViewById(R.id.button_capture);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(ARCloud.this, "Take picture", Toast.LENGTH_LONG).show();
                LinearLayout linearLayoutResultWrapper = (LinearLayout) findViewById(R.id.result_wrapper);
                if(linearLayoutResultWrapper.getChildCount() > 0){
                    linearLayoutResultWrapper.removeViewAt(0);
                    linearLayoutResultWrapper.removeAllViews();
                }
                if(mPreview.getSafeToTakePicture()) {
                    //mCameraManager.getCamera().takePicture(null, null, mPicture);
                    mPreview.getCamera().takePicture(null, null, mPicture);;
                    mPreview.setSafeToTakePicture(false);
                }
            }
        });

        //this.mCameraManager = new CameraManager(this);
        //this.mPreview = new CameraPreview(this, mCameraManager.getCamera());
        this.mPreview = new CameraPreview(this, 0, null);

        mPreview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    focusOnTouch(event);
                }
                return true;
            }
        });
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(this.mPreview, 0);
    }

    private void focusOnTouch(MotionEvent event) {
        if (mPreview.getCamera() != null ) {

            Camera.Parameters parameters = mPreview.getCamera().getParameters();
            if (parameters.getMaxNumMeteringAreas() > 0){
                Log.i("TAG","fancy !");
                Rect rect = calculateFocusArea(event.getX(), event.getY());

                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
                meteringAreas.add(new Camera.Area(rect, 800));
                parameters.setFocusAreas(meteringAreas);

                mPreview.getCamera().setParameters(parameters);
                mPreview.getCamera().autoFocus(mAutoFocusTakePictureCallback);
            }else {
                mPreview.getCamera().autoFocus(mAutoFocusTakePictureCallback);
            }
        }
    }

    private Rect calculateFocusArea(float x, float y) {
        int left = clamp(Float.valueOf((x / mPreview.getWidth()) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);
        int top = clamp(Float.valueOf((y / mPreview.getHeight()) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);

        return new Rect(left, top, left + FOCUS_AREA_SIZE, top + FOCUS_AREA_SIZE);
    }

    private int clamp(int touchCoordinateInCameraReper, int focusAreaSize) {
        int result;
        if (Math.abs(touchCoordinateInCameraReper)+focusAreaSize/2>1000){
            if (touchCoordinateInCameraReper>0){
                result = 1000 - focusAreaSize/2;
            } else {
                result = -1000 + focusAreaSize/2;
            }
        } else{
            result = touchCoordinateInCameraReper - focusAreaSize/2;
        }
        return result;
    }
    private Camera.AutoFocusCallback mAutoFocusTakePictureCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if (success) {
                // do something...
                Log.i("tap_to_focus","success!");
            } else {
                // do something...
                Log.i("tap_to_focus","fail!");
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.arcloud, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        switch (id){
            case R.id.action_settings:
                setting();
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    private void setting(){
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.server_setting, null);
        AlertDialog dialog = new AlertDialog.Builder(ARCloud.this)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setTitle(R.string.setting_title)
                .setView(textEntryView)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        EditText ipEdit = (EditText) textEntryView.findViewById(R.id.ip_edit);
                        EditText portEdit = (EditText) textEntryView.findViewById(R.id.port_edit);
                        mIP = ipEdit.getText().toString();
                        mPort = Integer.parseInt(portEdit.getText().toString());

                        Toast.makeText(ARCloud.this, "New address: " + mIP + ":" + mPort, Toast.LENGTH_LONG);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    /* User clicked cancel so do some stuff */
                    }
                })
                .create();
        dialog.show();

    }

    @Override
    protected void onPause(){
        super.onPause();
        //closeSocketClient();
        this.mPreview.stop();
        //this.mCameraManager.onPause();
        this.reset();
    }

    private void reset(){
        this.mButton.setText("Search");
        this.mIsOn = true;
    }

    @Override
    protected void onResume(){
        super.onResume();
//        this.mCameraManager.onResume();
 //       this.mPreview.setCamera(this.mCameraManager.getCamera());
    }



    Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            //File pictureFile = getOutputMediaFile();
            mPreview.getCamera().startPreview();

            /*
            if (pictureFile == null) {
                mPreview.setSafeToTakePicture(true);
                return;
            }
            */
           try {

                Bitmap thePicture = BitmapFactory.decodeByteArray(data, 0, data.length);
                Matrix m = new Matrix();
                int rotation =  getWindowManager().getDefaultDisplay().getRotation();
                int angle = 0;
                switch (rotation) {
                    case Surface.ROTATION_90:
                        angle = -90;
                        break;
                    case Surface.ROTATION_180:
                        angle = 180;
                        break;
                    case Surface.ROTATION_270:
                        angle = 180;
                        break;
                    default:
                        angle = 90;
                        break;
                }
                    //Toast.makeText(ARCloud.this, Integer.toString(angle), Toast.LENGTH_LONG).show();
                    m.postRotate(angle);
                    thePicture = Bitmap.createBitmap(thePicture, 0, 0, thePicture.getWidth(), thePicture.getHeight(), m, true);


                //thePicture = Bitmap.createBitmap(thePicture, 0, 0, 350, thePicture.getHeight() * 350/thePicture.getWidth(), m, true);
                thePicture = thePicture.createScaledBitmap(thePicture, 350, thePicture.getHeight() * 350/thePicture.getWidth(), false);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();

                thePicture.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                data = bos.toByteArray();

                String encodedImage = Base64.encodeToString(data, Base64.DEFAULT);
                bos.flush();
                /*
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                */
                /*
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                Util.rotateImage(pictureFile.getPath());
                Toast.makeText(ARCloud.this, pictureFile.toString(), Toast.LENGTH_LONG).show();

                String height = "500";
                String width = "500";

                String encodedImage = Util.convertToBase64(pictureFile.getAbsolutePath());
                */
                String height = "500";
                String width = "500";
                params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("height", height));
                params.add(new BasicNameValuePair("width", width));
                params.add(new BasicNameValuePair("imageData", data.toString()));
                params.add(new BasicNameValuePair("encodedImage", encodedImage));


                ServerRequest sr = new ServerRequest();
                //JSONObject json = sr.getJSON("http://192.168.1.80:4321/arcloud",params);
                JSONObject json = sr.getJSON("http://133.130.111.186:6996/arcloud",params);

                if(json != null){
                    try{
                        if(!json.getBoolean("res")){
                            Toast.makeText(getApplication(), "No result found!", Toast.LENGTH_LONG).show();
                        }else {
                            String resultStr = json.getString("response");

                            //videoStr = "http://133.130.111.186/pilot/uploads/" + resultStr;

                            String imageName = resultStr.substring(0, resultStr.indexOf("|"));
                            videoStr = "http://133.130.111.186/pilot/uploads/" + resultStr.substring(resultStr.indexOf("|") + 1, resultStr.length());

                            if(resultStr != null && resultStr.length() > 0){
                                File baseFolder = createDataFolders();
                                if(baseFolder != null){
                                    /*
                                    File markersFile = new File(baseFolder.getPath() + File.separator + "Data" + File.separator +
                                            "markers.dat");
                                    PrintWriter markersPrintWriter = new PrintWriter(markersFile);
                                    markersPrintWriter.println("1");
                                    markersPrintWriter.println("");
                                    markersPrintWriter.println("../DataNFT/" + imageName);
                                    markersPrintWriter.println("NFT");
                                    markersPrintWriter.println("FILTER 15.0");

                                    markersPrintWriter.flush();
                                    markersPrintWriter.close();


                                    File fsetFile = new File(baseFolder.getPath() + File.separator + "DataNFT" + File.separator +
                                            imageName +".fset");
                                    FileWriter fsetFileWriter = new FileWriter(fsetFile);
                                    fsetFileWriter.write(json.getString("fset"));
                                    fsetFileWriter.flush();
                                    fsetFileWriter.close();

                                    File fset3File = new File(baseFolder.getPath() + File.separator + "DataNFT" + File.separator +
                                            imageName +".fset3");
                                    FileWriter fset3FileWriter = new FileWriter(fset3File);
                                    fset3FileWriter.write(json.getString("fset3"));
                                    fset3FileWriter.flush();
                                    fset3FileWriter.close();

                                    File isetFile = new File(baseFolder.getPath() + File.separator + "DataNFT" + File.separator +
                                            imageName +".iset");
                                    PrintWriter isetPrintWriter = new PrintWriter(isetFile, "UTF-8");
                                    isetPrintWriter.write(json.getString("iset"));
                                    isetPrintWriter.flush();
                                    isetPrintWriter.close();
                                    */

                                    //int markerID = ARToolKit.getInstance().addMarker("multi;"+baseFolder.getPath() +"/marker.dat");

                                    Intent aRMovieIntent = new Intent(getApplicationContext(), ARMovieActivity.class);
                                    aRMovieIntent.putExtra("videoUrl", videoStr);
                                    aRMovieIntent.putExtra("baseFolderPath", baseFolder.getPath());
                                    startActivity(aRMovieIntent);
                                }
                                /*
                                TextView txtResult = new TextView(getApplicationContext());
                                //Toast.makeText(getApplication(), videoStr, Toast.LENGTH_LONG).show();
                                txtResult.setText(videoStr.substring(0, 20));
                                txtResult.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
                                txtResult.setGravity(FrameLayout.TEXT_ALIGNMENT_GRAVITY);

                                LinearLayout linearLayoutResultWrapper = (LinearLayout) findViewById(R.id.result_wrapper);
                                linearLayoutResultWrapper.addView(txtResult);

                                Button btnPlayVideo = new Button(getApplicationContext());
                                btnPlayVideo.setText("Play");
                                btnPlayVideo.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent videoIntent = new Intent(getApplicationContext(), VideoActivity.class);

                                        videoIntent.putExtra("videoUrl", videoStr);
                                        startActivity(videoIntent);
                                    }
                                });

                                linearLayoutResultWrapper.addView(btnPlayVideo);
                                */
                            }else{
                                Toast.makeText(getApplication(), "No result found!", Toast.LENGTH_LONG).show();
                            }

                        }
                    }catch (JSONException e) {
                        e.printStackTrace();
                    }
                }else{
                    Toast.makeText(ARCloud.this, "Can't connect to server", Toast.LENGTH_LONG).show();
                }
/*
            } catch (FileNotFoundException e) {
                e.printStackTrace();
*/
            } catch (IOException e) {
                Toast.makeText(ARCloud.this, "Can't connect to server", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }

            mPreview.setSafeToTakePicture(true);
        }
    };
    private static File getOutputMediaFile(){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "ARCloud");
        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){
                Log.d("ARCloud", "failed to create directory 2");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator +
                "IMG_"+ timeStamp + ".jpg");
    }

    private static File createDataFolders(){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "ARCloud");
        boolean isFolderExisted = false;
        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){
                Log.d("ARCloud", "failed to create directory 1");
                isFolderExisted = false;
                return null;
            }else{
                isFolderExisted = true;
            }
        }else{
            isFolderExisted = true;
        }
        Log.d("ARCloud", mediaStorageDir.getPath());
        if(isFolderExisted){
            File dataDir = new File(mediaStorageDir.getPath(), "Data");
            if (!dataDir.exists()){
                if (!dataDir.mkdirs()){
                    return null;
                }
            }
            File dataNFTDir = new File(mediaStorageDir.getPath(), "DataNFT");
            if (!dataNFTDir.exists()){
                if (!dataNFTDir.mkdirs()){
                    return null;
                }
            }
        }
        return mediaStorageDir;
    }
}
