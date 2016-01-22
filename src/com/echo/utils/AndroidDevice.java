package com.echo.utils;

import com.android.ddmlib.IDevice;


public class AndroidDevice {
	private IDevice device;
	
	public AndroidDevice(IDevice device) {
		this.device = device;
	}
	
	public IDevice getDevice() {
		return this.device;
	}

}
