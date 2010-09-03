package ca.mcgill.hs.uploader;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.os.PowerManager;
import android.os.Process;
import android.util.Log;

public class UploadThread extends Thread {
	private final Context mContext;
	private final UploadInfo mInfo;

	public UploadThread(final Context context, final UploadInfo info) {
		mContext = context;
		mInfo = info;
	}

	/**
	 * Executes the upload in a separate thread
	 */

	@Override
	public void run() {
		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
		int finalStatus = Constants.STATUS_UNKNOWN_ERROR;
		boolean countRetry = false;
		int retryAfter = 0;
		AndroidHttpClient client = null;
		PowerManager.WakeLock wakeLock = null;
		String filename = null;

		http_request_loop: while (true) {
			try {
				final PowerManager pm = (PowerManager) mContext
						.getSystemService(Context.POWER_SERVICE);
				wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
						Constants.TAG);
				wakeLock.acquire();

				filename = mInfo.mFileName;
				final File file = new File(filename);
				if (!file.exists()) {
					Log.e(Constants.TAG, "file" + filename
							+ " is to be uploaded, but cannot be found.");
					finalStatus = Constants.STATUS_FILE_ERROR;
					break http_request_loop;
				}
				client = AndroidHttpClient.newInstance(
						Constants.DEFAULT_USER_AGENT, mContext);
				Log.v(Constants.TAG, "initiating upload for " + mInfo.mUri);
				final HttpPost request = new HttpPost(Constants.UPLOAD_URL);
				request.addHeader("MAC", NetworkHelper.getMacAddress(mContext));

				final MultipartEntity mpEntity = new MultipartEntity();
				mpEntity.addPart("uploadedfile", new FileBody(file,
						"binary/octet-stream"));
				request.setEntity(mpEntity);

				HttpResponse response;
				try {
					response = client.execute(request);
					final HttpEntity resEntity = response.getEntity();

					String responseMsg = null;
					if (resEntity != null) {
						responseMsg = EntityUtils.toString(resEntity);
						Log.i(Constants.TAG, "Server Response: " + responseMsg);
					}
					if (resEntity != null) {
						resEntity.consumeContent();
					}

					if (!responseMsg.contains("SUCCESS 0x64asv65")) {
						Log.i(Constants.TAG, "Server Response: " + responseMsg);
					}
				} catch (final IllegalArgumentException e) {
					finalStatus = Constants.STATUS_BAD_REQUEST;
					request.abort();
					break http_request_loop;
				} catch (final IOException e) {
					if (!NetworkHelper.isNetworkAvailable(mContext)) {
						finalStatus = Constants.STATUS_RUNNING_PAUSED;
					} else if (mInfo.mNumFailed < Constants.MAX_RETRIES) {
						finalStatus = Constants.STATUS_RUNNING_PAUSED;
						countRetry = true;
					} else {
						Log.d(Constants.TAG,
								"IOException trying to excute request for "
										+ mInfo.mUri + " : " + e);
						finalStatus = Constants.STATUS_HTTP_DATA_ERROR;
					}
					request.abort();
					break http_request_loop;
				}

				final int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == 503
						&& mInfo.mNumFailed < Constants.MAX_RETRIES) {
					Log.v(Constants.TAG, "got HTTP response code 503");
					finalStatus = Constants.STATUS_RUNNING_PAUSED;
					countRetry = true;

					retryAfter = Constants.MIN_RETRY_AFTER;
					retryAfter += NetworkHelper.sRandom
							.nextInt(Constants.MIN_RETRY_AFTER + 1);
					retryAfter *= 1000;
					request.abort();
					break http_request_loop;
				} else {
					finalStatus = Constants.STATUS_SUCCESS;
				}
				break;
			} catch (final RuntimeException e) {
				finalStatus = Constants.STATUS_UNKNOWN_ERROR;
			} finally {
				mInfo.mHasActiveThread = false;
				if (wakeLock != null) {
					wakeLock.release();
					wakeLock = null;
				}
				if (client != null) {
					client.close();
					client = null;
				}
				if (finalStatus == Constants.STATUS_SUCCESS) {
					// TODO: Move the file.
				}
			}
		}

	}
}
