package com.simpfi.ui.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import com.simpfi.config.Settings;
import com.simpfi.export.VehicleCsvExporter;
import com.simpfi.export.VehiclePdfExporter;
import com.simpfi.object.Vehicle;
import com.simpfi.object.Edge;
import com.simpfi.sumo.wrapper.SumoConnectionManager;
import com.simpfi.sumo.wrapper.VehicleController;
import com.simpfi.ui.Button;
import com.simpfi.ui.Dropdown;
import com.simpfi.ui.Label;
import com.simpfi.ui.Panel;
import com.simpfi.ui.ScrollPane;
import com.simpfi.ui.TextBox;
import com.simpfi.ui.CheckBox;

/**
 * A UI panel used for inspecting vehicles. This class extends {@link Panel}.
 */

public class InspectPanel extends Panel {

    private static final long serialVersionUID = 1L;

    private VehicleController vehicleController;

    private List<Vehicle> selectedVehicles = new ArrayList<>();
    private Label modeLabel;
    private Label instructionLabel;
    private Label groupByLabel;
    private DefaultListModel<String> vehicleListModel;
    private JList<String> vehicleList;
    private Button changeModeButton;
    private Button selectAllButton;
    private Button clearButton;
    private Dropdown<String> groupByDropdown;
    private ScrollPane statsScrollPane;
    private List<Label> vehicleStaticLabels;
    private Timer speedUpdateTimer;

    enum Mode {
        PanMODE, SelectMODE
    }

    private Mode currentMode = Mode.PanMODE;

    public InspectPanel(SumoConnectionManager conn, MapPanel mapPanel) {
        this.vehicleController = new VehicleController(conn);

        this.setLayout(new BorderLayout());
        // includes listpanel and statswrapper panel
        Panel contentPanel = new Panel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        // includes buttonpanel and scrollpane(lower box)
        Panel listPanel = new Panel();
        listPanel.setLayout(new BorderLayout());

        // vehicle list in ScrollPane(top)
        vehicleListModel = new DefaultListModel<>();
        vehicleList = new JList<>(vehicleListModel);
        vehicleList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        vehicleList.addListSelectionListener(e -> {
            int listIndex = vehicleList.getSelectedIndex();
            if (listIndex == -1)
                return;

            String value = vehicleListModel.get(listIndex);

            // ignore header
            if (value.startsWith("---")) {
                vehicleList.clearSelection(); // deselect
                return;
            }

            // new index in selectedVehicles and update stats in statsfield
            int vehicleIndex = getVehicleIndexFromListIndex(listIndex);
            updateStatsFields(vehicleIndex);
        });

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.addItem(vehicleList);
        scrollPane.setPreferredSize(new Dimension(200, 150));
        listPanel.add(scrollPane, BorderLayout.CENTER);

        // Panel right for SelectAll, Clear and Dropdown
        Panel buttonPanel = new Panel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        selectAllButton = new Button("Select All");
        selectAllButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        selectAllButton.addActionListener(e -> {

            if (speedUpdateTimer != null) {
                speedUpdateTimer.stop();
            }

            selectedVehicles.clear();
            vehicleListModel.clear();

            for (Vehicle v : VehicleController.getVehicles()) {
                if (v != null && v.getIsActive()) {
                    selectedVehicles.add(v);
                    vehicleListModel.addElement(v.getId());
                }
            }

            if (!vehicleListModel.isEmpty()) {
                vehicleList.setSelectionInterval(0, vehicleListModel.size() - 1);
            }

            if (speedUpdateTimer != null) {
                speedUpdateTimer.start();
            }
        });


        buttonPanel.add(selectAllButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        clearButton = new Button("Clear");
        clearButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        clearButton.addActionListener(e -> {
            // simulate Select All
            selectedVehicles.clear();
            vehicleListModel.clear();
            List<Vehicle> allVehicles = VehicleController.getVehicles();
            selectedVehicles.addAll(allVehicles);
            for (Vehicle v : allVehicles) {
                vehicleListModel.addElement(v.getId());
            }
            if (!allVehicles.isEmpty()) {
                vehicleList.setSelectionInterval(0, allVehicles.size() - 1);
            }

            selectedVehicles.clear();
            vehicleListModel.clear();
        });

        buttonPanel.add(clearButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        groupByLabel = new Label("Group By:");
        groupByLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(groupByLabel);

        // GroupBy Dropdown
        groupByDropdown = new Dropdown<>(new String[] { "None", "Vehicle Type", "Color", "Speed", "Route" });
        groupByDropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        groupByDropdown.setAlignmentX(Component.CENTER_ALIGNMENT);
        groupByDropdown.addActionListener(e -> groupVehicles());
        buttonPanel.add(groupByDropdown);

        buttonPanel.add(Box.createRigidArea(new Dimension(0, 10)));

//Export CSV Button
        Button exportCsvButton = new Button("Export CSV");
        exportCsvButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exportCsvButton.addActionListener(e -> {

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save CSV Export");

            chooser.setSelectedFile(new File("vehicle_export.csv"));

            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();

                if (!file.getName().toLowerCase().endsWith(".csv")) {
                    file = new File(file.getAbsolutePath() + ".csv");
                }

                try {
                    VehicleCsvExporter.exportVehicles(selectedVehicles, file);

                    JOptionPane.showMessageDialog(
                            this,
                            "CSV file successfully exported.",
                            "Export Complete",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            this,
                            "CSV export failed:\n" + ex.getMessage(),
                            "Export Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                    ex.printStackTrace();
                }
            }
        });


        buttonPanel.add(exportCsvButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 10)));

//Export PDF Button
        Button exportPdfButton = new Button("Export PDF");
        exportPdfButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exportPdfButton.addActionListener(e -> {

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save PDF Report");

            chooser.setSelectedFile(new File("vehicle_report.pdf"));

            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();

                if (!file.getName().toLowerCase().endsWith(".pdf")) {
                    file = new File(file.getAbsolutePath() + ".pdf");
                }

                try {
                    int threshold = 5; // Default-Wert für congested edges
                    VehiclePdfExporter.exportVehicles(selectedVehicles, file, threshold);

                    JOptionPane.showMessageDialog(
                            this,
                            "PDF report successfully exported.",
                            "Export Complete",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            this,
                            "PDF export failed:\n" + ex.getMessage(),
                            "Export Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                    ex.printStackTrace();
                }
            }
        });

        buttonPanel.add(exportPdfButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 10)));


//Export All Buttons
        Button exportAllCsvButton = new Button("Export All CSV");
        exportAllCsvButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exportAllCsvButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save CSV Export - All Vehicles");
            chooser.setSelectedFile(new File("vehicle_export_all.csv"));
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(".csv")) {
                    file = new File(file.getAbsolutePath() + ".csv");
                }
                try {
                    VehicleCsvExporter.exportVehicles(VehicleController.getVehicles(), file);
                    JOptionPane.showMessageDialog(this, "CSV file successfully exported.", "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "CSV export failed:\n" + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        });
        buttonPanel.add(exportAllCsvButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        Button exportAllPdfButton = new Button("Export All PDF");
        exportAllPdfButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exportAllPdfButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save PDF Report - All Vehicles");
            chooser.setSelectedFile(new File("vehicle_report_all.pdf"));
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(".pdf")) {
                    file = new File(file.getAbsolutePath() + ".pdf");
                }
                try {
                    VehiclePdfExporter.exportVehicles(VehicleController.getVehicles(), file);
                    JOptionPane.showMessageDialog(this, "PDF report successfully exported.", "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "PDF export failed:\n" + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        });
        buttonPanel.add(exportAllPdfButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 10)));

//Filtered Export Button
        Button filteredExportButton = new Button("Filtered Export");
        filteredExportButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        filteredExportButton.addActionListener(e -> openFilterDialog());
        buttonPanel.add(filteredExportButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 10)));


        listPanel.add(buttonPanel, BorderLayout.EAST);

        contentPanel.add(listPanel);

        // Stats Panel
        statsScrollPane = new ScrollPane();
        statsScrollPane.setPreferredSize(new Dimension(300, 200));
        // vehicleTextBoxes = new ArrayList<>();
        vehicleStaticLabels = new ArrayList<>();

        // Vehicle Type Label
        Label headerVehicleTypeLabel = new Label("Vehicle Type");
        headerVehicleTypeLabel.setFont(headerVehicleTypeLabel.getFont().deriveFont(Font.BOLD));
        statsScrollPane.addItem(headerVehicleTypeLabel);

        Label typeLabel = new Label("-");
        statsScrollPane.addItem(typeLabel);
        vehicleStaticLabels.add(typeLabel);
        Panel statsWrapper = new Panel();
        statsWrapper.add(statsScrollPane);
        contentPanel.add(statsWrapper);

        // Color Label
        Label headerColorLabel = new Label("Color");
        headerColorLabel.setFont(headerColorLabel.getFont().deriveFont(Font.BOLD));
        statsScrollPane.addItem(headerColorLabel);

        Label colorLabel = new Label("R:0 G:0 B:0");
        statsScrollPane.addItem(colorLabel);
        vehicleStaticLabels.add(colorLabel);

        // Speed Label
        Label headerSpeedLabel = new Label("Speed (km/h)");
        headerSpeedLabel.setFont(headerSpeedLabel.getFont().deriveFont(Font.BOLD));
        statsScrollPane.addItem(headerSpeedLabel);

        Label speedLabel = new Label("0.0");
        statsScrollPane.addItem(speedLabel);
        vehicleStaticLabels.add(speedLabel);

        // Max Speed Label
        Label headerMaxSpeedLabel = new Label("Max Speed (km/h)");
        headerMaxSpeedLabel.setFont(headerMaxSpeedLabel.getFont().deriveFont(Font.BOLD));
        statsScrollPane.addItem(headerMaxSpeedLabel);

        Label maxSpeedLabel = new Label("0.0");
        statsScrollPane.addItem(maxSpeedLabel);
        vehicleStaticLabels.add(maxSpeedLabel);

        // Acceleration Label
        Label headerAccelerationLabel = new Label("Acceleration (m/s^2)");
        headerAccelerationLabel.setFont(headerAccelerationLabel.getFont().deriveFont(Font.BOLD));
        statsScrollPane.addItem(headerAccelerationLabel);

        Label accelerationLabel = new Label("0.0");
        statsScrollPane.addItem(accelerationLabel);
        vehicleStaticLabels.add(accelerationLabel);

        // Distance Traveled Label
        Label headerDistanceLabel = new Label("Distance Traveled (meters)");
        headerDistanceLabel.setFont(headerDistanceLabel.getFont().deriveFont(Font.BOLD));
        statsScrollPane.addItem(headerDistanceLabel);

        Label distanceLabel = new Label("0.0");
        statsScrollPane.addItem(distanceLabel);
        vehicleStaticLabels.add(distanceLabel);

        // Route Label
        Label headerRouteLabel = new Label("Route");
        headerRouteLabel.setFont(headerRouteLabel.getFont().deriveFont(Font.BOLD));
        statsScrollPane.addItem(headerRouteLabel);

        Label routeLabel = new Label("n/a");
        statsScrollPane.addItem(routeLabel);
        vehicleStaticLabels.add(routeLabel);

        this.add(contentPanel, BorderLayout.CENTER);

        // bottom Panel with change mode, current mode und instructions
        Panel bottomPanel = new Panel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));

        changeModeButton = new Button("Change Mode");
        changeModeButton.addActionListener(e -> toggleMode());
        bottomPanel.add(changeModeButton);

        modeLabel = new Label("Current Mode: PAN");
        bottomPanel.add(modeLabel);

        instructionLabel = new Label("Click 'Change Mode' to enter Select Mode.");
        bottomPanel.add(instructionLabel);

        this.add(bottomPanel, BorderLayout.SOUTH);

        // SELECT MODE feature realized by mouselistener
        mapPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (currentMode != Mode.SelectMODE)
                    return;

                Point2D world = screenToWorld(e.getX(), e.getY());
                Vehicle nearest = findNearestVehicle(world.getX(), world.getY(), 20.0);
                if (nearest != null)
                    addVehicleToInspect(nearest);
            }
        });

        // update Speed every 500ms
        speedUpdateTimer = new Timer(500, e -> updateLiveSpeedOnly());
        speedUpdateTimer.start();

    }

    /**
     * Toggles between Pan mode and Select mode for interacting with the map.
     * Updates the mode label and instructions accordingly. Used by
     * <code>changeModeButton</>.
     */
    private void toggleMode() {
        if (currentMode == Mode.PanMODE) {
            currentMode = Mode.SelectMODE;
            modeLabel.setText("Current Mode: SELECT");
            instructionLabel.setText("Click a vehicle on the map to inspect it.");
        } else {
            currentMode = Mode.PanMODE;
            modeLabel.setText("Current Mode: PAN");
            instructionLabel.setText("Drag the map.");
        }
    }

    /**
     * Toggles between Pan mode and Select mode for interacting with the map.
     * Updates the mode label and instructions accordingly.
     *
     * @param sx screen x coordinate
     * @param sy screen y coordinate
     *
     * @return A point representing the world coordinates
     */
    private Point2D screenToWorld(int sx, int sy) {
        double wx = (sx + Settings.config.OFFSET.getX()) / Settings.config.SCALE;
        double wy = -(sy + Settings.config.OFFSET.getY()) / Settings.config.SCALE;
        return new Point2D.Double(wx, wy);
    }

    /**
     * Finds the nearest vehicle to the given world coordinates within a specified
     * maximum distance.
     *
     * @param x           world x coordinate previously calculated by
     *                    {@link #screenToWorld(int, int)}
     * @param y           world y coordinate previously calculated by
     *                    {@link #screenToWorld(int, int)}
     * @param maxDistance Maximum search distance
     * @return The nearest Vehicle or null if no vehicle is within maxDistance
     */
    private Vehicle findNearestVehicle(double x, double y, double maxDistance) {
        Vehicle nearest = null;
        double bestDist = Double.MAX_VALUE;

        for (Vehicle v : VehicleController.getVehicles()) {
            double dx = v.getPosition().getX() - x;
            double dy = v.getPosition().getY() - y;
            double dist = Math.sqrt(dx * dx + dy * dy);

            if (dist < bestDist) {
                bestDist = dist;
                nearest = v;
            }
        }

        if (bestDist > maxDistance)
            return null;
        return nearest;
    }

    /**
     * Adds a vehicle to the list of selected vehicles for inspection. Avoids adding
     * duplicates based on vehicle ID.
     *
     * @param v The Vehicle to add
     */
    private void addVehicleToInspect(Vehicle v) {
        // if vehicle already in list
        boolean alreadyAdded = selectedVehicles.stream().anyMatch(vehicle -> vehicle.getId().equals(v.getId()));

        if (!alreadyAdded) {
            selectedVehicles.add(v);
            vehicleListModel.addElement(v.getId());
        }
    }

    /**
     * Updates the stats panel with the current values of the selected vehicle.
     * Includes type, color, speed, max speed, acceleration, distance traveled, and
     * route.
     *
     * @param vehicleIndex Index of the vehicle in the selectedVehicles list
     */
    private void updateStatsFields(int vehicleIndex) {

        if (vehicleIndex < 0 || vehicleIndex >= selectedVehicles.size())
            return;
        Vehicle v = selectedVehicles.get(vehicleIndex);

        // Vehicle Type
        vehicleStaticLabels.get(0).setText(v.getType().getId());

        Color c = v.getVehicleColor();
        if (c != null) {
            vehicleStaticLabels.get(1).setText("R:" + c.getRed() + " G:" + c.getGreen() + " B:" + c.getBlue());
        }

        vehicleStaticLabels.get(2).setText(String.format("%.2f", v.getSpeed()));

        vehicleStaticLabels.get(3).setText(String.format("%.2f", v.getMaxSpeed()));

        vehicleStaticLabels.get(4).setText(String.format("%.2f", v.getAcceleration()));

        vehicleStaticLabels.get(5).setText(String.format("%.2f", v.getDistance()));

        vehicleStaticLabels.get(6).setText(String.join(" -> ", v.getRoute()));
    }

    /**
     * Grouping Method for sorting Vehicles by Vehicle Type, Color, Speed, Route or
     * None. Gets activated when an Item gets selected in the
     * <code>groupByDropdown</>. Also implements Headers for the corresponding
     * group.
     *
     */
    private void groupVehicles() {
        String selected = (String) groupByDropdown.getSelectedItem();

        if (selected.equals("None")) {
            vehicleListModel.clear();
            for (Vehicle v : selectedVehicles) {
                vehicleListModel.addElement(v.getId());
            }
            return;
        }

        switch (selected) {
            case "Vehicle Type":
                selectedVehicles.sort((v1, v2) -> v1.getType().getId().compareTo(v2.getType().getId()));
                break;

            case "Color":
                selectedVehicles.sort((v1, v2) -> {
                    Color c1 = v1.getVehicleColor();
                    Color c2 = v2.getVehicleColor();
                    return Integer.compare(c1.getRGB(), c2.getRGB());
                });
                break;

            case "Speed":
                selectedVehicles.sort((v1, v2) -> Double.compare(v1.getSpeed(), v2.getSpeed()));
                break;

            case "Route":
                selectedVehicles.sort((v1, v2) -> {
                    String r1 = String.join("->", v1.getRoute());
                    String r2 = String.join("->", v2.getRoute());
                    return r1.compareTo(r2);
                });
                break;
        }
        // clear
        vehicleListModel.clear();
        String lastGroup = "";

        for (Vehicle v : selectedVehicles) {
            String currentGroup = "";

            switch (selected) {
                case "Vehicle Type":
                    currentGroup = v.getType().getId();
                    break;
                case "Color":
                    currentGroup = "RGB: " + v.getVehicleColor().getRGB();
                    break;
                case "Speed":
                    currentGroup = String.format("%.1f km/h", v.getSpeed());
                    break;
                case "Route":
                    currentGroup = String.join(" -> ", v.getRoute());
                    break;
            }

            // add header when new group
            if (!currentGroup.equals(lastGroup)) {
                vehicleListModel.addElement("--- " + currentGroup + " ---");
                lastGroup = currentGroup;
            }

            vehicleListModel.addElement(v.getId());
        }
    }

    /**
     * Translates the given listIndex indexes in the Vehilce-indexes because of
     * headers. Only increments count when object not header.
     *
     * @param listIndex JList index
     *
     * @return the index of the selectedVehicle List (count of elements without
     *         including headers)
     */
    private int getVehicleIndexFromListIndex(int listIndex) {
        int count = -1;
        for (int i = 0; i <= listIndex; i++) {
            if (!vehicleListModel.get(i).startsWith("---")) {
                count++;
            }
        }
        return count;
    }

    /**
     * Updates the displayed speed of the selected vehicle in the stats (lower)
     * panel. Called by <code>speedUpdateTimer</> every 500ms.
     *
     */
    private void updateLiveSpeedOnly() {
        if (vehicleController == null) return;

        int listIndex = vehicleList.getSelectedIndex();
        if (listIndex < 0 || listIndex >= vehicleListModel.size()) return;

        String value = vehicleListModel.get(listIndex);
        if (value.startsWith("---")) return;

        int vehicleIndex = getVehicleIndexFromListIndex(listIndex);
        if (vehicleIndex < 0 || vehicleIndex >= selectedVehicles.size()) return;

        Vehicle v = selectedVehicles.get(vehicleIndex);
        if (v == null || !v.getIsActive()) return;

        try {
            double liveSpeed = vehicleController.getSpeed(v.getId());
            vehicleStaticLabels.get(2).setText(String.format("%.2f", liveSpeed));
        } catch (Exception ex) {
            // SUMO closed → silently ignore
        }
    }

    /**
     * Opens a dialog that allows the user to filter vehicles for export.
     * The dialog provides options to filter by vehicle type (big, small, private,
     * commercial), active status, traveled distance and congestion threshold.
     * Users can then export the filtered vehicles as CSV or PDF.
     */
    private void openFilterDialog() {
        JDialog dialog = new JDialog((java.awt.Frame) SwingUtilities.getWindowAncestor(this), "Filtered Export", true);
        dialog.setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));
        dialog.setPreferredSize(new Dimension(400, 400));

        //Vehicle Type Checkboxes
        CheckBox bigVehiclesCheck = new CheckBox("Include Big Vehicles", false);
        CheckBox smallVehiclesCheck = new CheckBox("Include Small Vehicles", false);
        CheckBox privateVehiclesCheck = new CheckBox("Include Private Vehicles", false);
        CheckBox commercialVehiclesCheck = new CheckBox("Include Commercial Vehicles", false);

        smallVehiclesCheck.setToolTipText("Show private cars, motorcycles");
        bigVehiclesCheck.setToolTipText("Show trucks, buses, emergency vehicles");
        commercialVehiclesCheck.setToolTipText("Show trucks and buses");

        Panel typePanel = new Panel();
        typePanel.setLayout(new BoxLayout(typePanel, BoxLayout.Y_AXIS));
        typePanel.add(bigVehiclesCheck);
        typePanel.add(smallVehiclesCheck);
        typePanel.add(privateVehiclesCheck);
        typePanel.add(commercialVehiclesCheck);
        dialog.add(typePanel);
        dialog.add(Box.createRigidArea(new Dimension(0, 10)));

        CheckBox activeOnlyCheck = new CheckBox("Active vehicles only", false);
        activeOnlyCheck.setToolTipText("Show only vehicles that are currently active in the simulation");
        dialog.add(activeOnlyCheck);
        dialog.add(Box.createRigidArea(new Dimension(0, 10)));

        Panel distancePanel = new Panel();
        distancePanel.setLayout(new BoxLayout(distancePanel, BoxLayout.X_AXIS));
        distancePanel.add(new Label("Distance (m): min"));
        TextBox minDistanceField = new TextBox(0, true, true);
        distancePanel.add(minDistanceField);
        distancePanel.add(new Label("max"));
        TextBox maxDistanceField = new TextBox(500, true, true);
        distancePanel.add(maxDistanceField);
        dialog.add(distancePanel);
        dialog.add(Box.createRigidArea(new Dimension(0, 10)));

        Panel congestionPanel = new Panel();
        congestionPanel.setLayout(new BoxLayout(congestionPanel, BoxLayout.X_AXIS));
        CheckBox congestedEdgesCheck = new CheckBox("Only vehicles on congested edges", false);
        TextBox congestionThresholdField = new TextBox(5, true, true); // Default Threshold 5
        congestedEdgesCheck.setToolTipText("Show only vehicles on edges with more vehicles than threshold ");
        congestionPanel.add(congestedEdgesCheck);
        congestionPanel.add(new Label("Threshold:"));
        congestionPanel.add(congestionThresholdField);
        dialog.add(congestionPanel);
        dialog.add(Box.createRigidArea(new Dimension(0, 10)));

        Panel exportButtons = new Panel();
        Button exportCsvButton = new Button("Export CSV");
        Button exportPdfButton = new Button("Export PDF");
        exportButtons.add(exportCsvButton);
        exportButtons.add(exportPdfButton);
        dialog.add(exportButtons);

        exportCsvButton.addActionListener(e -> exportFiltered(true,
                bigVehiclesCheck,
                smallVehiclesCheck,
                privateVehiclesCheck,
                commercialVehiclesCheck,
                activeOnlyCheck,
                minDistanceField,
                maxDistanceField,
                congestedEdgesCheck,
                congestionThresholdField
        ));

        exportPdfButton.addActionListener(e -> exportFiltered(false,
                bigVehiclesCheck,
                smallVehiclesCheck,
                privateVehiclesCheck,
                commercialVehiclesCheck,
                activeOnlyCheck,
                minDistanceField,
                maxDistanceField,
                congestedEdgesCheck,
                congestionThresholdField
        ));

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void exportFiltered(
            boolean csv,    //true CSV, false PDF
            CheckBox bigVehiclesCheck,
            CheckBox smallVehiclesCheck,
            CheckBox privateVehiclesCheck,
            CheckBox commercialVehiclesCheck,
            CheckBox activeOnlyCheck,
            TextBox minDistanceField,
            TextBox maxDistanceField,
            CheckBox congestedEdgesCheck,
            TextBox congestionThresholdField
    ) {
        // Pause live speed updates to avoid SUMO access during export (crash source)
        if (speedUpdateTimer != null) {
            speedUpdateTimer.stop();
        }


        int threshold = (int) parseDouble(congestionThresholdField.getText(), 5);

        List<Vehicle> filtered = getFilteredVehicles(
                bigVehiclesCheck.isSelected(),
                smallVehiclesCheck.isSelected(),
                privateVehiclesCheck.isSelected(),
                commercialVehiclesCheck.isSelected(),
                activeOnlyCheck.isSelected(),
                parseDouble(minDistanceField.getText(), 0),
                parseDouble(maxDistanceField.getText(), Double.MAX_VALUE),
                congestedEdgesCheck.isSelected(),
                threshold
        );

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(csv ? "Save CSV Export" : "Save PDF Export");
        chooser.setSelectedFile(new File(csv ? "vehicle_filtered_export.csv" : "vehicle_filtered_report.pdf"));
        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (csv && !file.getName().toLowerCase().endsWith(".csv")) file = new File(file.getAbsolutePath() + ".csv");
            if (!csv && !file.getName().toLowerCase().endsWith(".pdf")) file = new File(file.getAbsolutePath() + ".pdf");

            try {
                if (csv) VehicleCsvExporter.exportVehicles(filtered, file);
                else VehiclePdfExporter.exportVehicles(filtered, file, threshold);

                JOptionPane.showMessageDialog(null, (csv ? "CSV" : "PDF") + " export successful.", "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, (csv ? "CSV" : "PDF") + " export failed:\n" + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
        // Resume live speed updates
        if (speedUpdateTimer != null) {
            speedUpdateTimer.start();
        }

    }
    /**
     * Returns a list of vehicles filtered according to the provided criteria.
     *
     * A snapshot of the current vehicle list is used to prevent race conditions
     * with the running simulation thread (e.g. SUMO updates).
     *
     * @return List of vehicles matching the filter criteria
     */
    private List<Vehicle> getFilteredVehicles(
            boolean includeBig,
            boolean includeSmall,
            boolean includePrivate,
            boolean includeCommercial,
            boolean activeOnly,
            double minDistance,
            double maxDistance,
            boolean congestedEdges,
            int congestionThreshold
    ) {
        // Take a snapshot to avoid concurrent modification during simulation updates
        List<Vehicle> snapshot = new ArrayList<>(VehicleController.getVehicles());

        return snapshot.stream()
                .filter(v -> {

                    // ===== Vehicle Type OR-Logic =====
                    boolean typeAllowed = false;
                    String typeId = v.getType().getId().toLowerCase();

                    // Big vehicles = trucks, buses, emergency
                    if (includeBig && (typeId.contains("truck") || typeId.contains("bus") || typeId.contains("emergency"))) {
                        typeAllowed = true;
                    }

                    // Small vehicles = private cars and motorcycles
                    if (includeSmall && (typeId.contains("private") || typeId.contains("motor"))) {
                        typeAllowed = true;
                    }

                    // Commercial vehicles = trucks and buses
                    if (includeCommercial && (typeId.contains("truck") || typeId.contains("bus"))) {
                        typeAllowed = true;
                    }

                    // Private vehicles = private cars that are not commercial
                    if (includePrivate && typeId.contains("private")) {
                        typeAllowed = true;
                    }

                    if (!typeAllowed) return false;

                    // ===== Active Filter =====
                    if (activeOnly && !v.getIsActive()) return false;

                    // ===== Distance Filter =====
                    if (v.getDistance() < minDistance || v.getDistance() > maxDistance) return false;

                    // ===== Congestion Filter =====
                    if (congestedEdges && !isOnCongestedEdge(v, congestionThreshold)) return false;

                    return true;
                })
                .toList();
    }

    /**
     * Parses a string into a double.
     * If parsing fails, returns the provided fallback value.
     *
     * @param s        The string to parse
     * @param fallback Value to return in case of parsing failure
     * @return Parsed double value or fallback
     */
    private double parseDouble(String s, double fallback) {
        try { return Double.parseDouble(s); } catch (Exception e) { return fallback; }
    }

    /**
     * Checks whether the given vehicle is on a congested edge.
     *
     * @param v         Vehicle to check
     * @param threshold Minimum number of vehicles on the edge to consider it congested
     * @return true if the vehicle is on a congested edge, false otherwise
     */
    private boolean isOnCongestedEdge(Vehicle v, int threshold) {
        Edge edge = v.getEdgeFromRoadID();
        if (edge == null) return false;

        List<Vehicle> snapshot = new ArrayList<>(VehicleController.getVehicles());

        long vehicleCount = snapshot.stream()
                .filter(veh ->
                        veh.getIsActive() &&
                                veh.getRoadID() != null &&
                                veh.getRoadID().equals(edge.getId())
                )
                .count();

        return vehicleCount >= threshold;
    }
}
