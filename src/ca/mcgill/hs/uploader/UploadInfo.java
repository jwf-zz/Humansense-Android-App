/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.uploader;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * Based largely off of com.android.providers.downloads.DownloadInfo
 * 
 */
public class UploadInfo {
	public Uri mUri;
	public String mFileName;
	public int mNumFailed;
	public long mLastMod;
	public int mStatus;
	public int mRetryAfter;
	public long mTotalBytes;
	public int mCurrentBytes;

	public int mFuzz;

	public volatile boolean mHasActiveThread;

	public UploadInfo(final Uri uri, final String fileName,
			final int numFailed, final int status, final long now,
			final int retryAfter, final long totalBytes, final int currentBytes) {
		mUri = uri;
		mFileName = fileName;
		mNumFailed = numFailed;
		mStatus = status;
		mLastMod = now;
		mRetryAfter = retryAfter;
		mTotalBytes = totalBytes;
		mCurrentBytes = currentBytes;
		mFuzz = NetworkHelper.sRandom.nextInt(1001);
	}

	/**
	 * Returns whether this download (which the download manager has already
	 * seen and therefore potentially started) should be restarted.
	 * 
	 * In a nutshell, this returns true if the download isn't already running
	 * but should be, and it can know whether the download is already running by
	 * checking the status.
	 */
	public boolean isReadyToRestart(final long now) {
		if (mStatus == 0) {
			// download hadn't been initialized yet
			return true;
		}
		if (mStatus == Constants.STATUS_PENDING) {
			// download is explicit marked as ready to start
			return true;
		}
		if (mStatus == Constants.STATUS_RUNNING_PAUSED) {
			if (mNumFailed == 0) {
				// download is waiting for network connectivity to return before
				// it can resume
				return true;
			}
			if (restartTime() < now) {
				// download was waiting for a delayed restart, and the delay has
				// expired
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns whether this download (which the download manager hasn't seen
	 * yet) should be started.
	 */
	public boolean isReadyToStart(final long now) {
		if (mStatus == 0) {
			// status hasn't been initialized yet, this is a new download
			return true;
		}
		if (mStatus == Constants.STATUS_PENDING) {
			// download is explicit marked as ready to start
			return true;
		}
		if (mStatus == Constants.STATUS_RUNNING) {
			// download was interrupted (process killed, loss of power) while it
			// was running,
			// without a chance to update the database
			return true;
		}
		if (mStatus == Constants.STATUS_RUNNING_PAUSED) {
			if (mNumFailed == 0) {
				// download is waiting for network connectivity to return before
				// it can resume
				return true;
			}
			if (restartTime() < now) {
				// download was waiting for a delayed restart, and the delay has
				// expired
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the time when a download should be restarted. Must only be called
	 * when numFailed > 0.
	 */
	public long restartTime() {
		if (mRetryAfter > 0) {
			return mLastMod + mRetryAfter;
		}
		return mLastMod + Constants.RETRY_FIRST_DELAY * (1000 + mFuzz)
				* (1 << (mNumFailed - 1));
	}

	public void sendDownloadCompletedIntent(final Context context) {
		final Intent intent = new Intent(UploadService.ACTION_UPLOAD_COMPLETED);
		intent.setClass(context, ca.mcgill.hs.uploader.UploadService.class);
		intent.setData(mUri);
		context.sendBroadcast(intent);
	}
}
