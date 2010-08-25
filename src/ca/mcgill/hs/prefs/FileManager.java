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

public class FileManager extends ListActivity {
	private void makeToast(final String message, final int duration) {
		final Toast slice = Toast.makeText(getBaseContext(), message, duration);
		slice.setGravity(slice.getGravity(), slice.getXOffset(), slice
				.getYOffset() + 100);
		slice.show();
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final List<Map<String, Boolean>> listItems = new ArrayList<Map<String, Boolean>>();

		final String pathName = Environment.getExternalStorageDirectory()
				+ File.separator
				+ (String) getBaseContext().getResources().getText(
						R.string.recent_file_path);
		final File path = new File(pathName);
		final String[] files = path.list();

		for (final String s : files) {
			final Map<String, Boolean> item = new HashMap<String, Boolean>();
			item.put(s, false);
			listItems.add(item);
		}

		final String[] displayFields = new String[] { "Checked", "File Name" };
		final int[] displayViews = new int[] { R.id.bcheck, R.id.btitle };

		final CheckboxFileListAdapter checkList = new CheckboxFileListAdapter(
				this, listItems, R.layout.file_check_list, pathName,
				displayFields, displayViews);

		final ListView listView = getListView();
		final View footerView = getLayoutInflater().inflate(
				R.layout.file_check_list_footer, listView, false);
		final Button deleteButton = (Button) footerView
				.findViewById(R.id.footer_delete_button);
		final Button closeButton = (Button) footerView
				.findViewById(R.id.footer_close_button);

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

		final AlertDialog.Builder deletePrompt = new AlertDialog.Builder(this);
		deletePrompt.setMessage(R.string.delete_files_warning)
				.setPositiveButton(R.string.yes, deleteUnUploadedFiles)
				.setNegativeButton(R.string.no, deleteUnUploadedFiles);

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

		closeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				finish();
			}
		});
		listView.addFooterView(footerView);
		setListAdapter(checkList);
	}

	public class CheckboxFileListAdapter extends SimpleAdapter {

		private final Context context;
		private final List<String> listItems;
		private final List<String> checkList = new ArrayList<String>();
		private final String uploadPathName;

		public CheckboxFileListAdapter(final Context context,
				final List<Map<String, Boolean>> listItems,
				final int fileCheckList, final String uploadPathName,
				final String[] from, final int[] to) {
			super(context, listItems, fileCheckList, from, to);
			this.context = context;

			final List<String> itemList = new ArrayList<String>();
			for (final Map<String, Boolean> map : listItems) {
				itemList.addAll(map.keySet());
			}

			this.listItems = itemList;
			this.uploadPathName = uploadPathName;
		}

		public String[] getCheckedFiles() {
			return checkList.toArray(new String[0]);
		}

		@Override
		public View getView(final int pos, final View inView,
				final ViewGroup parent) {
			View v = inView;
			if (v == null) {
				final LayoutInflater inflater = (LayoutInflater) context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = inflater.inflate(R.layout.file_check_list, null);
			}
			final CheckBox cBox = (CheckBox) v.findViewById(R.id.bcheck);
			cBox.setTag(uploadPathName + listItems.get(pos));
			cBox.setText(listItems.get(pos));
			cBox.setChecked(false);
			cBox.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					final CheckBox cBox = (CheckBox) v
							.findViewById(R.id.bcheck);
					if (cBox.isChecked()) {
						// cBox.setChecked(false);
						checkList.add((String) cBox.getTag());
					} else if (!cBox.isChecked()) {
						// cBox.setChecked(true);
						checkList.remove(cBox.getTag());
					}
					System.out.println(checkList.toString());
				}
			});
			return (v);
		}
	}
}
