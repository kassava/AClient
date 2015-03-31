package com.shiz.model.ui;

import java.io.IOException;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.shiz.model.ModelApplication;
import com.shiz.model.R;
import com.shiz.model.track.PacketSendingService;

@SuppressWarnings("deprecation")
public class ShowTrackActvity extends ActionBarActivity {
	private final String LOG_TAG = "ShowTrackActvity";
	private final boolean DEBUG = true;
	
	private TrackSurfaceView mTrackSurfaceView;
	private Context mContext;
	
	private boolean bound = false;
	private ServiceConnection sConn;
	private Intent intent;
	private PacketSendingService psService;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mTrackSurfaceView = new TrackSurfaceView(this);
		setContentView(mTrackSurfaceView);
		mContext = this;
		
		intent = new Intent(this, PacketSendingService.class);
		sConn = new ServiceConnection() {
			
			@Override
			public void onServiceDisconnected(ComponentName name) {
				if(DEBUG) Log.d(LOG_TAG, "onServiceDisconnected");
				bound = false;
			}
			
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				if(DEBUG) Log.d(LOG_TAG, "onServiceConnected");
				psService = ((PacketSendingService.ServiceBinder) service).getService();
				bound = true;
			}
		};
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		bindService(intent, sConn, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		 if (!bound) return;
		    unbindService(sConn);
		    bound = false;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		ModelApplication.getInstance().setTrackShowing(false);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
//		ModelApplication.getInstance().setTrackShowing(false);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public class TrackSurfaceView extends SurfaceView implements
			SurfaceHolder.Callback, Camera.PreviewCallback {
		SurfaceHolder mHolder;
		Camera mCamera;
		
		public TrackSurfaceView(Context context) {
			super(context);
			
			if(DEBUG) Log.d(LOG_TAG, "TrackSurfaceView is created.");
			
			mHolder = getHolder();
			mHolder.addCallback(this);
			mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		int i = 0;
		@Override
		public void onPreviewFrame(byte[] data, Camera camera) {
			// TODO Sending packets
			if (DEBUG) Log.d(LOG_TAG, "onPreviewFrame: data.length = " + data.length);
			
			if (!bound) {
				Log.d(LOG_TAG, "bound = " + String.valueOf(bound));
				return;
			} else {
				Log.d(LOG_TAG, "bound = " + String.valueOf(bound));
			}
				if (DEBUG) Log.d(LOG_TAG, "Frame #" + i);
//				psService.sendFrame(data);
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			mCamera = Camera.open();
			setCameraParam();
			
			try {
				mCamera.setPreviewDisplay(holder);
				mCamera.setPreviewCallback(this);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			if (mCamera != null) {
				try {
					mCamera.setPreviewDisplay(holder);
					mCamera.startPreview();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			mCamera.setPreviewCallback(null);
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
		
		/**
		 * Set camera parameters: resolution, focus mode, orientation.
		 */
		private void setCameraParam() {
			mCamera.setDisplayOrientation(90);
			
			Camera.Parameters p = mCamera.getParameters();
			List<String> focusModes = p.getSupportedFocusModes();
			if (focusModes != null) {

			}
			
			if (focusModes.contains(Camera.Parameters.FOCUS_MODE_MACRO)) {
				p.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
				if (DEBUG) Log.d(LOG_TAG, "Focus mode: macro");
			} else {
				if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
					p.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
					if (DEBUG) Log.d(LOG_TAG, "Focus mode: auto");
				}
			}
			int width = 0, height = 0;
			List<Camera.Size> sizes = p.getSupportedPictureSizes();
			if (sizes != null) {
				for (Camera.Size size : sizes) {
					if ((width <= size.width) && (height <= size.height)) {
						if (size.width < 2000) {
							width = size.width;
							height = size.height;
						}
					}
				}
				p.setPictureSize(width, height);
				ModelApplication.getInstance().setFrameHeight(height);
				ModelApplication.getInstance().setFrameWidth(width);
				
				if (DEBUG) Log.d(LOG_TAG, "Resolution: " + width + " x " + height);
				
//				p.setPreviewFormat(ImageFormat.NV21);
				p.setPreviewSize(width, height);
//				List<Camera.Size> previewSizes = p.getSupportedPreviewSizes();
//				for (int i = 0; i < previewSizes.size(); i++) {
//					Log.d(LOG_TAG, "pr: " + previewSizes.get(i).height + " x " + previewSizes.get(i).width);
//				}
				if (DEBUG) Log.d(LOG_TAG, "preview: " + p.getPreviewFormat()); 
//				p.setPreviewFormat(ImageFormat.RAW_SENSOR);
				
				mCamera.setParameters(p);
			}
		}
	}
}