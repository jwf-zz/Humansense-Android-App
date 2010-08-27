/**

 * TODO: Insert licenses here.
 */
package ca.mcgill.hs.prefs;

import java.io.File;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;
import ca.mcgill.hs.R;
import ca.mcgill.hs.serv.NewUploaderService;

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
	private final File recent = new File(Environment
			.getExternalStorageDirectory(), "hsandroidapp/data/recent/");

	private int filesToDelete;
	private int unuploadedFilesToDelete;
	private long bytes;

	private static void broadcastAutoUploaderIntent(final Context c) {
		final Intent i = new Intent();
		i.setAction(NewUploaderService.AUTO_UPLOAD_CHANGED_INTENT);
		c.sendBroadcast(i);
	}

	private static void broadcastWifiUploaderIntent(final Context c) {
		final Intent i = new Intent();
		i.setAction(NewUploaderService.WIFI_ONLY_CHANGED_INTENT);
		c.sendBroadcast(i);
	}

	/**
	 * Returns the number of unuploaded files.
	 * 
	 * @return the number of unuploaded files.
	 */
	private int getFilesUnuploaded() {
		if (recent.isDirectory()) {
			return recent.listFiles().length;
		} else {
			return 0;
		}
	}

	/**
	 * Returns the number of uploaded files.
	 * 
	 * @return the number of uploaded files.
	 */
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
	 * Helper method for making toasts.
	 * 
	 * @param message
	 *            the text to toast.
	 * @param duration
	 *            the duration of the toast.
	 */
	private void makeToast(final String message, final int duration) {
		final Toast slice = Toast.makeText(getBaseContext(), message, duration);
		slice.setGravity(slice.getGravity(), slice.getXOffset(), slice
				.getYOffset() + 100);
		slice.show();
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
		builder.setMessage(R.string.delete_files_warning).setPositiveButton(
				R.string.yes, dialogClickListener).setNegativeButton(
				R.string.no, dialogClickListener);

		manualClearData
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					public boolean onPreferenceClick(final Preference preference) {
						if (filesToDelete > 0) {
							builder.show();
						}
						return true;
					}
				});

		// MANAGE UNUPLOADED
		final Preference manageUnuploaded = findPreference("manageUnuploaded");
		manageUnuploaded
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					public boolean onPreferenceClick(final Preference preference) {
						final Intent i = new Intent(getBaseContext(),
								ca.mcgill.hs.prefs.FileManager.class);
						startActivity(i);
						return true;
					}
				});

		// CLEAR UNUPLOADED
		final DialogInterface.OnClickListener undeletedClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					for (final File f : recent.listFiles()) {
						f.delete();
					}
					makeToast(unuploadedFilesToDelete
							+ " "
							+ (unuploadedFilesToDelete == 1 ? getResources()
									.getString(R.string.file_deleted)
									: getResources().getString(
											R.string.files_deleted)),
							Toast.LENGTH_SHORT);
					unuploadedFilesToDelete = getFilesUnuploaded();
					break;

				case DialogInterface.BUTTON_NEGATIVE:
					break;
				}
			}
		};

		final AlertDialog.Builder aggressiveBuilder = new AlertDialog.Builder(
				this);
		aggressiveBuilder.setMessage(R.string.delete_files_big_warning)
				.setPositiveButton(R.string.yes, undeletedClickListener)
				.setNegativeButton(R.string.no, undeletedClickListener);

		final DialogInterface.OnClickListener aggressiveClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					aggressiveBuilder.show();
					break;

				case DialogInterface.BUTTON_NEGATIVE:
					break;
				}
			}
		};

		final AlertDialog.Builder unuploadedBuilder = new AlertDialog.Builder(
				this);
		unuploadedBuilder.setMessage(
				getResources().getString(R.string.Delete)
						+ " "
						+ getFilesUnuploaded()
						+ " "
						+ getResources().getString(R.string.unuploaded)
						+ " "
						+ (getFilesUnuploaded() == 1 ? getResources()
								.getString(R.string.file) : getResources()
								.getString(R.string.files)) + "?")
				.setPositiveButton(R.string.yes, aggressiveClickListener)
				.setNegativeButton(R.string.no, aggressiveClickListener);

		final Preference clearUnuploaded = findPreference("deleteUnuploaded");
		clearUnuploaded
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					public boolean onPreferenceClick(final Preference preference) {
						unuploadedFilesToDelete = getFilesUnuploaded();
						if (unuploadedFilesToDelete > 0) {
							unuploadedBuilder.show();
						} else {
							makeToast(getResources().getString(
									R.string.no_files_to_delete),
									Toast.LENGTH_SHORT);
						}
						return true;
					}
				});

		// AutoUpload intent launch
		final Context c = this;
		final CheckBoxPreference autoUpload = (CheckBoxPreference) findPreference("autoUploadData");
		autoUpload
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					public boolean onPreferenceChange(
							final Preference preference, final Object newValue) {
						autoUpload.setChecked((Boolean) newValue);
						if ((Boolean) newValue == true) {
							final Intent auto = new Intent(c,
									NewUploaderService.class);
							c.startService(auto);
						}
						broadcastAutoUploaderIntent(c);
						return false;
					}
				});

		final CheckBoxPreference wifiOnly = (CheckBoxPreference) findPreference("uploadWifiOnly");
		wifiOnly
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					public boolean onPreferenceChange(
							final Preference preference, final Object newValue) {
						wifiOnly.setChecked((Boolean) newValue);
						broadcastWifiUploaderIntent(c);
						return false;
					}
				});
	}
}
