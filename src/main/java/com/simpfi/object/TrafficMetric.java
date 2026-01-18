package com.simpfi.object;

/**
 * Represents traffic performance metrics for a simulation
 * <p>
 * The class stores average speed, total congestion count, and average travel time
 * </p>
 */
public class TrafficMetric {
	/** Average speed of vehicles */
	private double avgSpeed;

	/** Total number of congested edges */
	private int totalCongestion;

	/** Average travel time of vehicles */
	private double avgTravelTime;

	/**
     * Constructs a {@code TrafficMetric} object with the specified values.
     *
     * @param avgSpeed        the average speed of vehicles
     * @param tc              the total congestion count
     * @param att             the average travel time
     */
	public TrafficMetric(double avgSpeed, int tc, double att) {
		this.avgSpeed = avgSpeed;
		this.totalCongestion = tc;
		this.avgTravelTime = att;
	}

	/**
     * Returns the average speed of vehicles.
     *
     * @return the average speed
     */
	public double getAvgSpeed() {
		return this.avgSpeed;
	}

	/**
     * Returns the total number of congestion events.
     *
     * @return the total congestion count
     */
	public int getTotalCongestion() {
		return this.totalCongestion;
	}

	/**
     * Returns the average travel time of vehicles.
     *
     * @return the average travel time
     */
	public double getAvgTravelTime() {
		return this.avgTravelTime;
	}

}