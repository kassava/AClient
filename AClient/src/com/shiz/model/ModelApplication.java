package com.shiz.model;

import android.app.Application;

/**
 * Set of constants.
 * @author ultra
 *
 */
public class ModelApplication extends Application {
	private static ModelApplication singleton;
	
	private final int serverPort = 19655;
	private final int serverCommandPort = 20655;
	private String serverAddress = "192.168.1.15";
	private String serverLogin = "htc one";
	private int frameWidth = 0;
	private int frameHeight = 0;
	private int SSRC = 26121989;
	
	private boolean trackShowing = false;
	private int contentType = ContentType.JPEG;
	
	public final int MTU = 1500;
	public final int PROTOCOL_HEADER_LENGTH = 12;
	public final int JPEG_HEADER_LENGTH = 8;
	
	public static ModelApplication getInstance() {
		return singleton;
	}
	
	@Override
	public final void onCreate() {
		super.onCreate();
		
		singleton = this;
	}

	public int getServerPort() {
		return serverPort;
	}

	public String getServerAddress() {
		return serverAddress;
	}

	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	public String getServerLogin() {
		return serverLogin;
	}

	public void setServerLogin(String serverLogin) {
		this.serverLogin = serverLogin;
	}

	public boolean isTrackShowing() {
		return trackShowing;
	}

	public void setTrackShowing(boolean trackShowing) {
		this.trackShowing = trackShowing;
	}

	public int getFrameWidth() {
		return frameWidth;
	}

	public void setFrameWidth(int frameWidth) {
		this.frameWidth = frameWidth;
	}

	public int getFrameHeight() {
		return frameHeight;
	}

	public void setFrameHeight(int frameHeight) {
		this.frameHeight = frameHeight;
	}

	public int getSSRC() {
		return SSRC;
	}

	public void setSSRC(int sSRC) {
		SSRC = sSRC;
	}

	public int getServerCommandPort() {
		return serverCommandPort;
	}

	public int getContentType() {
		return contentType;
	}

	public void setContentType(int contentType) {
		this.contentType = contentType;
	}
}
