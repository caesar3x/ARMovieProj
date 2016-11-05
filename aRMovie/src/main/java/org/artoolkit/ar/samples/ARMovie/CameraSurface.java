/*
 *  CameraSurface.java
 *  ARToolKit5
 *
 *  Disclaimer: IMPORTANT:  This Daqri software is supplied to you by Daqri
 *  LLC ("Daqri") in consideration of your agreement to the following
 *  terms, and your use, installation, modification or redistribution of
 *  this Daqri software constitutes acceptance of these terms.  If you do
 *  not agree with these terms, please do not use, install, modify or
 *  redistribute this Daqri software.
 *
 *  In consideration of your agreement to abide by the following terms, and
 *  subject to these terms, Daqri grants you a personal, non-exclusive
 *  license, under Daqri's copyrights in this original Daqri software (the
 *  "Daqri Software"), to use, reproduce, modify and redistribute the Daqri
 *  Software, with or without modifications, in source and/or binary forms;
 *  provided that if you redistribute the Daqri Software in its entirety and
 *  without modifications, you must retain this notice and the following
 *  text and disclaimers in all such redistributions of the Daqri Software.
 *  Neither the name, trademarks, service marks or logos of Daqri LLC may
 *  be used to endorse or promote products derived from the Daqri Software
 *  without specific prior written permission from Daqri.  Except as
 *  expressly stated in this notice, no other rights or licenses, express or
 *  implied, are granted by Daqri herein, including but not limited to any
 *  patent rights that may be infringed by your derivative works or by other
 *  works in which the Daqri Software may be incorporated.
 *
 *  The Daqri Software is provided by Daqri on an "AS IS" basis.  DAQRI
 *  MAKES NO WARRANTIES, EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
 *  THE IMPLIED WARRANTIES OF NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS
 *  FOR A PARTICULAR PURPOSE, REGARDING THE DAQRI SOFTWARE OR ITS USE AND
 *  OPERATION ALONE OR IN COMBINATION WITH YOUR PRODUCTS.
 *
 *  IN NO EVENT SHALL DAQRI BE LIABLE FOR ANY SPECIAL, INDIRECT, INCIDENTAL
 *  OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) ARISING IN ANY WAY OUT OF THE USE, REPRODUCTION,
 *  MODIFICATION AND/OR DISTRIBUTION OF THE DAQRI SOFTWARE, HOWEVER CAUSED
 *  AND WHETHER UNDER THEORY OF CONTRACT, TORT (INCLUDING NEGLIGENCE),
 *  STRICT LIABILITY OR OTHERWISE, EVEN IF DAQRI HAS BEEN ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 *
 *  Copyright 2015 Daqri, LLC.
 *  Copyright 2011-2015 ARToolworks, Inc.
 *
 *  Author(s): Julian Looser, Philip Lamb
 *
 */

package org.artoolkit.ar.samples.ARMovie;

import java.io.IOException;
import java.util.List;

import org.artoolkit.ar.samples.ARMovie.R;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.Toast;

public class CameraSurface extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

	private static final String TAG = "CameraSurface";
	private Camera camera;

	private int mSurfaceChangedCallDepth = 0;
	protected boolean mSurfaceConfiguring = false;
	private static final String LOG_TAG = "CameraSurface";
	private static boolean DEBUGGING = true;
	protected List<Camera.Size> mPreviewSizeList;
	protected List<Camera.Size> mPictureSizeList;
	protected Camera.Size mPreviewSize;
	protected Camera.Size mPictureSize;

	private static final String CAMERA_PARAM_ORIENTATION = "orientation";
	private static final String CAMERA_PARAM_LANDSCAPE = "landscape";
	private static final String CAMERA_PARAM_PORTRAIT = "portrait";
	private int mCenterPosX = -1;
	private int mCenterPosY;
	protected Activity mActivity;
	private boolean safeToTakePicture = false;
	PreviewReadyCallback mPreviewReadyCallback = null;

	private LayoutMode mLayoutMode;

	public static enum LayoutMode {
		FitToParent, // Scale to the size that no side is larger than the parent
		NoBlank // Scale to the size that no side is smaller than the parent
	};
	public interface PreviewReadyCallback {
		public void onPreviewReady();
	}
	
    @SuppressWarnings("deprecation")
	public CameraSurface(Context context) {
        
    	super(context);
		mActivity = (Activity) context;
    	SurfaceHolder holder = getHolder();     
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // Deprecated in API level 11. Still required for API levels <= 10.
        
    }
 
    // SurfaceHolder.Callback methods

    @SuppressLint("NewApi")
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        
		Log.i(TAG, "Opening camera.");
    	try {
    		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
    			int cameraIndex = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("pref_cameraIndex", "0"));
    			camera = Camera.open(cameraIndex);
    		} else {
    			camera = Camera.open();
    		}

			Camera.Parameters cameraParams = camera.getParameters();
			mPreviewSizeList = cameraParams.getSupportedPreviewSizes();
			mPictureSizeList = cameraParams.getSupportedPictureSizes();

    	} catch (RuntimeException exception) {
    		Log.e(TAG, "Cannot open camera. It may be in use by another process.");
    	}
    	if (camera != null) {
    		try {
        	
    			camera.setPreviewDisplay(holder);
    			//camera.setPreviewCallback(this);
    			camera.setPreviewCallbackWithBuffer(this); // API level 8 (Android 2.2)
       	
    		} catch (IOException exception) {
        		Log.e(TAG, "Cannot set camera preview display.");
        		camera.release();
        		camera = null;  
    		}
    	}
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    	if (camera != null) {  	
    		Log.i(TAG, "Closing camera.");
    		camera.stopPreview();
    		camera.setPreviewCallback(null);
    		camera.release();
    		camera = null;
    	}
    }

 
    @SuppressLint("NewApi") // CameraInfo
	@SuppressWarnings("deprecation") // setPreviewFrameRate
	@Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
    	
    	if (camera != null) {

    		String camResolution = PreferenceManager.getDefaultSharedPreferences(getContext()).getString("pref_cameraResolution", getResources().getString(R.string.pref_defaultValue_cameraResolution));
            String[] dims = camResolution.split("x", 2);
            Camera.Parameters parameters = camera.getParameters();
            parameters.setPreviewSize(Integer.parseInt(dims[0]), Integer.parseInt(dims[1]));
            //parameters.setPreviewFrameRate(30);
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            camera.setParameters(parameters);        
            
            parameters = camera.getParameters();
    		int capWidth = parameters.getPreviewSize().width;
    		int capHeight = parameters.getPreviewSize().height;
            int pixelformat = parameters.getPreviewFormat(); // android.graphics.imageformat
            PixelFormat pixelinfo = new PixelFormat();
            PixelFormat.getPixelFormatInfo(pixelformat, pixelinfo);
            int cameraIndex = 0;
            boolean frontFacing = false;
    		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
    			Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
    			cameraIndex = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("pref_cameraIndex", "0"));
    			Camera.getCameraInfo(cameraIndex, cameraInfo);
    			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) frontFacing = true;
    		}

    		int bufSize = capWidth * capHeight * pixelinfo.bitsPerPixel / 8; // For the default NV21 format, bitsPerPixel = 12.
            
            for (int i = 0; i < 5; i++) camera.addCallbackBuffer(new byte[bufSize]);
            
            camera.startPreview();

            ARMovieActivity.nativeVideoInit(capWidth, capHeight, cameraIndex, frontFacing);

    	}
    }

    // Camera.PreviewCallback methods.
    
	@Override
	public void onPreviewFrame(byte[] data, Camera cam) {
		
		ARMovieActivity.nativeVideoFrame(data);
		
		cam.addCallbackBuffer(data);
	}
 	public Camera getCamera(){
		return camera;
	}

	private void doSurfaceChanged(int width, int height) {
		if(camera == null){
			camera = Camera.open();
		}
		camera.stopPreview();

		Camera.Parameters cameraParams = camera.getParameters();
		boolean portrait = isPortrait();

		// The code in this if-statement is prevented from executed again when surfaceChanged is
		// called again due to the change of the layout size in this if-statement.
		if (!mSurfaceConfiguring) {
			Camera.Size previewSize = determinePreviewSize(portrait, width, height);
			Camera.Size pictureSize = determinePictureSize(previewSize);
			if (DEBUGGING) { Log.v(LOG_TAG, "Desired Preview Size - w: " + width + ", h: " + height); }
			mPreviewSize = previewSize;
			mPictureSize = pictureSize;
			mSurfaceConfiguring = adjustSurfaceLayoutSize(previewSize, portrait, width, height);
			// Continue executing this method if this method is called recursively.
			// Recursive call of surfaceChanged is very special case, which is a path from
			// the catch clause at the end of this method.
			// The later part of this method should be executed as well in the recursive
			// invocation of this method, because the layout change made in this recursive
			// call will not trigger another invocation of this method.
			if (mSurfaceConfiguring && (mSurfaceChangedCallDepth <= 1)) {
				return;
			}
		}

		configureCameraParameters(cameraParams, portrait);
		mSurfaceConfiguring = false;

		try {
			camera.startPreview();
			safeToTakePicture = true;
		} catch (Exception e) {
			Log.w(LOG_TAG, "Failed to start preview: " + e.getMessage());

			// Remove failed size
			mPreviewSizeList.remove(mPreviewSize);
			mPreviewSize = null;

			// Reconfigure
			if (mPreviewSizeList.size() > 0) { // prevent infinite loop
				surfaceChanged(null, 0, width, height);
			} else {
				Toast.makeText(mActivity, "Can't start preview", Toast.LENGTH_LONG).show();
				Log.w(LOG_TAG, "Gave up starting preview");
			}
		}

		if (null != mPreviewReadyCallback) {
			mPreviewReadyCallback.onPreviewReady();
		}
	}
	public boolean isPortrait() {
		return (mActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
	}
	/**
	 * @param cameraParams
	 * @param portrait
	 * @param reqWidth must be the value of the parameter passed in surfaceChanged
	 * @param reqHeight must be the value of the parameter passed in surfaceChanged
	 * @return Camera.Size object that is an element of the list returned from Camera.Parameters.getSupportedPreviewSizes.
	 */
	protected Camera.Size determinePreviewSize(boolean portrait, int reqWidth, int reqHeight) {
		// Meaning of width and height is switched for preview when portrait,
		// while it is the same as user's view for surface and metrics.
		// That is, width must always be larger than height for setPreviewSize.
		int reqPreviewWidth; // requested width in terms of camera hardware
		int reqPreviewHeight; // requested height in terms of camera hardware
		if (portrait) {
			reqPreviewWidth = reqHeight;
			reqPreviewHeight = reqWidth;
		} else {
			reqPreviewWidth = reqWidth;
			reqPreviewHeight = reqHeight;
		}

		if (DEBUGGING) {
			Log.v(LOG_TAG, "Listing all supported preview sizes");
			for (Camera.Size size : mPreviewSizeList) {
				Log.v(LOG_TAG, "  w: " + size.width + ", h: " + size.height);
			}
			Log.v(LOG_TAG, "Listing all supported picture sizes");
			for (Camera.Size size : mPictureSizeList) {
				Log.v(LOG_TAG, "  w: " + size.width + ", h: " + size.height);
			}
		}

		// Adjust surface size with the closest aspect-ratio
		float reqRatio = ((float) reqPreviewWidth) / reqPreviewHeight;
		float curRatio, deltaRatio;
		float deltaRatioMin = Float.MAX_VALUE;
		Camera.Size retSize = null;
		for (Camera.Size size : mPreviewSizeList) {
			curRatio = ((float) size.width) / size.height;
			deltaRatio = Math.abs(reqRatio - curRatio);
			if (deltaRatio < deltaRatioMin) {
				deltaRatioMin = deltaRatio;
				retSize = size;
			}
		}

		return retSize;
	}

	protected Camera.Size determinePictureSize(Camera.Size previewSize) {
		Camera.Size retSize = null;
		for (Camera.Size size : mPictureSizeList) {
			if (size.equals(previewSize)) {
				return size;
			}
		}

		if (DEBUGGING) { Log.v(LOG_TAG, "Same picture size not found."); }

		// if the preview size is not supported as a picture size
		float reqRatio = ((float) previewSize.width) / previewSize.height;
		float curRatio, deltaRatio;
		float deltaRatioMin = Float.MAX_VALUE;
		for (Camera.Size size : mPictureSizeList) {
			curRatio = ((float) size.width) / size.height;
			deltaRatio = Math.abs(reqRatio - curRatio);
			if (deltaRatio < deltaRatioMin) {
				deltaRatioMin = deltaRatio;
				retSize = size;
			}
		}

		return retSize;
	}

	protected boolean adjustSurfaceLayoutSize(Camera.Size previewSize, boolean portrait,
											  int availableWidth, int availableHeight) {
		float tmpLayoutHeight, tmpLayoutWidth;
		if (portrait) {
			tmpLayoutHeight = previewSize.width;
			tmpLayoutWidth = previewSize.height;
		} else {
			tmpLayoutHeight = previewSize.height;
			tmpLayoutWidth = previewSize.width;
		}

		float factH, factW, fact;
		factH = availableHeight / tmpLayoutHeight;
		factW = availableWidth / tmpLayoutWidth;
		if (mLayoutMode == LayoutMode.FitToParent) {
			// Select smaller factor, because the surface cannot be set to the size larger than display metrics.
			if (factH < factW) {
				fact = factH;
			} else {
				fact = factW;
			}
		} else {
			if (factH < factW) {
				fact = factW;
			} else {
				fact = factH;
			}
		}

		FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams)this.getLayoutParams();

		int layoutHeight = (int) (tmpLayoutHeight * fact);
		int layoutWidth = (int) (tmpLayoutWidth * fact);
		if (DEBUGGING) {
			Log.v(LOG_TAG, "Preview Layout Size - w: " + layoutWidth + ", h: " + layoutHeight);
			Log.v(LOG_TAG, "Scale factor: " + fact);
		}

		boolean layoutChanged;
		if ((layoutWidth != this.getWidth()) || (layoutHeight != this.getHeight())) {
			layoutParams.height = layoutHeight;
			layoutParams.width = layoutWidth;
			if (mCenterPosX >= 0) {
				layoutParams.topMargin = mCenterPosY - (layoutHeight / 2);
				layoutParams.leftMargin = mCenterPosX - (layoutWidth / 2);
			}
			this.setLayoutParams(layoutParams); // this will trigger another surfaceChanged invocation.
			layoutChanged = true;
		} else {
			layoutChanged = false;
		}

		return layoutChanged;
	}

	/**
	 * @param x X coordinate of center position on the screen. Set to negative value to unset.
	 * @param y Y coordinate of center position on the screen.
	 */
	public void setCenterPosition(int x, int y) {
		mCenterPosX = x;
		mCenterPosY = y;
	}

	protected void configureCameraParameters(Camera.Parameters cameraParams, boolean portrait) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) { // for 2.1 and before
			if (portrait) {
				cameraParams.set(CAMERA_PARAM_ORIENTATION, CAMERA_PARAM_PORTRAIT);
			} else {
				cameraParams.set(CAMERA_PARAM_ORIENTATION, CAMERA_PARAM_LANDSCAPE);
			}
		} else { // for 2.2 and later
			int angle;
			Display display = mActivity.getWindowManager().getDefaultDisplay();
			switch (display.getRotation()) {
				case Surface.ROTATION_0: // This is display orientation
					angle = 90; // This is camera orientation
					break;
				case Surface.ROTATION_90:
					angle = 0;
					break;
				case Surface.ROTATION_180:
					angle = 270;
					break;
				case Surface.ROTATION_270:
					angle = 180;
					break;
				default:
					angle = 90;
					break;
			}
			Log.v(LOG_TAG, "angle: " + angle);
			camera.setDisplayOrientation(angle);
		}

		cameraParams.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
		cameraParams.setPictureSize(mPictureSize.width, mPictureSize.height);
		if (DEBUGGING) {
			Log.v(LOG_TAG, "Preview Actual Size - w: " + mPreviewSize.width + ", h: " + mPreviewSize.height);
			Log.v(LOG_TAG, "Picture Actual Size - w: " + mPictureSize.width + ", h: " + mPictureSize.height);
		}
		cameraParams.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
		camera.setParameters(cameraParams);
	}
}
