package com.simpfi.object;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.simpfi.config.Settings;
import com.simpfi.util.GraphicsSettings;
import com.simpfi.util.Point;

/**
 * Creates Junction class (may includes {@link de.tudresden.sumo.cmd.Junction}
 * in the future).
 */
public class Junction extends SumoObject implements Drawable {

	/** The junction type. */
	private String type;

	/** The junction shape. */
	private Point[] shape;

	/** The size of the junction shape. */
	private int shapeSize;

	/** The lanes which ends at the junction. */
	private List<String> incomingLane = new ArrayList<String>();

	/**
	 * Instantiates a new junction.
	 *
	 * @param id    the id
	 * @param type  the type
	 * @param shape the shape
	 */
	public Junction(String id, String type, Point[] shape) {
		this.id = id;
		this.shape = shape;
		this.type = type;
		this.shapeSize = this.shape.length;
	}

	/**
	 * Returns the type.
	 *
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Returns the shape.
	 *
	 * @return the shape
	 */
	public Point[] getShape() {
		return shape;
	}

	/**
	 * Returns the shape size.
	 *
	 * @return the shape size
	 */
	public int getShapeSize() {
		return shapeSize;
	}

	/**
	 * Returns the incoming lane.
	 *
	 * @return the incoming lane
	 */
	public List<String> getIncomingLane() {
		return incomingLane;
	}

	/**
	 * Adds lane to the incoming lanes list.
	 *
	 * @param lane the lane
	 */
	public void addIncomingLane(String lane) {
		incomingLane.add(lane);
	}

	/**
	 * Used to search over a list of junctions to find one with the matched id.
	 * 
	 * @param id        id of the junction that users look for.
	 * @param junctions given list of junctions.
	 * @return the junction with the passed id, {@code null} if not found.
	 */
	public static Junction searchForJunction(String id, List<Junction> junctions) {
		for (int i = 0; i < junctions.size(); i++) {
			if (junctions.get(i).getId().equals(id)) {
				return junctions.get(i);
			}
		}
		return null;
	}

	/**
	 * Overrides the built-in method toString() to provide a human-readable
	 * representation of Junction.
	 *
	 * @return the representation of Junction
	 */
	@Override
	public String toString() {
		return "Junction [id=" + id + ", type=" + type + ", shape=" + Arrays.toString(shape) + ", shapeSize="
			+ shapeSize + ", incomingLane=" + incomingLane + "]";
	}

	/**
	 * Draws a {@link Junction} on the map.
	 * 
	 * @param g the {@link Graphics2D}
	 * @param c the color
	 */
	@Override
	public void draw(Graphics2D g, Color c) {
		GraphicsSettings oldSettings = GraphicsSettings.saveCurrentGraphicsSettings(g);

		Point[] shape = getShape();
		int size = getShapeSize();

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

}
