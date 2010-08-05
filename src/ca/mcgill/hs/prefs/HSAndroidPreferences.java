/**
 * TODO: Insert licenses here.
 */
package ca.mcgill.hs.prefs;

import java.io.File;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import ca.mcgill.hs.R;

/**
 * HSAndroidPreferences is a class extending PreferenceActivity which defines
 * the settings menu for the HSAndroid Activity. Whenever the user accesses the
 * settings from the options menu, this PreferenceActivity is launched.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 * 
 */
public class HSAndroidPreferences extends PreferenceActivity {

	private final File path = new File(Environment
			.getExternalStorageDirectory(), "hsandroidapp/data/uploaded/");

	private int filesToDelete;
	private long bytes;

	private int getFilesUploaded() {
		if (path.isDirectory()) {
			return path.listFiles().length;
		} else {
			return 0;
		}
	}

	private long getFilesUploadedBytes() {
		long size = 0;
		try {
			for (final File f : path.listFiles()) {
				size += f.length();
			}
		} catch (final NullPointerException e) {
			// This will happen if the directory does not exist.
			// Therefore size will remain 0 and everything is fine.
		}
		return size;
	}

	/**
	 * This is called when the PreferenceActivity is requested and created. This
	 * allows the user to visually see the preferences menu on the screen.
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		final Preference inputPrefs = findPreference("inputPluginPrefs");
		inputPrefs
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					public boolean onPreferenceClick(final Preference preference) {
						final Intent i = new Intent(getBaseContext(),
								ca.mcgill.hs.prefs.InputPluginPreferences.class);
						startActivity(i);
						return true;
					}
				});

		final Preference outputPrefs = findPreference("outputPluginPrefs");
		outputPrefs
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					public boolean onPreferenceClick(final Preference preference) {
						final Intent i = new Intent(
								getBaseContext(),
								ca.mcgill.hs.prefs.OutputPluginPreferences.class);
						startActivity(i);
						return true;
					}
				});

		// CLEAR UPLOADED DATA
		filesToDelete = getFilesUploaded();
		bytes = getFilesUploadedBytes();

		final Preference manualClearData = findPreference("manualClearData");

		manualClearData
				.setSummary(getResources().getString(
						R.string.uploader_clear_data_desc)
						+ "("
						+ ((bytes / 1024) < 1 ? (bytes + " Bytes")
								: (((bytes / 1024) / 1024) < 1 ? ((bytes / 1024) + " kB")
										: (((bytes / 1024) / 1024) + " MB")))
						+ ")");

		// YES-NO DIALOG BOX FOR FILE CLEAR
		final DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					for (final File f : path.listFiles()) {
						f.delete();
					}
					manualClearData.setSummary(getResources().getString(
							R.string.uploader_clear_data_desc)
							+ "(0 Bytes)");

					filesToDelete = getFilesUploaded();
					bytes = getFilesUploadedBytes();
					break;

				case DialogInterface.BUTTON_NEGATIVE:
					break;
				}
			}
		};

		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(
				"Delete " + filesToDelete + " uploaded "
						+ (filesToDelete == 1 ? "file" : "files") + "?")
				.setPositiveButton("Yes", dialogClickListener)
				.setNegativeButton("No", dialogClickListener);

		manualClearData
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					public boolean onPreferenceClick(final Preference preference) {
						if (filesToDelete > 0) {
							builder.show();
						}
						return true;
					}
				});
	}
}
