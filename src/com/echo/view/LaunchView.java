package com.echo.view;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.log4j.Logger;
import com.android.ddmlib.IDevice;
import com.echo.constant.Constants;
import com.echo.log.LoggerManager;
import com.echo.monitor.ControllerMonitor;
import com.echo.utils.AdbHelper;

public class LaunchView extends JFrame{
	
	/**
	 * serial Version UID is auto generated
	 */
	private static final long serialVersionUID = 7845872493970114091L;
	private Logger logger = Logger.getLogger(LaunchView.class);
	private String author;
	private JPanel frame;
	private JButton btnMonkey;
	private JButton btnLaunchCost;
	private JButton btnFps;
	private JButton btnStartTest;
	private MemoryView viewMemory;
	private FlowView viewFlow;
	private CPUView viewCpu;
	private BatteryView viewBattery;
	private JLabel labelPackage;
	private JTextField textPackage;
	private JComboBox<String> comboPackageList;
	private List<String> packageList = new ArrayList<String>();
	private DefaultComboBoxModel<String> model;
	private JComboBox<String> comboDevices;
	private Checkbox boxUSBPowered;

	/**
	 * constructor to init a LaunchView instance
	 * create a JPanel instance to put other controller parts
	 * @param name: author name
	 * */
	public LaunchView(String name) {
		this.author = name;
		this.frame = new JPanel();
		setTitle(String.format("%s Version 1_0", author));
		setBounds(100, 50, 1249, 760);
		add(frame);
		setVisible(true);
	}
	
	/**
	 * constructor to init a LaunchView instance
	 * create a JPanel instance to put other controller parts
	 * */
	@SuppressWarnings({ "rawtypes", "unchecked", "serial" })
	public void createParts() {
		//必须显式设置布局格式为空，否则不会按照我们设置好的格式布局
		frame.setLayout(null);
		// combo box to select device sn
		comboDevices = new JComboBox<String>();
		frame.add(comboDevices);
		Rectangle rect = new Rectangle(0, 0, 300, 30);
		comboDevices.setBounds(rect);
		
	    //memory chart view
	    viewMemory = new MemoryView(Constants.MEMORY, Constants.MEMORY, Constants.MEMORY_UNIT);
	    frame.add(viewMemory);
	    viewMemory.setBounds(605, 40, 600, 200);
	    
	    //flow chart view
	    viewFlow = new FlowView(Constants.FLOW , Constants.FLOW, Constants.FLOW_UNIT);
	    frame.add(viewFlow);
	    rect = new Rectangle(0, 260, 600, 200);
	    viewFlow.setBounds(rect);
	    
	    //CPU chart view
	    viewCpu = new CPUView(Constants.CPU, Constants.CPU, Constants.CPU_UNIT);
	    frame.add(viewCpu);
	    rect = new Rectangle(0, 40, 600, 200);
	    viewCpu.setBounds(rect);
	    
	    //battery chart view
	    viewBattery = new BatteryView(Constants.BATTERY, Constants.BATTERY, Constants.BATTERY_UNIT);
	    frame.add(viewBattery);
	    rect = new Rectangle(605, 260, 600, 200);
	    viewBattery.setBounds(rect);
	    
	    //usb powered check box
	    boxUSBPowered = new Checkbox(Constants.USB_POWERED, false);
	    frame.add(boxUSBPowered);
	    rect = new Rectangle(900, 485, 150, 15);
	    boxUSBPowered.setBounds(rect);
	    
		//button to open fps view
		btnFps = new JButton(Constants.FPS_VIEW);
		frame.add(btnFps);
		btnFps.setBounds(80, 520, 200, 35);
				
		// time that costed to launch an activity
		btnLaunchCost = new JButton(Constants.LAUNCH_COST);
		frame.add(btnLaunchCost);
		btnLaunchCost.setBounds(360, 520, 200, 35);
		
		// monkey test button
		btnMonkey = new JButton(Constants.MONKEY);
		frame.add(btnMonkey);
		btnMonkey.setBounds(80, 570, 200, 35);
		
		// package name label
		labelPackage = new JLabel(Constants.PACKAGE_NAME);
		frame.add(labelPackage);
		rect = new Rectangle(520, 520, 45, 25);
		labelPackage.setBounds(rect);

		// package name selected in the text field
		model = new DefaultComboBoxModel();
		comboPackageList = new JComboBox(model) {
			public Dimension getPreferredSize() {
				return new Dimension(super.getPreferredSize().width, 0);
			}
		};
		textPackage = new JTextField();
		textPackage.setLayout(new BorderLayout());
		textPackage.add(comboPackageList, BorderLayout.SOUTH);
		textPackage.setToolTipText(Constants.SELECT_PACKAGE);
		frame.add(textPackage);
		rect = new Rectangle(580, 520, 180, 30);
		textPackage.setBounds(rect);

		// start test button to begin catch interested info
		btnStartTest = new JButton(Constants.START_TEST);
		frame.add(btnStartTest);
		rect = new Rectangle(860, 515, 200, 35);
		btnStartTest.setBounds(rect);
		
	}
	
	/**
	 * add action listener for all the controller parts
	 * */
	public void addActionListener() {
		//initial Logger Manager to use log4j
		if (!LoggerManager.isInited()) {
			LoggerManager.initLogger();
		}
		logger.info("LoggerManager is inited successfully!");
		
		//initial android debug bridge
		List<String> snList = AdbHelper.getInstance().getDevices();
		for (String sn : snList) {
			comboDevices.addItem(sn);
		}
		AdbHelper.getInstance().getDeviceMonitor().setCombo(comboDevices);
		
		//主窗口添加关闭监听器
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				exit();
			}
		});
		
		//为几个可操作的控件添加监听器
		addStartListener();
		addPackageListener();
		addMonkeyListener();
		addLaunchListener();
		addFpsListener();
	}
	
	private void addStartListener() {
		btnStartTest.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				
				Thread thread = new Thread(new Runnable() {
					@Override
					public void run() {
						if (btnStartTest.getText().equals(Constants.START_TEST)) {
							logger.info("click start test!");
							String packageName = textPackage.getText();
							if (packageName.equals(Constants.BLANK)) {
								logger.info(Constants.PACKAGE_NAME_NULL);
								JOptionPane.showMessageDialog(new JFrame(), Constants.PACKAGE_NAME_NULL);
							} else {
								if (null == comboDevices.getSelectedItem()) {
									logger.info(Constants.DEVICE_NULL);
									JOptionPane.showMessageDialog(new JFrame(), Constants.DEVICE_NULL);
								} else {
									IDevice dev = AdbHelper.getInstance().getDevice((String) comboDevices.getSelectedItem());
									ControllerMonitor.getInstance().setDevice(dev);
									ControllerMonitor.getInstance().getBatteryController().setUsbPowered(boxUSBPowered.getState());
									viewMemory.start(packageName);
									viewFlow.start(packageName);
									viewCpu.start(packageName);
									viewBattery.start(packageName);
									btnStartTest.setText(Constants.STOP_TEST);
								}
							}
						} else {
							viewMemory.stop();
							viewFlow.stop();
							viewCpu.stop();
							viewBattery.stop();
							btnStartTest.setText(Constants.START_TEST);
						}
					}
				});
				thread.start();
			}
		});
	}
	
	private boolean isAdjusting(JComboBox<String> cbInput) {
		if (cbInput.getClientProperty(Constants.ADJUSTING) instanceof Boolean) {
			return (Boolean) cbInput.getClientProperty(Constants.ADJUSTING);
		}
		return false;
	}

	private void setAdjusting(JComboBox<String> cbInput, boolean adjusting) {
		cbInput.putClientProperty(Constants.ADJUSTING, adjusting);
	}
	
	private void updateList(List<String> list) {
		setAdjusting(comboPackageList, true);
		model.removeAllElements();
		String input = textPackage.getText();
		if (!input.isEmpty()) {
			for (String item : list) {
				if (item.toLowerCase().startsWith(input.toLowerCase())) {
					model.addElement(item);
				}
			}
		} else {
			for (String item : list) {
				model.addElement(item);
			}
		}
		comboPackageList.setPopupVisible(model.getSize() > 0);
		setAdjusting(comboPackageList, false);
	}
	
	private void addPackageListener() {
		
		comboPackageList.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!isAdjusting(comboPackageList) && comboPackageList.getSelectedItem() != null) {
					textPackage.setText(comboPackageList.getSelectedItem().toString());
				}
			}
		});
		
		textPackage.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				IDevice dev = AdbHelper.getInstance().getDevice((String) comboDevices.getSelectedItem());
				ControllerMonitor.getInstance().setDevice(dev);
				List<String> ret = ControllerMonitor.getInstance().getPackageController().getInfo();
				Iterator<String> iterator = ret.iterator();
				while(iterator.hasNext()) {
					logger.info(iterator.next());
				}
				packageList = ret;
				//refresh package list
				updateList(packageList);
			}

			@Override
			public void focusLost(FocusEvent e) {
				if (comboPackageList.isPopupVisible()) {
					comboPackageList.setPopupVisible(false);
				}
			}
		});
		
		textPackage.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				updateList(packageList);
			}

			public void removeUpdate(DocumentEvent e) {
				updateList(packageList);
			}

			public void changedUpdate(DocumentEvent e) {
				updateList(packageList);
			}
			
		});
		
		textPackage.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				setAdjusting(comboPackageList, true);
				if (event.getKeyCode() == KeyEvent.VK_SPACE && comboPackageList.isPopupVisible()) {
					event.setKeyCode(KeyEvent.VK_ENTER);
				}
				if (event.getKeyCode() == KeyEvent.VK_ENTER
						|| event.getKeyCode() == KeyEvent.VK_UP
						|| event.getKeyCode() == KeyEvent.VK_DOWN) {
					event.setSource(comboPackageList);
					comboPackageList.dispatchEvent(event);
					if (event.getKeyCode() == KeyEvent.VK_ENTER) {
						textPackage.setText(comboPackageList.getSelectedItem().toString());
						comboPackageList.setPopupVisible(false);
					}
				}
				if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
					comboPackageList.setPopupVisible(false);
				}
				setAdjusting(comboPackageList, false);
			}
		});
	}
	
	private void addMonkeyListener() {
		btnMonkey.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
	}
	
	private void addLaunchListener() {
		IDevice dev = AdbHelper.getInstance().getDevice((String) comboDevices.getSelectedItem());
		ControllerMonitor.getInstance().setDevice(dev);
		btnLaunchCost.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LaunchCostView viewLaunchCost = new LaunchCostView(Constants.LAUNCH_COST_TEST);
				viewLaunchCost.createParts();
				viewLaunchCost.setVisible(true);
			}
		});
	}
	
	private void addFpsListener() {
		IDevice dev = AdbHelper.getInstance().getDevice((String) comboDevices.getSelectedItem());
		ControllerMonitor.getInstance().setDevice(dev);
		btnFps.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				FpsView viewFps = new FpsView(Constants.FPS_VIEW);
				viewFps.createParts();
				viewFps.setVisible(true);
			}
		});
	}
	
	/*
	 * add action listener on exit the dialog
	 * */
	private void exit() {
		Object[] options = { Constants.CONFIRM, Constants.CANCEL };
		JOptionPane warnPane = new JOptionPane("真想退出吗?",
				JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION, null,
				options, options[1]);
		JDialog dialog = warnPane.createDialog(this, "警告");
		dialog.setVisible(true);
		Object selectedValue = warnPane.getValue();
		if (selectedValue == null || selectedValue == options[1]) {
			setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // 这个是关键
		} else if (selectedValue == options[0]) {
			logger.info("program is exited!");
			setDefaultCloseOperation(EXIT_ON_CLOSE);
		}
	}

	public static void main(String[] args) {
		LaunchView launch = new LaunchView(Constants.AUTHOR);
		launch.createParts();
		launch.addActionListener();
		launch.setVisible(true);
	}
}
