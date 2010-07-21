package ca.mcgill.hs.graph;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.util.Log;
import android.view.View;

public class MagnitudeGraphView extends View {
	private final String title;
	private final float[] values;
	private final Paint paint;
	private float max;
	private float min;

	public MagnitudeGraphView(final Context context, final String title,
			final float[] values) {
		super(context);
		this.title = title;
		this.values = values;
		this.paint = new Paint();
		this.max = values[0];
		this.min = values[0];
	}

	@Override
	protected void onDraw(final Canvas canvas) {
		final int height = getHeight();
		final int width = getWidth();
		final int horizontalEdge = width / 10;
		final int verticalEdge = height / 10;
		final int netGraphWidth = width - 2 * horizontalEdge;
		final int netGraphHeight = height - 2 * verticalEdge;

		// Draw title
		paint.setTextAlign(Align.CENTER);
		paint.setTextSize(24);
		paint.setColor(Color.GREEN);
		canvas.drawText(title, width / 2, verticalEdge - 5, paint);

		// Draw X-axis label
		paint.setTextSize(18);
		paint.setTextAlign(Align.CENTER);
		canvas.drawText("Time", width / 2, height - 5, paint);

		// Draw Y-axis label
		paint.setTextAlign(Align.LEFT);
		canvas.drawText("Activity", 5, height / 2, paint);
		paint.setColor(Color.LTGRAY);
		canvas.drawLine(horizontalEdge, verticalEdge, width - horizontalEdge,
				verticalEdge, paint);
		canvas.drawLine(horizontalEdge, height - verticalEdge, width
				- horizontalEdge, height - verticalEdge, paint);
		canvas.drawLine(horizontalEdge, verticalEdge, horizontalEdge, height
				- verticalEdge, paint);
		canvas.drawLine(width - horizontalEdge, verticalEdge, width
				- horizontalEdge, height - verticalEdge, paint);

		// Draw the gridlines
		paint.setColor(Color.DKGRAY);
		for (int i = 1; i < 5; i++) {
			canvas.drawLine(horizontalEdge + i * netGraphWidth / 5,
					verticalEdge, horizontalEdge + i * netGraphWidth / 5,
					height - verticalEdge, paint);
		}

		for (int i = 1; i < 4; i++) {
			canvas.drawLine(horizontalEdge, verticalEdge + i * netGraphHeight
					/ 4, width - horizontalEdge, verticalEdge + i
					* netGraphHeight / 4, paint);
		}
		// If there are more values than pixels, crunch them to fit on the
		// screen
		float jumpFactor = 1;
		final int valuesLength = values.length;
		if (valuesLength > netGraphWidth) {
			jumpFactor = (float) valuesLength / (float) netGraphWidth;
			final float[] trimmedValues = new float[netGraphWidth];
			final float trimmedValuesLength = trimmedValues.length;
			int j = 0;
			for (float i = 0; j < trimmedValuesLength; i += jumpFactor, j++) {
				trimmedValues[j] = values[(int) i];
				if (trimmedValues[j] > max) {
					max = trimmedValues[j];
				} else if (trimmedValues[j] < min) {
					min = trimmedValues[j];
				}
			}
			final float verticalScale = netGraphHeight / 32;
			paint.setColor(Color.WHITE);
			for (int i = 0; i < trimmedValuesLength - 1; i++) {
				canvas.drawLine(horizontalEdge + i, height / 2
						- trimmedValues[i] * verticalScale, horizontalEdge + i
						+ 1, height / 2 - trimmedValues[i + 1] * verticalScale,
						paint);
			}

			Log.i("Graph", "" + values.length);
			Log.i("Graph", "" + trimmedValues.length);
			Log.i("Graph", "MIN = " + min);
			Log.i("Graph", "MAX = " + max);
			Log.i("Graph", "" + trimmedValues[0]);
			Log.i("Graph", "" + trimmedValues[trimmedValues.length - 1]);
		}

	}
}