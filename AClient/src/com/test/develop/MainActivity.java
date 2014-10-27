package com.test.develop;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

public class MainActivity extends ActionBarActivity implements OnClickListener {
	private Button startButton;
	private Button transmitButton;
	private Button viewButton;
	private EditText editText;
	private TextView textView;
	private int serverPort = 19655;
	private String address = "192.168.1.51";
	private static int threadcount = 0;
	private static int counter = 0;
	private DatagramSocket socket;
	private String login = "htc";
	private ShowStream ss;
	private ViewStream vs;
	
	public static int threadCount() {
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
			try {
				InetAddress addr = InetAddress.getByName(address);
				Log.d("CleintRTP", addr.getHostAddress());
				ConnectServer ct = new ConnectServer(addr);
				ct.execute();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
			String answer = sp.getString("answer", "No");
			if (answer.equals("Hello")) {
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
					ss = new ShowStream(addr);
					ss.execute();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			} else {
				transmitButton.setText("Transmit");
				if (ss == null) return;
			    ss.cancel(false);
				break;
			}
			break;
		case R.id.button3:
			if (viewButton.getText().equals("View")) {
				viewButton.setText("Stop");
				try {
					InetAddress addr = InetAddress.getByName(address);
					vs = new ViewStream(addr);
					vs.execute();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			} else {
				viewButton.setText("View");				
//				if (vs == null) break;
				vs.cancel(false);
				Log.d("ClientRTP", "break");
				break;
			}
			break;
		}
	}

	/**
	 * Класс для установления соединения с сервером
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
		         System.err.println("Socket failed");
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
						
						SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(params[0]);
						Editor ed = sp.edit();
						ed.putString("answer", answer);
						ed.commit();
						
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
	
	/**
	 * Класс просмотра трансляции
	 * @author ultra
	 *
	 */
	class ViewStream extends AsyncTask<Void, String, Void> {
		private final String LOG_TAG = "view stream";
		private InetAddress address;
		private DatagramSocket udpSocket;
		
		public ViewStream(InetAddress addr) {
			this.address = addr;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			textView.setText("Begin");
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			if (isCancelled()) {
				Log.d(LOG_TAG, "canceled true");
				return null;
			} else {
				Log.d(LOG_TAG, "cancled false");
			}
			try {
				udpSocket = new DatagramSocket(serverPort);
				
				// Отправка запроса приветствия серверу
				String str = new String("Client " + login);
				DatagramPacket sendPacket = new DatagramPacket(str.getBytes(), str.length(), 
						address, serverPort);
				udpSocket.send(sendPacket);
				
				// Получение ответа на приветствие
				byte[] buf = new byte[1024];
				DatagramPacket recvPacket = new DatagramPacket(buf, buf.length, address, serverPort);
				udpSocket.receive(recvPacket);
				
				// Отправка типа сессии
				str = "view";
				buf = new byte[1024];
				sendPacket = new DatagramPacket(str.getBytes(), str.length(), 
						address, serverPort);
				udpSocket.send(sendPacket);
				
				// Получение списка доступных трансляций
				recvPacket = new DatagramPacket(buf, buf.length);
				udpSocket.receive(recvPacket);
				
				Log.d(LOG_TAG, new String(recvPacket.getData()).trim());
				
				JSONObject jsonObject = new JSONObject(new String(recvPacket.getData()).trim());
				publishProgress(jsonObject.toString());
				Log.d(LOG_TAG, jsonObject.toString());
				
				JSONArray ja = jsonObject.getJSONArray("translations");
				
				Log.d(LOG_TAG, ja.toString());
				
				String trans = (String) ja.getString(0);
				
				if (trans.equals("end")) {
					Log.d(LOG_TAG, "Список трансляций пуст");
					publishProgress("Список трансляций пуст");
					return null;
				}
				
				Log.d(LOG_TAG, "trans: " + trans);
				
				// Отправка выбранной для просмтра сессии
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
				Log.d(LOG_TAG, "ошибка создания сокета");
				e.printStackTrace();
			} catch (IOException e) {
				Log.d(LOG_TAG, "IOException");
				e.printStackTrace();
			} catch (JSONException e) {
				Log.d(LOG_TAG, "json сломался");
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
				Log.d(LOG_TAG, "onCancelled: IOException");
				e.printStackTrace();
			}
	    	
	    	if (udpSocket != null) {
		    	  if (!udpSocket.isClosed()) {
		    		  udpSocket.close();
		    	  }
		      }
	    	Log.d(LOG_TAG, "Cancel");
	    }
	}
	
	/**
	 * Класс потока клиента
	 * @author ultra
	 *
	 */
	class ShowStream extends AsyncTask<Void, Void, Void> {
		private final String LOG_TAG = "client stream";
		private InetAddress address;
		private DatagramSocket udpSocket;
		
		public ShowStream(InetAddress addr) {
			Log.d(LOG_TAG, "создание потока клиента");
			address = addr;
		}
		@Override
		protected Void doInBackground(Void... params) {
			try {
				udpSocket = new DatagramSocket(serverPort);
				DatagramPacket sendPacket = new DatagramPacket("show".getBytes(), "show".length(), 
						address, serverPort);
				udpSocket.send(sendPacket);
				udpSocket.close();				
				for(int i = 0; i < 1000; i++) {
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
					// Отправка запроса-приветствия серверу
					udpSocket.send(sendPacket);
					udpSocket.close();
					// Задержка отправки пакета
					Thread.sleep(200);
				}
			} catch (SocketException e) {
				Log.d(LOG_TAG, "ошибка создания сокета");
				e.printStackTrace();
			} catch (IOException e) {
				Log.d(LOG_TAG, "IOException");
				e.printStackTrace();
			} catch (InterruptedException e) {
				Log.d(LOG_TAG, "Thread.sleep() сломался");
				e.printStackTrace();
			} finally {
				udpSocket.close();
			}
			return null;
		}
		
	}
}
