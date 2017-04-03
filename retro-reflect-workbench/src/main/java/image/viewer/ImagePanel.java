package image.viewer;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import image.processing.util.Line;

public class ImagePanel extends JPanel {

	class SelectRectangle {

		final MouseAdapter rectangleStartListener = new MouseAdapter() {

			public void mousePressed(MouseEvent e) {
				startDrag = new Point(e.getX(), e.getY());
				endDrag = startDrag;
				repaint();
			}

			public void mouseReleased(MouseEvent e) {
				if (endDrag != null && startDrag != null) {
					try {
						shape = makeRectangle(startDrag.x, startDrag.y, e.getX(), e.getY());
						subimage = image.getSubimage(startDrag.x, startDrag.y, e.getX() - startDrag.x,
								e.getY() - startDrag.y);
						view.setSelectedRegion(subimage, new Point(startDrag.x, startDrag.y), new Point(e.getX(), e.getY()));
						startDrag = null;
						endDrag = null;
						repaint();
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			}
		};
		final MouseMotionAdapter rectangleEndListener = new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				endDrag = new Point(e.getX(), e.getY());
				repaint();
			}
		};

		void register() {
			addMouseListener(rectangleStartListener);
			addMouseMotionListener(rectangleEndListener);
		}

		void unregister() {
			removeMouseListener(rectangleStartListener);
			removeMouseMotionListener(rectangleEndListener);
		}
	}

	class SelectPoint {
		final MouseAdapter listener = new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				final Point point = e.getPoint();
				view.setSelectedPoint(point);
			}

		};

		public void register() {
			setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
			addMouseListener(listener);
		}

		public void unregister() {
			setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			removeMouseListener(listener);
		}
	}

	@Deprecated
	private void adjustSubImagePoint(final Point point) {
		final Rectangle bounds = shape.getBounds();
		int offsetY = bounds.y;
		int offsetX = bounds.x;
		point.x -= offsetX;
		point.y -= offsetY;
	}

	final List<Line2D> lines = new ArrayList<>();

	class SelectLine {

		final MouseAdapter lineStartListener = new MouseAdapter() {

			public void mousePressed(MouseEvent e) {
				startDrag = new Point(e.getX(), e.getY());
				endDrag = startDrag;
				repaint();
			}

			public void mouseReleased(MouseEvent e) {
				if (endDrag != null && startDrag != null) {
					try {
						lines.add(makeLine2D(startDrag.x, startDrag.y, e.getX(), e.getY()));
						Line line = makeLine(startDrag.x, startDrag.y, e.getX(), e.getY());
						view.setSelectedLine(line);
						startDrag = null;
						endDrag = null;
						repaint();
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			}
		};

		private Line makeLine(int x1, int y1, int x2, int y2) {
			return new Line(new Point(Math.min(x1, x2), Math.min(y1, y2)),
					new Point(Math.max(x1, x2), Math.max(y1, y2)));
		}

		private Line2D makeLine2D(int x1, int y1, int x2, int y2) {
			return new Line2D.Float(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2));
		}

		final MouseMotionAdapter lineEndListener = new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				endDrag = new Point(e.getX(), e.getY());
				repaint();
			}
		};

		void register() {
			selectRectangle.unregister();
			addMouseListener(lineStartListener);
			addMouseMotionListener(lineEndListener);
		}

		void unregister() {
			removeMouseListener(lineStartListener);
			removeMouseMotionListener(lineEndListener);
		}
	}

	final SelectRectangle selectRectangle = new SelectRectangle();

	private static final long serialVersionUID = 1L;
	private BufferedImage image;
	private Shape shape = null;
	BufferedImage subimage;
	Point startDrag, endDrag;
	BufferedImage selectedRegion;

	private ImageProcessingView view;

	public ImagePanel(LayoutManager layout, ImageProcessingView view) {
		super(layout, true);
		this.view = view;
		selectRectangle.register();
	}

	SelectLine selectLine = new SelectLine();
	SelectPoint selectPoint = new SelectPoint();
	
	public void selectLines() {
		selectRectangle.unregister();
		selectPoint.unregister();
		selectLine.register();
	}

	protected void setSelectedRegion(BufferedImage subimage) {
		selectedRegion = subimage;
	}

	public void setImage(BufferedImage image) {
		selectRectangle.unregister();
		selectLine.unregister();
		selectPoint.unregister();
		shape = null;
		lines.clear();
		this.image = image;
		repaint();
	}

	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (image == null) {
			return;
		}
		Graphics2D g2 = (Graphics2D) g;
		g2.drawImage(image, 0, 0, null);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2.setStroke(new BasicStroke(2));
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.50f));

		if (shape != null) {
			g2.setPaint(Color.YELLOW);
			g2.draw(shape);
			// g2.setPaint(Color.YELLOW);
			// g2.fill(shape);
		}
		for (Line2D line2d : lines) {
			g2.setPaint(Color.YELLOW);
			g2.draw(line2d);
		}
		if (startDrag != null && endDrag != null) {
			g2.setPaint(Color.LIGHT_GRAY);
			Shape r = makeRectangle(startDrag.x, startDrag.y, endDrag.x, endDrag.y);
			g2.draw(r);
		}

	}

	private Rectangle2D.Float makeRectangle(int x1, int y1, int x2, int y2) {
		return new Rectangle2D.Float(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x1 - x2), Math.abs(y1 - y2));
	}

	public void selectPoints() {
		selectRectangle.unregister();
		selectLine.unregister();
		selectPoint.register();
	}

}