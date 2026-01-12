package com.simpfi.ui.panel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.simpfi.App;
import com.simpfi.config.Constants;
import com.simpfi.config.Settings;
import com.simpfi.object.Connection;
import com.simpfi.object.Edge;
import com.simpfi.object.Junction;
import com.simpfi.object.Lane;
import com.simpfi.object.TrafficLight;
import com.simpfi.object.Vehicle;
import com.simpfi.sumo.wrapper.VehicleController;
import com.simpfi.ui.Mouse;
import com.simpfi.ui.Panel;
import com.simpfi.util.GraphicsSettings;
import com.simpfi.util.Point;

/**
 * Custom MapPanel class that inherits {@link com.simpfi.ui.Panel}. Used to draw
 * objects on the user interface such as vehicles, edges, lanes, etc.
 */
public class MapPanel extends Panel {

	/** Logger. */
	private static final Logger logger = Logger.getLogger(MapPanel.class.getName());

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** Cached static layer (edges, junctions) */
	private BufferedImage staticLayer = null;

	/**
	 * Instantiates a new map panel.
	 */
	public MapPanel() {
		initializeMapControl();
	}

	/**
	 * Overrides paintComponent method from {@link java.awt.Component}. Gets the
	 * information from {@code Settings.network} and draw them on the panel.
	 * Graphics is replaced by Graphics2D for more advanced drawing features.
	 * 
	 * @param g the {@link Graphics}
	 */
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2D = (Graphics2D) g;

		AffineTransform old = g2D.getTransform();

		g2D.setStroke(Constants.DEFAULT_STROKE);

		double xCenter = getWidth() / 2.0;
		double yCenter = getHeight() / 2.0;

		if (Settings.config.getStaticLayerDirty()) {
			renderStaticLayer(Math.toRadians(Settings.config.ANGLE), xCenter, yCenter);
			Settings.config.setStaticLayerDirty(false);
		}

		// Draw cached static layer
		if (staticLayer != null) {
			g2D.drawImage(staticLayer, 0, 0, null);
		}

		g2D.rotate(Math.toRadians(Settings.config.ANGLE), xCenter, yCenter);

		// Draw the highlighted Route in a different color (if any)
		if (Settings.highlight.HIGHLIGHTED_ROUTE != null) {
			for (Edge e : Settings.highlight.HIGHLIGHTED_ROUTE.getEdges()) {
				drawObject(g2D, e, Settings.config.HIGHLIGHTED_ROUTE_COLOR);
			}
		}

		// Draw the highlighted Road (filter hover) in a different color (if any)
		if (Settings.highlight.HIGHLIGHTED_ROAD_FILTER != null) {
			for (Edge e : Settings.highlight.HIGHLIGHTED_ROAD_FILTER.getEdgesWithSameBaseName()) {
				drawObject(g2D, e, Settings.config.HIGHLIGHTED_ROAD_FILTER_COLOR);
			}
		}

		if (Settings.highlight.HIGHLIGHTED_CONNECTION != null) {
			drawObject(g2D, Settings.highlight.HIGHLIGHTED_CONNECTION.getFromLane(),
				Settings.config.HIGHLIGHTED_CONNECTION_COLOR);
			drawObject(g2D, Settings.highlight.HIGHLIGHTED_CONNECTION.getToLane(),
				Settings.config.HIGHLIGHTED_CONNECTION_COLOR);
		}

		if (Settings.highlight.HIGHLIGHTED_TRAFFIC_LIGHT != null) {
			drawObject(g2D, Settings.highlight.HIGHLIGHTED_TRAFFIC_LIGHT.getJunction(),
				Settings.config.HIGHLIGHTED_TRAFFIC_LIGHT_COLOR);
		}

		for (TrafficLight tl : Settings.network.getTrafficLights()) {
			try {
				drawObject(g2D, tl);
			} catch (Exception e1) {
				logger.log(Level.SEVERE,
					String.format("Failed to draw the traffic light (%s) in Map Panel!", tl.toString()), e1);
			}
		}

		App.lock.lock();
		try {
			for (Vehicle v : VehicleController.getVehicles()) {
				try {
					v.draw(g2D, v.getVehicleColor());
				} catch (Exception e) {
					logger.log(Level.SEVERE,
						String.format("Failed to draw the vehicle (%s) in Map Panel!", v.toString()), e);
				}
			}
		} finally {
			App.lock.unlock();
		}

		// Avoid changing too immediately, because it keep increase the angle
		g2D.setTransform(old);
	}

	public void updateVehicleStates(int step) {
		boolean blink = (step / 10) % 2 == 0; // blinking every 10 steps

		for (Vehicle v : VehicleController.getVehicles()) {
			// Brake light if speed < 2
			v.setBrake(v.getSpeed() < 2);

			// Headlights always on
			v.setHeadlightsOn(true);

			// Turn signals based on nextTurn
			v.setTurningLeft(v.getNextTurn() == Vehicle.Turn.LEFT && blink);
			v.setTurningRight(v.getNextTurn() == Vehicle.Turn.RIGHT && blink);

			// Emergency flashing
			v.setEmergencyFlashing(v.getType().getId().equals("emergency") && v.isEmergencyFlashing() && blink);
		}

	}

	/**
	 * Draws an {@link Edge} on the map.
	 *
	 * @param g the {@link Graphics2D}
	 * @param e the edge
	 * @param c the color
	 */
	private void drawObject(Graphics2D g, Edge e, Color c) {
		Lane[] lanes = e.getLanes();
		int laneSize = e.getLanesSize();
		for (int i = 0; i < laneSize; i++) {
			drawObject(g, lanes[i], c);
		}

		if (laneSize <= 1) {
			return;
		}

		// Draw lane dividers
		drawLaneDividers(g, lanes, laneSize);
	}

	/**
	 * Draw lane dividers.
	 *
	 * @param g        the {@link Graphics2D}
	 * @param lanes    the lanes
	 * @param laneSize the lane size
	 */
	private void drawLaneDividers(Graphics2D g, Lane[] lanes, int laneSize) {
		GraphicsSettings oldSettings = GraphicsSettings.saveCurrentGraphicsSettings(g);

		/*
		 * same amount of line and no line
		 * 
		 * so it's like ----- ----- -----
		 * 
		 * if want to change -> multiply it by a value
		 */
		float dashLength = (float) (Settings.config.LANE_DIVIDER_DASH_LENGTH * Settings.config.SCALE);
		float[] dashPattern = { dashLength, dashLength };

		g.setStroke(new BasicStroke((float) (Settings.config.LANE_DIVIDER_STROKE_SIZE * Settings.config.SCALE),
			BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, dashPattern, 0));
		g.setColor(Settings.config.LANE_DIVIDER_COLOR);

		for (int i = 0; i < laneSize - 1; i++) {
			Lane lane1 = lanes[i];
			Lane lane2 = lanes[i + 1];

			Point[] shape1 = lane1.getShape();
			Point[] shape2 = lane2.getShape();

			int pointsCnt = Math.min(lane1.getShapeSize(), lane2.getShapeSize());

			for (int j = 0; j < pointsCnt - 1; j++) {
				Point p11 = shape1[j].fromWorldToMap();
				Point p12 = shape2[j].fromWorldToMap();
				Point p21 = shape1[j + 1].fromWorldToMap();
				Point p22 = shape2[j + 1].fromWorldToMap();
				Point from = p11.add(p12).scale(0.5);
				Point to = p21.add(p22).scale(0.5);
				g.drawLine((int) from.getX(), (int) from.getY(), (int) to.getX(), (int) to.getY());
			}
		}

		GraphicsSettings.loadGraphicsSettings(g, oldSettings);
	}

	/**
	 * Draws a {@link Lane} on the map
	 *
	 * @param g the {@link Graphics2D}
	 * @param l the lane
	 * @param c the color
	 */
	private void drawObject(Graphics2D g, Lane l, Color c) {
		GraphicsSettings oldSettings = GraphicsSettings.saveCurrentGraphicsSettings(g);

		Point[] shape = l.getShape();
		int size = l.getShapeSize();

		if (size < 2) {
			return;
		}

		int[] xPoints = new int[size];
		int[] yPoints = new int[size];

		for (int i = 0; i < size; i++) {
			Point p = shape[i].fromWorldToMap();
			xPoints[i] = (int) p.getX();
			yPoints[i] = (int) p.getY();
		}

		float lineThickness = (float) (Settings.config.LANE_STROKE_SIZE * Settings.config.SCALE);

		g.setColor(c);
		g.setStroke(new BasicStroke(lineThickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		g.drawPolyline(xPoints, yPoints, size);

		GraphicsSettings.loadGraphicsSettings(g, oldSettings);
	}

	/**
	 * Draws a {@link TrafficLight} on the map.
	 *
	 * @param g  the {@link Graphics2D}
	 * @param tl the traffic light
	 */
	private void drawObject(Graphics2D g, TrafficLight tl) {
		String state = tl.getTLState();
		List<Connection> connections = tl.getConnections();
		// String previousLaneID = null;

		for (int i = 0; i < connections.size(); i++) {
			Connection connect = connections.get(i);

			char signal = state.charAt(i);
			Color color = TrafficLight.getTrafficLightColor(signal);

			Lane fromLane = connect.getFromLane();

			Point[] shape = fromLane.getShape();
			Point end = shape[shape.length - 1].fromWorldToMap();

			// If a lane contain 2 connection -> 2 traffic light. If
			// we care about this one again, we can comeback this code
			// if (fromLane.getLaneId().equals(previousLaneID))
			// {
			// end.modifyY(4);
			// }

			// previousLaneID = fromLane.getLaneId();
			int radius = (int) (Settings.config.TRAFFIC_LIGHT_RADIUS * Settings.config.SCALE);
			drawCircle(g, end, radius, color);
		}
	}

	/**
	 * Draws a {@link Junction} on the map.
	 * 
	 * @param g the {@link Graphics2D}
	 * @param j the junction
	 */
	// Draw Junction
	private void drawObject(Graphics2D g, Junction j, Color c) {
		GraphicsSettings oldSettings = GraphicsSettings.saveCurrentGraphicsSettings(g);

		Point[] shape = j.getShape();
		int size = j.getShapeSize();

		if (size < 2) {
			return;
		}

		int[] xPoints = new int[size];
		int[] yPoints = new int[size];

		for (int i = 0; i < size; i++) {
			Point p = shape[i].fromWorldToMap();
			xPoints[i] = (int) p.getX();
			yPoints[i] = (int) p.getY();
		}

		g.setColor(c);
		g.setStroke(new BasicStroke((float) (Settings.config.JUNCTION_STROKE_SIZE * Settings.config.SCALE)));

		g.fillPolygon(xPoints, yPoints, size);
		g.drawPolygon(xPoints, yPoints, size);

		GraphicsSettings.loadGraphicsSettings(g, oldSettings);
	}

	/**
	 * Draws a circle on the map.
	 *
	 * @param g      the {@link Graphics2D}
	 * @param center the center
	 * @param radius the radius
	 * @param color  the color
	 */
	private void drawCircle(Graphics2D g, Point center, int radius, Color color) {
		GraphicsSettings oldSettings = GraphicsSettings.saveCurrentGraphicsSettings(g);
		g.setColor(color);
		g.fillOval((int) center.getX() - radius, (int) center.getY() - radius, radius * 2, radius * 2);
		GraphicsSettings.loadGraphicsSettings(g, oldSettings);
	}

	/**
	 * @deprecated Converts the real-world coordinate to the map coordinate.
	 * 
	 * @param before the real-world coordinate
	 * @return the map coordinate
	 */
	@SuppressWarnings("unused")
	private Point translateCoords(Point before) {
		Point after = new Point();

		// -1 here to flip the Y-axis, because Y increases downward in graphics
		// coordinates

		// I will let this one at normally, But after implementing the rotation, please
		// delete
		// -Settings.config.OFFSET.getX() and getY() to the code that I commented ( In
		// rotation logic )
		double xCenter = 0;
		double yCenter = 0;
		double dx = Settings.config.OFFSET.getX() - xCenter;
		double dy = Settings.config.OFFSET.getY() - yCenter;

		double a = Math.toRadians(Settings.config.ANGLE);
		double rx = dx * Math.cos(-a) - dy * Math.sin(-a) + xCenter;
		double ry = dx * Math.sin(-a) + dy * Math.cos(-a) + yCenter;

		after.setX(before.getX() * Settings.config.SCALE - rx);
		after.setY(before.getY() * Settings.config.SCALE * -1 - ry);

		return after;
	}

	/**
	 * Render static layer (edges, junctions) and cache as BufferedImage. This is
	 * rendered once per zoom/pan operation.
	 */
	private void renderStaticLayer(double angle, double xCenter, double yCenter) {
		staticLayer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2D = staticLayer.createGraphics();

		g2D.setStroke(Constants.DEFAULT_STROKE);
		g2D.setColor(getBackground());
		g2D.fillRect(0, 0, getWidth(), getHeight());

		g2D.rotate(angle, xCenter, yCenter);

		// Draw edges (static, cached)
		for (Edge e : Settings.network.getEdges()) {
			drawObject(g2D, e, Settings.config.LANE_COLOR);
		}

		// Draw junctions (static, cached)
		for (Junction j : Settings.network.getJunctions()) {
			drawObject(g2D, j, Settings.config.JUNCTION_COLOR);
		}

		g2D.dispose();
	}

	/**
	 * Initialize map control.
	 */
	public void initializeMapControl() {
		// InputMap inputMap = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		// ActionMap actionMap = this.getActionMap();
		//
		// // Only for US keyboard
		// KeyStroke zoomInKey = KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS,
		// InputEvent.CTRL_DOWN_MASK);
		// KeyStroke zoomOutKey = KeyStroke.getKeyStroke(KeyEvent.VK_MINUS,
		// InputEvent.CTRL_DOWN_MASK);
		// KeyStroke moveUpKey = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0);
		// KeyStroke moveDownKey = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0);
		// KeyStroke moveRightKey = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0);
		// KeyStroke moveLeftKey = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0);
		//
		// inputMap.put(zoomInKey, "zoomIn");
		// inputMap.put(zoomOutKey, "zoomOut");
		// inputMap.put(moveUpKey, "moveUp");
		// inputMap.put(moveDownKey, "moveDown");
		// inputMap.put(moveLeftKey, "moveLeft");
		// inputMap.put(moveRightKey, "moveRight");
		//
		// actionMap.put("zoomIn", new AbstractAction() {
		// @Override
		// public void actionPerformed(ActionEvent e) {
		// Settings.config.modifyScale(Settings.config.SCALE_STEP);
		// Settings.config.invalidateStaticLayer(); // Invalidate cache when zoom
		// changes
		// }
		// });
		//
		// actionMap.put("zoomOut", new AbstractAction() {
		// @Override
		// public void actionPerformed(ActionEvent e) {
		// Settings.config.modifyScale(-Settings.config.SCALE_STEP);
		// Settings.config.invalidateStaticLayer(); // Invalidate cache when zoom
		// changes
		// }
		// });
		//
		// actionMap.put("moveUp", new AbstractAction() {
		// @Override
		// public void actionPerformed(ActionEvent e) {
		// Settings.config.modifyOffsetY(-Settings.config.OFFSET_STEP);
		// Settings.config.invalidateStaticLayer(); // Invalidate cache when pan changes
		// }
		//
		// });
		//
		// actionMap.put("moveDown", new AbstractAction() {
		// @Override
		// public void actionPerformed(ActionEvent e) {
		// Settings.config.modifyOffsetY(Settings.config.OFFSET_STEP);
		// Settings.config.invalidateStaticLayer(); // Invalidate cache when pan changes
		// }
		// });
		//
		// actionMap.put("moveRight", new AbstractAction() {
		// @Override
		// public void actionPerformed(ActionEvent e) {
		// Settings.config.modifyOffsetX(Settings.config.OFFSET_STEP);
		// Settings.config.invalidateStaticLayer(); // Invalidate cache when pan changes
		// }
		// });
		//
		// actionMap.put("moveLeft", new AbstractAction() {
		// @Override
		// public void actionPerformed(ActionEvent e) {
		// Settings.config.modifyOffsetX(-Settings.config.OFFSET_STEP);
		// Settings.config.invalidateStaticLayer(); // Invalidate cache when pan changes
		// }
		// });

		// TODO: Modify the angle using mouse

		Mouse mouseAction = new Mouse();
		this.addMouseListener(mouseAction);
		this.addMouseMotionListener(mouseAction);
		this.addMouseWheelListener(mouseAction);

		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				Settings.config.invalidateStaticLayer(); // Invalidate cache when users resize the frame
			}
		});
	}

}
