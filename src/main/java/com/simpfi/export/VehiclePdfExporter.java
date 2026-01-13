package com.simpfi.export;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
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
 * <<<<<<< HEAD Generates a PDF summary report for vehicle simulation data.
 * Includes a timestamp, metrics and a speed comparison chart. ======= Generates
 * a PDF summary report for vehicle simulation data using iText. Includes a
 * timestamp, metrics and a speed comparison chart. >>>>>>>
 * be0772bd3c4e4b9d2e249fb9e0c651f148b8c3f4
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

		Image chartImage = createSpeedChartImage(writer, vehicles);
		document.add(chartImage);

		document.close();
	}

	/**
	 * Creates a header for a given metric.
	 *
	 * @param table destination table
	 * @param text  header of the metric
	 */
	private static void addHeader(PdfPTable table, String text) {
		PdfPCell cell = new PdfPCell(new Phrase(text));
		cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
		table.addCell(cell);
	}

	/**
	 * Creates an image of the speed chart.
	 *
	 * @param writer   writer including information about the file
	 * @param vehicles the list of given vehicles
	 * @return returns the image ready to be added in the document
	 */
	private static Image createSpeedChartImage(PdfWriter writer, List<Vehicle> vehicles) throws Exception {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		for (Vehicle v : vehicles) {
			dataset.addValue(v.getSpeed(), "Speed", v.getId());
		}

		JFreeChart chart = ChartFactory.createBarChart("Vehicle Speed Comparison", "Vehicle", "Speed", dataset,
			PlotOrientation.VERTICAL, false, true, false);

		BufferedImage bufferedImage = chart.createBufferedImage(500, 300);
		Image image = Image.getInstance(writer, bufferedImage, 1.0f);
		image.setAlignment(Element.ALIGN_CENTER);

		return image;
	}
}
