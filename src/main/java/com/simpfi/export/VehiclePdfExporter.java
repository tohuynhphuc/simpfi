package com.simpfi.export;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.simpfi.object.Vehicle;

/**
 * Generates a PDF summary report for vehicle simulation data using iText.
 * Includes a timestamp, metrics and a speed comparison chart.
 */
public class VehiclePdfExporter {

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
		Image chartImage = createSimpleCongestionChart(writer, vehicles, congestionThreshold);
		chartImage.setAlignment(Element.ALIGN_CENTER);
		document.add(chartImage);

		document.close();
	}

	// Wrapper für Default Threshold
	public static void exportVehicles(List<Vehicle> vehicles, File file) throws Exception {
		exportVehicles(vehicles, file, 5);
	}

	private static void addHeader(PdfPTable table, String text) {
		PdfPCell cell = new PdfPCell(new Phrase(text));
		cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
		table.addCell(cell);
	}

	private static Image createSimpleCongestionChart(PdfWriter writer, List<Vehicle> vehicles, int threshold)
		throws Exception {

		// Dataset nur für congested edges
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		// EdgeID -> vehicle count
		vehicles.stream().map(Vehicle::getEdgeFromRoadID).filter(edge -> edge != null).distinct().forEach(edge -> {
			long count = vehicles.stream().filter(v -> v.getIsActive() && v.getEdgeFromRoadID() != null
				&& v.getEdgeFromRoadID().getId().equals(edge.getId())).count();
			if (count >= threshold) {
				dataset.setValue(count, "Vehicles", edge.getId());
			}
		});

		if (dataset.getColumnCount() == 0) {
			dataset.setValue(0, "Vehicles", "N/A");
		}

		// Gestapeltes Balkendiagramm horizontal
		JFreeChart chart = ChartFactory.createBarChart("Congested Edges", "Edge ID", // Domain Axis → Y-Achse bei
																						// horizontal
			"Vehicle Count", // Range Axis → X-Achse bei horizontal
			dataset, PlotOrientation.HORIZONTAL, false, // keine Legende nötig
			true, false);

		// Renderer Farbe + Werte auf Balken
		CategoryPlot plot = chart.getCategoryPlot();
		BarRenderer renderer = (BarRenderer) plot.getRenderer();

		for (int i = 0; i < dataset.getColumnCount(); i++) {
			Number value = dataset.getValue(0, i);
			if (value != null) {
				double ratio = value.doubleValue() / threshold;
				if (ratio < 1.5)
					renderer.setSeriesPaint(0, Color.GREEN);
				else if (ratio < 3)
					renderer.setSeriesPaint(0, Color.YELLOW);
				else
					renderer.setSeriesPaint(0, Color.RED);
			}
		}

		renderer.setDefaultItemLabelsVisible(true);
		renderer.setDefaultItemLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10));
		renderer.setDefaultItemLabelPaint(Color.BLACK);

		// Achsen Schriftgrößen
		plot.getDomainAxis().setTickLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));
		plot.getRangeAxis().setTickLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10));

		// Höhe dynamisch anpassen, damit 1 Edge nicht riesig wird
		int categoryHeight = 50;
		int minHeight = 200;
		int height = Math.max(minHeight, dataset.getColumnCount() * categoryHeight);
		int width = 600; // feste Breite für PDF

		// BufferedImage → PDF Image
		BufferedImage bufferedImage = chart.createBufferedImage(width, height);
		java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
		javax.imageio.ImageIO.write(bufferedImage, "png", baos);
		baos.flush();
		byte[] imageInBytes = baos.toByteArray();
		baos.close();

		return Image.getInstance(imageInBytes);
	}

}
