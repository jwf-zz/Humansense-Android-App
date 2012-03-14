package ca.mcgill.hs.widget;

import java.util.LinkedList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import ca.mcgill.hs.R;
import ca.mcgill.hs.plugin.LocationLogger;
import ca.mcgill.hs.plugin.LocationLogger.LocationLabelDictionary;
import ca.mcgill.hs.util.Log;

public class LocationLabelerDialog extends Activity {

	public class LocationLabelSelectedListener implements
			OnItemSelectedListener {
		@Override
		public void onItemSelected(final AdapterView<?> parent,
				final View view, final int pos, final long id) {
			final int N = parent.getCount();
			final EditText textInput = (EditText) findViewById(R.id.location_labeler_edittext);
			if (pos == N - 1) {
				textInput.setEnabled(true);
			} else {
				textInput.setEnabled(false);
			}
			/*
			 * Log.d(TAG, "N = " + N + ", pos = " + pos);
			 * Toast.makeText(parent.getContext(), "Selected: " +
			 * parent.getItemAtPosition(pos).toString(),
			 * Toast.LENGTH_LONG).show();
			 */
		}

		@Override
		public void onNothingSelected(final AdapterView<?> parent) {
			// Do nothing.
		}
	}

	public static final String TAG = "LocationLabelerDialog";

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
		super.onDestroy();
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d(TAG, "onStart");
		setContentView(R.layout.location_labeler_dialog);
		final Button okButton = (Button) findViewById(R.id.location_labeler_input_ok_button);
		final Button cancelButton = (Button) findViewById(R.id.location_labeler_input_cancel_button);
		final EditText textInput = (EditText) findViewById(R.id.location_labeler_edittext);

		final Spinner spinner = (Spinner) findViewById(R.id.location_labeler_spinner);
		final LocationLabelDictionary db = new LocationLabelDictionary(
				this.getApplicationContext());
		final LinkedList<String> labels = db.getLabels();
		labels.add(this.getResources().getString(R.string.other_location_label));
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, labels);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(new LocationLabelSelectedListener());

		// This should be uneccessary, as onItemSelected should get called when
		// the list is created, but just in case...
		if (spinner.getCount() > 1) {
			textInput.setEnabled(false);
		} else {
			textInput.setEnabled(true);
		}

		okButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				String label;
				if (spinner.getSelectedItemPosition() == spinner.getCount() - 1) {
					// Other... is selected
					label = textInput.getText().toString();
				} else {
					label = spinner.getSelectedItem().toString();
				}
				final Intent i = new Intent(
						LocationLogger.LOCATION_LABELED_ACTION);
				i.putExtra(LocationLabelDictionary.LABEL_COLUMN, label);
				sendBroadcast(i);
				finish();
			}
		});
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				finish();
			}
		});
	}
}