package com.echo.utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.sun.image.codec.jpeg.ImageFormatException;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageDecoder;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import org.apache.log4j.Logger;
import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.DdmPreferences;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.MultiLineReceiver;
import com.android.ddmlib.RawImage;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.echo.constant.Constants;

@SuppressWarnings("restriction")
public class AdbHelper {

	private static Logger logger = Logger.getLogger(AdbHelper.class);
	private AndroidDebugBridge bridge;
	private String adb;
	private DeviceMonitor deviceMonitor;
	private static String ADB_PATH = Constants.ADB_PATH;
	private static AdbHelper instance;
	
	private AdbHelper() {
		startAdb();
		// 连接bridge桥
		connectBridge();
		// 等待连接成功
		waitForConnect();
		//初始化DeviceMonitor
		deviceMonitor = new DeviceMonitor();
	}
	
	// 启动adb
	private void startAdb() {
		this.adb = "adb.exe";
		if (!isProcessRunning(this.adb)) {
			try {
				Process re = Runtime.getRuntime().exec(ADB_PATH);
				if (re != null) {
					logger.debug("in starting Adb!");
				}
			} catch (IOException e) {
				logger.error("", e);
			}
		}
	}
	
	private static boolean isProcessRunning(String fileName) {
		Process process = null;
		BufferedReader br = null;
		try {
			process = Runtime.getRuntime().exec("tasklist");
			br = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			String line;
			while ((line = br.readLine()) != null)
				if (line.contains(fileName))
					return true;
		} catch (IOException e) {
			logger.error(e.toString());
		} finally {
			if (null != process) {
				process.destroy();
			}

			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return false;
	}
	
	private void connectBridge() {
		// 如果是连接远端的服务,则进行远端连接
		AndroidDebugBridge.init(false);
		this.bridge = AndroidDebugBridge.createBridge(ADB_PATH, false);
	}
	
	/**
	 * 等待连接成功
	 */
	private void waitForConnect() {
		DdmPreferences.setTimeOut(20000);
		int i = 0;
		while (!bridge.hasInitialDeviceList() && i < 5) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			    logger.error("wait For connect failed!");
			}
			i++;
		}
		
		if(bridge.hasInitialDeviceList()) {
			logger.info("Adb Helper initial successfully!");
		}
	}
	
	public static AdbHelper getInstance() {
		if(null == instance) {
			instance = new AdbHelper();
		}
		return instance;
	}
	
	public static void setAdbPath(String adbPath) {
		AdbHelper.ADB_PATH = adbPath;
	}
	
	public DeviceMonitor getDeviceMonitor() {
		return this.deviceMonitor;
	}
	
	public void stop() {
		if (null != bridge) {
			AndroidDebugBridge.terminate();
		}
	}

	public List<String> reconnectAdbServer() {
		logger.info("Disconnect to adb daemon!");
		AndroidDebugBridge.disconnectBridge(); // disconnect all services
		logger.info("Connecting to adb daemon!");
		bridge = AndroidDebugBridge.createBridge(ADB_PATH, false); // start
																	// device
																	// monitor
		logger.info("Connect to adb daemon successfully!");
		return getDevices();
	}

	public IDevice getDevice(String sn) {
		IDevice[] devices = new IDevice[] {};
		if (null != bridge) {
			devices = bridge.getDevices();
		} else {
			logger.error("AndroidDebugBridge is null!");
		}

		for (IDevice device : devices) {
			if (device.getSerialNumber().equalsIgnoreCase(sn)) {
				return device;
			}
		}

		return null;
	}

	public List<String> getDevices() {
		IDevice[] devices = new IDevice[] {};
		List<String> snList = new ArrayList<String>(); 
		if (null != bridge) {
			devices = bridge.getDevices();
		} else {
			logger.error("AndroidDebugBridge is null!");
		}
		for(int i = 0; i < devices.length; i++) {
			snList.add(devices[i].getSerialNumber());
		}
		logger.info("Device List :" + snList.toString());

		return snList;
	}
	
	public List<String> executeShellCommandWithOutput(IDevice device, String cmd) {
		logger.info(cmd);

		final List<String> results = new ArrayList<String>();
		try {
			device.executeShellCommand(cmd, new MultiLineReceiver() {

				@Override
				public void processNewLines(String[] lines) {
					for (String line : lines) {
						logger.debug(line);
						results.add(line);
					}
				}

				@Override
				public boolean isCancelled() {
					return false;
				}
			});
		} catch (Exception e) {
			logger.error(
					"Execute " + cmd + " to device " + device.getSerialNumber()
							+ " error!", e);
		}

		return results;
	}
	
	/**
	 * 截取手机屏幕
	 * 
	 * @param sn:要截取的设备sn号
	 * @param filePath:截取屏幕生成的图片要保存的路径
	 * @return
	 */
	public boolean getScreenShot(String sn, String filePath) {
		IDevice device = AdbHelper.getInstance().getDevice(sn);
		RawImage rawScreen = null;
		try {
			rawScreen = device.getScreenshot();
		} catch (AdbCommandRejectedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (com.android.ddmlib.TimeoutException e) {
			e.printStackTrace();
		}
		if (rawScreen != null) {
			//判断是否是横屏
			Boolean landscape = false;
			int width2 = landscape ? rawScreen.height : rawScreen.width;
			int height2 = landscape ? rawScreen.width : rawScreen.height;
			BufferedImage image = new BufferedImage(width2, height2,BufferedImage.TYPE_INT_RGB);
			if (image.getHeight() != height2 || image.getWidth() != width2) {
				image = new BufferedImage(width2, height2,BufferedImage.TYPE_INT_RGB);
			}
			int index = 0;
			int indexInc = rawScreen.bpp >> 3;
			for (int y = 0; y < rawScreen.height; y++) {
				for (int x = 0; x < rawScreen.width; x++, index += indexInc) {
					int value = rawScreen.getARGB(index);
					if (landscape)
						image.setRGB(y, rawScreen.width - x - 1, value);
					else
						image.setRGB(x, y, value);
				}
			}
			try {
				ImageIO.write((RenderedImage) image, "JPG", new File(filePath));
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	public Map<String, String> getProperties(String sn) {
		IDevice device = AdbHelper.getInstance().getDevice(sn);
		return device.getProperties();
	}
	
	public String getProperty(String sn, String property) {
		IDevice device = AdbHelper.getInstance().getDevice(sn);
		return device.getProperty(property);
	}
	
	public Integer getBattries(String sn) {
		IDevice device = AdbHelper.getInstance().getDevice(sn);
		Integer batteryLevel = null;
		try {
			batteryLevel = device.getBatteryLevel();
		} catch (TimeoutException e) {
			e.printStackTrace();
		} catch (AdbCommandRejectedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ShellCommandUnresponsiveException e) {
			e.printStackTrace();
		}
		return batteryLevel;
	}
	
	public boolean isOnLine(String sn) {
		IDevice device = AdbHelper.getInstance().getDevice(sn);
		return device.isOnline();
	}
	
	public boolean isOffLine(String sn) {
		IDevice device = AdbHelper.getInstance().getDevice(sn);
		return device.isOffline();
	}
	
	/**
	 * 在jpg图片上面添加水印图片
	 * 
	 * @param sourceImage:要添加水印的图片
	 * @param smallImage:水印
	 * @param destImage:添加水印之后生成的拼接图片
	 * @return
	 */
	public boolean addIconOnImage(String sourceImage, String smallImage, String destImage) throws ImageFormatException, IOException {
		//1.jpg是你的 主图片的路径
        InputStream is = new FileInputStream(sourceImage);
        //通过JPEG图象流创建JPEG数据流解码器
        JPEGImageDecoder jpegDecoder = JPEGCodec.createJPEGDecoder(is);
        //解码当前JPEG数据流，返回BufferedImage对象
        BufferedImage buffImg = jpegDecoder.decodeAsBufferedImage();
        //得到画笔对象
        Graphics g = buffImg.getGraphics();
        //创建你要附加的图象。
        //2.jpg是你的小图片的路径
        ImageIcon imgIcon = new ImageIcon(smallImage); 
        //得到Image对象。
        Image img = imgIcon.getImage();
        //将小图片绘到大图片上。
        //buffImg.getWidth()/2, buffImg.getHeight()/2 .表示你的小图片在大图片上的位置。
        g.drawImage(img, buffImg.getWidth()/3, buffImg.getHeight()/3, null);
        g.dispose();
        OutputStream os = new FileOutputStream(destImage);
        //创键编码器，用于编码内存中的图象数据。
        JPEGImageEncoder en = JPEGCodec.createJPEGEncoder(os);
        en.encode(buffImg);
        is.close();
        os.close();
		return true;
	}
	
	/**
	 * 在jpg图片上面添加文本文字
	 * 
	 * @param sourceImage:要添加水印的图片
	 * @param smallImage:水印
	 * @param destImage:添加水印之后生成的拼接图片
	 * @return
	 */
	public boolean addTextOnImage(String sourceImage, String text, String font, String destImage) throws ImageFormatException, IOException {
		//1.jpg是主图片的路径
        InputStream is = new FileInputStream(sourceImage);
        //通过JPEG图象流创建JPEG数据流解码器
        JPEGImageDecoder jpegDecoder = JPEGCodec.createJPEGDecoder(is);
        //解码当前JPEG数据流，返回BufferedImage对象
        BufferedImage buffImg = jpegDecoder.decodeAsBufferedImage();
        //得到画笔对象
        Graphics g = buffImg.getGraphics();
        //设置颜色。
        g.setColor(Color.RED);
        //最后一个参数用来设置字体的大小
        Font f = new Font(font, Font.BOLD, 60);
        g.setFont(f);
        //buffImg.getWidth()/4, buffImg.getHeight()/4 表示这段文字在图片上的位置(x,y) .第一个是你设置的内容。
        g.drawString(text, buffImg.getWidth()/4, buffImg.getHeight()/4);
        g.dispose();
        OutputStream os = new FileOutputStream(destImage);
        //创键编码器，用于编码内存中的图象数据。
        JPEGImageEncoder en = JPEGCodec.createJPEGEncoder(os);
        en.encode(buffImg);
        is.close();
        os.close();
        
		return true;
	}
	
	public static void main(String args[]) {
		try {
			AdbHelper.getInstance().addIconOnImage("D:\\temp\\test.jpg", "D:\\temp\\small1.jpg", "D:\\temp\\dest.jpg");
			AdbHelper.getInstance().addTextOnImage("D:\\temp\\test.jpg", "添加文字", "楷体", "D:\\temp\\dest1.jpg");
		} catch (ImageFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
//		AdbHelper.getInstance().getScreenShot("e3cabcd+COM11+COM9", "D:\\temp\\test1.jpg");
//		System.out.println(AdbHelper.getInstance().getBattries("e3cabcd+COM11+COM9"));
//		System.out.println(AdbHelper.getInstance().getProperties("e3cabcd+COM11+COM9"));
//		System.out.println(AdbHelper.getInstance().getProperty("e3cabcd+COM11+COM9", "ro.build.version.release"));
//		System.out.println(AdbHelper.getInstance().isOnLine("e3cabcd+COM11+COM9"));
//		System.out.println(AdbHelper.getInstance().isOffLine("e3cabcd+COM11+COM9"));
	}
	
}
