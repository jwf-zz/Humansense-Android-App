package ca.mcgill.hs.prefs;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.SimpleAdapter;
import ca.mcgill.hs.R;

public class FileManager extends ListActivity {
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

		setListAdapter(new CheckboxFileListAdapter(this, listItems,
				R.layout.file_check_list, pathName, displayFields, displayViews));
	}
}
