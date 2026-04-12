package org.jackl.Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.jackl.Data.Database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DataAnalysis {

    @FXML
    public ComboBox<String> IV;
    @FXML
    public ComboBox<String> DV;
    @FXML
    public ComboBox<String> DV2;
    @FXML
    public ComboBox<String> DV3;
    @FXML
    public ComboBox<String> sortBy;
    @FXML
    public TextField limitToFirstLast;
    @FXML
    public TextField limitToNum;
    @FXML
    public ComboBox<String> constraintCol;
    @FXML
    public ComboBox<String> constraintOp;
    @FXML
    public TextField constraintVal;
    @FXML
    public Label constraintLabel;
    @FXML
    public CheckBox randomSample;
    @FXML
    private Label tableNameLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private ScatterChart<Number, Number> chart;
    @FXML
    private NumberAxis xAxis;
    @FXML
    private NumberAxis yAxis;

    private String tableName;
    private List<String> cols;
    private final List<Constraint> constraints = new ArrayList<>();

    public void init() throws Exception {
        Connection connection = Database.getConnection();

        cols = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(\"" + tableName + "\")")) {
            while (rs.next()) {
                cols.add(rs.getString("name"));
            }
        }
        for (String col : cols) {
            DV.getItems().add(col);
            DV2.getItems().add(col);
            DV3.getItems().add(col);
            IV.getItems().add(col);
            sortBy.getItems().add(col);
            constraintCol.getItems().add(col);
        }

        constraintOp.getItems().addAll("=", "!=", "<", "<=", ">", ">=");
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
        tableNameLabel.setText("Currently inspecting table: " + tableName);
    }

    @FXML
    private void onAddConstraint(ActionEvent actionEvent) {
        String col = constraintCol.getValue();
        String op = constraintOp.getValue();
        String val = constraintVal.getText().trim();

        if (col == null || op == null || val.isEmpty()) {
            statusLabel.setText("Fill in column, operator, and value");
            return;
        }

        constraints.add(new Constraint(col, op, val));
        constraintVal.clear();
        updateConstraintLabel();
    }

    @FXML
    private void onClearConstraints(ActionEvent actionEvent) {
        constraints.clear();
        updateConstraintLabel();
    }

    private void updateConstraintLabel() {
        if (constraints.isEmpty()) {
            constraintLabel.setText("(none)");
        } else {
            StringBuilder labelText = new StringBuilder();
            for (int i = 0; i < constraints.size(); i++) {
                if (i > 0) labelText.append(", ");
                Constraint c = constraints.get(i);
                labelText.append(c.col).append(" ").append(c.op).append(" ").append(c.val);
            }
            constraintLabel.setText(labelText.toString()); // "col < 5000, ..."
        }
    }

    @FXML
    private void onBack(ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/jackl/Layouts/MainScreen.fxml"));
            tableNameLabel.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goButton(ActionEvent actionEvent) {
        String ivCol = IV.getValue();
        String dvCol = DV.getValue();

        if (ivCol == null || dvCol == null) {
            statusLabel.setText("Select at least 1IV and 1DV");
            return;
        }

        try {
            String query = buildQuery(ivCol, dvCol);
            statusLabel.setText("Query: " + query);
            chart.getData().clear();
            xAxis.setLabel(ivCol);
            yAxis.setLabel(dvCol);
            XYChart.Series<Number, Number> series1 = queryToSeries(query, ivCol, dvCol, dvCol);
            chart.getData().add(series1);
            if (DV2.getValue() != null) { // optional DVs
                String q2 = buildQuery(ivCol, DV2.getValue());
                XYChart.Series<Number, Number> s2 = queryToSeries(q2, ivCol, DV2.getValue(), DV2.getValue());
                chart.getData().add(s2);
            }
            if (DV3.getValue() != null) {
                String q3 = buildQuery(ivCol, DV3.getValue());
                XYChart.Series<Number, Number> s3 = queryToSeries(q3, ivCol, DV3.getValue(), DV3.getValue());
                chart.getData().add(s3);
            }
            fitAxes();
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private String buildQuery(String ivCol, String dvCol) {
        StringBuilder Query = new StringBuilder();
        Query.append("SELECT \"").append(esc(ivCol)).append("\", \"").append(esc(dvCol)).append("\"");
        Query.append(" FROM \"").append(esc(tableName)).append("\"");
        Query.append(" WHERE \"").append(esc(ivCol)).append("\" IS NOT NULL");
        Query.append(" AND \"").append(esc(dvCol)).append("\" IS NOT NULL");
        for (Constraint c : constraints) { // constraints
            Query.append(" AND CAST(\"").append(esc(c.col)).append("\" AS REAL) ") // if column is text, this will just throw
              .append(c.op).append(" ").append(Double.parseDouble(c.val));
        }
        if (randomSample.isSelected()) { // random ordering
            Query.append(" ORDER BY RANDOM()");
        } else if (sortBy.getValue() != null) {
            Query.append(" ORDER BY \"").append(esc(sortBy.getValue())).append("\"");
            String fl = limitToFirstLast.getText().trim().toLowerCase();
            if (fl.equals("last")) {
                Query.append(" DESC");
            }
        }
        String numText = limitToNum.getText().trim();
        if (!numText.isEmpty()) {
            try {
                int limit = Integer.parseInt(numText);
                Query.append(" LIMIT ").append(limit);
            } catch (NumberFormatException ignored) {
            }
        }
        return Query.toString();
    }
    private XYChart.Series<Number, Number> queryToSeries(String query, String ivCol, String dvCol, String seriesName) throws Exception {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(seriesName);
        Connection Connect = Database.getConnection();
        try (Statement State = Connect.createStatement();
             ResultSet Results = State.executeQuery(query)) {
            while (Results.next()) {
                try {
                    double x = Results.getDouble(1);
                    double y = Results.getDouble(2);
                    if (!Results.wasNull()) {
                        series.getData().add(new XYChart.Data<>(x, y));
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return series;
    }
    private void fitAxes() {
        double xMin = Double.MAX_VALUE, xMax = -Double.MAX_VALUE;
        double yMin = Double.MAX_VALUE, yMax = -Double.MAX_VALUE;
        for (XYChart.Series<Number, Number> series : chart.getData()) {
            for (XYChart.Data<Number, Number> d : series.getData()) {
                double x = d.getXValue().doubleValue();
                double y = d.getYValue().doubleValue();
                xMin = Math.min(xMin, x);
                xMax = Math.max(xMax, x);
                yMin = Math.min(yMin, y);
                yMax = Math.max(yMax, y);
            }
        }
        if (xMin == Double.MAX_VALUE) return;
        double xPad = (xMax - xMin) * 0.05;
        double yPad = (yMax - yMin) * 0.05;
        if (xPad == 0) xPad = 1;
        if (yPad == 0) yPad = 1;
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(xMin - xPad);
        xAxis.setUpperBound(xMax + xPad);
        xAxis.setTickUnit((xMax - xMin) / 10);
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(yMin - yPad);
        yAxis.setUpperBound(yMax + yPad);
        yAxis.setTickUnit((yMax - yMin) / 10);
    }
    private String esc(String col) {
        return col.replace("\"", "\"\"");
    }

    private record Constraint(String col, String op, String val) { }
}
