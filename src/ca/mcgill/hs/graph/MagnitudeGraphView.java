/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.graph;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Paint.Align;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import ca.mcgill.hs.R;
import ca.mcgill.hs.util.ActivityIndex;

/**
 * This is the main view for the MagnitudeGraph activity. It draws the activity
 * graph between two timestamps, and the user can select bits of it in order to
 * label activities.
 */
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

	// The title of the graph
	private final String title;
	// These are the values and the two timestamps that are given/required to
	// draw the graph.
	private final Float[] values;
	private final int[] activities;
	private final long start;

	private final long end;

	// List of the activities present in this data set, for faster lookup in
	// activities index.
	private final int[] legendActs;

	// Set of 16 colors used for legend drawing
	private final int[] legendColors = { 0x00000000, 0xFFFF0000, 0xFF00FF00,
			0xFF0000FF, 0xFFFF8000, 0xFF00FFFF, 0xFF8000FF, 0xFFFFFF00,
			0xFF339933, 0xFF0080FF, 0xFFF5B800, 0xFF80FF00, 0xFFFF0080,
			0xFF00FF80, 0xFFFFFFFF, 0xFFB88A00, 0xFFFF00FF };
	// These are Date objects relating to the start and end timestamps
	private final Date startTime;
	private final Date endTime;

	private final SimpleDateFormat sdf;

	// The Paint object used to paint lines, rectangles and text on the canvas.
	private final Paint paint;
	// These floats are used in order to calculate the appropriate scaling of
	// the values.
	private float max;

	private float min;
	// These variables are used in order to correctly draw and label the
	// activity selections.
	private Rect tempRect;

	private Rect legendRect;

	private float legendBtnLeft;
	private float legendBtnWidth;
	private float legendBtnTop;
	private float legendBtnHeight;

	private float closeBtnLeft;
	private float closeBtnWidth;
	private float closeBtnTop;
	private float closeBtnHeight;

	private boolean showLegendButton = true;
	private boolean legendOn;

	private boolean possibleLegendPress;
	private int originalLeft;

	private String label;
	private int minRectSize;
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

	// For retrieving a single label, set this to be true.
	private boolean returnAfterFirstLabel = false;

	private ActivityIndex indexOfActivities;

	private MagnitudeGraph.GraphClosedRunnable onGraphClosed = null;

	/**
	 * The basic constructor for this object. This draws a graph with the
	 * appropriate values given and the correct timestamps.
	 * 
	 * @param context
	 *            The context in which this View will be drawn.
	 * @param title
	 *            The graph title.
	 * @param magValues
	 *            The values which will be drawn in the graph.
	 * @param start
	 *            The start timestamp for the graph.
	 * @param end
	 *            The end timestamp for the graph.
	 * @param closeAfterOneActivity
	 * @param onGraphClosed2
	 */
	public MagnitudeGraphView(final Context context, final String title,
			final Float[] magValues, final int[] activities, final long start,
			final long end, final boolean showLegendButton,
			final boolean closeAfterOneActivity,
			final MagnitudeGraph.GraphClosedRunnable onGraphClosed) {
		super(context);
		this.title = title;
		this.values = magValues;
		this.activities = activities;
		this.start = start;
		this.end = end;
		this.startTime = new Date(this.start);
		this.endTime = new Date(this.end);
		this.sdf = new SimpleDateFormat("H:mm:ss");
		this.paint = new Paint();
		paint.setAntiAlias(true);
		this.max = magValues[0];
		this.min = magValues[0];
		this.showLegendButton = showLegendButton;
		this.returnAfterFirstLabel = closeAfterOneActivity;
		this.onGraphClosed = onGraphClosed;

		instantiated = false;
		this.indexOfActivities = null;
		legendActs = new int[16];
		for (int i = 0; i < 16; i++) {
			legendActs[i] = -1;
		}
		legendRect = null;
		final boolean write = true;
		// Dummy code for testing only, used to generate a sample activity index
		// file
		if (write) {
			final String[] acts = { "no label", "walking", "running",
					"jumping", "biking", "driving", "sitting", "swimming",
					"nothing", "blah", "failing", "writing", "talking",
					"standing", "sleeping", "rollerblading", "kayaking" };
			final int[] codes = { 0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8,
					0x9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF };

			this.indexOfActivities = new ActivityIndex(acts, codes);

			try {
				final File j = new File(Environment
						.getExternalStorageDirectory(), (String) context
						.getResources().getText(R.string.activity_file_path));
				if (!j.isDirectory()) {
					if (!j.mkdirs()) {
						Log.e("Output Dir",
								"Could not create output directory!");
						return;
					}
				}
				final File file = new File(j, "ActivityIndex.aif");
				if (!file.exists()) {
					file.createNewFile();
				}
				final FileOutputStream fos = new FileOutputStream(file);
				final ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(indexOfActivities);
				oos.close();
				fos.close();
			} catch (final FileNotFoundException e) {
				e.printStackTrace();
			} catch (final IOException i) {
				i.printStackTrace();
			}
		} else {
			// Code to read the activity index file, this will stay here.
			try {

				final File j = new File(Environment
						.getExternalStorageDirectory(), (String) context
						.getResources().getText(R.string.activity_file_path));
				final File file = new File(j, "ActivityIndex.aif");
				final FileInputStream fis = new FileInputStream(file);
				final ObjectInputStream ois = new ObjectInputStream(fis);
				this.indexOfActivities = (ActivityIndex) ois.readObject();
				ois.close();
				fis.close();
			} catch (final FileNotFoundException e) {
				e.printStackTrace();
			} catch (final OptionalDataException e) {
				e.printStackTrace();
			} catch (final ClassNotFoundException e) {
				e.printStackTrace();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * Adjusts the drawn rectangle to prevent overlapping with other rectangles.
	 */
	private void adjustRect() {
		tempRect.left = originalLeft;
		for (final Rect r : rectList) {
			if (tempRect.left >= r.left && tempRect.left <= r.right
					&& tempRect.right > r.right) {
				tempRect.left = r.right + 1;
			} else if (tempRect.left >= r.left && tempRect.left <= r.right
					&& tempRect.right < r.left) {
				tempRect.left = r.left - 1;
			} else if (tempRect.left < r.left && tempRect.right >= r.left) {
				tempRect.right = r.left - 1;
			} else if (tempRect.left > r.right && tempRect.right <= r.right) {
				tempRect.right = r.right + 1;
			} else if (tempRect.left >= r.left && tempRect.left <= r.right
					&& tempRect.right >= r.left && tempRect.right <= r.right) {
				tempRect.left = tempRect.right;
			}
		}
	}

	/**
	 * Checks the text input label from the user to make sure it is not of
	 * length 0 and does not contain illegal characters.
	 * 
	 * @return true if the input string is acceptable, false otherwise
	 */
	private Boolean checkLabel() {
		if (label.length() > 0) {
			for (int i = 0; i < label.length(); i++) {
				final char c = label.charAt(i);
				if (!(Character.isLetter(c) || c == '-' || c == '\'')) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	public void disableReturnAfterFirstLabel() {
		returnAfterFirstLabel = false;
	}

	/**
	 * Draws the close button.
	 * 
	 * @param canvas
	 *            the canvas to draw on
	 */
	private void drawCloseButton(final Canvas canvas) {
		// Draw the legend button
		paint.setColor(Color.WHITE);

		// final float legendBtnRight = width - width / 160.f;
		// final float legendBtnBottom = height / 2.f + height / 13.f;
		final float textPadding = 2.f;
		final float cornerRadius = 5.f;
		final RectF outerRect = new RectF(legendBtnLeft, legendBtnTop,
				legendBtnLeft + legendBtnWidth, legendBtnTop + legendBtnHeight);
		canvas.drawRoundRect(outerRect, cornerRadius, cornerRadius, paint);

		paint.setColor(Color.BLACK);
		final RectF innerRect = new RectF(legendBtnLeft + textPadding,
				legendBtnTop + textPadding, legendBtnLeft + legendBtnWidth
						- textPadding, legendBtnTop + legendBtnHeight
						- textPadding);
		canvas.drawRoundRect(innerRect, cornerRadius, cornerRadius, paint);

		paint.setColor(Color.GREEN);
		paint.setTextSize(axisTitleSize);
		final float textX = width - horizontalEdge / 2.f;
		final float textY = height / 2.f + height / 80.f - height / 13.f;
		canvas
				.drawText(getResources().getString(
						R.string.mag_graph_close_label), textX, textY, paint);
	}

	/**
	 * Draws the curve based on the data set given.
	 * 
	 * @param canvas
	 *            the canvas to draw on
	 */
	private void drawCurve(final Canvas canvas) {
		// Set color and stroke width for graph curve
		paint.setStrokeWidth(2);
		paint.setAntiAlias(true);

		final int[] colors = { Color.rgb(255, 0, 0), Color.rgb(255, 128, 0),
				Color.rgb(255, 255, 0), Color.rgb(0, 255, 0),
				Color.rgb(255, 255, 0), Color.rgb(255, 128, 0),
				Color.rgb(255, 0, 0) };
		paint
				.setShader(new LinearGradient(
						0,
						(int) ((height / 2) - ((maxSpike > 20 ? maxSpike : 20)
								/ (maxSpike == 0 ? 1 : maxSpike) * (netGraphHeight / 2))),
						0,
						(int) ((height / 2) + ((maxSpike > 20 ? maxSpike : 20)
								/ (maxSpike == 0 ? 1 : maxSpike) * (netGraphHeight / 2))),
						colors, null, Shader.TileMode.CLAMP));

		// Draw a different graph depending on the size of values compared to
		// netGraphWidth
		int tempColor = -1;
		if (valuesLength > netGraphWidth) {
			// The actual curve is drawn
			for (int i = 0; i < trimmedValuesLength - 1; i++) {
				canvas.drawLine(horizontalEdge + i, height / 2
						- trimmedValues[i] * verticalScale, horizontalEdge + i
						+ 1, height / 2 - trimmedValues[i + 1] * verticalScale,
						paint);
			}
			paint.setShader(null);
			for (int i = 0; i < trimmedValuesLength - 1; i++) {
				// Check if same activity as before, to reduce loop
				// iterations
				if (tempColor != -1 && activities[i] == activities[i - 1]) {
					paint.setColor(tempColor);
				} else {
					// Check if the activity has already been seen
					boolean found = false;
					for (int a = 0; a < legendActs.length; a++) {
						if (legendActs[a] == activities[i]) {
							tempColor = legendColors[a];
							paint.setColor(legendColors[a]);
							found = true;
						}
					}
					// If activity not seen before in this data set, have to
					// look it up
					if (found == false) {
						for (int b = 0; b < indexOfActivities.activityCodes.length; b++) {
							if (indexOfActivities.activityCodes[b] == activities[i]) {
								int c = 0;
								while (c < legendActs.length - 1
										&& legendActs[c] != -1) {
									c++;
								}
								try {
									tempColor = legendColors[c];
									legendActs[c] = indexOfActivities.activityCodes[b];
									paint.setColor(legendColors[c]);
								} catch (final ArrayIndexOutOfBoundsException e) {
									Log
											.e("Graph Error",
													"Cannot draw any more activities, make sure you didn't send more than 16.");
								}
							}
						}
					}
				}
				// Draw a line for each point. This will create a colored bar at
				// the bottom of the graph.
				canvas.drawLine(1 + horizontalEdge + i, height - verticalEdge
						- padding / 3, 1 + horizontalEdge + i, height
						- verticalEdge, paint);
			}
		} else {
			// The actual curve is drawn
			for (int i = 0; i < values.length - 1; i++) {
				canvas.drawLine(horizontalEdge + (i * spacing), height / 2
						- values[i] * verticalScale, horizontalEdge + (i + 1)
						* spacing, height / 2 - values[i + 1] * verticalScale,
						paint);
			}
			paint.setShader(null);
			if (indexOfActivities != null) {
				for (int i = 0; i < values.length - 1; i++) {
					// Check if same activity as before, to reduce loop
					// iterations
					if (tempColor != -1 && activities[i] == activities[i - 1]) {
						paint.setColor(tempColor);
					} else {
						// Check if the activity has already been seen
						boolean found = false;
						for (int a = 0; a < legendActs.length; a++) {
							if (legendActs[a] == activities[i]) {
								tempColor = legendColors[a];
								paint.setColor(legendColors[a]);
								found = true;
							}
						}
						// If activity not seen before in this data set, have to
						// look it up
						if (found == false) {
							for (int b = 0; b < indexOfActivities.activityCodes.length; b++) {
								if (indexOfActivities.activityCodes[b] == activities[i]) {
									int c = 0;
									while (c < legendActs.length - 1
											&& legendActs[c] != -1) {
										c++;
									}
									try {
										tempColor = legendColors[c];
										legendActs[c] = indexOfActivities.activityCodes[b];
										paint.setColor(legendColors[c]);
									} catch (final ArrayIndexOutOfBoundsException e) {
										Log
												.e("Graph Error",
														"Cannot draw any more activities, make sure you didn't send more than 16.");
									}
								}
							}
						}
					}
					// Draw a colored rectangle between each point and the
					// previous point along the bottom of the graph
					canvas.drawRect(horizontalEdge + (i * spacing), height
							- verticalEdge - padding / 3, 1 + horizontalEdge
							+ (i + 1) * spacing, height - verticalEdge, paint);
				}
			}
		}
	}

	/**
	 * Draws the graph outline, axes, gridlines, titles and labels.
	 * 
	 * @param canvas
	 *            the canvas to draw on
	 */
	private void drawGraph(final Canvas canvas) {
		// Draw title
		paint.setTextAlign(Align.CENTER);
		paint.setColor(Color.rgb(0, 255, 0));
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
		paint.setAntiAlias(false);
		paint.setStrokeWidth(2);
		canvas.drawLine(horizontalEdge - 1, verticalEdge, width
				- horizontalEdge + 1, verticalEdge, paint);
		canvas.drawLine(horizontalEdge - 1, height - verticalEdge, width
				- horizontalEdge + 1, height - verticalEdge, paint);
		canvas.drawLine(horizontalEdge - 1, verticalEdge, horizontalEdge - 1,
				height - verticalEdge, paint);
		canvas.drawLine(width - horizontalEdge + 1, verticalEdge, width
				- horizontalEdge + 1, height - verticalEdge, paint);

		// Draw the gridlines
		paint.setColor(Color.DKGRAY);
		for (int i = 1; i < 5; i++) {
			canvas.drawLine(horizontalEdge + i * netGraphWidth / 5,
					verticalEdge, horizontalEdge + i * netGraphWidth / 5,
					height - verticalEdge, paint);
		}
		for (int i = 1; i < 4; i++) {
			canvas.drawLine(horizontalEdge - 1, verticalEdge + i
					* netGraphHeight / 4, width - horizontalEdge + 1,
					verticalEdge + i * netGraphHeight / 4, paint);
		}
	}

	/**
	 * Draws the legend over top of the graph.
	 * 
	 * @param canvas
	 *            the canvas to draw on
	 */
	private void drawLegend(final Canvas canvas) {
		// Draw part alpha black rectangle over graph area to fade out the
		// curve so legend can be seen easily.
		paint.setColor(0xA0000000);
		canvas.drawRect(legendRect, paint);
		for (int i = 0; i < legendActs.length && legendActs[i] != -1; i++) {
			for (int index = 0; i < indexOfActivities.activityCodes.length; index++) {
				if (indexOfActivities.activityCodes[index] == legendActs[i]) {
					paint.setColor(legendColors[i]);
					paint.setStrokeWidth(netGraphHeight / 74);
					paint.setTextAlign(Align.LEFT);
					paint.setTextSize(axisValueSize);
					canvas.drawLine(horizontalEdge + netGraphWidth / 30 + i % 4
							* (netGraphWidth / 4), height - verticalEdge
							- (netGraphHeight / 12) - i / 4
							* (netGraphHeight / 12), horizontalEdge
							+ (netGraphWidth / 30) + i % 4
							* (netGraphWidth / 4) + (netGraphWidth / 15),
							height - verticalEdge - (netGraphHeight / 12) - i
									/ 4 * (netGraphHeight / 12), paint);
					canvas.drawText(indexOfActivities.activityNames[index],
							horizontalEdge + (netGraphWidth / 30) + i % 4
									* (netGraphWidth / 4)
									+ (netGraphWidth / 15)
									+ (netGraphWidth / 128), height
									- verticalEdge - (netGraphHeight / 12) - i
									/ 4 * (netGraphHeight / 12) + height / 100,
							paint);
					break;
				}
			}
		}
		paint.setStrokeWidth(0);
	}

	/**
	 * Draws the legend button.
	 * 
	 * @param canvas
	 *            the canvas to draw on
	 */
	private void drawLegendButton(final Canvas canvas) {
		// Draw the legend button
		paint.setColor(Color.WHITE);

		// final float legendBtnRight = width - width / 160.f;
		// final float legendBtnBottom = height / 2.f + height / 13.f;
		final float textPadding = 2.f;
		final float cornerRadius = 5.f;
		final RectF outerRect = new RectF(legendBtnLeft, legendBtnTop,
				legendBtnLeft + legendBtnWidth, legendBtnTop + legendBtnHeight);
		canvas.drawRoundRect(outerRect, cornerRadius, cornerRadius, paint);

		paint.setColor(Color.BLACK);
		final RectF innerRect = new RectF(legendBtnLeft + textPadding,
				legendBtnTop + textPadding, legendBtnLeft + legendBtnWidth
						- textPadding, legendBtnTop + legendBtnHeight
						- textPadding);
		canvas.drawRoundRect(innerRect, cornerRadius, cornerRadius, paint);

		paint.setColor(Color.GREEN);
		paint.setTextSize(axisTitleSize);
		final float textX = width - horizontalEdge / 2.f;
		final float textY = height / 2.f + height / 80.f + 2.f * height / 13.f;
		canvas.drawText(getResources().getString(
				R.string.mag_graph_legend_label), textX, textY, paint);
	}

	/**
	 * Draws any rectangles that have been drawn by the user.
	 * 
	 * @param canvas
	 *            the canvas to draw on
	 */
	private void drawRectangles(final Canvas canvas) {
		if (tempRect != null) {
			paint
					.setColor(Math.abs(tempRect.right - tempRect.left) > minRectSize ? Color
							.rgb(0, 0, 125)
							: Color.rgb(125, 0, 0));
			canvas.drawRect(tempRect, paint);
		}
		paint.setAntiAlias(false);
		for (final Rect r : rectList) {
			paint.setColor(Color.rgb(0, 0, 75));
			canvas.drawRect(r, paint);
			paint.setColor(Color.LTGRAY);
			paint.setStrokeWidth(0);
			canvas.drawLine(r.left, r.bottom, r.left, r.top, paint);
			canvas.drawLine(r.right + 1, r.bottom, r.right + 1, r.top, paint);
		}
		paint.setAntiAlias(true);
	}

	public void enableReturnAfterFirstLabel() {
		returnAfterFirstLabel = true;
	}

	private boolean inCloseButton(final float x, final float y) {
		return x >= closeBtnLeft && x <= closeBtnLeft + closeBtnWidth
				&& y >= closeBtnTop && y <= closeBtnTop + closeBtnHeight;
	}

	private boolean inLegendButton(final float x, final float y) {
		return x >= legendBtnLeft && x <= legendBtnLeft + legendBtnWidth
				&& y >= legendBtnTop && y <= legendBtnTop + legendBtnHeight;
	}

	/**
	 * Instantiates all fields representing graph-drawing parameters. This
	 * method is only called once when onDraw() is first called and the view
	 * height and width are first available.
	 */
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
		padding = netGraphHeight / 15;

		// The minimum size of rectangle that can be selected to label
		minRectSize = width / 30;

		possibleLegendPress = false;

		// Calculate optimal font sizes
		titleSize = width / 32;
		axisTitleSize = width / 40;
		axisValueSize = height / 25;

		// Jump factor for how many points should be skipped if all don't
		// fit on screen
		jumpFactor = 1;
		valuesLength = values.length;

		// Trimmed array with only points that were not skipped
		trimmedValues = new float[netGraphWidth];
		trimmedValuesLength = trimmedValues.length;

		legendRect = new Rect(horizontalEdge + 1, verticalEdge + 1, width
				- horizontalEdge - 1, height - verticalEdge - padding / 3);
		legendBtnLeft = width - horizontalEdge + width / 160.f;
		legendBtnWidth = horizontalEdge - 2.f * width / 160.f;
		legendBtnTop = height / 2.f - 3.f * height / 13.f;
		legendBtnHeight = 2.f * height / 13.f;

		closeBtnLeft = width - horizontalEdge + width / 160.f;
		closeBtnWidth = horizontalEdge - 2.f * width / 160.f;
		closeBtnTop = height / 2.f + height / 13.f;
		closeBtnHeight = 2.f * height / 13.f;

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

	/**
	 * Draws the view.
	 */
	@Override
	protected void onDraw(final Canvas canvas) {
		// Must instantiate here because height and width of the canvas are
		// unavailable until onDraw is called.
		if (!instantiated) {
			instantiate();
		}

		drawRectangles(canvas);
		drawGraph(canvas);
		drawCurve(canvas);
		// if (showLegendButton) {
		drawLegendButton(canvas);
		// }
		drawCloseButton(canvas);

		// If the legend button has been pressed, draw the legend
		if (legendOn) {
			drawLegend(canvas);
		}
	}

	/**
	 * This method gets called whenever a touch-screen event happens, such as
	 * when the user touches the screen with their finger.
	 */
	@Override
	public boolean onTouchEvent(final MotionEvent event) {
		final int action = event.getAction();
		final float x = event.getX();
		final float y = event.getY();
		final int leftLimit = horizontalEdge + 1;
		final int rightLimit = width - horizontalEdge - 1;
		// Touch+drag rectangle code
		if (action == MotionEvent.ACTION_DOWN) {
			possibleLegendPress = false;
			if (showLegendButton && inLegendButton(x, y)) {
				possibleLegendPress = true;
			}
			tempRect = new Rect((int) x, height - verticalEdge, (int) x,
					verticalEdge);
			if (x <= leftLimit) {
				tempRect.left = leftLimit;
				tempRect.right = tempRect.left;
			} else if (x >= rightLimit) {
				tempRect.left = rightLimit;
				tempRect.right = tempRect.left;
			}
			originalLeft = tempRect.left;
		} else if (action == MotionEvent.ACTION_MOVE) {
			if (tempRect != null) {
				if (x <= leftLimit) {
					tempRect.right = leftLimit;
				} else if (x >= rightLimit) {
					tempRect.right = rightLimit;
				} else {
					tempRect.right = (int) event.getX();
				}
				adjustRect();
			}
		} else if (action == MotionEvent.ACTION_UP) {
			if (possibleLegendPress == true && inLegendButton(x, y)) {
				if (legendOn == false) {
					legendOn = true;
				} else {
					legendOn = false;
				}
				possibleLegendPress = false;
			}
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
				if (Math.abs(tempRect.right - tempRect.left) > minRectSize) {
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

	/**
	 * Shows the text input dialog for user labelling.
	 */
	private void showDialog() {
		label = "";
		final AlertDialog.Builder builder;
		final LayoutInflater inflater = LayoutInflater.from(this.getContext());

		final View thisView = this;
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
									final Toast slice = Toast
											.makeText(
													getContext(),
													R.string.mag_graph_incorrect_input_toast,
													Toast.LENGTH_LONG);
									slice.setGravity(slice.getGravity(), slice
											.getXOffset(), height - 2
											* verticalEdge);
									slice.show();
									showDialog();
									return;
								}

								if (tempRect.left > tempRect.right) {
									final int tempLeft = tempRect.right;
									tempRect.right = tempRect.left;
									tempRect.left = tempLeft;
								}

								rectList.add(tempRect);
								final long rectStart = start
										+ (long) (((float) tempRect.left / (float) netGraphWidth) * (end - start));
								final long rectEnd = start
										+ (long) (((float) tempRect.right / (float) netGraphWidth) * (end - start));
								final int indexStart = (int) ((float) (rectStart - start)
										/ (float) (end - start) * activities.length);
								final int indexEnd = (int) ((float) (rectEnd - start)
										/ (float) (end - start) * activities.length);
								labels
										.add(new Node(label, indexStart,
												indexEnd));
								tempRect = null;
								if (returnAfterFirstLabel) {
									if (onGraphClosed != null) {
										// Log.d("MagnitudeGraphView", "Start: "
										// + start + ", End: " + end
										// + ", indexStart: " + indexStart
										// + ", indexEnd: " + indexEnd
										// + ", rectStart: " + rectStart
										// + ", rectEnd: " + rectEnd);
										final float[] data = new float[indexEnd
												- indexStart + 1];
										for (int i = 0; i < indexEnd
												- indexStart + 1; i++) {
											data[i] = values[i + indexStart];
										}
										onGraphClosed.setLabelData(label, data);
										Log.d("MagnitudeGraphView", "Start: "
												+ rectStart + ", End: "
												+ rectEnd);
										thisView.post(onGraphClosed);
									}
									((MagnitudeGraph) thisView.getContext())
											.finish();
								}
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