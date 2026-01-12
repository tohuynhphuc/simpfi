package com.simpfi.export;

import com.simpfi.object.Vehicle;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Exports vehicle simulation data to a CSV file for external analysis.
 */
public class VehicleCsvExporter {

    /**
     * Writes the given vehicles to a CSV file.
     *
     * @param vehicles list of vehicles to export
     * @param file     target CSV file
     * @throws IOException if writing fails
     */
    public static void exportVehicles(List<Vehicle> vehicles, File file) throws IOException {

        FileWriter writer = new FileWriter(file);

        writer.write("id,type,speed,maxSpeed,acceleration,distance,route\n");

        for (Vehicle v : vehicles) {
            writer.write(
                    v.getID() + "," +
                            v.getType().getId() + "," +
                            v.getSpeed() + "," +
                            v.getMaxSpeed() + "," +
                            v.getAcceleration() + "," +
                            v.getDistance() + "," +
                            String.join("->", v.getRoute()) +
                            "\n"
            );
        }

        writer.flush();
        writer.close();
    }
}
