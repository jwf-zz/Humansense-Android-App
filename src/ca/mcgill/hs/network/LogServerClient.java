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

	public LogServerClient() {
		sockAddr = new InetSocketAddress("localhost", 12021);
	}

	public LogServerClient(final String host, final int port) {
		sockAddr = new InetSocketAddress(host, port);
	}

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
				e.printStackTrace();
				return null;
			}
		}
		return cdataOut;
	}

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
				e.printStackTrace();
				return null;
			}
		}
		return classOut;
	}

	public void connect() {
		try {
			Log.d(TAG, "Connecting to LogServer");
			classSock = new Socket();
			classSock.connect(sockAddr, 5000);
			cdataSock = new Socket();
			cdataSock.connect(sockAddr, 5000);

			classOut = new DataOutputStream(classSock.getOutputStream());
			classIn = new DataInputStream(classSock.getInputStream());
			classOut.writeUTF("HELLO");
			if (classIn.readLine().compareTo("HELLO") != 0) {
				Log
						.e(TAG,
								"Protocol Error: Did not receive HELLO from server.");
			} else {
				Log.d(TAG, "Connected to Classifying LogServer.");
			}

			cdataOut = new DataOutputStream(cdataSock.getOutputStream());
			cdataIn = new DataInputStream(cdataSock.getInputStream());
			cdataOut.writeUTF("HELLO");
			if (cdataIn.readLine().compareTo("HELLO") != 0) {
				Log
						.e(TAG,
								"Protocol Error: Did not receive HELLO from server.");
			} else {
				Log.d(TAG, "Connected to CDATA LogServer.");
			}
		} catch (final UnknownHostException e) {
			Log.e(TAG, "Unknown host: " + sockAddr.getHostName());
			cdataSock = null;
		} catch (final IOException e) {
			Log.e(TAG, "Could not get I/O for connection to "
					+ sockAddr.getHostName());
			cdataSock = null;
		}
	}

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

	public boolean isConnected() {
		return (cdataSock != null && classSock != null);
	}

}
