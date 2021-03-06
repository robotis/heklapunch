package is.heklapunch;

import is.heklapunch.bluetooth.BlueToothClient;

import java.util.ArrayList;
import java.util.Iterator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.Window;
import android.widget.Toast;

import com.google.gson.GsonBuilder;

public class SendActivity extends Activity {

	private static final boolean DEBUG = true;
	private static final String TAG = "SendActivity";
	private BlueToothClient mClient;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_send);
		setProgressBarIndeterminateVisibility(true);
		setTitle(R.string.sending);
		
		mClient = new BlueToothClient(this);
		mClient.setDiscoverableTimeout(30);
		if(!mClient.start()) {
			Toast.makeText(this, "Unable to start bluetooth", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		
		if(!mClient.isDiscoverable()) {
			mClient.ensureDiscoverable();
		} else {
			startBTpairingIntent();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_send, menu);
		return true;
	}

	/*
	 * Smíðar JSON gagnastreng til að senda á umsjónarmanns
	 * */
	private String get_payload() {
			
		//Tengjast gagnasafni
		SQLHandler handler = new SQLHandler(this);
		ArrayList<ArrayList<String>> results = handler.getAllStations();
		//Gögn frá gagnasafni í ArrayLista
		Iterator<ArrayList<String>> i = results.iterator();
		
		//Hlutur sem verður sendur
		ArrayList<ArrayList<String>> payload = new ArrayList<ArrayList<String>>();
		
		//Ná í nafn keppanda
		SharedPreferences pref = this.getSharedPreferences("competitor_name", Context.MODE_PRIVATE);
        String restoredText = pref.getString("competitor_name", null);
        //Setjum nafn ef það finnst ekki
        if(restoredText == null) restoredText = "NO USERNAME";
        
        //Bæta við upplýsingum um stöðvar
		while (i.hasNext()) {
			ArrayList<String> entry = i.next();
			//Bæta við nafni keppanda við stöðina
			entry.add(restoredText);
			payload.add(entry);
		}
		
		//Byggjum JSON streng
		String json = new GsonBuilder().create().toJson(payload,ArrayList.class);
		
		if (DEBUG){
			//Toast.makeText(this, json, Toast.LENGTH_LONG).show();
		}
		return json;
	}
	
	/* */
	private void startBTpairingIntent() {
		// Störtum valmynd bluetooth tækja
		Intent serverIntent = new Intent(this, BtDeviceListActivity.class);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
			// Gamlir símar styðja ekki óöruggan staðal, og þurfa því 
			// að para sig við umsjónarmann.
			startActivityForResult(serverIntent,
					BlueToothClient.REQUEST_CONNECT_DEVICE_INSECURE);
		} else {
			// Óöruggur staðal, þarfnast ekki pörunar
			startActivityForResult(serverIntent,
					BlueToothClient.REQUEST_CONNECT_DEVICE_SECURE);
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (DEBUG)
			Log.d(TAG, "onActivityResult " + resultCode);
		if(resultCode == RESULT_CANCELED) {
			finish();
			return;
		}
		switch (requestCode) {
		case BlueToothClient.REQUEST_MAKE_DISCOVERABLE:
			startBTpairingIntent();
			break;
		case BlueToothClient.REQUEST_CONNECT_DEVICE_SECURE:
			if (DEBUG)
				Log.d(TAG, "REQUEST_CONNECT_DEVICE_SECURE");
			if (resultCode == Activity.RESULT_OK) {
				mClient.connectToDevice(data, true);
				String json = get_payload();
				if (json.length() > 0) {
					mClient.send(json);
				}
			}
			break;
		case BlueToothClient.REQUEST_CONNECT_DEVICE_INSECURE:
			if (DEBUG)
				Log.d(TAG, "REQUEST_CONNECT_DEVICE_INSECURE");
			if (resultCode == Activity.RESULT_OK) {
				mClient.connectToDevice(data, false);
				String json = get_payload();
				if (json.length() > 0) {
					mClient.send(get_payload());
				}
			}
			break;
		case BlueToothClient.REQUEST_ENABLE_BT:
			if (resultCode == Activity.RESULT_OK) {
				if (DEBUG)
					Log.d(TAG, "Bluetooth enabled");
			} else {
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this,
						"User did not enable Bluetooth or an error occured",
						Toast.LENGTH_SHORT).show();
				finish();
			}
			break;
		}
	}
	
	@Override
    public void onStart() {
        super.onStart();
        if(DEBUG) Log.e(TAG, "-- ON START --");
    }
	
	@Override
    public void onStop() {
        super.onStop();
        if(DEBUG) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth services
        if(mClient != null) {
        	mClient.stop();
        }
        if(DEBUG) Log.e(TAG, "--- ON DESTROY ---");
    }
   
}
