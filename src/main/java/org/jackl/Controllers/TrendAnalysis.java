package org.jackl.Controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.jackl.Data.Database;
import org.jackl.Data.MemoryGuard;
import org.jackl.Data.QueryBuilder;
import org.jackl.Data.CsvLoader;
import org.jackl.Data.TableRegistry;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

public class TrendAnalysis {

    @FXML private ComboBox<String> IndependentVariable;
    @FXML private ComboBox<String> DependentVariable;
    @FXML private ComboBox<String> RegressionType;
    @FXML private ComboBox<String> ConstraintColumn;
    @FXML private ComboBox<String> ConstraintString;
    @FXML private TextField ConstraintValue;
    @FXML private Label ConstraintLabel;
    @FXML private ComboBox<String> SortBy;
    @FXML private TextField LimitToFirstLast;
    @FXML private TextField LimitToNumber;
    @FXML private CheckBox RandomSample;
    @FXML private Label TableNameLabel;
    @FXML private Label EquationLabel;
    @FXML private Label StatusLabel;
    @FXML private LineChart<Number, Number> Chart;
    @FXML private NumberAxis X;
    @FXML private NumberAxis Y;

    private String TableName;
    private final List<Constraint> Constraints = new ArrayList<>();

    public void init() throws Exception {
        Connection Connection = Database.getConnection();

        IndependentVariable.getItems().clear();
        DependentVariable.getItems().clear();
        SortBy.getItems().clear();
        ConstraintColumn.getItems().clear();
        ConstraintString.getItems().clear();
        RegressionType.getItems().clear();

        List<String> RealColumns = new ArrayList<>();
        try (Statement Stmt = Connection.createStatement();
             ResultSet Results = Stmt.executeQuery("PRAGMA table_info(\"" + esc(TableName) + "\")")) {
            while (Results.next()) {
                String Type = Results.getString("type").toUpperCase();
                if (Type.equals("REAL") || Type.equals("INTEGER") || Type.equals("INT")) {
                    RealColumns.add(Results.getString("name"));
                }
            }
        }
        List<String> AllColumns = new ArrayList<>();
        try (Statement Stmt_ = Connection.createStatement();
             ResultSet ResultSet_ = Stmt_.executeQuery("PRAGMA table_info(\"" + esc(TableName) + "\")")) {
            while (ResultSet_.next()) AllColumns.add(ResultSet_.getString("name"));
        }
        IndependentVariable.getItems().addAll(RealColumns);
        DependentVariable.getItems().addAll(RealColumns);
        SortBy.getItems().addAll(AllColumns);
        ConstraintColumn.getItems().addAll(AllColumns);
        ConstraintString.getItems().addAll("=", "!=", "<", "<=", ">", ">=");
        RegressionType.getItems().addAll("Linear", "Quadratic", "Cubic", "Exponential", "Power", "Logarithmic");
        RegressionType.setValue("Linear");
    }

    public void setTableName(String TableName) {
        this.TableName = TableName;
        TableNameLabel.setText("Table: " + TableName);
    }

    @FXML
    private void onHelp(ActionEvent Event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Trend / Regression Analysis -- Help");
        alert.setHeaderText("Trend and Regression Analysis");
        alert.setContentText("""
            This screen fits mathematical trend lines to your data and displays the R-squared goodness of fit.
            
            BACK -- Return to the scatter-plot analysis screen (your variable selections are preserved).
            
            VARIABLES ROW:
              - IV (Independent Variable): The X-axis column. Only numeric (REAL/INTEGER) columns are shown.
              - DV (Dependent Variable): The Y-axis column. Only numeric columns are shown.
              - Regression: Choose the curve type to fit.
                  Linear       -- y = ax + b
                  Quadratic    -- y = ax^2 + bx + c
                  Cubic        -- y = ax^3 + bx^2 + cx + d
                  Exponential  -- y = a * e^(bx)   (requires y > 0)
                  Power        -- y = a * x^b      (requires x > 0, y > 0)
                  Logarithmic  -- y = a * ln(x) + b (requires x > 0)
              - Fit Trend: Compute and display the regression curve.
            
            CONSTRAINTS ROW:
              - Filter the data using conditions like "col < 5000".
              - Choose a column, operator, numeric value, then click Add. Click Clear to remove all.
            
            SAMPLING ROW:
              - Sort by: Order data by a column before applying a limit.
              - Limit to first/last: "first" or "last" to take top or bottom rows after sorting.
              - # of entries: Maximum number of data points to use.
              - Random sample: Pick rows randomly instead of sorting.
            
            EQUATION -- After fitting, the regression formula is displayed here, along with R-squared and sample size.
            """);
        alert.showAndWait();
    }

    @FXML
    private void onRefresh(ActionEvent Event) {
        try {
            String sourcePath = TableRegistry.getAll().stream()
                .filter(t -> t.tableName().equals(TableName))
                .findFirst()
                .map(TableRegistry.TableInfo::sourcePath)
                .orElse(null);
            if (sourcePath == null) {
                StatusLabel.setText("Source file path not found in registry");
                return;
            }
            File file = new File(sourcePath);
            if (!file.exists()) {
                StatusLabel.setText("Source file no longer exists: " + sourcePath);
                return;
            }

            CsvLoader.load(file);

            Chart.getData().clear();
            Constraints.clear();
            updateConstraintLabel();
            IndependentVariable.getSelectionModel().clearSelection();
            DependentVariable.getSelectionModel().clearSelection();
            SortBy.getSelectionModel().clearSelection();
            ConstraintColumn.getSelectionModel().clearSelection();
            ConstraintString.getSelectionModel().clearSelection();
            LimitToFirstLast.clear();
            LimitToNumber.clear();
            RandomSample.setSelected(false);
            EquationLabel.setText("(none)");
            X.setAutoRanging(true);
            Y.setAutoRanging(true);
            init();
            StatusLabel.setText("Reloaded " + TableName + " from " + sourcePath);
        } catch (Exception ex) {
            StatusLabel.setText("Refresh error: " + ex.getMessage());
        }
    }

    @FXML
    private void onFit(ActionEvent Event) {
        String IndpVar = IndependentVariable.getValue();
        String DepVar = DependentVariable.getValue();
        String Type = RegressionType.getValue();

        if (IndpVar == null || DepVar == null) {
            StatusLabel.setText("Select IV and DV");
            return;
        }
        if (IndpVar.equals(DepVar)) {
            StatusLabel.setText("IV and DV must be different columns");
            return;
        }

        try {
            int RowCount = countRows(IndpVar, DepVar);
            int UserLimit = parseLimit();
            int EffectiveRows = (UserLimit > 0) ? Math.min(UserLimit, RowCount) : RowCount;

            if (!MemoryGuard.checkAndWarn(EffectiveRows)) {
                return;
            }

            List<double[]> Pts = loadData(IndpVar, DepVar);
            if (Pts.size() < 2) {
                StatusLabel.setText("Not enough data points (need \u2265 2)");
                return;
            }

            double[] xs = Pts.stream().mapToDouble(p -> p[0]).toArray();
            double[] ys = Pts.stream().mapToDouble(p -> p[1]).toArray();

            RegressionResult Res = switch (Type) {
                case "Quadratic"   -> fitPolynomial(xs, ys, 2);
                case "Cubic"       -> fitPolynomial(xs, ys, 3);
                case "Exponential" -> fitExponential(xs, ys);
                case "Power"       -> fitPower(xs, ys);
                case "Logarithmic" -> fitLogarithmic(xs, ys);
                default            -> fitLinear(xs, ys);
            };

            Chart.getData().clear();
            X.setLabel(IndpVar);
            Y.setLabel(DepVar);

            XYChart.Series<Number, Number> Scatter = new XYChart.Series<>();
            Scatter.setName("Data");
            for (double[] Point : Pts) Scatter.getData().add(new XYChart.Data<>(Point[0], Point[1]));
            Chart.getData().add(Scatter);
            Platform.runLater(() -> {
                if (Scatter.getNode() != null) Scatter.getNode().setStyle("-fx-stroke: transparent;");
            });

            double XMin = xs[0], XMax = xs[0];
            for (double x : xs) { if (x < XMin) XMin = x; if (x > XMax) XMax = x; }
            XYChart.Series<Number, Number> Trend = new XYChart.Series<>();
            Trend.setName(Type + " fit");
            double Step = (XMax - XMin) / 300.0;
            for (int i = 0; i <= 300; i++) {
                double x = XMin + i * Step;
                double y = Res.fn.applyAsDouble(x);
                if (Double.isFinite(y)) Trend.getData().add(new XYChart.Data<>(x, y));
            }
            Chart.getData().add(Trend);
            Platform.runLater(() -> Trend.getData().forEach(d -> {
                if (d.getNode() != null) d.getNode().setVisible(false);
            }));

            fitAxes();
            EquationLabel.setText(Res.equation);
            StatusLabel.setText(String.format("R\u00B2 = %.4f  (n = %d)", Res.r2(xs, ys), xs.length));

        } catch (Exception ex) {
            StatusLabel.setText("Error: " + ex.getMessage());
        }
    }

    private String buildWhere(String IndpVar, String DepVar) {
        QueryBuilder qb = QueryBuilder.where()
            .notNull(IndpVar)
            .notNull(DepVar);
        for (Constraint Cons : Constraints) {
            qb.constrain(Cons.col(), Cons.op(), Double.parseDouble(Cons.val()));
        }
        return qb.build();
    }

    private int countRows(String IndpVar, String DepVar) throws Exception {
        String Sql = "SELECT COUNT(*) FROM \"" + QueryBuilder.esc(TableName) + "\"" + buildWhere(IndpVar, DepVar);
        try (Statement Stmt = Database.getConnection().createStatement();
             ResultSet Results = Stmt.executeQuery(Sql)) {
            return Results.next() ? Results.getInt(1) : 0;
        }
    }

    private int parseLimit() {
        String NumberText = LimitToNumber.getText().trim();
        if (!NumberText.isEmpty()) {
            try {
                return Integer.parseInt(NumberText);
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    private List<double[]> loadData(String IndpVar, String DepVar) throws Exception {
        List<double[]> Points = new ArrayList<>();
        QueryBuilder Query = QueryBuilder.select()
            .cast(IndpVar).cast(DepVar)
            .from(TableName)
            .where(buildWhere(IndpVar, DepVar));
        if (RandomSample.isSelected()) {
            Query.orderByRandom();
        } else if (SortBy.getValue() != null) {
            Query.orderBy(SortBy.getValue());
            if ("last".equals(LimitToFirstLast.getText().trim().toLowerCase())) Query.desc();
        }
        int Limit = parseLimit();
        if (Limit > 0) {
            Query.limit(Limit);
        }
        try (Statement Stmt = Database.getConnection().createStatement();
             ResultSet Results = Stmt.executeQuery(Query.build())) {
            while (Results.next()) {
                double X = Results.getDouble(1), Y = Results.getDouble(2);
                if (!Results.wasNull()) Points.add(new double[]{X, Y});
            }
        }
        return Points;
    }

    @FXML
    private void onAddConstraint(ActionEvent Event) {
        String Cols = ConstraintColumn.getValue();
        String Operator = ConstraintString.getValue();
        String Value = ConstraintValue.getText().trim();
        if (Cols == null || Operator == null || Value.isEmpty()) {
            StatusLabel.setText("Fill in column, operator, and value");
            return;
        }
        Constraints.add(new Constraint(Cols, Operator, Value));
        ConstraintValue.clear();
        updateConstraintLabel();
    }

    @FXML
    private void onClearConstraints(ActionEvent Event) {
        Constraints.clear();
        updateConstraintLabel();
    }

    private void updateConstraintLabel() {
        if (Constraints.isEmpty()) {
            ConstraintLabel.setText("(none)");
        } else {
            StringBuilder ConstraintText = new StringBuilder();
            for (int i = 0; i < Constraints.size(); i++) {
                if (i > 0) ConstraintText.append(", ");
                Constraint Cons = Constraints.get(i);
                ConstraintText.append(Cons.col()).append(" ").append(Cons.op()).append(" ").append(Cons.val());
            }
            ConstraintLabel.setText(ConstraintText.toString());
        }
    }

    private record Constraint(String col, String op, String val) {}

    private RegressionResult fitLinear(double[] xs, double[] ys) {
        double[] c = linearCoeffs(xs, ys);
        return new RegressionResult(
            x -> c[0] * x + c[1],
            String.format("y = %.4fx %s %.4f", c[0], c[1] >= 0 ? "+" : "-", Math.abs(c[1]))
        );
    }

    private RegressionResult fitPolynomial(double[] xs, double[] ys, int degree) {
        int d = degree + 1, n = xs.length;
        double[][] A = new double[d][d];
        double[] b = new double[d];
        for (int i = 0; i < d; i++) {
            for (int j = 0; j < d; j++) {
                double s = 0;
                for (double x : xs) s += Math.pow(x, i + j);
                A[i][j] = s;
            }
            double s = 0;
            for (int k = 0; k < n; k++) s += ys[k] * Math.pow(xs[k], i);
            b[i] = s;
        }
        double[] Coeffs = gaussElim(A, b);
        return new RegressionResult(
            x -> { double y = 0; for (int i = 0; i < Coeffs.length; i++) y += Coeffs[i] * Math.pow(x, i); return y; },
            buildPolyEq(Coeffs)
        );
    }

    private RegressionResult fitExponential(double[] xs, double[] ys) {
        double[] lnY = new double[ys.length];
        for (int i = 0; i < ys.length; i++) {
            if (ys[i] <= 0) throw new IllegalArgumentException("Exponential fit requires y > 0 for all points");
            lnY[i] = Math.log(ys[i]);
        }
        double[] c = linearCoeffs(xs, lnY);
        double a = Math.exp(c[1]), b = c[0];
        return new RegressionResult(
            x -> a * Math.exp(b * x),
            String.format("y = %.4f\u00B7e^(%.4fx)", a, b)
        );
    }

    private RegressionResult fitPower(double[] xs, double[] ys) {
        double[] lnX = new double[xs.length], lnY = new double[ys.length];
        for (int i = 0; i < xs.length; i++) {
            if (xs[i] <= 0) throw new IllegalArgumentException("Power fit requires x > 0");
            if (ys[i] <= 0) throw new IllegalArgumentException("Power fit requires y > 0");
            lnX[i] = Math.log(xs[i]);
            lnY[i] = Math.log(ys[i]);
        }
        double[] c = linearCoeffs(lnX, lnY);
        double a = Math.exp(c[1]), b = c[0];
        return new RegressionResult(
            x -> a * Math.pow(x, b),
            String.format("y = %.4f\u00B7x^%.4f", a, b)
        );
    }

    private RegressionResult fitLogarithmic(double[] xs, double[] ys) {
        double[] lnX = new double[xs.length];
        for (int i = 0; i < xs.length; i++) {
            if (xs[i] <= 0) throw new IllegalArgumentException("Logarithmic fit requires x > 0");
            lnX[i] = Math.log(xs[i]);
        }
        double[] c = linearCoeffs(lnX, ys);
        return new RegressionResult(
            x -> c[0] * Math.log(x) + c[1],
            String.format("y = %.4f\u00B7ln(x) %s %.4f", c[0], c[1] >= 0 ? "+" : "-", Math.abs(c[1]))
        );
    }

    private double[] linearCoeffs(double[] xs, double[] ys) {
        int n = xs.length;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            sumX += xs[i]; sumY += ys[i]; sumXY += xs[i] * ys[i]; sumX2 += xs[i] * xs[i];
        }
        double denom = n * sumX2 - sumX * sumX;
        double slope = (n * sumXY - sumX * sumY) / denom;
        double intercept = (sumY - slope * sumX) / n;
        return new double[]{slope, intercept};
    }

    private double[] gaussElim(double[][] A, double[] b) {
        int n = A.length;
        double[][] M = new double[n][n + 1];
        for (int i = 0; i < n; i++) { System.arraycopy(A[i], 0, M[i], 0, n); M[i][n] = b[i]; }
        for (int Col = 0; Col < n; Col++) {
            int Piv = Col;
            for (int Row = Col + 1; Row < n; Row++)
                if (Math.abs(M[Row][Col]) > Math.abs(M[Piv][Col])) Piv = Row;
            double[] Tmp = M[Col]; M[Col] = M[Piv]; M[Piv] = Tmp;
            if (Math.abs(M[Col][Col]) < 1e-12) continue;
            for (int Row = Col + 1; Row < n; Row++) {
                double f = M[Row][Col] / M[Col][Col];
                for (int j = Col; j <= n; j++) M[Row][j] -= f * M[Col][j];
            }
        }
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            x[i] = M[i][n];
            for (int j = i + 1; j < n; j++) x[i] -= M[i][j] * x[j];
            x[i] /= M[i][i];
        }
        return x;
    }

    private String buildPolyEq(double[] Coeffs) {
        StringBuilder Sb = new StringBuilder("y = ");
        boolean First = true;
        for (int i = Coeffs.length - 1; i >= 0; i--) {
            double c = Coeffs[i];
            if (Math.abs(c) < 1e-10) continue;
            if (!First) Sb.append(c >= 0 ? " + " : " - ");
            else if (c < 0) Sb.append("-");
            First = false;
            Sb.append(String.format("%.4f", Math.abs(c)));
            if (i == 1) Sb.append("x");
            else if (i > 1) Sb.append("x^").append(i);
        }
        if (First) Sb.append("0");
        return Sb.toString();
    }

    private void fitAxes() {
        double xMin = Double.MAX_VALUE, xMax = -Double.MAX_VALUE;
        double yMin = Double.MAX_VALUE, yMax = -Double.MAX_VALUE;
        for (XYChart.Series<Number, Number> S : Chart.getData()) {
            for (XYChart.Data<Number, Number> D : S.getData()) {
                double x = D.getXValue().doubleValue(), y = D.getYValue().doubleValue();
                xMin = Math.min(xMin, x); xMax = Math.max(xMax, x);
                yMin = Math.min(yMin, y); yMax = Math.max(yMax, y);
            }
        }
        if (xMin == Double.MAX_VALUE) return;
        double xPad = Math.max((xMax - xMin) * 0.05, 1);
        double yPad = Math.max((yMax - yMin) * 0.05, 1);
        X.setAutoRanging(false);
        X.setLowerBound(xMin - xPad); X.setUpperBound(xMax + xPad);
        X.setTickUnit((xMax - xMin) / 10.0);
        Y.setAutoRanging(false);
        Y.setLowerBound(yMin - yPad); Y.setUpperBound(yMax + yPad);
        Y.setTickUnit((yMax - yMin) / 10.0);
    }

    @FXML
    private void onBack(ActionEvent Event) {
        try {
            FXMLLoader Loader = new FXMLLoader(getClass().getResource("/org/jackl/Layouts/DataAnalysis.fxml"));
            Parent Root = Loader.load();
            DataAnalysis Ctrl = Loader.getController();
            Ctrl.setTableName(TableName);
            Ctrl.init();
            TableNameLabel.getScene().setRoot(Root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String esc(String S) { return QueryBuilder.esc(S); }

    private record RegressionResult(DoubleUnaryOperator fn, String equation) {
        double r2(double[] xs, double[] ys) {
            double Mean = 0;
            for (double y : ys) Mean += y;
            Mean /= ys.length;
            double SsTot = 0, SsRes = 0;
            for (int i = 0; i < xs.length; i++) {
                SsTot += Math.pow(ys[i] - Mean, 2);
                SsRes += Math.pow(ys[i] - fn.applyAsDouble(xs[i]), 2);
            }
            return SsTot == 0 ? 1.0 : 1.0 - SsRes / SsTot;
        }
    }
}
