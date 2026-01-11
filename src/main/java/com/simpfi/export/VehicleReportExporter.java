package com.simpfi.export;

import com.simpfi.object.Vehicle;

import java.io.File;
import java.util.List;

/**
 * Coordinates all vehicle report exports.
 */
public class VehicleReportExporter {

    public static void exportAll(List<Vehicle> vehicles) throws Exception {
        VehicleCsvExporter.exportVehicles(vehicles, new File("vehicles.csv"));
        VehiclePdfExporter.exportVehicles(vehicles, new File("vehicles.pdf"));
    }
}
