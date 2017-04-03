package image.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import image.processing.HsbFilter;
import image.processing.Searcher;
import image.processing.exception.NotFound;
import image.processing.finder.Figure;
import image.processing.finder.Finder;
import image.processing.profile.TargetProfile;
import image.processing.util.ColorChart;
import image.processing.util.Line;
import image.processing.util.PositionUtil;
import image.processing.util.Util;
import image.processing.vision.target.Lift;
import unittest.TargetSearchTest;

public class ImageProcessingView {

	public static BufferedImage copyImage(BufferedImage bi) {
		ColorModel cm = bi.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = bi.copyData(bi.getRaster().createCompatibleWritableRaster());
		return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ImageProcessingView window = new ImageProcessingView();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	JButton backButton;

	private BufferedImage currentImage;

	private int currentIndex;

	JButton endButton;

	JButton forwardButton;

	private JFrame frame;

	JTextArea imageInfo;

	private ImagePanel imagePanel;
	final List<BufferedImage> images = new ArrayList<BufferedImage>();

	JButton startButton;

	JCheckBox traceRouteCheckBox;

	private JCheckBox useInteractiveHsbCheckBox;
	private JButton clearHsv;

	private JLabel fileName;

	private JSpinner hueMin;

	private JSpinner hueMax;

	private JSpinner saturationMin;

	private JSpinner saturationMax;

	private JSpinner brightnessMax;

	private JSpinner brightnessMin;
	private JLabel hsvLabel;
	private JButton selectPoint;

	private void resetSelectedProfile() {
		selectedProfile.clearValues();
		showTargetProfiles();
	}

	/**
	 * Create the application.
	 */
	public ImageProcessingView() {
		initialize();
	}

	private String hsvToString() {
		if (selectedProfile == null) {
			return "";
		}
		final TargetProfile p = selectedProfile;
		StringBuilder b = new StringBuilder();
		b.append("// " + p.label + "\n");
		b.append("new TargetProfile(\n\t");
		b.append(String.format("%.3f", p.hueMin));
		b.append("f, ");
		b.append(String.format("%.3f", p.hueMax));
		b.append("f,\n\t");
		b.append(String.format("%.3f", p.saturationMin));
		b.append("f, ");
		b.append(String.format("%.3f", p.saturationMax));
		b.append("f,\n\t");
		b.append(String.format("%.3f", p.brightnessMin));
		b.append("f, ");
		b.append(String.format("%.3f", p.brightnessMax));
		b.append("f\n");
		b.append(");");
		final String string = b.toString();
		return string;
	}

	protected void getHSV(Point a, Point b) {
		int xMin = Math.min(a.x, b.x);
		int xMax = Math.max(a.x, b.x);
		int yMin = Math.min(a.y, b.y);
		int yMax = Math.max(a.y, b.y);
		List<float[]> list = new ArrayList<float[]>();
		for (int i = xMin; i < xMax; i++) {
			for (int j = yMin; j < yMax; j++) {
				list.add(Util.getHSV(new Point(i, j), currentImage));
			}

		}
		appendHsbToSelectedProfile((float[][]) list.toArray(new float[list.size()][]));
	}

	private TargetProfile getTargetProfile() {
		if (selectedProfile == null) {
			setSelectedProfile(getTargetProfiles().get(0));
		}
		return selectedProfile;
	}

	final ArrayList<TargetProfile> profiles = new ArrayList<>();

	private JPanel targetProfilePanel;

	private TargetProfile selectedProfile;

	private int processedCount = 0;
	private int processedTotal = 0;

	private List<TargetProfile> getTargetProfiles() {
		if (profiles.isEmpty()) {
			profiles.add(TargetProfile.goodReflection());
			profiles.add(TargetProfile.lateralReflection());
			final TargetProfile custom1 = new TargetProfile("Raj's images", 0.321f, 0.458f, 0.167f, 1.000f, 0.094f, 0.761f,
					Color.CYAN);
			profiles.add(custom1);
			final TargetProfile custom2 = new TargetProfile();
			custom2.label = "Custom 2";
			profiles.add(custom2);
			// final TargetProfile custom3 = new TargetProfile("hopper yellow",
			// 0.140f, 0.170f, 0.540f, 1.000f, 0.520f,
			// 0.660f, Color.CYAN);
			final TargetProfile custom3 = new TargetProfile("hopper yellow", 0.130f, 0.210f, 0.299f, 1.0f, 0.520f,
					0.98f, Color.CYAN);
			profiles.add(custom3);

			final TargetProfile distantAngle = new TargetProfile("Distant angle", 0.47f, 0.53f, 0.49f, 1f, 0.36f, 0.76f,
					Color.DARK_GRAY);
			final TargetProfile ledColumn = new TargetProfile("LED column", 0.51f, 0.62f, 0.90f, 1f, 0.56f, 0.69f,
					Color.RED);
			profiles.add(ledColumn);
			TargetProfile topTwo = TargetProfile.goodReflection().merge(TargetProfile.lateralReflection());
			topTwo.label = "Top Two";
			profiles.add(topTwo);
			final TargetProfile composite = Util.getCompositeTargetProfile(profiles);
			composite.label = "Composite";
			profiles.add(composite);
		}
		return profiles;
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 1086, 676);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);

		fileName = new JLabel("File Name");
		fileName.setBounds(10, 11, 310, 14);
		frame.getContentPane().add(fileName);

		this.imagePanel = new ImagePanel(new BorderLayout(), this);
		imagePanel.setBounds(10, 36, 640, 480);
		frame.getContentPane().add(imagePanel);
		// imagePanel.addMouseListener(new MouseAdapter() {
		// private Point mousePressedPoint;
		//
		// @Override
		// public void mousePressed(MouseEvent e) {
		// imagePanel.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		// mousePressedPoint = e.getPoint();
		// }
		//
		// @Override
		// public void mouseReleased(MouseEvent e) {
		// imagePanel.setCursor(null);
		// getHSV(mousePressedPoint, e.getPoint());
		// mousePressedPoint = null;
		// }
		// });
		imagePanel.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				final Point point = e.getPoint();
				BufferedImage image = images.get(currentIndex);
				if (point.x >= image.getWidth() || point.y >= image.getHeight()) {
					hsvLabel.setText("out of bounds");
					return;
				}
				final float[] hsb = Util.getHSV(point, image);
				boolean matches = getTargetProfile().matches(hsb);
				hsvLabel.setText("H: " + String.format("%.2f", hsb[0]) + "  S: " + String.format("%.2f", hsb[1])
						+ "  B: " + String.format("%.2f", hsb[2]) + "     " + point.x + '@' + point.y + " Matches: " + matches);
			}
		});

		imageInfo = new JTextArea();
		imageInfo.setBounds(660, 386, 398, 130);
		frame.getContentPane().add(imageInfo);

		startButton = new JButton("<<");
		startButton.setBounds(10, 527, 49, 23);
		frame.getContentPane().add(startButton);
		startButton.addActionListener(e -> showImage(0));

		backButton = new JButton("<");
		backButton.setBounds(70, 527, 49, 23);
		frame.getContentPane().add(backButton);
		backButton.addActionListener(e -> showImage(currentIndex - 1));

		forwardButton = new JButton(">");
		forwardButton.setBounds(132, 527, 49, 23);
		frame.getContentPane().add(forwardButton);
		forwardButton.addActionListener(e -> showImage(currentIndex + 1));

		endButton = new JButton(">>");
		endButton.setBounds(191, 527, 49, 23);
		frame.getContentPane().add(endButton);
		endButton.addActionListener(e -> showImage(images.size() - 1));

		traceRouteCheckBox = new JCheckBox("Trace Route");
		traceRouteCheckBox.setBounds(389, 527, 97, 23);
		frame.getContentPane().add(traceRouteCheckBox);
		traceRouteCheckBox.setSelected(true);

		clearHsv = new JButton("Clear ");
		clearHsv.addActionListener(e -> resetSelectedProfile());
		clearHsv.setBounds(670, 352, 119, 23);
		frame.getContentPane().add(clearHsv);

		hueMin = new JSpinner();
		hueMin.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		hueMin.setBounds(816, 290, 54, 20);
		frame.getContentPane().add(hueMin);
		hueMin.addChangeListener(e -> updateHsv(0, (int) hueMin.getValue()));

		hueMax = new JSpinner();
		hueMax.setBounds(893, 290, 54, 20);
		frame.getContentPane().add(hueMax);
		hueMax.addChangeListener(e -> updateHsv(1, (int) hueMax.getValue()));

		saturationMin = new JSpinner();
		saturationMin.setBounds(816, 321, 54, 20);
		frame.getContentPane().add(saturationMin);
		saturationMin.addChangeListener(e -> updateHsv(2, (int) saturationMin.getValue()));

		saturationMax = new JSpinner();
		saturationMax.setBounds(893, 321, 54, 20);
		frame.getContentPane().add(saturationMax);
		saturationMax.addChangeListener(e -> updateHsv(3, (int) saturationMax.getValue()));

		brightnessMin = new JSpinner();
		brightnessMin.setBounds(816, 349, 54, 20);
		frame.getContentPane().add(brightnessMin);
		brightnessMin.addChangeListener(e -> updateHsv(4, (int) brightnessMin.getValue()));
		brightnessMax = new JSpinner();
		brightnessMax.setBounds(893, 349, 54, 20);
		frame.getContentPane().add(brightnessMax);
		brightnessMax.addChangeListener(e -> updateHsv(5, (int) brightnessMax.getValue()));

		JLabel lblHue = new JLabel("Hue");
		lblHue.setBounds(957, 294, 75, 14);
		frame.getContentPane().add(lblHue);

		JLabel lblSaturation = new JLabel("Saturation");
		lblSaturation.setBounds(957, 324, 75, 14);
		frame.getContentPane().add(lblSaturation);

		JLabel lblBrightness = new JLabel("Brightness");
		lblBrightness.setBounds(957, 352, 75, 14);
		frame.getContentPane().add(lblBrightness);

		JLabel lblMax = new JLabel("Max");
		lblMax.setBounds(893, 272, 54, 14);
		frame.getContentPane().add(lblMax);

		JLabel lblMin = new JLabel("Min");
		lblMin.setBounds(816, 272, 54, 14);
		frame.getContentPane().add(lblMin);

		hsvLabel = new JLabel("HSV");
		hsvLabel.setBounds(340, 11, 310, 14);
		frame.getContentPane().add(hsvLabel);
		hsvLabel.setHorizontalAlignment(SwingConstants.RIGHT);

		JButton btnSlideShow = new JButton("Slide Show");
		btnSlideShow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			}
		});
		btnSlideShow.setBounds(507, 527, 109, 23);
		btnSlideShow.addActionListener(e -> showSlideshow());
		frame.getContentPane().add(btnSlideShow);

		selectPoint = new JButton("Select Point");
		selectPoint.addActionListener(e -> imagePanel.selectPoints());
		selectPoint.setBounds(670, 284, 119, 23);
		frame.getContentPane().add(selectPoint);

		targetProfilePanel = new JPanel();
		targetProfilePanel.setBounds(660, 36, 400, 225);
		frame.getContentPane().add(targetProfilePanel);
		GridBagLayout gbl_targetProfilePanel = new GridBagLayout();
		// gbl_targetProfilePanel.columnWidths = new int[] { 199, 2, 0 };
		// gbl_targetProfilePanel.rowHeights = new int[] { 2, 0 };
		// gbl_targetProfilePanel.columnWeights = new double[] { 0.0, 0.0,
		// Double.MIN_VALUE };
		// gbl_targetProfilePanel.rowWeights = new double[] { 0.0,
		// Double.MIN_VALUE };
		targetProfilePanel.setLayout(gbl_targetProfilePanel);

		JScrollPane scrollPane = new JScrollPane();
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.anchor = GridBagConstraints.NORTHWEST;
		gbc_scrollPane.gridx = 1;
		gbc_scrollPane.gridy = 0;
		targetProfilePanel.add(scrollPane, gbc_scrollPane);

		JButton selectLine = new JButton("Select Line");
		selectLine.setBounds(670, 318, 119, 23);
		frame.getContentPane().add(selectLine);
		selectLine.addActionListener(e -> imagePanel.selectLines());

		loadImages();
		showImage(0);
		updateButtons();

		showTargetProfiles();
	}

	private void showTargetProfiles() {
		if (targetProfilePanel == null) {
			return;
		}
		targetProfilePanel.removeAll();
		targetProfilePanel.repaint();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridy = 0;
		ButtonGroup group = new ButtonGroup();
		for (TargetProfile profile : getTargetProfiles()) {
			if (selectedProfile == null) {
				setSelectedProfile(profile);
			}
			gbc.gridx = 0;
			final JRadioButton radio = new JRadioButton("", selectedProfile == profile);
			radio.addActionListener(e -> setSelectedProfile(profile));
			group.add(radio);
			targetProfilePanel.add(radio, gbc);
			gbc.gridx++;
			JTextField label = new JTextField(profile.label);
			label.addActionListener(e -> profile.label = label.getText());
			label.setForeground(profile.getDisplayColor());
			targetProfilePanel.add(label, gbc);
			gbc.gridx++;
			JLabel icon = new JLabel(new ImageIcon(new ColorChart(profile).getImage(30, 20)));
			targetProfilePanel.add(icon, gbc);
			gbc.gridx++;
			JCheckBox active = new JCheckBox("Active", profile.active);
			active.addActionListener(e -> profile.active = !profile.active);
			targetProfilePanel.add(active, gbc);
			gbc.gridy++;
		}
		targetProfilePanel.revalidate();
		targetProfilePanel.validate();
	}

	private void setSelectedProfile(TargetProfile profile) {
		selectedProfile = profile;
		updateHsbEditor();
		showImage(currentIndex);
	}

	private void showSlideshow() {
		TimerTask task = new TimerTask() {
			int count = 0;

			@Override
			public void run() {
				if (count < images.size()) {
					showImage(count++);
				} else {
					cancel();
				}
			}
		};
		final Timer timer = new Timer("slide show");
		timer.scheduleAtFixedRate(task, 0, 1);
	}

	private void updateHsv(int idx, int value) {
		final float f = value / 100f;
		switch (idx) {
		case 0:
			selectedProfile.hueMin = f;
			break;
		case 1:
			selectedProfile.hueMax = f;
			break;
		case 2:
			selectedProfile.saturationMin = f;
			break;
		case 3:
			selectedProfile.saturationMax = f;
			break;
		case 4:
			selectedProfile.brightnessMin = f;
			break;
		case 5:
			selectedProfile.brightnessMax = f;
			break;

		default:
			break;
		}
		imageInfo.setText(hsvToString());
		showImage(currentIndex);
	}

	void loadImages() {
		final int[] imageNames = TargetSearchTest.getImageIdentifiers();
		for (int i : imageNames) {
			images.add(TargetSearchTest.getImage(i));
		}
	}

	private void recognizeFeatures(BufferedImage image) {
		Date start = new Date();
		final HsbFilter hsbFilter = new HsbFilter(image);
		hsbFilter.setRememberPath(traceRouteCheckBox.isSelected());
		List<Figure> figures = new ArrayList<>();
		try {
			Searcher searcher = new Searcher(hsbFilter, getTargetProfile(),
					new Point(image.getWidth() / 2, image.getHeight() / 2), Searcher.FigureType.GearLift);
			Finder finder = searcher.finder();
			figures.addAll(finder.figures);
		} catch (NotFound e) {
			// e.printStackTrace();
		}
		hsbFilter.drawAccessPath();
		for (Figure figure : figures) {
			figure.showCorners();
			figure.showTargetCorners(getTargetProfile().getDisplayColor());
		}
		// experiment
		Lift lift = new Lift();
		lift.setFigures(figures);
		Date stop = new Date();
		long milliseconds = stop.getTime() - start.getTime();
		processedCount++;
		processedTotal += milliseconds;
		System.out.println("Processing time: " + milliseconds + " Average milliseconds: "
				+ ((int) processedTotal / processedCount));
		try {
			lift.validate();
			Graphics2D gc = (Graphics2D) image.getGraphics();
			lift.draw(gc);
			Point2D center = lift.getCenter();
			// if (center != null) {
			// String string = String.valueOf(center);
			// gc.drawChars(string.toCharArray(), 0, string.length(), (int)
			// center.getX(), (int) center.getY());
			// }
			System.out.println("Steering Direction: " + PositionUtil.getSteeringDirection(lift, image, 0));
			System.out.println("Distance (feet): " + PositionUtil.getDistance(lift, image, 29));
		} catch (NotFound e) {
		}

	}

	private void appendHsbToSelectedProfile(float[]... hsv) {
		selectedProfile.appendHsb(hsv);
		updateHsbEditor();
		imageInfo.setText(hsvToString());
	}

	private void updateHsbEditor() {
		hueMin.setValue((int) (selectedProfile.hueMin * 100));
		hueMax.setValue((int) (selectedProfile.hueMax * 100));
		saturationMin.setValue((int) (selectedProfile.saturationMin * 100));
		saturationMax.setValue((int) (selectedProfile.saturationMax * 100));
		brightnessMin.setValue((int) (selectedProfile.brightnessMin * 100));
		brightnessMax.setValue((int) (selectedProfile.brightnessMax * 100));
	}

	void showImage(BufferedImage image) {
		currentImage = copyImage(image);
		recognizeFeatures(currentImage);
		imagePanel.setImage(currentImage);
	}

	private void showImage(int i) {
		currentIndex = i;
		final BufferedImage image = images.get(i);
		showImage(image);
		updateButtons();
		fileName.setText(
				TargetSearchTest.getImageFileName(i) + "  -  " + (images.indexOf(image) + 1) + " of " + images.size());
		imageInfo.setText(hsvToString());
	}

	private void updateButtons() {
		startButton.setEnabled(currentIndex > 0);
		backButton.setEnabled(currentIndex > 0);
		forwardButton.setEnabled(currentIndex < images.size() - 1);
		endButton.setEnabled(currentIndex < images.size() - 1);
	}

	@Deprecated
	public void setSelectedRegion(BufferedImage subimage, Point origin, Point corner) {
		// TODO Auto-generated method stub

	}

	public void setSelectedLine(Line line) {
		final float[][] sampleHsv = Util.getSampleHsv(images.get(currentIndex), line, 2);
		selectedProfile.appendHsb(sampleHsv);
		showTargetProfiles();
		showImage(currentIndex);
	}

	public void setSelectedPoint(Point point) {
		final float[][] sampleHsv = Util.getSampleHsv(images.get(currentIndex), point, 3);
		selectedProfile.appendHsb(sampleHsv);
		showTargetProfiles();
		showImage(currentIndex);
	}
}
