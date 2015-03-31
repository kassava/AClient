package com.shiz.model.track;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import com.shiz.model.ModelApplication;

import android.os.AsyncTask;
import android.util.Log;

/**
 * Client track class.
 * @author ultra
 *
 */
public class ShowTrack extends AsyncTask<Void, Void, Void> {
	private final String LOG_TAG = "Client track";
	private final boolean DEBUG = true;
	
	private InetAddress address;
	private DatagramSocket udpSocket;
	private DatagramPacket sendPacket;
	private int serverPort = ModelApplication.getInstance().getServerPort();
	
	public ShowTrack(InetAddress addr) {
		if (DEBUG) Log.d(LOG_TAG, "Client track creation.");
		address = addr;
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		try {
			udpSocket = new DatagramSocket(serverPort);
			sendPacket = new DatagramPacket("show".getBytes(), "show".length(), 
					address, serverPort);
			udpSocket.send(sendPacket);
			udpSocket.close();	
			
			for(int i = 0; i < 1; i++) {
				udpSocket = new DatagramSocket(serverPort);
				if (isCancelled()) {
					sendPacket = new DatagramPacket("buy".getBytes(), "buy".length(), 
							address, serverPort);
					udpSocket.send(sendPacket);
					udpSocket.close();
					return null;
				}
				String str = new String("frame " + i);
				sendPacket = new DatagramPacket(str.getBytes(), str.length(), 
						address, serverPort);
				
				// Greeting request sending to server.
				udpSocket.send(sendPacket);
				udpSocket.close();
				
				// Packet sending delay.
				Thread.sleep(200);
			}
		} catch (SocketException e) {
			if (DEBUG) Log.e(LOG_TAG, "Socket creation exception.");
			e.printStackTrace();
		} catch (IOException e) {
			if (DEBUG) Log.e(LOG_TAG, "IOException");
			e.printStackTrace();
		} catch (InterruptedException e) {
			if (DEBUG) Log.e(LOG_TAG, "Thread.sleep() exception.");
			e.printStackTrace();
		} finally {
			udpSocket.close();
		}
		return null;
	}
}
