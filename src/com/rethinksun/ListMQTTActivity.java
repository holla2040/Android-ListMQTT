package com.rethinksun;

/* references
 * 	http://www.hardill.me.uk/wordpress/?p=204
 *  http://dalelane.co.uk/blog/?p=1599 - for more complete example using a service for MQTT
 *  http://saeedsiam.blogspot.com/2009/02/first-look-into-android-thread.html - handler example
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import android.app.ListActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.Secure;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.ibm.mqtt.MqttClient;
import com.ibm.mqtt.MqttException;
import com.ibm.mqtt.MqttSimpleCallback;

public class ListMQTTActivity extends ListActivity {
	/** Called when the activity is first created. */
	private String android_id;
	private MqttClient client;
	final static String broker = "tcp://horton.rethinksun.com:1883";
	private ArrayAdapter<String> aa;
	private ArrayList<String> kv;
	private TreeMap<String,String> hm;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		android_id = Secure.getString(this.getContentResolver(),Secure.ANDROID_ID);
		kv = new ArrayList<String>();
		hm = new TreeMap<String,String>();
		aa = new ArrayAdapter<String>(this, R.layout.main, kv);
		aa.add("Connecting to "+broker);
		this.setListAdapter(aa);

		connect();
	}

	final Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			String m0 = msg.getData().getString("message");
			String m = m0.substring(0, Math.min(m0.length(),20));
			String t = msg.getData().getString("topic").replaceFirst("us/co/montrose/121 Apollo/", "");
			hm.put(t, m.toString());
			kv.clear();
			Set set = hm.entrySet(); 
			Iterator i = set.iterator(); 
			while(i.hasNext()) { 
				Map.Entry me = (Map.Entry)i.next();
				kv.add(me.getKey()+"="+me.getValue());
			}
			aa.notifyDataSetChanged();
		}
	};

	private boolean connect() {
		try {
			client = (MqttClient) MqttClient.createMqttClient(broker, null);
			client.registerSimpleHandler(new MessageHandler());
			client.connect("HM" + android_id, true, (short) 240);
			String topics[] = { "#" };
			int qos[] = { 1 };
			client.subscribe(topics, qos);
			return true;
		} catch (MqttException e) {
			e.printStackTrace();
			return false;
		}
	}

	@SuppressWarnings("unused")
	private class MessageHandler implements MqttSimpleCallback {
		public void publishArrived(String _topic, byte[] payload, int qos,
				boolean retained) throws Exception {
			String _message = new String(payload);
			Bundle b = new Bundle();
			b.putString("topic", _topic);
			b.putString("message", _message);
			Message msg = handler.obtainMessage();
			msg.setData(b);
			handler.sendMessage(msg);
			Log.d("MQTT", _message);

		}

		public void connectionLost() throws Exception {
			client = null;
			Log.v("ListMQTT", "connection dropped");
			Thread t = new Thread(new Runnable() {

				public void run() {
					do {// pause for 5 seconds and try again;
						Log.v("ListMQTT",
								"sleeping for 10 seconds before trying to reconnect");
						try {
							Thread.sleep(10 * 1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} while (!connect());
					System.err.println("reconnected");
				}
			});
		}
	}
}
