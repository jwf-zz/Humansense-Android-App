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
	/**
	 * Node class used only for storing labels and timestamps from this graph.
	 * 
	 * @author Cicerone Cojocaru
	 * 
	 */
	public class Node {
		public final String label;
		public final long startTime;
		public final long endTime;

		private Node(final String label, final long startTime,
				final long endTime) {
			this.label = label;
			this.startTime = startTime;
			this.endTime = endTime;
		}
	}

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
	private Rect tempRect;
	private String label;
	private final LinkedList<Rect> rectList = new LinkedList<Rect>();

	private final LinkedList<Node> labels = new LinkedList<Node>();
	// Get screen dimensions for this phone
	private int height;

	private int width;
	// Calculate graph edge locations
	private int horizontalEdge;

	private int verticalEdge;
	// The net dimensions of the graph on screen
	private int netGraphWidth;

	private int netGraphHeight;

	// The vertical padding inside the graph
	private int padding;
	// Font sizes
	private int titleSize;
	private int axisTitleSize;

	private int axisValueSize;

	// X-axis jump factor, used if more data points than pixels
	private int jumpFactor;

	// Number of data points
	private int valuesLength;
	// Trimmed array of data points, compressed from values using the jumpFactor
	private float[] trimmedValues;

	private int trimmedValuesLength;

	// Largest amplitude point
	private float maxSpike;

	// Amount to scale the curve by so it fits nicely in the graph window
	private float verticalScale;

	// The spacing of points on the graph, used only if fewer points than pixels
	private float spacing;

	// Boolean check if all vars are instantiated, prevents repeated calls of
	// calculations during onDraw()
	private boolean instantiated;

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
		instantiated = false;
	}

	private void adjustRect() {
		int rectStart;
		int rectEnd;
		if (tempRect.left < tempRect.right) {
			rectStart = tempRect.left;
			rectEnd = tempRect.right;
		} else {
			rectStart = tempRect.right;
			rectEnd = tempRect.left;
		}
		if (rectEnd > rectStart) {
			for (final Rect r : rectList) {
				if (rectStart < r.left) {
					if (rectEnd >= r.left) {
						rectEnd = r.left - 1;
					}
				} else if (rectStart < r.right) {
					rectStart = r.right + 1;
				}
			}
			tempRect.left = rectStart;
			tempRect.right = rectEnd;
		} else {
			for (final Rect r : rectList) {
				if (rectStart > r.right) {
					if (rectEnd < r.right) {
						rectEnd = r.right + 1;
					}
				} else if (rectStart >= r.left) {
					rectStart = r.left - 1;
				}
			}
			tempRect.left = rectEnd;
			tempRect.right = rectStart;
		}
	}

	private Boolean checkLabel() {
		if (label.length() > 0) {
			for (int i = 0; i < label.length(); i++) {
				final char c = label.charAt(i);
				if (!(Character.isDigit(c) || Character.isLetter(c) || c == '-' || c == '\'')) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	private void instantiate() {
		// Get screen dimensions.
		height = getHeight();
		width = getWidth();

		// Calculate graph edge locations
		horizontalEdge = width / 10;
		verticalEdge = height / 9;

		// The net dimensions of the graph on screen
		netGraphWidth = width - 2 * horizontalEdge;
		netGraphHeight = height - 2 * verticalEdge;

		// Padding inside the graph to keep curve from touching top/bottom
		padding = netGraphHeight / 20;

		// Calculate optimal font sizes
		titleSize = width / 32;
		axisTitleSize = width / 40;
		axisValueSize = height / 25;

		// Jump factor for how many points should be skipped if all don't
		// fit on
		// screen
		jumpFactor = 1;
		valuesLength = values.length;

		// Trimmed array with only points that were not skipped
		trimmedValues = new float[netGraphWidth];
		trimmedValuesLength = trimmedValues.length;

		if (valuesLength > netGraphWidth) {
			jumpFactor = (int) ((float) valuesLength / (float) netGraphWidth);
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
		} else {
			// If fewer datapoints than pixels of width, use the values
			// array
			for (final float value : values) {
				if (value > max) {
					max = value;
				} else if (value < min) {
					min = value;
				}
			}

			// Calculate spacing of points based on ratio of graph width to
			// number of values
			spacing = (float) netGraphWidth / (float) (values.length - 1);

			maxSpike = (Math.abs(max) > Math.abs(min) ? Math.abs(max) : Math
					.abs(min));
			verticalScale = ((netGraphHeight - padding) / 2) / maxSpike;
		}
		instantiated = true;
	}

	@Override
	protected void onDraw(final Canvas canvas) {
		// Must instantiate here because height and width of the canvas are
		// unavailable until onDraw is called.
		if (!instantiated) {
			instantiate();
		}

		// Draw Rectangles
		if (tempRect != null) {
			paint
					.setColor(Math.abs(tempRect.right - tempRect.left) > (width / 40) ? Color
							.rgb(0, 0, 125)
							: Color.rgb(125, 0, 0));
			canvas.drawRect(tempRect, paint);
		}
		for (final Rect r : rectList) {
			paint.setColor(Color.rgb(0, 0, 75));
			canvas.drawRect(r, paint);
			paint.setColor(Color.rgb(255, 255, 255));
			paint.setStrokeWidth(0);
			canvas.drawLine(r.right, r.top, r.right, r.bottom, paint);
			canvas.drawLine(r.left, r.bottom, r.left, r.top, paint);
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

		// Draw a different graph depending on the size of values compared to
		// netGraphWidth
		if (valuesLength > netGraphWidth) {
			for (int i = 0; i < trimmedValuesLength - 1; i++) {
				canvas.drawLine(horizontalEdge + i, height / 2
						- trimmedValues[i] * verticalScale, horizontalEdge + i
						+ 1, height / 2 - trimmedValues[i + 1] * verticalScale,
						paint);
			}
		} else {
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

		// Touch+drag rectangle code
		if (action == MotionEvent.ACTION_DOWN) {
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
					adjustRect();
				}
			}
		} else if (action == MotionEvent.ACTION_UP) {
			if (tempRect != null) {
				if (x <= leftLimit) {
					tempRect.right = leftLimit;
				} else if (x >= rightLimit) {
					tempRect.right = rightLimit;
				} else if (x == tempRect.left) {
					tempRect = null;
					return true;
				} else {
					tempRect.right = (int) x;
				}
				adjustRect();
				// Check that the rectangle is actually big enough to mean
				// anything. If not, ignore it because it may have been an
				// accidental touch. Width/40 is the threshold for minimum
				// rectangle size
				if (Math.abs(tempRect.right - tempRect.left) > (width / 40)) {
					showDialog();
				} else {
					tempRect = null;
					invalidate();
				}
			}
		}
		invalidate();
		return true;
	}

	private void showDialog() {
		label = "";
		final AlertDialog.Builder builder;
		final LayoutInflater inflater = LayoutInflater.from(this.getContext());

		final View layout = inflater.inflate(R.layout.log_name_dialog,
				(ViewGroup) findViewById(R.id.layout_root));
		layout.setPadding(10, 10, 10, 10);

		final EditText text = (EditText) layout
				.findViewById(R.id.log_name_text);
		text.setHint(R.string.mag_graph_label_hint);

		builder = new AlertDialog.Builder(this.getContext());
		builder.setView(layout).setMessage(R.string.mag_graph_label_query)
				.setCancelable(false).setPositiveButton(R.string.OK,
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog,
									final int id) {
								// If OK is pressed, save rectangle
								// and label to linked lists
								label = text.getText().toString();
								if (!checkLabel()) {
									showDialog();
									return;
								}
								rectList.add(tempRect);
								long rectStart;
								long rectEnd;
								if (tempRect.left < tempRect.right) {
									rectStart = tempRect.left;
									rectEnd = tempRect.right;
								} else {
									rectStart = tempRect.right;
									rectEnd = tempRect.left;
								}
								rectStart = (rectStart / netGraphWidth)
										* (end - start) + start;
								rectEnd = (rectEnd / netGraphWidth)
										* (end - start) + start;
								labels.add(new Node(label, rectStart, rectEnd));
								tempRect = null;
								invalidate();
							}
						}).setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog,
									final int id) {
								tempRect = null;
								invalidate();
							}
						});
		builder.show();
	}
}