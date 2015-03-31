package com.shiz.model.track;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.os.AsyncTask;
import android.util.Log;

import com.shiz.model.ModelApplication;

/**
 * Class to send the one packet.
 * @author ultra
 *
 */
public class PacketSender extends AsyncTask<byte[], Void, Void>{
	private final String LOG_TAG = "PacketSender";
	private boolean DEBUG = true;
	
	private DatagramSocket mUDPSocket;
	private DatagramPacket mSendPacket;
	private int mServerPort = ModelApplication.getInstance().getServerPort(); 
	private String mServerAddress = ModelApplication.getInstance().getServerAddress();
	
	private int jpegHeaderLength = ModelApplication.getInstance().JPEG_HEADER_LENGTH;
	private byte[] mPacket;
	private byte[] mPacketHeader; 		// The header of the main packet
	private byte[] mJPEGPacketHeader = new byte[jpegHeaderLength];;	// The header of the jpeg packet
	private static int mSequenceNumber = 0;
	private long mTimestamp = 0;
	private int mSSRC;
	private long mClock = 0;
	private int protocoloHeaderLength = ModelApplication.getInstance().PROTOCOL_HEADER_LENGTH;
	
	public PacketSender() {
		mPacketHeader = new byte[protocoloHeaderLength];
		
		// Version		(2b)	=	2	\
		// Padding		(1b) 	= 	0	 \ 	Total 1 byte 
		// Extension	(1b)	=	0	 /	  10000000
		// CSRC			(4b)	=	0	/
		mPacketHeader[0] = (byte) Integer.parseInt("10000000", 2);
		
		// Payload type	(8b). Marker M included in the payload type byte.
		mPacketHeader[1] = (byte) ModelApplication.getInstance().getContentType();
		
		// Byte 2,3        ->  Sequence Number                   
		// Byte 4,5,6,7    ->  Timestamp                         
		// Byte 8,9,10,11  ->  Synchronization Source Identifier (SSRC)            
	}
	
	/**
	 * Sets RTP marker bit.
	 */
	public void setRTPMarkerBit() {
		mPacketHeader[1] |= 0x80;
	}
	
	/**
	 * Sets the SSRC.
	 */
	public void setSSRC(int ssrc) {
		this.mSSRC = ssrc;
		setBytes(mPacketHeader, ssrc, 8, 12);
	}
	
	/**
	 * Return SSRC.
	 */
	public int getSSRC() {
		return this.mSSRC;
	}
	
	/** Sets the clock frequency in Hz. */
	public void setClockFrequency(long clock) {
		mClock = clock;
	}
	
	/** 
	 * Overwrites the timestamp in the packet.
	 * @param timestamp The new timestamp in ns.
	 **/
	public void updateTimestamp(long timestamp) {
		mTimestamp = timestamp;
		setBytes(mPacketHeader, (mTimestamp / 100L) * (mClock / 1000L) / 10000L, 4, 8);
	}
	
	private void setBytes(byte[] buffer, long n, int begin, int end) {
		for (end--; end >= begin; end--) {
			buffer[end] = (byte) (n % 256);
			n >>= 8;
		}
	}
	
	/** Increments the sequence number. */
	private void updateSequence() {
		setBytes(mPacketHeader, ++mSequenceNumber, 2, 4);
		
		if (DEBUG) Log.d(LOG_TAG, "SequenceNumber: " + mSequenceNumber);
	}
	
	/**
	 * Set main jpeg header.
	 * <br>==========================================================================
	 * <br>= Type-specific (1 byte) = ---------- Fragment offset (3 byte) --------- =
	 * <br>==========================================================================
	 * <br>= ---- Type (1 byte) --- = Q (1 byte) = Width (1 byte) = Height (1 byte) = 
	 * <br>==========================================================================
	 */
	private void setMainJpegHeader() {
		mJPEGPacketHeader[0] = 0; 		// Type-specific
		mJPEGPacketHeader[1] = 0;		// \
		mJPEGPacketHeader[2] = 0;		//  \ Fragment offset
		mJPEGPacketHeader[3] = 0;		//	/
		mJPEGPacketHeader[4] = 0;		// Type
		mJPEGPacketHeader[5] = 0;		// Q
		mJPEGPacketHeader[6] = (byte) (ModelApplication.getInstance().
				getFrameWidth() / 8);	// Width
		mJPEGPacketHeader[7] = (byte) (ModelApplication.getInstance().
				getFrameHeight() / 8);	// Height
	}
	
	@Override
	protected Void doInBackground(byte[]... params) {
		
		if (params[0].length == 4) Log.d(LOG_TAG, "4: " + new String(params[0]));
		
		try {
			if (DEBUG) Log.d(LOG_TAG, "params[0].length = " + params[0].length);
			
			mUDPSocket = new DatagramSocket(mServerPort);
			int MTU = ModelApplication.getInstance().MTU;
			int correctedMTU = MTU - protocoloHeaderLength - jpegHeaderLength;
			
			mPacket = new byte[MTU];
			
			setMainJpegHeader();
			setBytes(mJPEGPacketHeader, correctedMTU, 1, 4); //???
			
			if (params[0].length > (correctedMTU)) {
				int packetIndex = 0, frameIndex = 0;
				
				while (frameIndex < params[0].length) {
					if (packetIndex >= 0 && packetIndex < protocoloHeaderLength) {
						// rtp header
						mPacket[packetIndex] = mPacketHeader[packetIndex];
						packetIndex++;
						continue;
					}
					if (packetIndex >= protocoloHeaderLength 
							&& packetIndex < (protocoloHeaderLength + jpegHeaderLength)) {
						// jpeg main header
						mPacket[packetIndex] = mJPEGPacketHeader[packetIndex - protocoloHeaderLength];
						packetIndex++;
						continue;
					}
					mPacket[packetIndex] = params[0][frameIndex];
					if (((frameIndex + 1) % correctedMTU) == 0) {						
//						if (DEBUG) Log.d(LOG_TAG, "frameIndex = " + frameIndex 
//								+ " packetIndex = " + packetIndex);
						
						mSendPacket = new DatagramPacket(mPacket, MTU, 
								InetAddress.getByName(mServerAddress), mServerPort);
						//TODO uncomment after
//						mUDPSocket.send(mSendPacket);
						packetIndex = -1;
						mPacket = new byte[MTU];
						
						updateSequence();
						
//						if (DEBUG) Log.d(LOG_TAG, "frameIndex = " + frameIndex 
//								+ " packetIndex = " + packetIndex);
					}
					frameIndex++;
					packetIndex++;
				}
				
//				if (DEBUG) Log.d(LOG_TAG, "frameIndex = " + frameIndex 
//						+ " packetIndex = " + packetIndex);
				
				setRTPMarkerBit();
				mSendPacket = new DatagramPacket(mPacket, packetIndex, 
						InetAddress.getByName(mServerAddress), mServerPort);
				//TODO uncomment after
//				mUDPSocket.send(mSendPacket);
				
				updateSequence();
				if (DEBUG) Log.d(LOG_TAG, "The end of the frame");
			} else {
				mSendPacket = new DatagramPacket(mPacket, mPacket.length, 
						InetAddress.getByName(mServerAddress), mServerPort);
				//TODO uncomment after
//				mUDPSocket.send(mSendPacket);
				
				updateSequence();
			}
			
			mUDPSocket.close();
		} catch (SocketException e) {
			if (DEBUG) Log.e(LOG_TAG, "SocketException");
			e.printStackTrace();
		} catch (UnknownHostException e) {
			if (DEBUG) Log.e(LOG_TAG, "UnknownHostException");
			e.printStackTrace();
		} catch (IOException e) {
			if (DEBUG) Log.e(LOG_TAG, "IOException");
			e.printStackTrace();
		}
		return null;
	}

}
