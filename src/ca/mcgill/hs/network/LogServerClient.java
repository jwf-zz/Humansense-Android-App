/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

import ca.mcgill.hs.util.Log;

/**
 * Used for demos, this allows the app to connect to a logging server running on
 * another computer, and send data for visualisation. We create two data
 * streams, one for sending the class labels from the classifier, and one for
 * sending the raw sensor data.
 * 
 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
 * 
 */
public class LogServerClient {
	private static final String TAG = "LogServerClient";

	private static String joinCommas(final List<String> list) {
		if (list.size() == 0) {
			return "";
		}
		final StringBuffer buf = new StringBuffer();
		buf.append(list.get(0));
		for (int i = 1; i < list.size(); i++) {
			buf.append("\t" + list.get(i));
		}
		return buf.toString();
	}

	private final InetSocketAddress sockAddr;

	private Socket classSock = null;

	private DataOutputStream classOut = null;

	private DataInputStream classIn = null;

	private Socket cdataSock = null;

	private DataOutputStream cdataOut = null;

	private DataInputStream cdataIn = null;

	/**
	 * Creates a client and sets up the socket connection to localhost, port
	 * 12021. We connect to localhost because we tend to use ssh tunneling to
	 * forward the local port to the server.
	 */
	public LogServerClient() {
		sockAddr = new InetSocketAddress("localhost", 12021);
	}

	/**
	 * Alternatively we can specify the host and port.
	 * 
	 * @param host
	 *            Hostname to connect to.
	 * @param port
	 *            Port to connect to.
	 */
	public LogServerClient(final String host, final int port) {
		sockAddr = new InetSocketAddress(host, port);
	}

	/**
	 * Returns the stream for sending the raw sensor data.
	 * 
	 * @param classNames
	 *            The list of class labels.
	 * @return The stream to which raw sensor data should be written.
	 */
	public DataOutputStream cdata(final List<String> classNames) {
		if (!isConnected()) {
			return null;
		} else {
			try {
				cdataOut.writeUTF("CDATA");
				cdataOut.writeInt(classNames.size());
				cdataOut.writeUTF(joinCommas(classNames));
				Log.d(TAG, "Initialied CDATA Connection.");
			} catch (final IOException e) {
				Log.e(TAG, e);
				return null;
			}
		}
		return cdataOut;
	}

	/**
	 * Returns the stream for sending the class scores.
	 * 
	 * @param classNames
	 *            The list of class labels.
	 * @return The stream to which class scores should be written.
	 */
	public DataOutputStream classify(final List<String> classNames) {
		if (!isConnected()) {
			return null;
		} else {
			try {
				classOut.writeUTF("CLASSIFY");
				classOut.writeInt(classNames.size());
				classOut.writeUTF(joinCommas(classNames));
				Log.d(TAG, "Initialied Classify Connection.");
			} catch (final IOException e) {
				Log.e(TAG, e);
				return null;
			}
		}
		return classOut;
	}

	/**
	 * Initializes the connection to the server. Must be called before
	 * retrieving the output streams.
	 */
	public void connect() {
		try {
			Log.d(TAG, "Connecting to LogServer");
			classSock = new Socket();
			classSock.connect(sockAddr, 5000);
			cdataSock = new Socket();
			cdataSock.connect(sockAddr, 5000);
			Log.d(TAG, "Sending HELLO");

			classOut = new DataOutputStream(classSock.getOutputStream());
			classIn = new DataInputStream(classSock.getInputStream());
			classOut.writeUTF("HELLO");
			Log.d(TAG, "Sent HELLO");
			final String response = classIn.readLine();
			Log.d(TAG, "Received " + response);
			if (response.compareTo("HELLO") != 0) {
				Log.e(TAG, "Protocol Error: Did not receive HELLO from server.");
			} else {
				Log.d(TAG, "Connected to Classifying LogServer.");
			}

			cdataOut = new DataOutputStream(cdataSock.getOutputStream());
			cdataIn = new DataInputStream(cdataSock.getInputStream());
			cdataOut.writeUTF("HELLO");
			if (cdataIn.readLine().compareTo("HELLO") != 0) {
				Log.e(TAG, "Protocol Error: Did not receive HELLO from server.");
			} else {
				Log.d(TAG, "Connected to CDATA LogServer.");
			}
		} catch (final UnknownHostException e) {
			Log.e(TAG, "Unknown host: " + sockAddr.getHostName());
			cdataSock = null;
		} catch (final IOException e) {
			Log.e(TAG,
					"Could not get I/O for connection to "
							+ sockAddr.getHostName());
			cdataSock = null;
		}
	}

	/**
	 * Closes the connections. Any attempts to write to the output streams after
	 * calling this method will cause an exception to be thrown.
	 * 
	 * @throws IOException
	 *             Thrown if any of the streams cannot be closed.
	 */
	public void disconnect() throws IOException {
		if (cdataOut != null) {
			cdataOut.close();
			cdataOut = null;
		}
		if (classOut != null) {
			classOut.close();
			classOut = null;
		}
		if (cdataIn != null) {
			cdataIn.close();
			cdataIn = null;
		}
		if (classIn != null) {
			classIn.close();
			classIn = null;
		}
		if (cdataSock != null) {
			cdataSock.close();
			cdataSock = null;
		}
		if (classSock != null) {
			classSock.close();
			classSock = null;
		}
	}

	/**
	 * @return True if the connection to the server is active, or false
	 *         otherwise.
	 */
	public boolean isConnected() {
		return (cdataSock != null && classSock != null);
	}

}
