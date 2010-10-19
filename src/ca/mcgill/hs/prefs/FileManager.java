package ca.mcgill.hs.prefs;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import ca.mcgill.hs.R;

/**
 * Provides a simple file manager for a preference panel. Shows file names with
 * checkboxes next to them.
 */
public abstract class FileManager extends ListActivity {

	public class CheckboxFileListAdapter extends SimpleAdapter {

		private final Context context;

		/**
		 * This is the list of items to show, so it is the shortened filenames,
		 * not the full path names
		 */
		private final List<CheckListEntry> listItems = new ArrayList<CheckListEntry>();

		/**
		 * This is the list of files that have been checked off, and this list
		 * includes the full pathnames of the files
		 */
		private final Set<String> checkList = new HashSet<String>();

		/**
		 * This is the full path where the uploaded directory lives.
		 */
		private final String pathName;

		public CheckboxFileListAdapter(final Context context,
				final List<Map<String, Boolean>> listItems,
				final int fileCheckList, final String pathName,
				final String[] from, final int[] to) {
			super(context, listItems, fileCheckList, from, to);
			this.context = context;

			/*
			 * The SimpleAdapter class requires the funny List<Map<?,?>> object,
			 * but for our purposes it is much easier to just keep a list of the
			 * items, so we pull out all of the entries from the listItems.
			 */
			for (final Map<String, Boolean> map : listItems) {
				for (final Entry<String, Boolean> entry : map.entrySet()) {
					this.listItems.add(new CheckListEntry(entry.getKey(), entry
							.getValue()));
				}
			}
			this.pathName = pathName;
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
				v = ((LayoutInflater) context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
						.inflate(R.layout.file_check_list, null);
			}
			final CheckBox cBox = (CheckBox) v.findViewById(R.id.bcheck);

			final CheckListEntry entry = listItems.get(pos);
			/*
			 * We set the tag for the checkbox (like the value) to be the full
			 * pathname of the file
			 */
			cBox.setTag(new File(pathName, entry.filename).getAbsolutePath());
			if (entry.checked) {
				checkList.add((String) cBox.getTag());
			}

			/*
			 * The text is just the filename, which is much shorter
			 */
			cBox.setText(entry.filename);
			/*
			 * Start with everything unchecked
			 */
			cBox.setChecked(entry.checked);

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

	protected class CheckListEntry {
		public String filename;
		public boolean checked;

		public CheckListEntry(final String filename, final boolean checked) {
			this.filename = filename;
			this.checked = checked;
		}
	}

	private static final String TAG = "FileManager";

	protected abstract CheckListEntry[] getCheckListEntries(File path);

	protected abstract View getFooterView(ListView listView,
			CheckboxFileListAdapter checkList);

	protected abstract String getPathName();

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
	protected void makeToast(final String message, final int duration) {
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

		final String pathName = getPathName();
		Log.d(TAG, "Listing files for directory: " + pathName);
		final File path = new File(pathName);
		if (!path.exists()) {
			path.mkdirs();
		}

		/*
		 * Collect all of the files in the unuploaded directory
		 */
		final CheckListEntry[] entries = getCheckListEntries(path);
		Log.d(TAG, "\tFound " + entries.length + " entries.");

		/*
		 * Build a map for each unuploaded file. The Boolean parameter is never
		 * used.
		 */
		if (entries != null) {
			for (final CheckListEntry s : entries) {
				final Map<String, Boolean> item = new HashMap<String, Boolean>();
				item.put(s.filename, s.checked);
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
		final View footerView = getFooterView(listView, checkList);

		/*
		 * It is important that this is done before the checkList is set as the
		 * list adapter
		 */
		listView.addFooterView(footerView);

		setListAdapter(checkList);
	}
}
