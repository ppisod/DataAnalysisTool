package org.jackl.Data;

import javafx.scene.control.Alert;

public class MemoryGuard {

    private static final int BYTES_PER_POINT = 2048;
    private static final double HEAP_FRACTION = 0.4;
    private static final int ABSOLUTE_MAX = 100_000;

    public static int calculateMaxPoints() {
        long maxHeap = Runtime.getRuntime().maxMemory();
        long usable = (long)(maxHeap * HEAP_FRACTION);
        int estimated = (int)(usable / BYTES_PER_POINT);
        return Math.min(estimated, ABSOLUTE_MAX);
    }

    public static boolean checkAndWarn(int totalPoints) {
        int max = calculateMaxPoints();
        if (totalPoints > max) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Too many data points.");
            alert.setHeaderText("Query would return " + String.format("%,d", totalPoints) + " data points");
            alert.setContentText("The maximum safe limit is " + String.format("%,d", max)
                + " points. Add constraints, reduce columns, or set a row limit.");
            alert.showAndWait();
            return false;
        }
        return true;
    }
}
