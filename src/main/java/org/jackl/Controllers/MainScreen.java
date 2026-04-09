package org.jackl.Controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.FileChooser;
import org.jackl.Data.CsvLoader;
import org.jackl.Data.Database;
import org.jackl.Data.TableRegistry;

import java.io.File;

public class MainScreen {

    @FXML
    public Button MountButton;
    public Button ContinueButton;
    public Label selectedLabel;

    @FXML
    private ListView<String> tableList;

    @FXML
    private Label statusLabel;

    private final ObservableList<String> tables = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        tableList.setItems(tables);
        refreshTableList();
    }

    @FXML
    public void onAdd(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("select CSV files for analysis?");

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        File selectedFile = fileChooser.showOpenDialog(MountButton.getScene().getWindow());

        if (selectedFile != null) {
            try {
                String tableName = CsvLoader.load(selectedFile);
                statusLabel.setText("Loaded: " + tableName);
                refreshTableList();
            } catch (Exception e) {
                statusLabel.setText("Error: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onRemove(ActionEvent actionEvent) {
        String selected = tableList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            String tableName = selected.split(", at:")[0];
            TableRegistry.unregister(tableName);
            statusLabel.setText("Removed: " + tableName);
            refreshTableList();
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private void refreshTableList() {
        try {
            tables.clear();
            for (TableRegistry.TableInfo info : TableRegistry.getAll()) {
                tables.add(info.tableName() + ", at:" + info.sourcePath());
            }
        } catch (Exception e) {
            statusLabel.setText("Error loading tables: " + e.getMessage());
        }
    }

    @FXML
    public void cont(ActionEvent actionEvent) {

        String selected = tableList.getSelectionModel().getSelectedItem();
        int index = tableList.getSelectionModel().getSelectedIndex();
        if (index == -1) {
            return;
        }
        String tb_name = selected.split(", at:")[0];

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/jackl/Layouts/DataAnalysis.fxml"));
            Parent root = loader.load();
            DataAnalysis controller = loader.getController();
            controller.setTableName(tb_name);
            MountButton.getScene().setRoot(root);
            controller.init();
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }
}
