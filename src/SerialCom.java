
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.util.Enumeration;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.MeterInterval;
import org.jfree.chart.plot.MeterPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DefaultValueDataset;
import org.jfree.data.general.ValueDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

public class SerialCom extends ApplicationFrame implements SerialPortEventListener {
	public SerialCom(String title) {
		super(title);
		JPanel chartPanel = createDemoPanel();
		chartPanel.setPreferredSize(new Dimension(500, 270));
		setContentPane(chartPanel);
	}

	static SerialPort serialPort;

	static int x = 0;
	private static final String PORT_NAMES[] = { "/dev/tty.usbserial-A9007UX1", // Mac
											// OS
											// X
			"/dev/ttyACM0", // Raspberry Pi
			"/dev/ttyUSB0", // Linux
			"COM3", // Windows
	};
	private BufferedReader input;
	private OutputStream output;
	private static final int TIME_OUT = 2000;
	private static final int DATA_RATE = 9600;

	static XYSeries serie = new XYSeries("Serie1");

	public static JPanel createDemoPanel() {
		XYSeriesCollection collection = new XYSeriesCollection();
		collection.addSeries(serie);
		JFreeChart chart = createChart(collection);
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new ChartPanel(chart));

		return panel;
	}

	private static JFreeChart createChart(XYSeriesCollection collection) {

		JFreeChart chart = ChartFactory.createXYLineChart(null, "Mediciones", "Temperatura (ºC)", collection);

		/*
		 * MeterPlot plot = new MeterPlot(dataset); plot.addInterval(new
		 * MeterInterval("High", new Range(0.0, 100.0)));
		 * plot.setDialOutlinePaint(Color.white); JFreeChart chart = new
		 * JFreeChart("Meter Chart 2", JFreeChart.DEFAULT_TITLE_FONT,
		 * plot, false);
		 */
		return chart;
	}

	public void initialize() {
		// the next line is for Raspberry Pi and
		// gets us into the while loop and was suggested here was
		// suggested
		// http://www.raspberrypi.org/phpBB3/viewtopic.php?f=81&t=32186
		System.setProperty("gnu.io.rxtx.SerialPorts", "/dev/ttyACM0");

		CommPortIdentifier portId = null;

		@SuppressWarnings("rawtypes")
		Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();

		// First, Find an instance of serial port as set in PORT_NAMES.
		while (portEnum.hasMoreElements()) {
			CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
			for (String portName : PORT_NAMES) {
				if (currPortId.getName().equals(portName)) {
					portId = currPortId;
					break;
				}
			}
		}
		if (portId == null) {
			System.out.println("Could not find COM port.");
			return;
		}

		try {
			// open serial port, and use class name for the appName.
			serialPort = (SerialPort) portId.open(this.getClass().getName(), TIME_OUT);

			// set port parameters
			serialPort.setSerialPortParams(DATA_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);

			// open the streams
			input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
			output = serialPort.getOutputStream();
			// add event listeners
			serialPort.addEventListener(this);
			serialPort.notifyOnDataAvailable(true);
		} catch (Exception e) {
			System.err.println(e.toString());
		}
	}

	/**
	 * This should be called when you stop using the port. This will prevent
	 * port locking on platforms like Linux.
	 */
	public synchronized void close() {
		if (serialPort != null) {
			serialPort.removeEventListener();
			serialPort.close();
			System.out.println("Se cerro correctamente el pueto serie");
		}
	}

	/**
	 * Handle an event on the serial port. Read the data and print it.
	 */
	public synchronized void serialEvent(SerialPortEvent oEvent) {
		if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
			try {
				int inputLine = Integer.valueOf(input.readLine());
				System.out.println(inputLine);
				if (inputLine > 0) {
					serie.add(x++, inputLine);
				} else if (input.ready() == false) {
					close();
					Thread.interrupted();
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
				/* System.err.println(e.toString()); */}
		}
		// Ignore all the other eventTypes, but you should consider the
		// other ones.
	}

	public static void main(String[] args) throws Exception {
		SerialCom main = new SerialCom("Prueba");
		main.initialize();
		main.pack();
		RefineryUtilities.centerFrameOnScreen(main);
		main.setVisible(true);
		if (serialPort != null) {
			main.output.write(0);

			Thread t = new Thread() {
				public void run() {
					// the following line will keep this app
					// alive
					// for 1000 seconds,
					// waiting for events to occur and
					// responding to
					// them (printing incoming messages to
					// console).

					try {

						Thread.sleep(1000000);
					} catch (InterruptedException ie) {
						System.out.println("ERROR con el hilo");
					}
				}
			};
			t.start();

			System.out.println("Started");
		} else {
			System.exit(0);
		}
	}
}
