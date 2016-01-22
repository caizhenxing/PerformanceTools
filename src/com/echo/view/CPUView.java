package com.echo.view;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.echo.constant.Constants;
import com.echo.monitor.ControllerMonitor;

public class CPUView extends ChartPanel {

	/**
	 * serial UID is auto generated
	 */
	private static final long serialVersionUID = -9117953104090754946L;
	private Logger logger = Logger.getLogger(CPUView.class);
	private Thread CPUThread;
	private static TimeSeries timeSeries;
	private boolean stopFlag = false;

	public CPUView(String chartContent, String title, String yaxisName) {
		this(createChart(chartContent, title, yaxisName));
	}
	
	public CPUView(JFreeChart chart) {
		super(chart);
	}
	
	public void start(final String packageName) {
		CPUThread = new Thread(new Runnable() {
			@Override
			public void run() {
				stopFlag = false;
				while(true) {
					if (!stopFlag) {
						try {
							double info = ControllerMonitor.getInstance().getCPUController().getInfo(packageName);
							timeSeries.add(new Millisecond(), info);
							logger.info(String.format("Package \"%s\" CPU: %f%%", packageName,info));
							Thread.sleep(500);
						} catch (InterruptedException e) {
							logger.error(e.getMessage(), e.getCause());
							e.printStackTrace();
						}
					} else {
						logger.info("Cpu View test is stoped!");
						break;
					}
				}
			}
		});
		CPUThread.start();
	}
	
	public void stop() {
		stopFlag = true;
	}
	
	/**create chart controller
	 * @param chartContent: the name of line to be presented,eg flow,memory
	 * @param title: the title of this chart, eg flow,memory
	 * @param yaxisName: y value Axis Label
	 * */
	public static JFreeChart createChart(String chartContent,String title,String yaxisName) {
		timeSeries = new TimeSeries(chartContent, Millisecond.class);
		TimeSeriesCollection dataset = new TimeSeriesCollection(timeSeries);
		JFreeChart timeSeriesChart = ChartFactory.createTimeSeriesChart(title, Constants.TIME_UNIT, yaxisName, dataset, true, true, false);
		//��ȡplot����
		XYPlot xyplot = timeSeriesChart.getXYPlot();
		// ��ȡx�����
		ValueAxis valueaxis = xyplot.getDomainAxis();
		// �Զ��������������ݷ�Χ
		valueaxis.setAutoRange(true);
		// ������̶����ݷ�Χ 30s
		valueaxis.setFixedAutoRange(60000D);
		//��ȡy�����
		valueaxis = xyplot.getRangeAxis();
		return timeSeriesChart;
	}

}
