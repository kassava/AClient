package com.shiz.model.track;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.shiz.model.ModelApplication;

/**
 * Class for sending packets
 * @author ultra
 *
 */
public class PacketSendingService extends Service {
	private final String LOG_TAG = "PacketSendingService";
	private final boolean DEBUG = true;
	
//	private ExecutorService es; 						// for run threads
	private ServiceBinder binder = new ServiceBinder(); 
	private byte[] packet; 								// data for sending 
	
	public void onCreate() {
	    super.onCreate();
	    if (DEBUG) Log.d(LOG_TAG, "onCreate");
	}
	
	public void onDestroy() {
	    super.onDestroy();
	    if (DEBUG) Log.d(LOG_TAG, "onDestroy");
	}
	
	public int onStartCommand(Intent intent, int flags, int startId) {
	    if (DEBUG) Log.d(LOG_TAG, "onStartCommand");
	    
	    return super.onStartCommand(intent, flags, startId);
	}
	
	public boolean sendFrame(byte[] frame) {
		this.packet = frame;
		
		if (ModelApplication.getInstance().isTrackShowing()) {
    		PacketSender ps = new PacketSender();
    		
    		ps.setSSRC(ModelApplication.getInstance().getSSRC());
    		ps.updateTimestamp(System.currentTimeMillis());
    		
    		ps.execute(packet);
    	} else {
    		PacketSender ps = new PacketSender();
    		ps.execute("show".getBytes());
    		
    		ModelApplication.getInstance().setTrackShowing(true);
    	}
		
		return true;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		if (DEBUG) Log.d(LOG_TAG, "onBind");
		return binder;
	}
	
	public class ServiceBinder extends Binder {
		public PacketSendingService getService() {
			return PacketSendingService.this;
		}
	}

//	class MyRun implements Runnable {
//	    
//	    byte[] frame;
//	    int startId;
//	    
//	    public MyRun(byte[] frame, int startId) {
//	      this.frame = frame.clone();
//	      this.startId = startId;
//	    }
//	    
//	    public void run() {
////	    	InetAddress address = null;
////			try {
////				address = InetAddress.getByName(ModelApplication.
////						getInstance().getServerAddress());
////			} catch (UnknownHostException e) {
////				if(DEBUG) Log.e(LOG_TAG, "UnknownHostException");
////			}
////	    	if(address != null) {
////	    		ShowTrack st = new ShowTrack(address);
////	    		st.execute();
////	    	}
//	    	if (ModelApplication.getInstance().isTrackShowing()) {
//	    		PacketSender ps = new PacketSender();
//	    		ps.execute(frame);
//	    	} else {
//	    		PacketSender ps = new PacketSender();
//	    		ps.execute("show".getBytes());
//	    		ModelApplication.getInstance().setTrackShowing(true);
//	    	}
//	    	stop();
//	    }
//	    
//	    void stop() {
//	      stopSelf(startId);
//	    }
//	  }
}
