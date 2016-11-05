package org.artoolkit.ar.samples.ARMovie;

import android.content.Context;
import android.hardware.Camera;
import android.widget.Toast;

/**
 * Created by TUANWM on 8/29/2016.
 */
public class CameraManager {
    private Camera mCamera;
    private Context mContext;

    public CameraManager(Context context){
        this.mContext = context;
        this.mCamera = this.getCameraInstance();
    }
    public Camera getCamera(){
        return this.mCamera;
    }
    private void releaseCamera(){
        if(this.mCamera != null){
            this.mCamera.release();
            this.mCamera = null;
        }
    }
    public void onPause(){
        this.releaseCamera();
    }
    public void onResume(){
        if(this.mCamera == null){
            this.mCamera = this.getCameraInstance();
        }
        Toast.makeText(this.mContext, "preview size = " + this.mCamera.getParameters().getPreviewSize().width + ", " + this.mCamera.getParameters().getPreviewSize().height, Toast.LENGTH_LONG).show();
    }
    private static Camera getCameraInstance(){
        Camera c = null;
        try{
            c = Camera.open();
        }catch (Exception e){
            e.printStackTrace();
        }
        return c;
    }
}
