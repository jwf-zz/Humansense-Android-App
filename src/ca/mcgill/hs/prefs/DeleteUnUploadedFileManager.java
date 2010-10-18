package ca.mcgill.hs.prefs;

import java.io.File;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import ca.mcgill.hs.HSAndroid;
import ca.mcgill.hs.R;

public class DeleteUnUploadedFileManager extends FileManager {
	public static final File RECENT_FILES_DIRECTORY = new File(Environment
			.getExternalStorageDirectory(), HSAndroid
			.getAppString(R.string.recent_file_path));
	public static final File UPLOADED_FILES_DIRECTORY = new File(Environment
			.getExternalStorageDirectory()
			+ HSAndroid.getAppString(R.string.uploaded_file_path));

	@Override
	protected CheckListEntry[] getCheckListEntries(final File path) {
		// Return all files in the path
		final String[] files = path.list();
		final CheckListEntry[] entries = new CheckListEntry[files.length];
		for (int i = 0; i < files.length; i++) {
			entries[i] = new CheckListEntry(files[i], false);
		}
		return entries;
	}

	@Override
	protected View getFooterView(final ListView listView,
			final CheckboxFileListAdapter checkList) {
		final View footerView = getLayoutInflater().inflate(
				R.layout.manage_unuploaded_files_check_list_footer, listView,
				false);
		final Button deleteButton = (Button) footerView
				.findViewById(R.id.manage_unuploaded_footer_delete_button);
		final Button closeButton = (Button) footerView
				.findViewById(R.id.manage_unuploaded_footer_close_button);

		/*
		 * This does the work of deleting the files, and is called after the
		 * delete prompt is shown, whether the user clicks YES or NO
		 */
		final DialogInterface.OnClickListener deleteUnUploadedFiles = new DialogInterface.OnClickListener() {

			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					final String[] filesToDelete = checkList.getCheckedFiles();
					for (final String fname : filesToDelete) {
						final File f = new File(fname);
						if (f.exists()) {
							Log.d("HSFileManager", "Deleting file "
									+ f.getName());
							f.delete();
						}
					}
					/*
					 * Alert the user once the files have been deleted.
					 */
					makeToast(getResources().getString(R.string.Deleted)
							+ " "
							+ filesToDelete.length
							+ " "
							+ (filesToDelete.length == 1 ? getResources()
									.getString(R.string.file) : getResources()
									.getString(R.string.files)) + ".",
							Toast.LENGTH_SHORT);

					finish();
					break;

				case DialogInterface.BUTTON_NEGATIVE:
					break;
				}
			}
		};

		/*
		 * Prompt the user before deleting the files
		 */
		final AlertDialog.Builder deletePrompt = new AlertDialog.Builder(this);
		deletePrompt.setMessage(R.string.delete_files_warning)
				.setPositiveButton(R.string.yes, deleteUnUploadedFiles)
				.setNegativeButton(R.string.no, deleteUnUploadedFiles);

		/*
		 * Connect the delete button to the prompt, which then calls the
		 * deleteUnUploaded dialog interface to do the real work
		 */
		deleteButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				final String[] filesToDelete = checkList.getCheckedFiles();
				if (filesToDelete.length == 0) {
					makeToast(getResources().getString(
							R.string.no_files_to_delete), Toast.LENGTH_SHORT);
					return;
				}
				deletePrompt.show();
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
	public String getPathName() {
		return RECENT_FILES_DIRECTORY.getAbsolutePath();
	}
}
