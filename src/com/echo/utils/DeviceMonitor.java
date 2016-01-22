package com.echo.utils;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;

import org.apache.log4j.Logger;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener;
import com.android.ddmlib.IDevice.DeviceState;

public class DeviceMonitor {
	private Logger logger = Logger.getLogger(DeviceMonitor.class);
	private IDeviceChangeListener deviceChangeListener;
	private List<AndroidDevice> devices = new ArrayList<AndroidDevice>();
	private JComboBox<String> comboDevices;
	
	public DeviceMonitor() {
		addDeviceChangeListener();
	}
	
	public void setCombo(JComboBox<String> comboDevices) {
		this.comboDevices = comboDevices;
	}

	private void addDeviceChangeListener() {
		deviceChangeListener = new IDeviceChangeListener() {

			@Override
			public void deviceDisconnected(IDevice idevice) {
				for (AndroidDevice dev : devices) {
					if (dev.getDevice().getSerialNumber()
							.equals(idevice.getSerialNumber())) {
						logger.info(String.format("device %s disconnected!",idevice.getSerialNumber()));
						DeviceMonitor.this.deviceDisconnected(dev);
						break;
					}
				}
			}

			@Override
			public void deviceConnected(IDevice idevice) {
				if (idevice.isOnline()) {
					logger.info(String.format("device %s connected!",idevice.getSerialNumber()));
					DeviceMonitor.this.deviceConnected(idevice);
				}
			}

			@Override
			public void deviceChanged(IDevice idevice, int i) {
				if (i != IDevice.CHANGE_STATE) {
					return;
				}
				DeviceState state = idevice.getState();
				if (state == DeviceState.ONLINE) {
					logger.info(String.format("device %s changed state: ONLINE",idevice.getSerialNumber()));
					deviceConnected(idevice);
				} else if (state == DeviceState.OFFLINE) {
					logger.info(String.format("device %s changed state: OFFLINE",idevice.getSerialNumber()));
					deviceDisconnected(idevice);
				}
			}
		};

		AndroidDebugBridge.addDeviceChangeListener(deviceChangeListener);
	}
	
	private void deviceConnected(IDevice idevice) {
		if (devices.size() != 0) {
			for (AndroidDevice dev : devices) {
				if (!dev.getDevice().getSerialNumber()
						.equals(idevice.getSerialNumber())) {
					AndroidDevice ad = new AndroidDevice(idevice);
					devices.add(ad);
					comboDevices.addItem(idevice.getSerialNumber());
				}
			}
		} else {
			comboDevices.removeAllItems();
			AndroidDevice ad = new AndroidDevice(idevice);
			devices.add(ad);
			comboDevices.addItem(idevice.getSerialNumber());
		}
	}
	
	private void deviceDisconnected(AndroidDevice ad) {
		comboDevices.removeItem(ad.getDevice().getSerialNumber());
		devices.remove(ad);
	}
}
