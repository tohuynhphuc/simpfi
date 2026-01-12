package com.simpfi.ui;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import com.simpfi.config.Settings;

public class Mouse extends MouseAdapter {
	private static Point previousPoint;

	@Override
	public void mousePressed(MouseEvent event) {
		previousPoint = event.getPoint();
		Settings.config.invalidateStaticLayer();
	}

	@Override
	public void mouseDragged(MouseEvent event) {
		Point currentPoint = event.getPoint();

		// Calculate the distance to move
		int dx = currentPoint.x - previousPoint.x;
		int dy = currentPoint.y - previousPoint.y;

		double a = Math.toRadians(Settings.config.ANGLE);
		double rx = dx * Math.cos(-a) - dy * Math.sin(-a);
		double ry = dx * Math.sin(-a) + dy * Math.cos(-a);

		if (event.isControlDown()) {
			Settings.config.modifyAngle(dx);
		} else {
			Settings.config.modifyOffsetX(-rx);
			Settings.config.modifyOffsetY(-ry);
		}
		previousPoint = currentPoint;
		Settings.config.invalidateStaticLayer();
		event.getComponent().repaint();
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent event) {
		double rotation = event.getPreciseWheelRotation();
		Settings.config.modifyScale(-rotation);
		Settings.config.invalidateStaticLayer();

		// Apply the change immediately
		event.getComponent().repaint();
	}

}
