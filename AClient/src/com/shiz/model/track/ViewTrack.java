package com.shiz.model.track;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

/**
 * Track viewing class
 * @author shiz dbyb
 *
 */
public class ViewTrack extends AsyncTask<Void, String, Void> {
	private final String LOG_TAG = "view track";
	private final boolean DEBUG = true;
	private InetAddress address;
	private DatagramSocket udpSocket;
	private TextView textView;
	private int serverPort = 19655; 				// protocol port
	private String login = "htc m8"; 				// for definition client
	
	public ViewTrack(InetAddress addr, TextView tv) {
		this.address = addr;
		this.textView = tv;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		textView.setText("Begin.");
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		if (isCancelled()) {
			if (DEBUG) Log.d(LOG_TAG, "Canceled is true.");
			return null;
		} else {
			if (DEBUG) Log.d(LOG_TAG, "Cancled is false.");
		}
		try {
			udpSocket = new DatagramSocket(serverPort);
			
			// Sending a start request to server.
			String str = new String("Client " + login);
			DatagramPacket sendPacket = new DatagramPacket(str.getBytes(), str.length(), 
					address, serverPort);
			udpSocket.send(sendPacket);
			
			// Receiving answer from server.
			byte[] buf = new byte[1024];
			DatagramPacket recvPacket = new DatagramPacket(buf, buf.length, address, serverPort);
			udpSocket.receive(recvPacket);
			
			// Sending session type.
			str = "view";
			buf = new byte[1024];
			sendPacket = new DatagramPacket(str.getBytes(), str.length(), 
					address, serverPort);
			udpSocket.send(sendPacket);
			
			// Receiving available translations list.
			recvPacket = new DatagramPacket(buf, buf.length);
			udpSocket.receive(recvPacket);
			
			if (DEBUG) Log.d(LOG_TAG, new String(recvPacket.getData()).trim());
			
			JSONObject jsonObject = new JSONObject(new String(recvPacket.getData()).trim());
			publishProgress(jsonObject.toString());
			if (DEBUG) Log.d(LOG_TAG, jsonObject.toString());
			
			JSONArray ja = jsonObject.getJSONArray("translations");
			
			if (DEBUG) Log.d(LOG_TAG, ja.toString());
			
			String trans = (String) ja.getString(0);
			
			if (trans.equals("end")) {
				if (DEBUG) Log.d(LOG_TAG, "The translations list is empty.");
				publishProgress("The translations list is empty.");
				return null;
			}
			
			if (DEBUG) Log.d(LOG_TAG, "trans: " + trans);
			
			// Sending id of the chosen translation. 
			sendPacket = new DatagramPacket(trans.getBytes(), trans.length(), 
					address, serverPort);
			udpSocket.send(sendPacket);
			
			while (!udpSocket.isClosed()) {
				buf = new byte[1024];
				recvPacket = new DatagramPacket(buf, buf.length);
				udpSocket.receive(recvPacket);
				Log.d(LOG_TAG, new String(recvPacket.getData()).trim());
				if (new String (recvPacket.getData()).trim().equals("buy")) {
					publishProgress("Трансляция оффлайн");
				} else {
					publishProgress(new String(recvPacket.getData()).trim());
				}
			}
		} catch (SocketException e) {
			if (DEBUG) Log.d(LOG_TAG, "Socket creating error!");
			e.printStackTrace();
		} catch (IOException e) {
			if (DEBUG) Log.d(LOG_TAG, "IOException");
			e.printStackTrace();
		} catch (JSONException e) {
			if (DEBUG) Log.d(LOG_TAG, "JSON error!");
			e.printStackTrace();
		}
		if (udpSocket != null) {
			if (!udpSocket.isClosed()) {
				udpSocket.close();
			}
		}
		return null;
	}
	
	@Override
    protected void onProgressUpdate(String... values) {
      super.onProgressUpdate(values);
      textView.setText(values[0]);
    }

    @Override
    protected void onPostExecute(Void result) {
      super.onPostExecute(result);
      
      if (udpSocket != null) {
    	  if (!udpSocket.isClosed()) {
    		  udpSocket.close();
    	  }
      }
      textView.setText("End");
    }	
    
    @Override
    protected void onCancelled() {
    	textView.setText("Cancel");
    	
    	DatagramPacket sendPacket = new DatagramPacket("buy".getBytes(), "buy".length(), 
				address, serverPort);
		try {
			udpSocket.send(sendPacket);
		} catch (IOException e) {
			if (DEBUG) Log.d(LOG_TAG, "onCancelled: IOException");
			e.printStackTrace();
		}
    	
    	if (udpSocket != null) {
	    	  if (!udpSocket.isClosed()) {
	    		  udpSocket.close();
	    	  }
	      }
    	if (DEBUG) Log.d(LOG_TAG, "Cancel");
    }
}
