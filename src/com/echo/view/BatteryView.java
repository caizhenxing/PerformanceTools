package com.echo.view;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeriesCollection;

import com.echo.constant.Constants;
import com.echo.monitor.ControllerMonitor;

public class BatteryView extends ChartPanel {

	/**
	 * serial UID is auto generated
	 */
	private static final long serialVersionUID = 6214606803165478469L;
	private Logger logger = Logger.getLogger(BatteryView.class);
	private Thread batteryThread;
	private static TimeSeries timeSeries;
	private boolean stopFlag = false;
	
	public BatteryView(String chartContent, String title, String yaxisName) {
		this(createChart(chartContent, title, yaxisName));
	}

	public BatteryView(JFreeChart chart) {
		super(chart);
	}
	
	public void start(final String packageName) {
		batteryThread = new Thread(new Runnable() {
			@Override
			public void run() {
				stopFlag = false;
				while(true) {
					if (!stopFlag) {
						try {
							double info = ControllerMonitor.getInstance().getBatteryController().getInfo(packageName);
							timeSeries.add(new Millisecond(), info);
							logger.info(String.format("Package \"%s\" Battery: %f%%", packageName, info));
							Thread.sleep(500);
						} catch (InterruptedException e) {
							logger.error(e.getMessage(), e.getCause());
							e.printStackTrace();
						}
					} else {
						logger.info("Battery View test is stoped!");
						break;
					}
				}
			}
		});
		batteryThread.start();
	}
	
	public void stop() {
		stopFlag = true;
	}
	
	public static JFreeChart createChart(String chartContent, String title, String yaxisName) {
		timeSeries = new TimeSeries(chartContent, Millisecond.class);
		TimeSeriesCollection dataset = new TimeSeriesCollection(timeSeries); 
		//params:ͼ����⣬ͼ��x�ᣬͼ��y�ᣬ���ݼ�����ʾͼ�������ñ�׼���������Ƿ����ɳ�����
		JFreeChart timeSeriesChart = ChartFactory.createTimeSeriesChart(title, Constants.TIME_UNIT, yaxisName, dataset, true, true, false);
		// ��ȡplot����
		XYPlot xyplot = timeSeriesChart.getXYPlot();
		// ��ȡx�����
		ValueAxis valueaxis = xyplot.getDomainAxis();
		// �Զ��������������ݷ�Χ
		valueaxis.setAutoRange(true);
		// ������̶����ݷ�Χ 30s
		valueaxis.setFixedAutoRange(60000D);
		// ��ȡy�����
		valueaxis = xyplot.getRangeAxis();
		return timeSeriesChart;
	}

}
