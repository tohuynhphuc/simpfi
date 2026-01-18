package com.simpfi.object;

import java.util.Map;

public final class SimulationSnapshot {

    public final int step;
    public final Map<String, Vehicle> vehicles;
    public final Map<String, String> trafficLightStates;

    public SimulationSnapshot(
        int step,
        Map<String, Vehicle> vehicles,
        Map<String, String> trafficLightStates
    ){
        this.step = step;
        this.vehicles = Map.copyOf(vehicles);
        this.trafficLightStates = Map.copyOf(trafficLightStates);
    }
}