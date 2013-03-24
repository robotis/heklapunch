package is.heklapunch;

import java.util.ArrayList;
import java.util.Iterator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class OrganizeModifyActivity extends Activity {
	TableLayout station_table;
	SQLHandler handler;
	EditText stationNameField;
	EditText courseNameField;
	String courseName = "";
	public ArrayList<ArrayList<String>> stationList = new ArrayList<ArrayList<String>>();
	//int stationNumber = 1;
	int courseID = -1;
	
	@Override
	public void onBackPressed() {
	    new AlertDialog.Builder(this)
	        .setIcon(android.R.drawable.ic_dialog_alert)
	        .setTitle("Loka glugga")
	        .setMessage("Ert þú viss um að þú viljir hætta? Allar óvistaðar breytingar munu fyrnast")
	        .setPositiveButton("já", new DialogInterface.OnClickListener()
	    {
	        
	        public void onClick(DialogInterface dialog, int which) {
	            finish();    
	        }

	    })
	    .setNegativeButton("Nei", null)
	    .show();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_organize_modify);
		
		// create database object
		handler = new SQLHandler(this);
		courseNameField = (EditText) findViewById(R.id.editTextCourseName);
		stationNameField = (EditText) findViewById(R.id.EditTextStationName);
		
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		courseNameField.setImeOptions(EditorInfo.IME_ACTION_DONE);
		stationNameField.setImeOptions(EditorInfo.IME_ACTION_DONE);

		// get the course ID from sent to us form the create screen
		Bundle b = getIntent().getExtras();
		if (b.containsKey("courseID")) {
			courseID = b.getInt("courseID");
		}
		//if we are given a courseID to modify, open it for editing, if it exists
		if(courseID != -1 && handler.checkCoursebyID(courseID)){
			stationList = handler.getCoursebyID(courseID);
			//find the course name
			CourseData[] courseIDs = handler.getCourseIDs();
			for(int i =0; i < courseIDs.length; i++){
				if(Integer.valueOf(courseIDs[i].getValue()) == courseID){
					courseName = handler.getCourseIDs()[i].getSpinnerText();
					break;
				}
			}
			//place course name in course name field
			courseNameField.setText(courseName);
		}
		//create a new course ID if needed
		else{
			courseID = handler.getMaxCourseID() + 1;
		}
		
		// make table
		station_table = (TableLayout) findViewById(R.id.Create_Station_Table);
		this.fillTable();
	}

	// fill table with content, we do NOT read from the database here! we only
	// work
	// with new data in the stationList object
	public void fillTable() {

		TableRow row;
		TextView t1, t2;
		// Converting to dip unit
		int dip = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				(float) 1, getResources().getDisplayMetrics());
		
		/*if(stationNameField.getText() != null){
			stationName = stationNameField.getText().toString();
		}*/

		Iterator<ArrayList<String>> i = stationList.iterator();

		while (i.hasNext()) {

			ArrayList<?> entry = i.next();

			row = new TableRow(this);

			t1 = new TextView(this);
			t2 = new TextView(this);

			t1.setText(entry.get(2).toString());
			t2.setText(entry.get(1).toString());

			t1.setTypeface(null, 1);
			t2.setTypeface(null, 1);

			t1.setTextSize(15);
			t2.setTextSize(15);

			t1.setWidth(40 * dip);
			t2.setWidth(260 * dip);
			row.addView(t1);
			row.addView(t2);

			station_table.addView(row, new TableLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

		}
	}

	// Save list contents to database
	public void saveList(View view) {
		String courseTitle = courseNameField.getText().toString();
		Iterator<ArrayList<String>> i = stationList.iterator();
		handler.removeCourseByID(courseID);
		while (i.hasNext()) {
			ArrayList<?> entry = i.next();
			int stationNumber = Integer.valueOf(entry.get(2).toString());
			String stationTitle = entry.get(1).toString();
			String QRValue = entry.get(5).toString();
			String GPSValue = entry.get(6).toString();
			handler.addStation(courseTitle, courseID, stationTitle,
					stationNumber, QRValue, GPSValue);
		}
	}

	// Go to QR mode
	public void read_qr(View view) {
		IntentIntegrator integrator = new IntentIntegrator(this);
		integrator.initiateScan();
	}
	
	//get highest station number in the station number list
	public int getNextStation(){
		//we start the stations at 30, dunno why....
		int num = 30;
		Iterator<ArrayList<String>> i = stationList.iterator();
		while (i.hasNext()) {
			ArrayList<?> entry = i.next();
			num = Integer.valueOf(entry.get(2).toString()) + 1;
		}
		return num;
	}
	
	//add in a new station
	public void addStation(String stationID, String name,
			String number, String courseID, String courseName, 
			String QR, String GPS ){
		ArrayList<String> tempStation = new ArrayList<String>();
		//add station ID NOTE: this value never needs to be read for new values,
		// so we just set it to -1 to save time
		tempStation.add(stationID);
		// add station name
		tempStation.add(name);
		// add station number
		tempStation.add(number);
		//add course ID
		tempStation.add(courseID);
		//add course name
		tempStation.add(courseName);
		// add QR code
		tempStation.add(QR);
		// add GPS
		// TODO: add working gps!
		tempStation.add(GPS);
		stationList.add(tempStation);
		TableLayout vg = (TableLayout) findViewById(R.id.Create_Station_Table);
		vg.removeAllViews();
		// redraw table
		this.fillTable();
	}

	// QR Scan result
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		IntentResult scanResult = IntentIntegrator.parseActivityResult(
				requestCode, resultCode, intent);
		int stationNumber = getNextStation();
		if (scanResult != null && scanResult.getContents().length() != 0) {
			// handle scan result
			Toast.makeText(this, scanResult.getContents(), Toast.LENGTH_SHORT)
					.show();
			// write to db
			if (stationNameField.getText().toString() != null) {
				this.addStation(String.valueOf(-1), stationNameField.getText().toString(),
						String.valueOf(stationNumber), String.valueOf(courseID),
						String.valueOf(courseName), String.valueOf(scanResult.getContents()),
						"12345");
			} else {
				this.addStation(String.valueOf(-1), "Stöð nr. " + stationNumber,
						String.valueOf(stationNumber), String.valueOf(courseID),
						String.valueOf(courseName), String.valueOf(scanResult.getContents()),
						"12345");
			}

		} else {
			Toast.makeText(this, "No scan", Toast.LENGTH_SHORT).show();
		}
	}
}
