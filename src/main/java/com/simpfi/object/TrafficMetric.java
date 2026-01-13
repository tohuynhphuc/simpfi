package com.simpfi.object;

public class TrafficMetric {

	private double avgSpeed;
	private int totalCongestion;
	private double avgTravelTime;
	// private TrafficStatistics stats;

	public TrafficMetric(double avgSpeed, int tc, double att) {
		this.avgSpeed = avgSpeed;
		this.totalCongestion = tc;
		this.avgTravelTime = att;
	}

	public double getAvgSpeed() {
		return this.avgSpeed;
	}

	public int getTotalCongestion() {
		return this.totalCongestion;
	}

	public double getAvgTravelTime() {
		return this.avgTravelTime;
	}

}