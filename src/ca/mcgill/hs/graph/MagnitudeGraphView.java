package ca.mcgill.hs.graph;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Align;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import ca.mcgill.hs.R;

public class MagnitudeGraphView extends View {
	private final String title;
	private final float[] values;
	private final long start;
	private final long end;
	private final Date startTime;
	private final Date endTime;
	private final SimpleDateFormat sdf;
	private final Paint paint;
	private float max;
	private float min;
	private float rectStart;
	private float rectEnd;
	private Rect tempRect;
	private final LinkedList<Rect> rectList = new LinkedList<Rect>();
	private final LinkedList<String> labels = new LinkedList<String>();

	// Get screen dimensions for this phone
	int height;
	int width;

	// Calculate graph edge locations
	int horizontalEdge;
	int verticalEdge;

	public MagnitudeGraphView(final Context context, final String title,
			final float[] values, final long start, final long end) {
		super(context);
		this.title = title;
		this.values = values;
		this.start = start;
		this.end = end;
		this.startTime = new Date(this.start);
		this.endTime = new Date(this.end);
		this.sdf = new SimpleDateFormat("H:mm:ss");
		this.paint = new Paint();
		paint.setAntiAlias(true);
		this.max = values[0];
		this.min = values[0];
	}

	@Override
	protected void onDraw(final Canvas canvas) {

		// Get screen dimensions.
		height = getHeight();
		width = getWidth();

		// Calculate graph edge locations
		horizontalEdge = width / 10;
		verticalEdge = height / 9;

		// The net dimensions of the graph on screen
		final int netGraphWidth = width - 2 * horizontalEdge;
		final int netGraphHeight = height - 2 * verticalEdge;

		// Padding inside the graph to keep curve from touching top/bottom
		final int padding = netGraphHeight / 20;

		// Calculate optimal font sizes
		final int titleSize = width / 32;
		final int axisTitleSize = width / 40;
		final int axisValueSize = height / 25;

		// Jump factor for how many points should be skipped if all don't fit on
		// screen
		float jumpFactor = 1;
		final int valuesLength = values.length;

		// Trimmed array with only points that were not skipped
		final float[] trimmedValues = new float[netGraphWidth];
		final float trimmedValuesLength = trimmedValues.length;

		// Value by which points should be scaled in order to fit the graph
		// nicely
		final float verticalScale;

		// Point of maximum amplitude, either positive or negative
		final float maxSpike;

		// Draw Rectangles
		paint.setColor(Color.rgb(0, 0, 125));
		if (tempRect != null) {
			canvas.drawRect(tempRect, paint);
		}
		paint.setColor(Color.rgb(0, 0, 75));
		for (final Rect r : rectList) {
			canvas.drawRect(r, paint);
		}

		// Draw title
		paint.setTextAlign(Align.CENTER);
		paint.setColor(Color.GREEN);
		paint.setTextSize(titleSize);
		canvas.drawText(title, width / 2, verticalEdge - titleSize / 2, paint);

		// Draw X-axis title
		paint.setTextAlign(Align.CENTER);
		paint.setTextSize(axisTitleSize);
		canvas.drawText(
				getResources().getString(R.string.mag_graph_time_label),
				width / 2, height - height / 80, paint);

		// Draw Y-axis title
		paint.setTextAlign(Align.LEFT);
		paint.setColor(Color.GREEN);
		canvas.drawText(getResources().getString(
				R.string.mag_graph_magnitude_label), width / 160, height / 2,
				paint);

		// Draw X-axis tick values
		paint.setTextAlign(Align.CENTER);
		paint.setColor(Color.LTGRAY);
		paint.setTextSize(axisValueSize);

		canvas.drawText(sdf.format(startTime), horizontalEdge, height
				- verticalEdge + height / 20, paint);
		canvas.drawText(sdf.format(endTime), width - horizontalEdge, height
				- verticalEdge + height / 20, paint);
		final Date axisValueTime = new Date();
		for (int i = 1; i < 5; i++) {
			axisValueTime.setTime(start + i * (end - start) / 5);
			canvas.drawText(sdf.format(axisValueTime), (horizontalEdge + i
					* netGraphWidth / 5), height - verticalEdge + height / 20,
					paint);
		}

		// Draw the outline of the graph
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

		// Set color and stroke width for graph curve
		paint.setColor(Color.rgb(255, 128, 0));
		paint.setStrokeWidth(2);

		// If there are more values than pixels, crunch them to fit on the
		// screen
		if (valuesLength > netGraphWidth) {
			jumpFactor = (float) valuesLength / (float) netGraphWidth;
			int j = 0;
			for (float i = 0; j < trimmedValuesLength; i += jumpFactor, j++) {
				trimmedValues[j] = values[(int) i];
				if (trimmedValues[j] > max) {
					max = trimmedValues[j];
				} else if (trimmedValues[j] < min) {
					min = trimmedValues[j];
				}
			}

			// Calculate scaling coefficients
			maxSpike = (Math.abs(max) > Math.abs(min) ? Math.abs(max) : Math
					.abs(min));
			verticalScale = ((netGraphHeight - padding) / 2) / maxSpike;

			// Draw the graph
			for (int i = 0; i < trimmedValuesLength - 1; i++) {
				canvas.drawLine(horizontalEdge + i, height / 2
						- trimmedValues[i] * verticalScale, horizontalEdge + i
						+ 1, height / 2 - trimmedValues[i + 1] * verticalScale,
						paint);
			}
		} else {
			// If fewer datapoints than pixels of width, use the values array
			for (final float value : values) {
				if (value > max) {
					max = value;
				} else if (value < min) {
					min = value;
				}
			}

			// Calculate spacing of points based on ratio of graph width to
			// number of values
			final float spacing = (float) netGraphWidth
					/ (float) (values.length - 1);

			maxSpike = (Math.abs(max) > Math.abs(min) ? Math.abs(max) : Math
					.abs(min));
			verticalScale = ((netGraphHeight - padding) / 2) / maxSpike;

			// Draw the graph
			for (int i = 0; i < values.length - 1; i++) {
				canvas.drawLine(horizontalEdge + (i * spacing), height / 2
						- values[i] * verticalScale, horizontalEdge + (i + 1)
						* spacing, height / 2 - values[i + 1] * verticalScale,
						paint);
			}
		}

	}

	@Override
	public boolean onTouchEvent(final MotionEvent event) {
		final int action = event.getAction();
		final float x = event.getX();
		final int leftLimit = horizontalEdge + 1;
		final int rightLimit = width - horizontalEdge - 1;
		if (action == MotionEvent.ACTION_DOWN) {
			rectStart = x;
			tempRect = new Rect((int) x, height - verticalEdge, (int) x,
					verticalEdge);
			if (x <= leftLimit) {
				tempRect.left = leftLimit;
				tempRect.right = tempRect.left;
			} else if (x >= rightLimit) {
				tempRect.left = rightLimit;
				tempRect.right = tempRect.left;
			}
		} else if (action == MotionEvent.ACTION_MOVE) {
			if (tempRect != null) {
				if (x <= leftLimit) {
					tempRect.right = leftLimit;
				} else if (x >= rightLimit) {
					tempRect.right = rightLimit;
				} else {
					tempRect.right = (int) event.getX();
				}
			}
		} else if (action == MotionEvent.ACTION_UP) {
			if (tempRect != null) {
				if (x <= leftLimit) {
					rectEnd = leftLimit;
					tempRect.right = leftLimit;
				} else if (x >= rightLimit) {
					rectEnd = rightLimit;
					tempRect.right = rightLimit;
				} else if (x == tempRect.left) {
					tempRect = null;
					return true;
				} else {
					rectEnd = x;
					tempRect.right = (int) x;
				}
				// Check that the rectangle is actually big enough to mean
				// anything. If not, ignore it because it may have been an
				// accidental touch. Width/40 is the threshold for minimum
				// rectangle size
				if (Math.abs(rectEnd - rectStart) > (width / 40)) {
					AlertDialog.Builder builder;
					final LayoutInflater inflater = LayoutInflater.from(this
							.getContext());
					final View layout = inflater.inflate(
							R.layout.log_name_dialog,
							(ViewGroup) findViewById(R.id.layout_root));
					layout.setPadding(10, 10, 10, 10);
					final EditText text = (EditText) layout
							.findViewById(R.id.log_name_text);
					text.setHint(R.string.mag_graph_label_hint);
					builder = new AlertDialog.Builder(this.getContext());
					builder.setView(layout).setMessage(
							R.string.mag_graph_label_query)
							.setCancelable(false).setPositiveButton(
									R.string.OK,
									new DialogInterface.OnClickListener() {
										public void onClick(
												final DialogInterface dialog,
												final int id) {

											final String label = text.getText()
													.toString();
											rectList.add(tempRect);
											labels.add(label);
											tempRect = null;
											invalidate();
											dialog.dismiss();
										}
									}).setNegativeButton(R.string.cancel,
									new DialogInterface.OnClickListener() {
										public void onClick(
												final DialogInterface dialog,
												final int id) {
											tempRect = null;
											invalidate();
											dialog.cancel();
										}
									});
					builder.show();
				} else {
					tempRect = null;
					invalidate();
				}
			}
		}
		invalidate();
		return true;
	}
}