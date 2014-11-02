package com.test.develop;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.shiz.model.track.ShowTrack;
import com.shiz.model.track.ViewTrack;

public class MainActivity extends ActionBarActivity implements OnClickListener {
	private final String LOG_TAG = "rtpClient";
	
	private Button startButton;
	private Button transmitButton;
	private Button viewButton;
	private EditText editText;
	private TextView textView;
	
	private int serverPort = 19655;
	private String address = "192.168.1.51";
	private String login = "htc";
	
	private static int threadcount = 0;
	private static int counter = 0;
	private DatagramSocket socket;

	private ShowTrack st;
	private ViewTrack vt;
	
	public int getThreadCount() {
	      return threadcount;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_main);
		
		startButton = (Button) findViewById(R.id.button1);
		transmitButton = (Button) findViewById(R.id.button2);
		viewButton = (Button) findViewById(R.id.button3);
		startButton.setOnClickListener(MainActivity.this);
		transmitButton.setOnClickListener(this);
		viewButton.setOnClickListener(this);
		
		editText = (EditText) findViewById(R.id.editText1);
		editText.setText("192.168.1.43");
		
		textView = (TextView) findViewById(R.id.textView1);
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

	@Override
	public void onClick(View v) {
		if(editText != null) {
			address = editText.getText().toString();
		}
		
		switch (v.getId()){
		case R.id.button1: 
			Toast.makeText(this, "try to send", Toast.LENGTH_LONG).show();
			String serverAnswer = "No";
			try {
				InetAddress addr = InetAddress.getByName(address);
				Log.d(LOG_TAG, addr.getHostAddress());
				ConnectServer cs = new ConnectServer(addr);
				cs.execute();
				serverAnswer = cs.get(10, TimeUnit.SECONDS);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				Log.e(LOG_TAG, "InterruptedException!");
				e.printStackTrace();
			} catch (ExecutionException e) {
				Log.e(LOG_TAG, "ExecutionException!");
				e.printStackTrace();
			} catch (TimeoutException e) {
				Log.e(LOG_TAG, "TimeoutException!");
				Toast.makeText(this, "Сервер не отвечает!", Toast.LENGTH_LONG).show();
				e.printStackTrace();
			}
//			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
//			String answer = sp.getString("answer", "No");
			if (serverAnswer.equals("Hello")) {
				Log.d(LOG_TAG, "serverAnswer = " + serverAnswer);
				
				transmitButton.setEnabled(true);
				viewButton.setEnabled(true);
			} else {
				Toast.makeText(this, "Сервер не отвечает!", Toast.LENGTH_LONG).show();
			}
			break;
		case R.id.button2:
			if (transmitButton.getText().equals("Transmit")) {
				transmitButton.setText("Stop");
				try {
					InetAddress addr = InetAddress.getByName(address);
					st = new ShowTrack(addr);
					st.execute();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			} else {
				transmitButton.setText("Transmit");
				if (st == null) return;
			    st.cancel(false);
				break;
			}
			break;
		case R.id.button3:
			if (viewButton.getText().equals("View")) {
				viewButton.setText("Stop");
				try {
					InetAddress addr = InetAddress.getByName(address);
					vt = new ViewTrack(addr, textView);
					vt.execute();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			} else {
				viewButton.setText("View");				
//				if (vs == null) break;
				vt.cancel(false);
				Log.d("ClientRTP", "break");
				break;
			}
			break;
		}
	}

	/**
	 * Server connection opening class.
	 * @author ultra
	 */
	class ConnectServer extends AsyncTask<Context, Void, String> {
		private InetAddress addr;
		
		ConnectServer(InetAddress addr) {
			Log.d("Task", "Making clinet " + counter);
			threadcount++;
			counter++;
			this.addr = addr;
		}

		@Override
		protected String doInBackground(Context... params) {
			String answer = null;
			try {
		         socket = new DatagramSocket(serverPort);
		    }
			catch (IOException e) {
		         System.err.println("Socket failed.");
		    }
			try {
				String str = new String("Client " + login);
				DatagramPacket sendPacket = new DatagramPacket(str.getBytes(), str.length(), 
						addr, serverPort);
				// Отправка запроса-приветствия серверу
				socket.send(sendPacket);
				
				byte[] buf = new byte[1024];
				DatagramPacket recvPacket = new DatagramPacket(buf, buf.length, addr, serverPort);
				socket.receive(recvPacket);
				
				answer = new String(recvPacket.getData()).trim();
				
				if (answer != null) {
					Log.d("Task", answer);
					if (answer.equals("Hello")) {
//						str = "show";
						str = "view";
//						sendPacket = new DatagramPacket(str.getBytes(), str.length(), 
//								addr, serverPort);
//						socket.send(sendPacket);
						
//						SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(params[0]);
//						Editor ed = sp.edit();
//						ed.putString("answer", answer);
//						ed.commit();
						
					}
				}
		      }
		      catch (IOException e) {
		    	  e.printStackTrace();
		    	  Log.d("Task", "IO Exception");
		         
		      }
		      finally {
		         socket.close();
		         threadcount--; // Завершаем эту нить
		      }
//			socket.close();
			return answer;
		}		
	}
}
