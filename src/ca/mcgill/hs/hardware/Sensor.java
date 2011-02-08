/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.hardware;

import java.io.FileDescriptor;

import android.os.Bundle;

/**
 * An attempt at building our own sensor interface, in hopes of retreiving
 * sensor readings at a more regular frequency than is provided by the APIs.
 * 
 * TODO: This class currently does not do anything useful. This is a work in
 * progress.
 * 
 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
 */
public class Sensor {

	static {
		System.loadLibrary("humansense");
		Sensor.nativeClassInit();
	}

	public static native int androidInit();

	public static native Bundle androidOpen();

	public static native void nativeClassInit();

	public static native int sensorsDataClose();

	public static native int sensorsDataInit();

	public static native int sensorsDataOpen(FileDescriptor[] descriptors,
			int[] handles);

	public static native int sensorsDataPoll(float[] values, int[] status,
			long[] timestamp);

	public static native int sensorsDataUninit();

	public native static int sensorsModuleGetNextSensor(Sensor sensor, int next);

	public static native int sensorsModuleInit();

	private String mName;
	private String mVendor;
	private int mVersion;
	private int mHandle;
	private int mType;
	private float mMaxRange;
	private float mResolution;
	private float mPower;

	public int getHandle() {
		return mHandle;
	}

	public String getName() {
		return mName;
	}

	public float getPower() {
		return mPower;
	}

	public float getRange() {
		return mMaxRange;
	}

	public float getResolution() {
		return mResolution;
	}

	public int getType() {
		return mType;
	}

	public String getVendor() {
		return mVendor;
	}

	public int getVersion() {
		return mVersion;
	}

	public void setHandle(final int handle) {
		this.mHandle = handle;
	}

	public void setName(final String name) {
		this.mName = name;
	}

	public void setPower(final float power) {
		this.mPower = power;
	}

	public void setRange(final float range) {
		this.mMaxRange = range;
	}

	public void setResolution(final float resolution) {
		this.mResolution = resolution;
	}

	public void setType(final int type) {
		this.mType = type;
	}

	public void setVendor(final String vendor) {
		this.mVendor = vendor;
	}

	public void setVersion(final int version) {
		this.mVersion = version;
	}

}
