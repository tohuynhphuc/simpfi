package com.simpfi.object;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.List;

import com.simpfi.config.Constants;
import com.simpfi.config.Settings;
import com.simpfi.util.GraphicsSettings;
import com.simpfi.util.Point;

/**
 * Creates Vehicle class.
 */
public class Vehicle extends SumoObject implements Drawable {

	/** The vehicle position. */
	private Point position;

	/** The vehicle speed. */
	private double speed;

	/** The road ID that the vehicle is on. */
	private String roadID;

	/** The vehicle type. */
	private VehicleType type;

	/** The vehicle angle. */
	private double angle;

	/** The vehicle width. */
	private double width;

	/** The vehicle height. */
	private double height;

	private double maxSpeed;
	private double acceleration;
	private double distance;
	private List<String> route;

	/**
	 * The active state of the vehicle. A vehicle is considered inactive if it has
	 * completed its journey.
	 */
	private Boolean isActive;

	/** Lights and states */
	private boolean headlightsOn = true;
	private boolean brakeOn = false;
	private boolean turningLeft = false;
	private boolean turningRight = false;
	private boolean emergencyFlashing = false;

	public enum Turn {
		LEFT, RIGHT, STRAIGHT, NONE
	}

	private Turn nextTurn = Turn.NONE;

	/**
	 * Overloaded Constructor assigning provided parameters. Compares the provided
	 * type ID to the IDs of all {@link VehicleType} objects in
	 * {@code Settings.network}.
	 *
	 * @param id     the id
	 * @param point  the point
	 * @param roadID the road ID
	 * @param type   the type
	 * @param angle  the angle
	 * @param width  the width
	 * @param height the height
	 * @param speed  the speed
	 */

	public Vehicle(String id, Point point, String roadID, String type, double angle, double width, double height,
		double speed, double maxSpeed, double acceleration, double distance, List<String> route) {
		this.id = id;
		this.position = point;
		this.roadID = roadID;
		this.angle = angle;
		this.width = width;
		this.height = height;
		this.speed = speed;

		this.type = null;

		this.route = route;
		this.maxSpeed = maxSpeed;
		this.acceleration = acceleration;
		this.distance = distance;

		for (VehicleType vType : Settings.network.getVehicleTypes()) {
			if (vType.getId().equals(type)) {
				this.type = vType;
				break;
			}
		}
		if (this.type == null) {
			System.err.println("Invalid vehicle type: " + type);
		}

		isActive = false;
	}

	/**
	 * Returns the position.
	 *
	 * @return the position
	 */
	public Point getPosition() {
		return position;
	}

	/**
	 * Returns the speed.
	 *
	 * @return the speed
	 */
	public double getSpeed() {
		return speed;
	}

	/**
	 * Returns the road ID.
	 *
	 * @return the road ID
	 */
	public String getRoadID() {
		return roadID;
	}

	/**
	 * Returns the angle.
	 *
	 * @return the angle
	 */
	public double getAngle() {
		return angle;
	}

	/**
	 * Returns the type.
	 *
	 * @return the type
	 */
	public VehicleType getType() {
		return type;
	}

	/**
	 * Returns the width.
	 *
	 * @return the width
	 */
	public double getWidth() {
		return width;
	}

	/**
	 * Returns the height.
	 *
	 * @return the height
	 */
	public double getHeight() {
		return height;
	}

	/**
	 * Returns the checks if vehicle is active.
	 *
	 * @return the checks if vehicle is active
	 */
	public Boolean getIsActive() {
		return isActive;
	}

	public double getMaxSpeed() {
		return maxSpeed;
	}

	public double getAcceleration() {
		return acceleration;
	}

	public double getDistance() {
		return distance;
	}

	public List<String> getRoute() {
		return route;
	}

	/**
	 * Sets the active state of vehicle.
	 *
	 * @param isActive the active state of vehicle
	 */
	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

	/**
	 * Returns the vehicle color based on type. Vehicle colors are defined in
	 * {@link com.simpfi.config.Constants}
	 *
	 * @return the vehicle color
	 */
	public Color getVehicleColor() {
		return switch (type.getId()) {
		case "truck" -> Settings.config.TRUCK_COLOR;
		case "bus" -> Settings.config.BUS_COLOR;
		case "motorcycle" -> Settings.config.MOTORCYCLE_COLOR;
		case "emergency" -> Settings.config.EMERGENCY_COLOR;
		default -> Settings.config.NORMAL_VEHICLE_COLOR;
		};
	}

	/**
	 * Returns the edge derived from RoadID.
	 *
	 * @return the {@code Edge} object
	 */
	public Edge getEdgeFromRoadID() {
		for (Edge e : Settings.network.getEdges()) {
			if (e.getId().equals(this.roadID)) {
				return e;
			}
		}
		return null;
	}

	/**
	 * Overrides the built-in method toString() to provide a human-readable
	 * representation of Vehicle.
	 *
	 * @return the representation of Vehicle
	 */
	@Override
	public String toString() {
		return "Vehicle [id=" + id + ", position=" + position + ", speed=" + speed + ", roadID=" + roadID + ", type="
			+ type + ", isActive=" + isActive + "]";
	}

	/**
	 * Returns whether the vehicle's headlights are currently on.
	 * 
	 * @return {@code true} if headlights are on, {@code false} otherwise
	 */
	public boolean isHeadlightsOn() {
		return headlightsOn;
	}

	/**
	 * Sets the state of the vehicle's headlights.
	 * 
	 * @param state {@code true} to turn headlights on, {@code false} to turn them
	 *              off
	 */
	public void setHeadlightsOn(boolean state) {
		this.headlightsOn = state;
	}

	/**
	 * Returns whether the vehicle is currently braking.
	 * 
	 * @return {@code true} if brakes are applied, {@code false} otherwise
	 */
	public boolean isBraking() {
		return brakeOn;
	}

	/**
	 * Sets the braking state of the vehicle.
	 * 
	 * @param state {@code true} to apply brakes, {@code false} to release them
	 */
	public void setBrake(boolean state) {
		this.brakeOn = state;
	}

	/**
	 * Returns whether the vehicle is currently turning left.
	 * 
	 * @return {@code true} if the left turn signal is active, {@code false}
	 *         otherwise
	 */
	public boolean isTurningLeft() {
		return turningLeft;
	}

	/**
	 * Sets the left turn signal state.
	 * 
	 * @param state {@code true} to indicate a left turn, {@code false} otherwise
	 */
	public void setTurningLeft(boolean state) {
		this.turningLeft = state;
	}

	/**
	 * Returns whether the vehicle is currently turning right.
	 * 
	 * @return {@code true} if the right turn signal is active, {@code false}
	 *         otherwise
	 */
	public boolean isTurningRight() {
		return turningRight;
	}

	/**
	 * Sets the right turn signal state.
	 * 
	 * @param state {@code true} to indicate a right turn, {@code false} otherwise
	 */
	public void setTurningRight(boolean state) {
		this.turningRight = state;
	}

	/**
	 * Returns whether the vehicle's emergency lights are flashing.
	 * 
	 * @return {@code true} if emergency lights are on, {@code false} otherwise
	 */
	public boolean isEmergencyFlashing() {
		return emergencyFlashing;
	}

	/**
	 * Sets the state of the vehicle's emergency lights.
	 * 
	 * @param state {@code true} to turn on emergency lights, {@code false} to turn
	 *              them off
	 */
	public void setEmergencyFlashing(boolean state) {
		this.emergencyFlashing = state;
	}

	/**
	 * Returns the next turn that the vehicle will take.
	 * 
	 * @return a {@link Turn} object representing the next turn
	 */
	public Turn getNextTurn() {
		return nextTurn;
	}

	/**
	 * Sets the next turn for the vehicle.
	 * 
	 * @param t a {@link Turn} object representing the next turn
	 */
	public void setNextTurn(Turn t) {
		this.nextTurn = t;
	}

	@Override
	public void draw(Graphics2D g, Color c) {
		// We don't draw inactive vehicles
		if (this == null || !getIsActive()) {
			return;
		}

		// Implement Lazy Drawing: only vehicles within the view are drawn
		int screenWidth = g.getClipBounds().width;
		int screenHeight = g.getClipBounds().height;
		Point position = getPosition().fromWorldToMap();
		int size = (int) (Math.max(screenWidth, screenHeight) * Settings.config.SCALE
			* Settings.config.VEHICLE_UPSCALE);

		// Skip if vehicle is off-screen
		if (position.getX() < -size || position.getX() > screenWidth + size || position.getY() < -size
			|| position.getY() > screenHeight + size) {
			System.out.println("Position: " + position.toString() + " size: " + size + " getWidth: " + screenWidth
				+ " getHeight() " + screenHeight);
			return;
		}

		// We don't draw vehicles whose type is filtered out
		if (getType() != null && !getType().getFilterFlag()) {
			return;
		}

		// We don't draw vehicles which run on unselected roads
		if (getRoadID() != null && getRoadID().charAt(1) != 'J') {
			Road road = Settings.network.getRoadFromEdge(getEdgeFromRoadID());
			if (road != null && !road.getFilterFlag()) {
				return;
			}
		}

		// We don't draw vehicles which are not with the filtered speed range
		if (getSpeed() < Settings.highlight.LOWER_BOUND_LIMIT || getSpeed() > Settings.highlight.UPPER_BOUND_LIMIT) {
			return;
		}

		GraphicsSettings oldSettings = GraphicsSettings.saveCurrentGraphicsSettings(g);

		double lengthMultipler = 1.5;
		double narrowWidth = 0.8;
		Point pos = getPosition().fromWorldToMap();
		int width = (int) (getWidth() * lengthMultipler * Settings.config.SCALE * Settings.config.VEHICLE_UPSCALE);
		int height = (int) (getHeight() * narrowWidth * Settings.config.SCALE * Settings.config.VEHICLE_UPSCALE);

		// int x = (int) pos.getX() - width / 2;
		// int y = (int) pos.getY() - height / 2;

		int drawX = -width / 2;
		int drawY = -height;

		// int light = height / 6;
		// int headlightFrontY = drawY + height / 6;

		int bodyLeft = drawX;
		int bodyRight = drawX + width;
		int bodyTop = drawY;
		// int bodyBottom = drawY + height;
		// int frontOffset = (int) (height / 2.0);

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setStroke(Constants.DEFAULT_STROKE);
		g2.translate(pos.getX(), pos.getY());
		g2.rotate(Math.toRadians(getAngle() - 90));
		g2.translate(0, height / 2);

		// Draw vehicle body
		g2.setColor(c);
		g2.fillRoundRect(drawX, drawY, width, height, 8, 8);

		// 4 Windows

		Color windowColor = new Color(30, 30, 30, 180);

		// local bounding box
		int BX = drawX;
		int BY = drawY;
		int BW = width;
		int BH = height;

		int cx = BX + BW / 2; // center X
		int cy = BY + BH / 2; // center Y

		// SIZE FACTORS
		int inward = (int) (BH * 0.18); // inward small edge length
		int outward = (int) (BH * 0.40); // outward wide edge length

		// increase factor for width
		double widthFactor = 3.0;

		// FRONT WINDOW
		int fShortW = (int) (inward * widthFactor);
		int fLongW = (int) (outward * widthFactor);
		int fTopY = BY + (int) (BH * 0.05);
		int fBotY = fTopY + (int) (BH * 0.30);

		Polygon poly = new Polygon();
		poly.addPoint(cx - fLongW / 2, fTopY); // wide top-left
		poly.addPoint(cx + fLongW / 2, fTopY); // wide top-right
		poly.addPoint(cx + fShortW / 2, fBotY); // short bottom-right (toward center)
		poly.addPoint(cx - fShortW / 2, fBotY); // short bottom-left
		g2.setColor(windowColor);
		g2.fillPolygon(poly);

		// REAR WINDOW
		int rBotY = BY + BH - (int) (BH * 0.05);
		int rTopY = rBotY - (int) (BH * 0.30);

		poly = new Polygon();
		poly.addPoint(cx - fShortW / 2, rTopY); // short inner top-left
		poly.addPoint(cx + fShortW / 2, rTopY); // short inner top-right
		poly.addPoint(cx + fLongW / 2, rBotY); // wide bottom-right
		poly.addPoint(cx - fLongW / 2, rBotY); // wide bottom-left
		g2.fillPolygon(poly);

		// LEFT WINDOW (short edge faces center → right side)
		int lLeftX = BX + (int) (BW * 0.02);
		int lRightX = lLeftX + (int) (BW * 0.32); // vertical thickness

		int lShortW = inward;
		int lLongW = outward;

		// int lTopY = cy - (int) (BH * 0.14);
		// int lBotY = cy + (int) (BH * 0.14);

		poly = new Polygon();
		poly.addPoint(lLeftX, cy - (lLongW / 2)); // wide top-left
		poly.addPoint(lLeftX, cy + (lLongW / 2)); // wide bottom-left
		poly.addPoint(lRightX, cy + (lShortW / 2)); // short inward edge bottom
		poly.addPoint(lRightX, cy - (lShortW / 2)); // short inward edge top
		g2.fillPolygon(poly);

		// RIGHT WINDOW (short edge faces center → left side)
		int rRightX = BX + BW - (int) (BW * 0.05);
		int rLeftX = rRightX - (int) (BW * 0.32);

		poly = new Polygon();
		poly.addPoint(rLeftX, cy - (lShortW / 2)); // short inward edge top
		poly.addPoint(rLeftX, cy + (lShortW / 2)); // short inward edge bottom
		poly.addPoint(rRightX, cy + (lLongW / 2)); // wide bottom-right
		poly.addPoint(rRightX, cy - (lLongW / 2)); // wide top-right
		g2.fillPolygon(poly);

		// Draw Head Lights
		int lightSize = (int) (height * 0.20);

		int headlightX = bodyRight - lightSize - 2;
		int headlightY1 = bodyTop + (int) (height * 0.20);
		int headlightY2 = bodyTop + (int) (height * 0.70);

		if (isHeadlightsOn()) {
			g2.setColor(new Color(255, 255, 200));
			g2.fillOval(headlightX, headlightY1, lightSize, lightSize);
			g2.fillOval(headlightX, headlightY2, lightSize, lightSize);
		}

		// Brake lights
		int brakeX = bodyLeft + 2;
		int brakeY1 = headlightY1;
		int brakeY2 = headlightY2;

		g2.setColor(isBraking() ? new Color(255, 60, 60) : new Color(150, 0, 0));
		g2.fillOval(brakeX, brakeY1, lightSize, lightSize);
		g2.fillOval(brakeX, brakeY2, lightSize, lightSize);

		// Turn signals
		g2.setColor(new Color(255, 150, 0));

		if (isTurningLeft()) {
			g2.fillOval(brakeX, brakeY1, lightSize, lightSize);
			g2.fillOval(brakeX, brakeY2, lightSize, lightSize);
		}
		if (isTurningRight()) {
			g2.fillOval(headlightX, headlightY1, lightSize, lightSize);
			g2.fillOval(headlightX, headlightY2, lightSize, lightSize);
		}
		g2.dispose();
		GraphicsSettings.loadGraphicsSettings(g, oldSettings);
	}
}
