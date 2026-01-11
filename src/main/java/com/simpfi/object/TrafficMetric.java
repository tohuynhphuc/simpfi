package com.simpfi.object;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


import com.simpfi.object.TrafficStatistics;

public class TrafficMetric{
    private double avgSpeed;
    private int totalCongestion;
    private double avgTravelTime;
    private TrafficStatistics stats;

    public TrafficMetric(double avgSpeed, int tc, double att){
        this.avgSpeed = avgSpeed;
        this.totalCongestion = tc;
        this.avgTravelTime = att;
    }

    public double getAvgSpeed(){
        return this.avgSpeed;
    }

    public int getTotalCongestion(){
        return this.totalCongestion;
    }

    public double getAvgTravelTime(){
        return this.avgTravelTime;
    }
}