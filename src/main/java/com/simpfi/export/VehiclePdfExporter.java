package com.simpfi.export;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.simpfi.object.Vehicle;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates a PDF summary report for vehicle simulation data.
 * Includes metrics, timestamps, and a speed comparison chart.
 */
public class VehiclePdfExporter {

    /**
     * Exports the given vehicles to a PDF file.
     *
     * @param vehicles vehicles to export
     * @param file     target PDF file
     * @throws Exception if PDF generation fails
     */
    public static void exportVehicles(List<Vehicle> vehicles, File file) throws Exception {

        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        // ===== TITLE =====
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        Paragraph title = new Paragraph("Vehicle Simulation Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        document.add(Chunk.NEWLINE);

        // ===== TIMESTAMP =====
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        document.add(new Paragraph("Generated: " + timestamp));

        document.add(Chunk.NEWLINE);

        // ===== TABLE =====
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);

        addHeader(table, "ID");
        addHeader(table, "Type");
        addHeader(table, "Speed");
        addHeader(table, "Max Speed");
        addHeader(table, "Acceleration");
        addHeader(table, "Distance");

        for (Vehicle v : vehicles) {
            table.addCell(v.getID());
            table.addCell(v.getType().getId());
            table.addCell(String.format("%.2f", v.getSpeed()));
            table.addCell(String.format("%.2f", v.getMaxSpeed()));
            table.addCell(String.format("%.2f", v.getAcceleration()));
            table.addCell(String.format("%.2f", v.getDistance()));
        }

        document.add(table);
        document.add(Chunk.NEWLINE);

        // ===== CHART =====
        Image chartImage = createSpeedChartImage(writer, vehicles);
        document.add(chartImage);

        document.close();
    }

    private static void addHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        table.addCell(cell);
    }

    private static Image createSpeedChartImage(PdfWriter writer, List<Vehicle> vehicles) throws Exception {

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Vehicle v : vehicles) {
            dataset.addValue(v.getSpeed(), "Speed", v.getID());
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Vehicle Speed Comparison",
                "Vehicle",
                "Speed",
                dataset,
                PlotOrientation.VERTICAL,
                false, true, false
        );

        BufferedImage bufferedImage = chart.createBufferedImage(500, 300);
        Image image = Image.getInstance(writer, bufferedImage, 1.0f);
        image.setAlignment(Element.ALIGN_CENTER);

        return image;
    }
}
