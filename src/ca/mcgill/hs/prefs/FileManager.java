package ca.mcgill.hs.prefs;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import ca.mcgill.hs.R;

/**
 * Provides a simple file manager for a preference panel. Shows file names with
 * checkboxes next to them.
 */
public class FileManager extends ListActivity {

	public class CheckboxFileListAdapter extends SimpleAdapter {

		private final Context context;

		/**
		 * This is the list of items to show, so it is the shortened filenames,
		 * not the full path names
		 */
		private final List<String> listItems;

		/**
		 * This is the list of files that have been checked off, and this list
		 * includes the full pathnames of the files
		 */
		private final List<String> checkList = new ArrayList<String>();

		/**
		 * This is the full path where the uploaded directory lives.
		 */
		private final String uploadPathName;

		public CheckboxFileListAdapter(final Context context,
				final List<Map<String, Boolean>> listItems,
				final int fileCheckList, final String uploadPathName,
				final String[] from, final int[] to) {
			super(context, listItems, fileCheckList, from, to);
			this.context = context;

			/*
			 * The SimpleAdapter class requires the funny List<Map<?,?>> object,
			 * but for our purposes it is much easier to just keep a list of the
			 * items, so we pull out all of the strings from the listItems.
			 */
			final List<String> itemList = new ArrayList<String>();
			for (final Map<String, Boolean> map : listItems) {
				itemList.addAll(map.keySet());
			}
			this.listItems = itemList;
			this.uploadPathName = uploadPathName;
		}

		/**
		 * Gets the full pathnames of the files that have been checked off in
		 * the list
		 * 
		 * @return An array where each element is the full pathname of a file
		 *         that has been checked off
		 */
		public String[] getCheckedFiles() {
			return checkList.toArray(new String[0]);
		}

		/**
		 * This actually does the work of showing the checkboxes.
		 */
		@Override
		public View getView(final int pos, final View inView,
				final ViewGroup parent) {
			View v = inView;
			if (v == null) {
				/*
				 * R.layout.file_check_list is the layout for a single row in
				 * the checkbox list
				 */
				final LayoutInflater inflater = (LayoutInflater) context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = inflater.inflate(R.layout.file_check_list, null);
			}
			final CheckBox cBox = (CheckBox) v.findViewById(R.id.bcheck);

			/*
			 * We set the tag for the checkbox (like the value) to be the full
			 * pathname of the file
			 */
			cBox.setTag(uploadPathName + listItems.get(pos));
			/*
			 * The text is just the filename, which is much shorter
			 */
			cBox.setText(listItems.get(pos));
			/*
			 * Start with everything unchecked
			 */
			cBox.setChecked(false);

			/*
			 * Instead of collecting the checked items at the end, we maintain
			 * the list of checked items whenever an item is checked or
			 * unchecked
			 */
			cBox.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					final CheckBox cBox = (CheckBox) v
							.findViewById(R.id.bcheck);
					if (cBox.isChecked()) {
						checkList.add((String) cBox.getTag());
					} else if (!cBox.isChecked()) {
						checkList.remove(cBox.getTag());
					}
				}
			});
			return (v);
		}
	}

	public static final File RECENT_FILES_DIRECTORY = new File(Environment
			.getExternalStorageDirectory(), "hsandroidapp/data/recent/");
	public static final File UPLOADED_FILES_DIRECTORY = new File(Environment
			.getExternalStorageDirectory()
			+ "hsandroidapp/data/uploaded/");

	/**
	 * Helper function for displaying brief messages
	 * 
	 * @param message
	 *            message to display
	 * @param duration
	 *            duration to show the text for, should be DURATION_SHORT or
	 *            DURATION_LONG
	 * 
	 */
	private void makeToast(final String message, final int duration) {
		final Toast slice = Toast.makeText(getBaseContext(), message, duration);
		slice.setGravity(slice.getGravity(), slice.getXOffset(), slice
				.getYOffset() + 100);
		slice.show();
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/*
		 * The SimpleAdapter constructer requires this funny list of maps. In
		 * our case, each row in the adapter has only one value, and so our Maps
		 * only have one item.
		 */
		final List<Map<String, Boolean>> listItems = new ArrayList<Map<String, Boolean>>();

		final String pathName = Environment.getExternalStorageDirectory()
				+ File.separator
				+ (String) getBaseContext().getResources().getText(
						R.string.recent_file_path);
		final File path = new File(pathName);
		if (!path.exists()) {
			path.mkdirs();
		}

		/*
		 * Collect all of the files in the unuploaded directory
		 */
		final String[] files = path.list();

		/*
		 * Build a map for each unuploaded file. The Boolean parameter is never
		 * used.
		 */
		if (files != null) {
			for (final String s : files) {
				final Map<String, Boolean> item = new HashMap<String, Boolean>();
				item.put(s, false);
				listItems.add(item);
			}
		}
		/*
		 * The titles of the columns in our view. In our case we have a column
		 * of checkboxes, and a column of file names. However, these titles are
		 * never displayed.
		 */
		final String[] displayFields = new String[] { "Checked", "File Name" };
		/*
		 * The view id's corresponding to the titles in displayFields.
		 */
		final int[] displayViews = new int[] { R.id.bcheck, R.id.btitle };

		final CheckboxFileListAdapter checkList = new CheckboxFileListAdapter(
				this, listItems, R.layout.file_check_list, pathName,
				displayFields, displayViews);

		/*
		 * Add a footer with the delete and cancel buttons.
		 */
		final ListView listView = getListView();
		final View footerView = getLayoutInflater().inflate(
				R.layout.file_check_list_footer, listView, false);
		final Button deleteButton = (Button) footerView
				.findViewById(R.id.footer_delete_button);
		final Button closeButton = (Button) footerView
				.findViewById(R.id.footer_close_button);

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

		/*
		 * It is important that this is done before the checkList is set as the
		 * list adapter
		 */
		listView.addFooterView(footerView);

		setListAdapter(checkList);
	}
}
