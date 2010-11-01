/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.uploader;

public class Constants {
	public static final String TAG = "UploadManager";

	/** The default user agent used for uploads */
	public static final String DEFAULT_USER_AGENT = "HumansenseDownloadManager";

	/** The buffer size used to stream the data */
	public static final int BUFFER_SIZE = 4096;

	/**
	 * The minimum amount of progress that has to be done before the progress
	 * bar gets updated
	 */
	public static final int MIN_PROGRESS_STEP = 4096;

	/**
	 * The minimum amount of time that has to elapse before the progress bar
	 * gets updated, in ms
	 */
	public static final long MIN_PROGRESS_TIME = 1500;

	/**
	 * The number of times that the download manager will retry its network
	 * operations when no progress is happening before it gives up.
	 */
	public static final int MAX_RETRIES = 5;

	/**
	 * The minimum amount of time that the download manager accepts for a
	 * Retry-After response header with a parameter in delta-seconds.
	 */
	public static final int MIN_RETRY_AFTER = 30; // 30s

	/**
	 * The maximum amount of time that the download manager accepts for a
	 * Retry-After response header with a parameter in delta-seconds.
	 */
	public static final int MAX_RETRY_AFTER = 24 * 60 * 60; // 24h

	/**
	 * The time between a failure and the first retry after an IOException. Each
	 * subsequent retry grows exponentially, doubling each time. The time is in
	 * seconds.
	 */
	public static final int RETRY_FIRST_DELAY = 30; // 30s

	public static final int STATUS_PENDING = 0x0;
	public static final int STATUS_RUNNING = 0x1;
	public static final int STATUS_RUNNING_PAUSED = 0x2;
	public static final int STATUS_UNKNOWN_ERROR = 0x3;
	public static final int STATUS_BAD_REQUEST = 0x4;
	public static final int STATUS_HTTP_DATA_ERROR = 0x5;
	public static final int STATUS_SUCCESS = 0x6;
	public static final int STATUS_FILE_ERROR = 0x7;

	/** The intent that gets sent when the service must wake up for a retry */
	public static final String ACTION_RETRY = "android.intent.action.UPLOAD_WAKEUP";

	public static final String MIME_TYPE = "binary/octet-stream";
	public static final String UPLOAD_URL = "http://www.cs.mcgill.ca/~jfrank8/humansense/uploader.php";

	private Constants() {
	}
}
