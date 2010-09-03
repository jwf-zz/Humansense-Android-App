package ca.mcgill.hs.prefs;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class SeekBarPreference extends DialogPreference implements
		SeekBar.OnSeekBarChangeListener {

	private static final String MAX_VALUE_ID = "max";
	private static final String DEFAULT_VALUE_ID = "defaultValue";
	private static final String DIALOG_TEXT_ID = "text";
	private static final String DIALOG_MESSAGE_ID = "dialogMessage";
	private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

	private SeekBar mSeekBar;
	private TextView mSplashText, mValueText;
	private final Context mContext;

	private final String mDialogMessage, mSuffix;
	private final int mDefault;

	private int mMax, mValue = 0;

	public SeekBarPreference(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		mContext = context;

		mDialogMessage = attrs.getAttributeValue(ANDROID_NS, DIALOG_MESSAGE_ID);
		mSuffix = attrs.getAttributeValue(ANDROID_NS, DIALOG_TEXT_ID);
		mDefault = attrs.getAttributeIntValue(ANDROID_NS, DEFAULT_VALUE_ID, 0);
		mMax = attrs.getAttributeIntValue(ANDROID_NS, MAX_VALUE_ID, 100);

	}

	public int getMax() {
		return mMax;
	}

	public int getProgress() {
		return mValue;
	}

	@Override
	protected void onBindDialogView(final View v) {
		super.onBindDialogView(v);
		mSeekBar.setMax(mMax);
		mSeekBar.setProgress(mValue);
	}

	@Override
	protected View onCreateDialogView() {
		LinearLayout.LayoutParams params;
		final LinearLayout layout = new LinearLayout(mContext);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setPadding(6, 6, 6, 6);

		mSplashText = new TextView(mContext);
		if (mDialogMessage != null) {
			mSplashText.setText(mDialogMessage);
		}
		layout.addView(mSplashText);

		mValueText = new TextView(mContext);
		mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
		mValueText.setTextSize(32);
		params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		layout.addView(mValueText, params);

		mSeekBar = new SeekBar(mContext);
		mSeekBar.setOnSeekBarChangeListener(this);
		layout.addView(mSeekBar, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));

		if (shouldPersist()) {
			mValue = getPersistedInt(mDefault);
		}

		mSeekBar.setMax(mMax);
		mSeekBar.setProgress(mValue);
		return layout;
	}

	public void onProgressChanged(final SeekBar seek, final int value,
			final boolean fromTouch) {
		final String t = String.valueOf(value);
		mValueText.setText(mSuffix == null ? t : t.concat(mSuffix));
		if (shouldPersist()) {
			persistInt(value);
		}
		callChangeListener(new Integer(value));
	}

	@Override
	protected void onSetInitialValue(final boolean restore,
			final Object defaultValue) {
		super.onSetInitialValue(restore, defaultValue);
		if (restore) {
			mValue = shouldPersist() ? getPersistedInt(mDefault) : 0;
		} else {
			mValue = (Integer) defaultValue;
		}
	}

	public void onStartTrackingTouch(final SeekBar seek) {
	}

	public void onStopTrackingTouch(final SeekBar seek) {
	}

	public void setMax(final int max) {
		mMax = max;
	}

	public void setProgress(final int progress) {
		mValue = progress;
		if (mSeekBar != null) {
			mSeekBar.setProgress(progress);
		}
	}

}
