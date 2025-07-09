package com.example.polydb;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EditTablesController {

    @FXML
    private Label tableNameLabel;

    @FXML private TableView<ObservableList<String>> dataTableView; //sets up tableview that will display data
    private DatabaseTable selectedTable;


    public void receiveTable(DatabaseTable table){
        this.selectedTable = table;
        tableNameLabel.setText(selectedTable.getName());
        loadData();
    }

    @FXML
    private void handleRefresh() {
        dataTableView.getItems().clear();
        loadData();
    }

    //make the table view editable by user
    public void initialize(){
        dataTableView.setEditable(false);
    }

    private void loadData(){
        dataTableView.getColumns().clear();

        try{
            //1 - get column names from db
            List<String> columnNames = getColumnNames();

            //2 - create tables dynamically
            for (int i = 0; i < columnNames.size(); i++) {
                final int columnIndex = i;

                TableColumn<ObservableList<String>, String> column = new TableColumn<>(columnNames.get(i));

                //set how to get data for each cell
                column.setCellValueFactory(cellData -> {
                    ObservableList<String> row = cellData.getValue();
                    String cellValue = row.get(columnIndex);
                    return new SimpleStringProperty(cellValue);
                });


                //make cells editable
                column.setCellFactory(TextFieldTableCell.forTableColumn());

                //handle the edits
                column.setOnEditCommit(event -> {
                    ObservableList<String> row = event.getRowValue();
                    row.set(columnIndex, event.getNewValue());
                });

                dataTableView.getColumns().add(column);

            }

            //3 - load the actual table data
            List<ObservableList<String>> tableData = getTableData();
            dataTableView.getItems().addAll(tableData);
        } catch (SQLException e){
            // Show error to user
            showAlert("Database Error", "Failed to load table data: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }


    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private List<String> getColumnNames() throws SQLException {
        List<String> columns = new ArrayList<>();

        try(Connection conn = getConnection();
            ResultSet rs = conn.getMetaData().getColumns(null, null, selectedTable.getName(), null)) {

            while(rs.next()){
                String columnName = rs.getString("COLUMN_NAME");
                // For Oracle, ensure we handle case properly
                if (selectedTable.getType().equals("OracleDB")) {
                    columnName = columnName.toUpperCase();
                }
                columns.add(columnName);
            }
        }
        return columns;
    }


    private List<ObservableList<String>> getTableData() throws SQLException {
        List<ObservableList<String>> data = new ArrayList<>();

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + getTableNameWithQuotes())) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                ObservableList<String> row = FXCollections.observableArrayList();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getString(i) != null ? rs.getString(i) : "");
                }
                data.add(row);
            }
        }
        return data;
    }

    private Connection getConnection() throws SQLException {
        switch (selectedTable.getType()) {
            case "PostgreSQL":
                return DriverManager.getConnection(
                        "jdbc:postgresql://localhost:5432/TestD",
                        "postgres", "samplePass");
            case "MySQL":
                return DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/world",
                        "root", "samplePass");
            case "OracleDB":
                return DriverManager.getConnection(
                        "jdbc:oracle:thin:@localhost:1521/free",
                        "system", "samplePass");
            default:
                throw new SQLException("Unknown database type");
        }
    }

    private String getTableNameWithQuotes() {
        switch (selectedTable.getType()) {
            case "PostgreSQL":
                return "\"" + selectedTable.getName() + "\"";
            case "MySQL":
                return "`" + selectedTable.getName() + "`";
            case "OracleDB":
                return "\"" + selectedTable.getName().toUpperCase() + "\"";
            default:
                return selectedTable.getName();
        }
    }

    @FXML
    private void handleBackButton() throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("tableManagement.fxml"));
        Stage stage = (Stage) dataTableView.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.centerOnScreen();
    }

    // Add row

    @FXML
    private void handleAddRow() {
        try {
            // Get column names
            List<String> columnNames = getColumnNames();

            // Get column types
            List<String> columnTypeRaw = getColumnTypes(); // Ensure this returns types in the same order
            for (int i = 0; i < columnTypeRaw.size(); i++) {
                System.out.println("Column: " + columnNames.get(i) + " Type: [" + columnTypeRaw.get(i) + "]");
            }
            List<String> columnTypes = new ArrayList<>(); // To store normalized (uppercase) types

            // Create a dialog
            Dialog<ObservableList<String>> dialog = new Dialog<>();
            dialog.setTitle("Add New Row");
            dialog.setHeaderText("Enter data for the new row in table: " + selectedTable.getName());

            // Set the button types
            ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

            // Create a grid pane for the form
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            // Create text fields for each column
            List<TextField> textFields = new ArrayList<>();
            for (int i = 0; i < columnNames.size(); i++) {
                Label label = new Label(columnNames.get(i) + ":");
                TextField textField = new TextField();

                String type = columnTypeRaw.get(i).toUpperCase();
                columnTypes.add(type); // Track type for later use

                // Set prompt text based on column type
                if (type.contains("VARCHAR")) {
                    textField.setPromptText("Enter Text");
                } else if (type.contains("INT")) {
                    textField.setPromptText("Enter Whole Number");
                } else if (type.contains("FLOAT") || type.contains("DOUBLE") || type.contains("REAL")) {
                    textField.setPromptText("Enter Number");
                } else if (type.contains("DATE")) {
                    textField.setPromptText("Enter YYYY-MM-DD");
                } else if (type.contains("BIT") || type.contains("BOOL") || type.contains("TINYINT")) {

                    switch (selectedTable.getType()){
                        case "PostgreSQL":
                            textField.setPromptText("Enter true/false");
                            break;
                        case "MySQL":
                            textField.setPromptText("Enter 1/0");
                            break;
                        case "OracleDB":
                            textField.setPromptText("Enter true/false");
                            break;
                    }
                } else if (type.contains("NUMBER") || type.contains("ID") || type.contains("PRIMARY KEY") || type.contains("SERIAL")) {
                    textField.setPromptText("PrimKey Required");
                }
                else {
                    textField.setPromptText("Enter value");
                }

                grid.add(label, 0, i);
                grid.add(textField, 1, i);
                textFields.add(textField);
            }

            dialog.getDialogPane().setContent(grid);

            // Handle result conversion
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == addButtonType) {
                    ObservableList<String> rowData = FXCollections.observableArrayList();
                    boolean emptyFieldsFound = false;

                    for (int i = 0; i < textFields.size(); i++) {
                        String value = textFields.get(i).getText().trim();
                        String type = columnTypes.get(i);

                        if (value.isEmpty()) {
                            emptyFieldsFound = true;
                            // Default value based on type
                            if (type.contains("VARCHAR") || type.contains("CHAR") || type.contains("TEXT") || type.contains("CLOB")) {
                                rowData.add("null");
                            } else if (type.contains("INT") || type.contains("NUMBER") || type.contains("NUMERIC") ||
                                    type.contains("FLOAT") || type.contains("DOUBLE") || type.contains("REAL") ||
                                    type.contains("DECIMAL")) {
                                rowData.add("0");
                            } else if (type.contains("DATE") || type.contains("TIMESTAMP")) {
                                rowData.add("1111-11-11");
                            } else if (type.contains("BIT") || type.contains("BOOL") || type.contains("TINYINT")) {
                                // Add database-specific default boolean values
                                switch (selectedTable.getType()) {
                                    case "PostgreSQL":
                                        rowData.add("FALSE");  // PostgreSQL accepts 'f' for false
                                        break;
                                    case "MySQL":
                                    case "OracleDB":
                                        rowData.add("0");  // MySQL and Oracle use 0 for false
                                        break;
                                    default:
                                        rowData.add("0");
                                }
                            } else {
                                rowData.add("null");
                            }

                        } else {
                            rowData.add(value);
                        }
                    }
                    if(emptyFieldsFound){
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Notice");
                        alert.setHeaderText("Some fields were left empty");
                        alert.setContentText("Default values were added for empty fields.");
                        alert.showAndWait();
                    }
                    return rowData;
                }
                return null;
            });

            // Show the dialog and process the result
            Optional<ObservableList<String>> result = dialog.showAndWait();

            List<String> primaryKeys = getPrimaryKeyColumns();
            result.ifPresent(rowData -> {
                try {
                    boolean success = insertRowWithData(rowData);
                    if (success) {
                        showAlert("Success", "Row added successfully", Alert.AlertType.INFORMATION);
                        handleRefresh();
                    } else {
                        showAlert("Error", "Failed to add row", Alert.AlertType.ERROR);
                    }
                } catch (SQLException e) {
                    showAlert("Database Error", "(This may be a PRIMARY KEY column) Failed to add row: " + e.getMessage(), Alert.AlertType.ERROR);
               }
            });

        } catch (SQLException e) {
            showAlert("Database Error", "Failed to get column information: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }




    private List<String> getColumnTypes() throws SQLException {
        List<String> types = new ArrayList<>();
        try (Connection conn = getConnection();
             ResultSet rs = conn.getMetaData().getColumns(null, null, selectedTable.getName(), null)) {
            while (rs.next()) {
                types.add(rs.getString("TYPE_NAME"));  // Gets the SQL type name
            }
        }
        return types;
    }


    private boolean insertRowWithData(ObservableList<String> rowData) throws SQLException {
        try (Connection conn = getConnection();
             Statement statement = conn.createStatement()) {

            List<String> columnNames = getColumnNames();
            List<String> columnTypes = getColumnTypes(); // Get column types

            // Build the SQL INSERT statement
            StringBuilder columnsBuilder = new StringBuilder("(");
            StringBuilder valuesBuilder = new StringBuilder("(");

            for (int i = 0; i < columnNames.size(); i++) {
                if (i > 0) {
                    columnsBuilder.append(", ");
                    valuesBuilder.append(", ");
                }
                columnsBuilder.append(getColumnNameWithQuotes(columnNames.get(i)));

                String value = rowData.get(i);
                String columnType = columnTypes.get(i).toUpperCase();

                if (value == null || value.isEmpty()) {
                    valuesBuilder.append("NULL");
                } else {
                    // Handle different value types properly
                    if (value.startsWith("DATE '") && value.endsWith("'")) {
                        // Oracle DATE literal - don't wrap in additional quotes
                        valuesBuilder.append(value);
                    } else if (columnType.contains("DATE") || columnType.contains("TIMESTAMP")) {
                        // Handle date formatting based on database type
                        if (selectedTable.getType().equals("OracleDB")) {
                            valuesBuilder.append("DATE '").append(value).append("'");
                        } else {
                            valuesBuilder.append("'").append(value).append("'");
                        }
                    } else if (columnType.contains("INT") || columnType.contains("NUMBER") ||
                            columnType.contains("FLOAT") || columnType.contains("DOUBLE") ||
                            columnType.contains("REAL") || columnType.contains("DECIMAL")) {
                        // Numeric types - no quotes needed
                        valuesBuilder.append(value);
                    } else if (columnType.contains("BIT") || columnType.contains("BOOL") ||
                            columnType.contains("TINYINT")) {
                        String boolValue = value.trim().toLowerCase();

                        switch (selectedTable.getType()) {
                            case "PostgreSQL":
                                // PostgreSQL accepts TRUE/FALSE (without quotes)
                                if (boolValue.equals("f") || boolValue.equals("false") ||
                                        boolValue.equals("0") || boolValue.equals("FALSE")) {
                                    valuesBuilder.append("FALSE");
                                } else if (boolValue.equals("t") || boolValue.equals("true") ||
                                        boolValue.equals("1") || boolValue.equals("TRUE")) {
                                    valuesBuilder.append("TRUE");
                                } else {
                                    valuesBuilder.append("FALSE");  // default to FALSE
                                }
                                break;
                            case "MySQL":
                            case "OracleDB":
                                // MySQL and Oracle use 0/1 for boolean
                                if (boolValue.equals("1") || boolValue.equals("true") ||
                                        boolValue.equals("t") || boolValue.equals("TRUE")) {
                                    valuesBuilder.append("1");
                                } else {
                                    valuesBuilder.append("0");
                                }
                                break;
                            default:
                                valuesBuilder.append(value);
                        }
                    } else {
                        // String types - wrap in quotes
                        valuesBuilder.append("'").append(value).append("'");
                    }
                }
            }

            columnsBuilder.append(")");
            valuesBuilder.append(")");

            String sql = "INSERT INTO " + getTableNameWithQuotes() + " " +
                    columnsBuilder.toString() + " VALUES " + valuesBuilder.toString();

            System.out.println("Generated SQL: " + sql); // Debug output

            int affectedRows = statement.executeUpdate(sql);
            return affectedRows > 0;
        }
    }


//understand
private String getColumnNameWithQuotes(String columnName) {
    switch (selectedTable.getType()) {
        case "PostgreSQL":
            return "\"" + columnName + "\"";
        case "MySQL":
            return "`" + columnName + "`";
        case "OracleDB":
            // Oracle: use uppercase and quotes
            return "\"" + columnName.toUpperCase() + "\"";
        default:
            return columnName;
    }
}

    @FXML
    private void handleDeleteRow(){

        //selecting row
        ObservableList<String> selectedRow = dataTableView.getSelectionModel().getSelectedItem();
        if (selectedRow == null){
            showAlert("Selection Error", "Please select a row to delete", Alert.AlertType.WARNING);
            return;
        }

        try{
            boolean success = false;

            switch (selectedTable.getType()){

                case "PostgreSQL":
                case "MySQL":
                case "OracleDB":
                    success = deleteGeneralRow(selectedRow);
                    break;
            }

            if (success){
                handleRefresh();
            } else {
                showAlert("Error", "Failed to delete row", Alert.AlertType.ERROR);
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to delete row: "+ e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private boolean deleteGeneralRow(ObservableList<String> rowData) throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Get primary key columns (if any)
            List<String> primaryKeys = getPrimaryKeyColumns();
            List<String> columnNames = getColumnNames();
            List<String> columnTypes = getColumnTypes(); // Add this to get column types
            StringBuilder whereClause = new StringBuilder();

            if (!primaryKeys.isEmpty()) {
                // using primary key for deletion (best)
                for (int i = 0; i < dataTableView.getColumns().size(); i++) {
                    String columnName = dataTableView.getColumns().get(i).getText();
                    if (primaryKeys.contains(columnName)) {
                        String value = rowData.get(i);
                        String columnType = columnTypes.get(i).toUpperCase();

                        if (whereClause.length() > 0) whereClause.append(" AND ");

                        whereClause.append(getColumnNameWithQuotes(columnName));

                        if (value == null || value.trim().isEmpty()) {
                            whereClause.append(" IS NULL");
                        } else if (columnType.contains("DATE") || columnType.contains("TIMESTAMP")) {
                            // Handle date formatting based on database type
                            if (selectedTable.getType().equals("OracleDB")) {
                                // For Oracle, extract just the date part and use DATE literal
                                String dateOnly = value.split(" ")[0]; // Get just YYYY-MM-DD part
                                whereClause.append(" = DATE '").append(dateOnly).append("'");
                            } else {
                                whereClause.append(" = '").append(value).append("'");
                            }
                        } else {
                            whereClause.append(" = '").append(value).append("'");
                        }
                    }
                }
            } else {
                // Fallback: Match ALL columns (risky if duplicates exist)
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Warning");
                alert.setHeaderText(null);
                alert.setContentText("No primary key. Proceed with deleting by matching all columns?");

                ButtonType proceed = new ButtonType("Proceed");
                ButtonType cancel = new ButtonType("Cancel");
                alert.getButtonTypes().setAll(proceed,cancel);

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isEmpty() || result.get() == cancel) {
                    return false;
                } else {
                    for (int i = 0; i < dataTableView.getColumns().size(); i++) {
                        String columnName = dataTableView.getColumns().get(i).getText();
                        String value = rowData.get(i);
                        String columnType = columnTypes.get(i).toUpperCase();

                        if (whereClause.length() > 0) whereClause.append(" AND ");

                        whereClause.append(getColumnNameWithQuotes(columnName));

                        if (value == null || value.trim().isEmpty()) {
                            whereClause.append(" IS NULL");
                        } else if (columnType.contains("DATE") || columnType.contains("TIMESTAMP")) {
                            // Handle date formatting based on database type
                            if (selectedTable.getType().equals("OracleDB")) {
                                // For Oracle, extract just the date part and use DATE literal
                                String dateOnly = value.split(" ")[0]; // Get just YYYY-MM-DD part
                                whereClause.append(" = DATE '").append(dateOnly).append("'");
                            } else {
                                whereClause.append(" = '").append(value).append("'");
                            }
                        } else {
                            whereClause.append(" = '").append(value).append("'");
                        }
                    }
                }
            }

            String sql = "DELETE FROM " + getTableNameWithQuotes() +
                    " WHERE " + whereClause;

            // For MySQL only - add LIMIT 1 if no primary key
            if (selectedTable.getType().equals("MySQL") && primaryKeys.isEmpty()) {
                sql += " LIMIT 1";
            }

            System.out.println("Generated DELETE SQL: " + sql); // Debug output

            int affectedRows = stmt.executeUpdate(sql);
            return affectedRows > 0;
        }
    }
    private List<String> getPrimaryKeyColumns() throws SQLException {
        List<String> primaryKeys = new ArrayList<>();

        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData(); //DatabaseMetaData - special class in JDBC that lets you ask database about itself
            ResultSet rs = metaData.getPrimaryKeys(null, null, selectedTable.getName());

            while (rs.next()) {
                primaryKeys.add(rs.getString("COLUMN_NAME"));
            }
        }

        return primaryKeys;
    } //gets primary keys

    @FXML
    private void handleEditRow(){
        ObservableList<String> selectedRow = dataTableView.getSelectionModel().getSelectedItem();
        if (selectedRow == null){
            showAlert("Selection Error", "Please select a row to edit", Alert.AlertType.WARNING);
            return;
        }

        try{
            //get column names
            List<String> columnNames = getColumnNames();

            //creating box
            Dialog<ObservableList<String>> dialogBox = new Dialog<>();
            dialogBox.setTitle("Edit Row");
            dialogBox.setHeaderText("Edit row data in table: " + selectedTable.getName());

            //buttons
            ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            dialogBox.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            //grid pane for the form
            GridPane formBox = new GridPane();
            formBox.setHgap(10);
            formBox.setVgap(10);
            formBox.setPadding(new Insets(20, 150, 10, 10));

            //text fields for each column with data in them
            List<TextField> textFields = new ArrayList<>();
            List<String> columnTypes = getColumnTypes(); // Get column types for better prompts

            for(int i = 0; i < columnNames.size(); i++){
                Label label = new Label(columnNames.get(i) + ":");
                TextField textField = new TextField(selectedRow.get(i));

                String columnType = columnTypes.get(i).toUpperCase();

                // Set appropriate prompt text based on column type
                if (columnType.contains("DATE") || columnType.contains("TIMESTAMP")) {
                    textField.setPromptText("Enter YYYY-MM-DD (e.g., 2024-12-25)");
                } else if (columnType.contains("VARCHAR") || columnType.contains("CHAR") || columnType.contains("TEXT")) {
                    textField.setPromptText("Enter text");
                } else if (columnType.contains("INT") || columnType.contains("NUMBER")) {
                    textField.setPromptText("Enter whole number");
                } else if (columnType.contains("FLOAT") || columnType.contains("DOUBLE") || columnType.contains("REAL")) {
                    textField.setPromptText("Enter decimal number");
                } else if (columnType.contains("BIT") || columnType.contains("BOOL") || columnType.contains("TINYINT")) {
                    switch (selectedTable.getType()){
                        case "PostgreSQL":
                            textField.setPromptText("Enter true/false");
                            break;
                        case "MySQL":
                            textField.setPromptText("Enter 1/0");
                            break;
                        case "OracleDB":
                            textField.setPromptText("Enter 1/0");
                            break;
                    }
                } else {
                    textField.setPromptText("Enter " + columnNames.get(i));
                }

                formBox.add(label,0,i);
                formBox.add(textField, 1, i);
                textFields.add(textField);

            }

            dialogBox.getDialogPane().setContent(formBox);

            //converting result to observable list when save is clicked
            dialogBox.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    ObservableList<String> rowData = FXCollections.observableArrayList();
                    for (TextField textField : textFields) {
                        rowData.add(textField.getText());
                    }
                    return  rowData;
                }
                return null;
            });

            //show dialog and process
            Optional<ObservableList<String>> result = dialogBox.showAndWait();

            List<String> primaryKeys = getPrimaryKeyColumns();
            result.ifPresent(newRowData -> {

                try {
                    boolean success = updateRowInDatabase(selectedRow, newRowData);
                    if (success) {
                        showAlert("Success", "Row updated successfully", Alert.AlertType.INFORMATION);
                        handleRefresh();
                    } else{
                        showAlert("Error", "Failed to update row", Alert.AlertType.INFORMATION);
                    }
                } catch (SQLException e){
                    showAlert("Database Error", "(This may be a PRIMARY KEY column) Failed to update row: " + e.getMessage(), Alert.AlertType.ERROR);

                }
            });

        } catch (SQLException e){
            showAlert("Database Error","Failed to get column information: "+e.getMessage(),Alert.AlertType.ERROR);
        }
    }

    private boolean updateRowInDatabase(ObservableList<String> oldRowData, ObservableList<String> newRowData) throws SQLException {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            List<String> columnNames = getColumnNames();
            List<String> columnTypes = getColumnTypes(); // Get column types
            List<String> primaryKeys = getPrimaryKeyColumns();

            //build update for rows
            StringBuilder setClause = new StringBuilder();
            for (int i = 0; i < columnNames.size(); i++){
                if (i>0) setClause.append(", ");

                String columnType = columnTypes.get(i).toUpperCase();
                String value = newRowData.get(i);

                setClause.append(getColumnNameWithQuotes(columnNames.get(i)))
                        .append(" = ");

                if (value == null || value.trim().isEmpty()) {
                    setClause.append("NULL");
                } else {
                    // Handle different value types properly
                    if (columnType.contains("DATE") || columnType.contains("TIMESTAMP")) {
                        // Handle date formatting based on database type
                        if (selectedTable.getType().equals("OracleDB")) {
                            setClause.append("DATE '").append(value.trim()).append("'");
                        } else {
                            setClause.append("'").append(value.trim()).append("'");
                        }
                    } else if (columnType.contains("INT") || columnType.contains("NUMBER") ||
                            columnType.contains("FLOAT") || columnType.contains("DOUBLE") ||
                            columnType.contains("REAL") || columnType.contains("DECIMAL")) {
                        // Numeric types - no quotes needed
                        setClause.append(value.trim());
                    } else if (columnType.contains("BIT") || columnType.contains("BOOL") ||
                            columnType.contains("TINYINT")) {
                        // Boolean types - handle properly based on database type
                        String boolValue = value.trim().toLowerCase();

                        switch (selectedTable.getType()) {
                            case "PostgreSQL":
                                // PostgreSQL accepts TRUE/FALSE
                                if (boolValue.equals("t") || boolValue.equals("true") || boolValue.equals("1")) {
                                    setClause.append("TRUE");
                                } else if (boolValue.equals("f") || boolValue.equals("false") || boolValue.equals("0")) {
                                    setClause.append("FALSE");
                                } else {
                                    setClause.append("FALSE");
                                }
                                break;
                            case "MySQL":
                                // MySQL uses 1/0 for boolean
                                if (boolValue.equals("1") || boolValue.equals("true") || boolValue.equals("t")) {
                                    setClause.append("1");
                                } else {
                                    setClause.append("0");
                                }
                                break;
                            case "OracleDB":
                                // Oracle uses 1/0 for boolean
                                if (boolValue.equals("1") || boolValue.equals("true") || boolValue.equals("t")) {
                                    setClause.append("1");
                                } else {
                                    setClause.append("0");
                                }
                                break;
                            default:
                                setClause.append(value.trim());
                        }
                    }
                    else {
                        // String types - wrap in quotes
                        setClause.append("'").append(value.trim()).append("'");
                    }
                }
            }

            //build where using primary keys if available
            StringBuilder whereClause = new StringBuilder();
            if (!primaryKeys.isEmpty()) {
                for (int i = 0; i < columnNames.size(); i++) {
                    String columnName = columnNames.get(i);
                    if (primaryKeys.contains(columnName)) {
                        if (whereClause.length() > 0) whereClause.append(" AND ");

                        String oldValue = oldRowData.get(i);
                        String columnType = columnTypes.get(i).toUpperCase();

                        whereClause.append(getColumnNameWithQuotes(columnName));

                        if (oldValue == null) {
                            whereClause.append(" IS NULL");
                        } else if (columnType.contains("DATE") || columnType.contains("TIMESTAMP")) {
                            // Handle date comparison properly
                            if (selectedTable.getType().equals("OracleDB")) {
                                // For Oracle, use DATE function to compare just the date part
                                String dateOnly = oldValue.split(" ")[0]; // Get just YYYY-MM-DD part
                                whereClause.append(" = DATE '").append(dateOnly).append("'");
                            } else {
                                whereClause.append(" = '").append(oldValue).append("'");
                            }
                        } else {
                            whereClause.append(" = '").append(oldValue).append("'");
                        }
                    }
                }
            } else {
                // Fallback: Match all columns if no primary key (risky)
                for (int i = 0; i < columnNames.size(); i++) {
                    if (whereClause.length() > 0) whereClause.append(" AND ");

                    String oldValue = oldRowData.get(i);
                    String columnType = columnTypes.get(i).toUpperCase();

                    whereClause.append(getColumnNameWithQuotes(columnNames.get(i)));

                    if (oldValue == null) {
                        whereClause.append(" IS NULL");
                    } else if (columnType.contains("DATE") || columnType.contains("TIMESTAMP")) {
                        // Handle date comparison properly
                        if (selectedTable.getType().equals("OracleDB")) {
                            String dateOnly = oldValue.split(" ")[0]; // Get just YYYY-MM-DD part
                            whereClause.append(" = DATE '").append(dateOnly).append("'");
                        } else {
                            whereClause.append(" = '").append(oldValue).append("'");
                        }
                    } else {
                        whereClause.append(" = '").append(oldValue).append("'");
                    }
                }
            }

            String sql = "UPDATE " + getTableNameWithQuotes() + " SET " + setClause.toString() + " WHERE " + whereClause.toString();

            System.out.println("Generated UPDATE SQL: " + sql); // Debug output

            int impactedRows = statement.executeUpdate(sql);
            return impactedRows > 0;
        }
    }
    @FXML
    private void handleAddColumn(){

        try{
            //dialogue box to get user input about column
            Dialog<ColumnDetails> dialog = new Dialog<>();
            dialog.setTitle("Add Column");
            dialog.setHeaderText("Add new column to "+ selectedTable.getName());

            //set button type
            ButtonType addButton = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(addButton, ButtonType.CANCEL);

            //create the form
            GridPane form = new GridPane();
            form.setHgap(10);
            form.setVgap(10);
            form.setPadding(new Insets(20,150,10,10));

            TextField columnNameField = new TextField();
            columnNameField.setPromptText("Column Name");
            ComboBox<String> dataTypeComboBox = new ComboBox<>();
            dataTypeComboBox.getItems().addAll("VARCHAR(255)","INTEGER", "FLOAT", "DATE", "BOOLEAN");
            dataTypeComboBox.setValue("VARCHAR(255)");

            form.add(new Label("Column Name:"), 0, 0);
            form.add(columnNameField, 1, 0);
            form.add(new Label("Data Type:"), 0, 1);
            form.add(dataTypeComboBox, 1, 1);

            dialog.getDialogPane().setContent(form);

            // Convert the result to a ColumnDetails object when the Add button is clicked
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == addButton) {
                    return new ColumnDetails(
                            columnNameField.getText(),
                            dataTypeComboBox.getValue()
                    );
                }
                return null;
            });

            //show dialog and process result
            Optional<ColumnDetails> result = dialog.showAndWait();

            result.ifPresent(columnDetails -> {
                try {
                    boolean success = addColumnToTable(columnDetails);
                    if (success) {
                        showAlert("Success", "Column added successfully", Alert.AlertType.INFORMATION);
                        handleRefresh(); // Refresh to show the new column
                    } else {
                        showAlert("Error", "Failed to add column", Alert.AlertType.ERROR);
                    }
                } catch (SQLException e) {
                    showAlert("Database Error", "Failed to add column: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            });
        } catch (Exception e) {
            showAlert("Error", "An error occurred: "+ e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private boolean addColumnToTable(ColumnDetails columnDetails) throws SQLException {

        switch (selectedTable.getType()){
            case "PostgreSQL":
                return addPostgreSQLColumn(columnDetails);
            case "MySQL":
                return addMySQLColumn(columnDetails);  //sql
            case "OracleDB":
                return addOracleDBColumn(columnDetails);//oracle db
            default:
                throw new SQLException("Unknown database type");
        }

    }

    private String getDefaultValueForType(String dataType) {
        String type = dataType.toUpperCase();

        if (type.contains("VARCHAR") || type.contains("CHAR") || type.contains("TEXT")) {
            return "'null'";
        } else if (type.contains("INT") || type.contains("FLOAT") || type.contains("DOUBLE") || type.contains("REAL")) {
            return "0";
        } else if (type.contains("DATE")) {
            // handle date differently for each database
            return getDateDefault();
        } else if (type.contains("BIT") || type.contains("BOOL") || type.contains("TINYINT")) {
            return getBooleanDefault();
        } else {
            return "'null'";
        }
    }

    // Add this new method:
    private String getDateDefault() {
        switch (selectedTable.getType()) {
            case "PostgreSQL":
                return "'1111-11-11'";  // PostgreSQL accepts this format
            case "MySQL":
                return "'1111-11-11'";  // MySQL accepts this format
            case "OracleDB":
                return "DATE '1111-11-11'";  // Oracle needs DATE literal
            default:
                return "'1111-11-11'";
        }
    }

    private String getBooleanDefault() {
        switch (selectedTable.getType()) {
            case "PostgreSQL":
                return "FALSE";  // PostgreSQL boolean literal
            case "MySQL":
                return "0";      // MySQL uses 0/1 for boolean
            case "OracleDB":
                return "0";      // Oracle uses 0/1 for boolean
            default:
                return "0";
        }
    }

    private boolean addPostgreSQLColumn(ColumnDetails columnDetails) throws SQLException{
        try(Connection conn = getConnection();
            Statement statement = conn.createStatement()) {
            String defaultValue = getDefaultValueForType(columnDetails.getType());
            String sql = String.format("ALTER TABLE \"%s\" ADD COLUMN \"%s\" %s DEFAULT %s",
                    selectedTable.getName(),
                    columnDetails.getName(),
                    columnDetails.getType(),
                    defaultValue);
            statement.execute(sql);
            return true;
        }
    }

    private boolean addMySQLColumn(ColumnDetails columnDetails) throws SQLException{
        try(Connection conn = getConnection();
            Statement statement = conn.createStatement()) {
            String defaultValue = getDefaultValueForType(columnDetails.getType());
            String sql = String.format("ALTER TABLE `%s` ADD COLUMN `%s` %s DEFAULT %s",
                    selectedTable.getName(),
                    columnDetails.getName(),
                    columnDetails.getType(),
                    defaultValue);
            statement.execute(sql);
            return true;
        }
    }

    private boolean addOracleDBColumn(ColumnDetails columnDetails) throws SQLException{
        try(Connection conn = getConnection();
            Statement statement = conn.createStatement()) {
            String defaultValue = getDefaultValueForType(columnDetails.getType());
            String sql = String.format("ALTER TABLE \"%s\" ADD \"%s\" %s DEFAULT %s",
                    selectedTable.getName().toUpperCase(),
                    columnDetails.getName().toUpperCase(),
                    columnDetails.getType(),
                    defaultValue);
            statement.execute(sql);
            return true;
        }
    }


    private static class ColumnDetails {
        private final String name;
        private final String type;

        public ColumnDetails(String name, String type){
            this.name = name;
            this.type = type;
        }

        public String getName(){
            return name;
        }

        public String getType(){
            return type;
        }

    }

    @FXML
    private void handleDeleteColumn(){

        try{
            //getting current column names from database
            List<String> columnNames = getColumnNames();

            List<String> primaryKeys = getPrimaryKeyColumns();

            //select which column to delete
            ChoiceDialog<String> dialog = new ChoiceDialog<>(columnNames.get(0), columnNames);
            dialog.setTitle("Delete Column");
            dialog.setHeaderText("Select column to delete from "+ selectedTable.getName());
            dialog.setContentText("Column:");

            //show box and wait
            Optional<String> result = dialog.showAndWait();

            result.ifPresent(columnName -> {

                //check if column is primary key column

                if(primaryKeys.contains(columnName)) {
                    //show warning
                    Alert primaryKeyAlert  = new Alert(Alert.AlertType.WARNING);
                    primaryKeyAlert.setContentText("Primary Key Warning");
                    primaryKeyAlert.setHeaderText("You are about to delete a PRIMARY KEY column");
                    primaryKeyAlert.setContentText("Column '" + columnName + "' is a primary key. Deleting it may cause:\n" +
                            "➡ Loss of data integrity\n" +
                            "➡ Problems with editing and deleting rows\n" +
                            "➡ Foreign key constraint violations\n\n" +
                            "Delete at your own risk. Are you sure you want to continue?");

                    ButtonType deleteDoubleConfirm = new ButtonType("Delete Anyway", ButtonBar.ButtonData.OK_DONE);
                    ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                    primaryKeyAlert.getButtonTypes().setAll(deleteDoubleConfirm, cancel);

                    Optional<ButtonType> primaryKeyResult = primaryKeyAlert.showAndWait();
                    if (primaryKeyResult.isEmpty() || primaryKeyResult.get() == cancel) {
                        return; // User cancelled, don't proceed with deletion
                    }
                    // If chose Delete Anyway, continue with normal deletion
                }

                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setContentText("Confirm Deletion");
                confirm.setHeaderText("Delete Column "+columnName);
                confirm.setContentText("This action cannot be undone. Continue?");

                if(confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try{
                    boolean success = deleteColumnFromTable(columnName);
                    if(success) {
                        showAlert("Success", "Column deleted successfully", Alert.AlertType.INFORMATION);
                        handleRefresh();
                    } else {
                        showAlert("Error", "Failed to delete column", Alert.AlertType.ERROR);
                    }
                } catch (SQLException e){
                    showAlert("Database Error", "Error: " + e.getMessage(), Alert.AlertType.ERROR);
                }
                }
            });

        } catch (SQLException e){
            showAlert("Database Error", "Failed to get column information", Alert.AlertType.ERROR);
        }

    }

    private boolean deleteColumnFromTable(String columnName) throws SQLException {
        try(Connection conn = getConnection();
            Statement statement = conn.createStatement()){

            String sql;
            switch (selectedTable.getType()) {
                case "PostgreSQL":
                    sql = String.format("ALTER TABLE \"%s\" DROP COLUMN \"%s\"",
                            selectedTable.getName(), columnName);
                    statement.execute(sql);
                    break;
                case "MySQL":
                    sql = String.format("ALTER TABLE `%s` DROP COLUMN `%s`",
                            selectedTable.getName(), columnName);
                    statement.execute(sql);
                    break;
                case "OracleDB":
                    // Oracle needs uppercase and CASCADE CONSTRAINTS if column has constraints
                    sql = String.format("ALTER TABLE \"%s\" DROP COLUMN \"%s\" CASCADE CONSTRAINTS",
                            selectedTable.getName().toUpperCase(),
                            columnName.toUpperCase());
                    statement.execute(sql);
                    break;
                default:
                    System.out.println("Unsupported database type");
            }
            return true;
        } catch (SQLException e){
            System.err.println("Error deleting column:");
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            System.err.println("Message: " + e.getMessage());
            throw e;
        }

    }

    @FXML
    private void handleRenameColumn() {
        try {
            List<String> columnNames = getColumnNames();

            ChoiceDialog<String> selectDialog = new ChoiceDialog<>(columnNames.get(0), columnNames);
            selectDialog.setTitle("Rename Column");
            selectDialog.setHeaderText("Select column to rename from " + selectedTable.getName());
            selectDialog.setContentText("Column:");

            Optional<String> selectedColumn = selectDialog.showAndWait();
            if (!selectedColumn.isPresent()) return;

            String oldName = selectedColumn.get();

            TextInputDialog renameDialog = new TextInputDialog(oldName);
            renameDialog.setTitle("Rename Column");
            renameDialog.setHeaderText("Enter new name for column '" + oldName + "'");
            renameDialog.setContentText("New name:");

            Optional<String> newNameResult = renameDialog.showAndWait();
            if (!newNameResult.isPresent() || newNameResult.get().trim().isEmpty()) return;

            String newName = newNameResult.get().trim();

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Rename");
            confirm.setHeaderText("Rename Column");
            confirm.setContentText("Are you sure you want to rename '" + oldName + "' to '" + newName + "'?");
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                boolean success = renameColumn(oldName, newName);
                if (success) {
                    showAlert("Success", "Column renamed successfully", Alert.AlertType.INFORMATION);
                    handleRefresh();
                } else {
                    showAlert("Error", "Failed to rename column", Alert.AlertType.ERROR);
                }
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to rename column: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    private boolean renameColumn(String oldName, String newName) throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            String sql;
            switch (selectedTable.getType()) {
                case "PostgreSQL":
                    sql = sql = String.format("ALTER TABLE \"%s\" RENAME COLUMN \"%s\" TO \"%s\"",
                            selectedTable.getName(), oldName, newName);
                    break;
                case "MySQL":
                    sql = String.format("ALTER TABLE `%s` RENAME COLUMN `%s` TO `%s`",
                            selectedTable.getName(), oldName, newName);
                    break;
                case "OracleDB":
                    sql = String.format("ALTER TABLE \"%s\" RENAME COLUMN \"%s\" TO \"%s\"",
                            selectedTable.getName().toUpperCase(), oldName.toUpperCase(), newName.toUpperCase());
                    break;
                default:
                    throw new SQLException("Unsupported database type");
            }
            stmt.executeUpdate(sql);
            return true;
        }
    }
}
