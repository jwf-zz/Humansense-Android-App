package ca.mcgill.hs.prefs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;

import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import ca.mcgill.hs.HSAndroid;
import ca.mcgill.hs.R;

public class ManageModelsFileManager extends FileManager {

	public static final File MODELS_DIR = new File(Environment
			.getExternalStorageDirectory(), HSAndroid
			.getAppString(R.string.models_path));
	public static final File MODELS_INI_FILE = new File(Environment
			.getExternalStorageDirectory(), HSAndroid
			.getAppString(R.string.model_ini_path));
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
		for (int i = 0; i < files.length; i++) {
			entries[i] = new CheckListEntry(files[i], checkedEntries
					.contains(files[i]));
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
		 * Set up the CLOSE button
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
					e.printStackTrace();
				} finally {
					if (writer != null) {
						try {
							writer.close();
						} catch (final IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
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
