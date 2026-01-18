package com.simpfi.export;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.simpfi.object.Vehicle;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

public class VehiclePdfExporter {

    /**
     * Exports a list of vehicles as a PDF report.
     *
     * @param vehicles List of vehicles to export
     * @param file Target PDF file
     * @param congestionThreshold Threshold defining when an edge is considered congested
     */
    public static void exportVehicles(List<Vehicle> vehicles, File file, int congestionThreshold) throws Exception {
        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        Paragraph title = new Paragraph("Vehicle Simulation Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(Chunk.NEWLINE);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        document.add(new Paragraph("Generated: " + timestamp));
        document.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        addHeader(table, "ID");
        addHeader(table, "Type");
        addHeader(table, "Speed");
        addHeader(table, "Max Speed");
        addHeader(table, "Acceleration");
        addHeader(table, "Distance");

        for (Vehicle v : vehicles) {
            table.addCell(v.getId());
            table.addCell(v.getType().getId());
            table.addCell(String.format("%.2f", v.getSpeed()));
            table.addCell(String.format("%.2f", v.getMaxSpeed()));
            table.addCell(String.format("%.2f", v.getAcceleration()));
            table.addCell(String.format("%.2f", v.getDistance()));
        }

        document.add(table);
        document.add(Chunk.NEWLINE);

        // Chart: Congestion pro Edge & Vehicle Type
        Image chartImage = createCongestionChart(writer, vehicles, congestionThreshold);
        chartImage.setAlignment(Element.ALIGN_CENTER);
        document.add(chartImage);

        document.close();
    }

    /**
     * Wrapper for exportVehicles using a default congestion threshold of 5.
     *
     * @param vehicles List of vehicles
     * @param file     Target PDF file
     */
    public static void exportVehicles(List<Vehicle> vehicles, File file) throws Exception {
        exportVehicles(vehicles, file, 5);
    }

    /**
     * Adds a header cell to a PDF table.
     *
     * @param table Table to add the header to
     * @param text  Header text
     * */
    private static void addHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        table.addCell(cell);
    }
    /**
     * Creates a simple horizontal bar chart showing congested edges.
     * Bar color indicates congestion: green = low, yellow = medium, red = high.
     *
     * @param writer    PDFWriter instance
     * @param vehicles  List of vehicles
     * @param threshold Threshold for congested edges
     * @return PDF-compatible Image for insertion
     */
    private static Image createCongestionChart(PdfWriter writer, List<Vehicle> vehicles, int threshold) throws Exception {

        // Dataset nur für congested edges
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // EdgeID -> vehicle count
        vehicles.stream()
                .map(Vehicle::getEdgeFromRoadID)
                .filter(edge -> edge != null)
                .distinct()
                .forEach(edge -> {
                    long count = vehicles.stream()
                            .filter(v -> v.getIsActive() &&
                                    v.getEdgeFromRoadID() != null &&
                                    v.getEdgeFromRoadID().getId().equals(edge.getId()))
                            .count();
                    if (count >= threshold) {
                        dataset.setValue(count, "Vehicles", edge.getId());
                    }
                });

        if (dataset.getColumnCount() == 0) {
            dataset.setValue(0, "Vehicles", "N/A");
        }

        // Gestapeltes Balkendiagramm horizontal
        JFreeChart chart = ChartFactory.createBarChart(
                "Congested Edges",
                "Edge ID",          // Domain Axis → Y-Achse bei horizontal
                "Vehicle Count", // Range Axis → X-Achse bei horizontal
                dataset,
                PlotOrientation.HORIZONTAL,
                false,
                true,
                false
        );

        // Renderer Farbe + Werte auf Balken
        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();

        for (int i = 0; i < dataset.getColumnCount(); i++) {
            Number value = dataset.getValue(0, i);
            if (value != null) {
                double ratio = value.doubleValue() / threshold;
                if (ratio < 1.5) renderer.setSeriesPaint(0, Color.GREEN);
                else if (ratio < 3) renderer.setSeriesPaint(0, Color.YELLOW);
                else renderer.setSeriesPaint(0, Color.RED);
            }
        }

        renderer.setBaseItemLabelsVisible(true);
        renderer.setBaseItemLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10));
        renderer.setBaseItemLabelPaint(Color.BLACK);

        plot.getDomainAxis().setTickLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));
        plot.getRangeAxis().setTickLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10));

        int categoryHeight = 50;
        int minHeight = 200;
        int height = Math.max(minHeight, dataset.getColumnCount() * categoryHeight);
        int width = 600;

        BufferedImage bufferedImage = chart.createBufferedImage(width, height);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(bufferedImage, "png", baos);
        baos.flush();
        byte[] imageInBytes = baos.toByteArray();
        baos.close();

        return Image.getInstance(imageInBytes);
    }

}
