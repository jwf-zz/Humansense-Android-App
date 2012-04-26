/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.prefs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import ca.mcgill.hs.HSAndroid;
import ca.mcgill.hs.R;
import ca.mcgill.hs.util.Log;

/**
 * Allows the user to select which models are used by the TDE classifier.
 * 
 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
 * 
 */
public class ManageModelsFileManager extends FileManager {

	/** Directory where the models live. */
	public static final File MODELS_DIR = new File(
			HSAndroid.getStorageDirectory(),
			HSAndroid.getAppString(R.string.models_path));

	/** File containing the active models, one per line. */
	public static final File MODELS_INI_FILE = new File(
			HSAndroid.getStorageDirectory(),
			HSAndroid.getAppString(R.string.model_ini_path));

	private static final String TAG = "ManageModelsFileManager";

	@Override
	protected CheckListEntry[] getCheckListEntries(final File path) {
		// Only return .dmp files, don't return the models.ini file
		final FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(final File dir, final String filename) {
				if (filename.endsWith(".dmp")) {
					return true;
				} else {
					return false;
				}
			}
		};
		final String[] files = path.list(filter);
		final CheckListEntry[] entries = new CheckListEntry[files.length];
		final HashSet<String> checkedEntries = new HashSet<String>();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(MODELS_INI_FILE));
			String line;
			while ((line = reader.readLine()) != null) {
				final File f = new File(line);
				Log.d(TAG, "\tAdding checked file " + f.getName());
				checkedEntries.add(f.getName());
			}
		} catch (final FileNotFoundException e) {
			// Do Nothing
		} catch (final IOException e) {
			// Do Nothing
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (final IOException e) {
					// Do Nothing.
				}
			}
		}
		// Create the entries.
		for (int i = 0; i < files.length; i++) {
			entries[i] = new CheckListEntry(files[i],
					checkedEntries.contains(files[i]));
		}
		return entries;
	}

	@Override
	protected View getFooterView(final ListView listView,
			final CheckboxFileListAdapter checkList) {

		final View footerView = getLayoutInflater().inflate(
				R.layout.model_files_check_list_footer, listView, false);
		final Button saveButton = (Button) footerView
				.findViewById(R.id.model_files_footer_save_button);
		final Button closeButton = (Button) footerView
				.findViewById(R.id.model_files_footer_close_button);

		/*
		 * Set up the SAVE button
		 */
		saveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				FileWriter writer = null;
				try {
					writer = new FileWriter(MODELS_INI_FILE);
					final String[] models = checkList.getCheckedFiles();
					for (final String model : models) {
						writer.write(model + "\n");
					}
					writer.close();
				} catch (final IOException e) {
					Log.e(TAG, e);
				} finally {
					if (writer != null) {
						try {
							writer.close();
						} catch (final IOException e) {
							// TODO Auto-generated catch block
							Log.e(TAG, e);
						}
					}
				}
				finish();
			}
		});

		/*
		 * Set up the CLOSE button
		 */
		closeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				finish();
			}
		});

		return footerView;
	}

	@Override
	protected String getPathName() {
		return MODELS_DIR.getAbsolutePath();
	}
}
