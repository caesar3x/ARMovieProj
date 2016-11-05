/*
 *  ARMovieActivity.java
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.artoolkit.ar.base.ARToolKit;
import org.artoolkit.ar.base.NativeInterface;
import org.artoolkit.ar.base.camera.CameraPreferencesActivity;
import org.artoolkit.ar.samples.ARMovie.R;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

public class ARMovieActivity extends Activity {

    private static final String TAG = "ARMovieActivity";

    private static final String movieFile = "Data/sample.mp4";

    // Load the native libraries.
    static {
    	System.loadLibrary("c++_shared");
    	System.loadLibrary("ARMovieNative");
    }

	// Lifecycle functions.
    public static native boolean nativeCreate(Context ctx);
    public static native boolean nativeStart();
    public static native boolean nativeStop();
    public static native boolean nativeDestroy();
    // Camera functions.
    public static native boolean nativeVideoInit(int w, int h, int cameraIndex, boolean cameraIsFrontFacing);
    public static native void nativeVideoFrame(byte[] image);
    // OpenGL functions.
    public static native void nativeSurfaceCreated();
    public static native void nativeSurfaceChanged(int w, int h);
    public static native void nativeDrawFrame(int movieWidth, int movieHeight, int movieTextureID, float[] movieTextureMtx);
    // Movie texture functions.
    public static native void nativeMovieInit(Object movieRendererThis, Object movieRendererWeakThis);
    public static native void nativeMovieFinal();
    // Other functions.
    public static native void nativeDisplayParametersChanged(int orientation, int w, int h, int dpi); // 0 = portrait, 1 = landscape (device rotated 90 degrees ccw), 2 = portrait upside down, 3 = landscape reverse (device rotated 90 degrees cw).
    public static native void nativeSetInternetState(int state);

	private GLSurfaceView glView;

	private CameraSurface camSurface;

	private FrameLayout mainLayout;

	private MovieController movieController;
	private boolean movieControllerPausedByUs;

	private Button mButton;
	List<NameValuePair> params;
	private String videoStr;
	private static  final int FOCUS_AREA_SIZE= 300;

	/** Called when the activity is first created. */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(TAG, "onCreate()");
		super.onCreate(savedInstanceState);

		boolean needActionBar = false;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				if (!ViewConfiguration.get(this).hasPermanentMenuKey()) needActionBar = true;
			} else {
				needActionBar = true;
			}
        }
		if (needActionBar) {
			requestWindowFeature(Window.FEATURE_ACTION_BAR);
		} else {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); // Force landscape-only.
        updateNativeDisplayParameters();

		//Bundle extras = getIntent().getExtras();
		//String baseFolderPath = extras.getString("baseFolderPath");

		//int markerID = ARToolKit.getInstance().addMarker("multi;"+baseFolderPath +"/marker.dat");

        setContentView(R.layout.main_video);
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

				camSurface.getCamera().takePicture(null, null, mPicture);;
			}
		});



		ARMovieActivity.nativeCreate(this);
    }

    @Override
    public void onStart()
    {
    	Log.i(TAG, "onStart()");
   	    super.onStart();

    	mainLayout = (FrameLayout)this.findViewById(R.id.mainLayout);

		mainLayout.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					//focusOnTouch(event);
				}
				return true;
			}
		});

//		Bundle extras = getIntent().getExtras();
//		String videoUrl = extras.getString("videoUrl");
//		Log.i(TAG, videoUrl);
        try {
			movieController = new MovieController(this.getCacheDir() + "/" + movieFile);
			//movieController = new MovieController("http://133.130.111.186/pilot/uploads/p1270436.MP4");

			//movieController = new MovieController(videoUrl);

	        movieController.mLoop = true;
	        movieController.mStartPaused = true;
		} catch (IOException e) {
        	Log.e(TAG, "Unable to create movie controller.");
			movieController = null;
		}

		ARMovieActivity.nativeStart();
    }

    @SuppressWarnings("deprecation") // FILL_PARENT still required for API level 7 (Android 2.1)
    @Override
    public void onResume() {
    	Log.i(TAG, "onResume()");
    	super.onResume();

    	// Update info on whether we have an Internet connection.
    	ConnectivityManager cm = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    	boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    	nativeSetInternetState(isConnected ? 1 : 0);

   		// In order to ensure that the GL surface covers the camera preview each time onStart
   		// is called, remove and add both back into the FrameLayout.
   		// Removing GLSurfaceView also appears to cause the GL surface to be disposed of.
   		// To work around this, we also recreate GLSurfaceView. This is not a lot of extra
   		// work, since Android has already destroyed the OpenGL context too, requiring us to
   		// recreate that and reload textures etc.

		// Create the camera view.
		camSurface = new CameraSurface(this);
		camSurface.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					focusOnTouch(event);
				}
				return true;
			}
		});

		// Create/recreate the GL view.
	    glView = new GLSurfaceView(this);
		//glView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Do we actually need a transparent surface? I think not, (default is RGB888 with depth=16) and anyway, Android 2.2 barfs on this.
		Renderer r = new Renderer();
		r.setMovieController(movieController);
		glView.setRenderer(r);
		glView.setZOrderMediaOverlay(true); // Request that GL view's SurfaceView be on top of other SurfaceViews (including CameraPreview's SurfaceView).

        mainLayout.addView(camSurface, new LayoutParams(128, 128));
 		mainLayout.addView(glView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

 		if (glView != null) glView.onResume();

	    // Resume movieController after resuming glView.
	    if (movieController != null) movieController.onResume(glView);
}

	@Override
	protected void onPause() {
    	Log.i(TAG, "onPause()");
	    super.onPause();

	    // Pause movieController before pausing glView.
	    if (movieController != null) movieController.onPause(glView);

	    if (glView != null) glView.onPause();

	    // System hardware must be release in onPause(), so it's available to
	    // any incoming activity. Removing the CameraPreview will do this for the
	    // camera. Also do it for the GLSurfaceView, since it serves no purpose
	    // with the camera preview gone.
	    mainLayout.removeView(glView);
	    mainLayout.removeView(camSurface);
	}

	@Override
	public void onStop() {
    	Log.i(TAG, "onStop()");
		super.onStop();

	    if (movieController != null) {
	    	movieController.finish();
	    	movieController = null;
	    }

		ARMovieActivity.nativeStop();
	}

    @Override
    public void onDestroy()
    {
    	Log.i(TAG, "onDestroy()");
   	    super.onDestroy();

		ARMovieActivity.nativeDestroy();
    }

    private void updateNativeDisplayParameters()
    {
    	Display d = getWindowManager().getDefaultDisplay();
    	int orientation = d.getRotation();
    	DisplayMetrics dm = new DisplayMetrics();
    	d.getMetrics(dm);
    	int w = dm.widthPixels;
    	int h = dm.heightPixels;
    	int dpi = dm.densityDpi;
        nativeDisplayParametersChanged(orientation, w, h, dpi);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
    	super.onConfigurationChanged(newConfig);

    	// We won't use the orientation from the config, as it only tells us the layout type
    	// and not the actual orientation angle.
        //int nativeOrientation;
        //int orientation = newConfig.orientation; // Only portrait or landscape.
    	//if (orientation == Configuration.ORIENTATION_LANSCAPE) nativeOrientation = 0;
        //else /* orientation == Configuration.ORIENTATION_PORTRAIT) */ nativeOrientation = 1;
    	updateNativeDisplayParameters();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.options, menu);
	    return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    if (item.getItemId() == R.id.settings) {
			startActivity(new Intent(this, CameraPreferencesActivity.class));
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}
	Camera.PictureCallback mPicture = new Camera.PictureCallback() {
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			camSurface.getCamera().startPreview();

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

							String imageName = resultStr.substring(0, resultStr.indexOf("|") - 4);
							videoStr = "http://133.130.111.186/pilot/uploads/" + resultStr.substring(resultStr.indexOf("|") + 1, resultStr.length());

							if(resultStr != null && resultStr.length() > 0){
								File baseFolder = createDataFolders();
								if(baseFolder != null){
									String cacheFolderPath = getBaseContext().getCacheDir().getAbsolutePath();
                                    //File markersFile = new File(baseFolder.getPath() + File.separator + "Data" + File.separator + "markers.dat");
									File markersFile = new File(cacheFolderPath + File.separator + "Data" + File.separator + "markers.dat");

                                    PrintWriter markersPrintWriter = new PrintWriter(markersFile);
                                    markersPrintWriter.println("1");
                                    markersPrintWriter.println("");
									markersPrintWriter.println("");
                                    markersPrintWriter.println("../DataNFT/" + imageName);
                                    markersPrintWriter.println("NFT");
                                    markersPrintWriter.println("FILTER 15.0");

                                    markersPrintWriter.flush();
                                    markersPrintWriter.close();
									List<NameValuePair> downloadParams = new ArrayList<NameValuePair>();

									DownloadFile downloadFile = new DownloadFile();
									/*
									Boolean resultFset = downloadFile.download("http://133.130.111.186/pilot/uploads/" + imageName + ".fset", baseFolder.getPath() + File.separator + "DataNFT" + File.separator +
											imageName +".fset",downloadParams);
									Boolean resultFset3 = downloadFile.download("http://133.130.111.186/pilot/uploads/" + imageName + ".fset3", baseFolder.getPath() + File.separator + "DataNFT" + File.separator +
											imageName +".fset3",downloadParams);

									Boolean resultIset = downloadFile.download("http://133.130.111.186/pilot/uploads/" + imageName + ".iset", baseFolder.getPath() + File.separator + "DataNFT" + File.separator +
											imageName +".iset",downloadParams);


									File mediaStorageDir = new File(getBaseContext().getCacheDir().getAbsolutePath() +"/Data/markers.dat");
									if (mediaStorageDir.exists()){
										Log.i("File existed: ", "True");
									}else{
										Log.i("File existed: ", "False");
									}

									ARToolKit.getInstance().setDebugMode(true);
									NativeInterface.arwRemoveAllMarkers();
									//NativeInterface.arwChangeToResourcesDir(baseFolder.getPath());

									String configMarkerString = "multi;"+baseFolder.getPath() +"/Data/markers.dat";

									*/

									Boolean resultFset = downloadFile.download("http://133.130.111.186/pilot/uploads/" + imageName + ".fset", cacheFolderPath + File.separator + "DataNFT" + File.separator +
											imageName +".fset",downloadParams);
									Boolean resultFset3 = downloadFile.download("http://133.130.111.186/pilot/uploads/" + imageName + ".fset3", cacheFolderPath + File.separator + "DataNFT" + File.separator +
											imageName +".fset3",downloadParams);

									Boolean resultIset = downloadFile.download("http://133.130.111.186/pilot/uploads/" + imageName + ".iset", cacheFolderPath + File.separator + "DataNFT" + File.separator +
											imageName +".iset",downloadParams);

									ARToolKit.getInstance().setDebugMode(true);
									NativeInterface.arwRemoveAllMarkers();
									//NativeInterface.arwChangeToResourcesDir(baseFolder.getPath());

									//String configMarkerString = "multi;"+baseFolder.getPath() +"/Data/markers.dat";
									String configMarkerString = "multi;"+ cacheFolderPath +"/Data/markers.dat";

									File mediaStorageDir = new File(getBaseContext().getCacheDir().getAbsolutePath() +"/DataNFT/student1.iset");
									if (mediaStorageDir.exists()){
										Log.i("File existed: ", "True");
									}else{
										Log.i("File existed: ", "False");
									}

									Log.i("configMarkerString: ", configMarkerString);
									int markerID = ARToolKit.getInstance().addMarker(configMarkerString);
									Log.i("markerID: ", Integer.toString(markerID));

									Log.i("Video: ", videoStr);
									Log.i("getCacheDir: ", getBaseContext().getCacheDir().getAbsolutePath());
									ARMovieActivity.nativeCreate(getBaseContext());

									movieController = new MovieController(getBaseContext().getCacheDir() + "/" + movieFile);
									movieController.updateTexture();
									//ARMovieActivity.nativeDrawFrame(movieController.mMovieWidth, movieController.mMovieHeight, movieController.mGLTextureID, movieController.mGLTextureMtx);

									movieController = new MovieController(videoStr);
									/*
									movieController.mLoop = true;
									movieController.mStartPaused = true;
									*/
								}
							}else{
								Toast.makeText(getApplication(), "No result found!", Toast.LENGTH_LONG).show();
							}

						}
					}catch (JSONException e) {
						e.printStackTrace();
					}
				}else{
					Toast.makeText(ARMovieActivity.this, "Can't connect to server", Toast.LENGTH_LONG).show();
				}
/*
            } catch (FileNotFoundException e) {
                e.printStackTrace();
*/
			} catch (IOException e) {
				Toast.makeText(ARMovieActivity.this, "Can't connect to server", Toast.LENGTH_LONG).show();
				e.printStackTrace();
			}
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

	private void focusOnTouch(MotionEvent event) {
		Log.i("TAG","Touched !");
		if (camSurface.getCamera() != null ) {

			Camera.Parameters parameters = camSurface.getCamera().getParameters();
			if (parameters.getMaxNumMeteringAreas() > 0){
				Log.i("TAG","fancy !");
				Rect rect = calculateFocusArea(event.getX(), event.getY());

				parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
				List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
				meteringAreas.add(new Camera.Area(rect, 800));
				parameters.setFocusAreas(meteringAreas);

				camSurface.getCamera().setParameters(parameters);
				camSurface.getCamera().autoFocus(mAutoFocusTakePictureCallback);
			}else {
				camSurface.getCamera().autoFocus(mAutoFocusTakePictureCallback);
			}
		}
	}

	private Rect calculateFocusArea(float x, float y) {
		int left = clamp(Float.valueOf((x / camSurface.getWidth()) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);
		int top = clamp(Float.valueOf((y / camSurface.getHeight()) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);

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
}