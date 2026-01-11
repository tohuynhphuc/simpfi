package com.simpfi.export.filter;

import com.simpfi.object.Vehicle;

import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;

public class VehicleFilters {

    // ===== COLOR =====
    public static List<Vehicle> byColor(List<Vehicle> vehicles, Color color) {
        return vehicles.stream()
                .filter(v -> color.equals(v.getVehicleColor()))
                .collect(Collectors.toList());
    }

    // ===== VEHICLE TYPE =====
    public static List<Vehicle> byType(List<Vehicle> vehicles, String typeId) {
        return vehicles.stream()
                .filter(v -> v.getType().getId().equals(typeId))
                .collect(Collectors.toList());
    }

    // ===== SPEED =====
    public static List<Vehicle> fasterThan(List<Vehicle> vehicles, double minSpeed) {
        return vehicles.stream()
                .filter(v -> v.getSpeed() >= minSpeed)
                .collect(Collectors.toList());
    }

    public static List<Vehicle> slowerThan(List<Vehicle> vehicles, double maxSpeed) {
        return vehicles.stream()
                .filter(v -> v.getSpeed() <= maxSpeed)
                .collect(Collectors.toList());
    }

    // ===== CONGESTED EDGES (INTERPRETATION) =====
    /**
     * Vehicles considered to be on congested edges based on low speed.
     * This is an indirect congestion approximation.
     */
    public static List<Vehicle> onCongestedEdges(List<Vehicle> vehicles, double congestionSpeedThreshold) {
        return vehicles.stream()
                .filter(v -> v.getSpeed() <= congestionSpeedThreshold)
                .collect(Collectors.toList());
    }
}
