package com.simpfi.object;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.List;

import com.simpfi.config.Settings;
import com.simpfi.util.GraphicsSettings;
import com.simpfi.util.Point;

/**
 * Creates Edge class (may includes {@link de.tudresden.sumo.cmd.Edge} in the
 * future).
 */
public class Edge extends SumoObject implements Drawable {

	/** The conjunction where the edge starts. */
	Junction conjunctionFrom;

	/** The conjunction where the edge ends. */
	Junction conjunctionTo;

	/** All lanes of the edge. */
	Lane[] lanes;

	/** Number of lanes of the edge. */
	int lanesSize;

	/**
	 * Instantiates a new edge.
	 *
	 * @param id    the id of the edge
	 * @param from  the conjunction where the edge starts
	 * @param to    the onjunction where the edge ends
	 * @param lanes all the lanes of the edge
	 */
	public Edge(String id, Junction from, Junction to, Lane[] lanes) {
		this.id = id;
		this.conjunctionFrom = from;
		this.conjunctionTo = to;
		this.lanes = lanes;
		this.lanesSize = this.lanes.length;
	}

	/**
	 * Returns the lanes.
	 *
	 * @return the lanes
	 */
	public Lane[] getLanes() {
		return lanes;
	}

	/**
	 * Returns the lanes size.
	 *
	 * @return the lanes size
	 */
	public int getLanesSize() {
		return lanesSize;
	}

	/**
	 * Used to search over a list of edges to find one with the matched id.
	 * 
	 * @param id    the id of the edge to be searched for
	 * @param edges the list of edges
	 * @return the edge with the passed id, {@code null} if not found
	 */
	public static Edge searchForEdge(String id, List<Edge> edges) {
		for (int i = 0; i < edges.size(); i++) {
			if (edges.get(i).getId().equals(id)) {
				return edges.get(i);
			}
		}
		return null;
	}

	/**
	 * Overrides the built-in method toString() to provide a human-readable
	 * representation of Edge.
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return "Edge [edgeId=" + id + ", conjunctionFrom=" + conjunctionFrom + ", conjunctinTo=" + conjunctionTo
			+ ", lanes=" + Arrays.toString(lanes) + "]";
	}

	/**
	 * Draws an {@link Edge} on the map.
	 *
	 * @param g the {@link Graphics2D}
	 * @param c the color
	 */
	@Override
	public void draw(Graphics2D g, Color c) {
		Lane[] lanes = getLanes();
		int laneSize = getLanesSize();
		for (int i = 0; i < laneSize; i++) {
			lanes[i].draw(g, c);
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

}
