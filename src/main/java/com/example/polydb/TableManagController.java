package com.example.polydb;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.swing.text.html.Option;
import java.io.IOException;
import java.sql.*;
import java.util.Optional;

public class TableManagController {

    @FXML
    private TableView<DatabaseTable> tablesTableView;
    @FXML
    private TableColumn<DatabaseTable, String> nameColumn;
    @FXML
    private TableColumn<DatabaseTable, String> typeColumn;
    @FXML
    private ComboBox<String> dbFilterComboBox;
    @FXML
    private TextField searchField;
    @FXML
    private Button removeButton;
    @FXML
    private Button renameButton;
    @FXML
    private Button editButton;
    @FXML
    private Label refreshMessage;

    ObservableList<DatabaseTable> allTables = FXCollections.observableArrayList();
    private FilteredList<DatabaseTable> filteredTables = new FilteredList<>(allTables);

    public void initialize() {
        // Set up the table columns
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));

        // Set up the ComboBox
        dbFilterComboBox.getItems().addAll("All Databases", "PostgreSQL", "MySQL", "OracleDB");

        // Set the filtered list to the TableView
        tablesTableView.setItems(filteredTables);

        tablesTableView.setOnKeyPressed(this::handleKeyPress);

        // Add listener to ComboBox
        dbFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.equals("All Databases")) {
                filteredTables.setPredicate(table -> true); //show all tables
            } else {
                filteredTables.setPredicate(table -> table.getType().equals(newVal)); //show selected type tables
            }
        });

        //search feature
        filteredTables = new FilteredList<>(allTables, p -> true);

        SortedList<DatabaseTable> sortedData = new SortedList<>(filteredTables);
        sortedData.comparatorProperty().bind(tablesTableView.comparatorProperty());
        tablesTableView.setItems(sortedData);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterTables(newValue);
        });

        //remove feature
        removeButton.disableProperty().bind(tablesTableView.getSelectionModel().selectedItemProperty().isNull());

        //disable rename button at first
        renameButton.disableProperty().bind(tablesTableView.getSelectionModel().selectedItemProperty().isNull());

        //disable edit button at first
        editButton.disableProperty().bind(tablesTableView.getSelectionModel().selectedItemProperty().isNull());

        // Load data
        handleRefresh(false);
    }


    //search feature
    private void filterTables(String filterText) {
        if (filterText == null || filterText.isEmpty()) {
            filteredTables.setPredicate(table -> true);
        } else {
            String lowerCaseFilter = filterText.toLowerCase();
            filteredTables.setPredicate(table ->
                    table.getName().toLowerCase().contains(lowerCaseFilter)
            );
        }
    }

    @FXML
    //wrapper for handleRefresh
    public void handleRefresh() {
        handleRefresh(true);
    }

    //for refresh
    public void handleRefresh(boolean userTrig) {

        //clear existing data
        allTables.clear();

        //load the new data
        loadPostgresDatabaseTables();
        loadMySQLDatabaseTables();
        loadOracleTables();

        if (userTrig) {
            refreshMessage.setText("Data Refreshed!");
            refreshMessage.setOpacity(1);

            PauseTransition pause = new PauseTransition(Duration.seconds(3));
            pause.setOnFinished(e -> refreshMessage.setText(""));
            pause.play();
        }

    }

    //loading tables
    private void loadPostgresDatabaseTables() {

        //postgres
        String url = "jdbc:postgresql://localhost:5432/TestD";
        String user = "postgres";
        String password = "samplePass";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getTables(null, null, null, new String[]{"TABLE"});

            if (!rs.isBeforeFirst()) {
                System.out.println("No tables found in the database");
            }

            while (rs.next()) {
                System.out.println("Found table: " + rs.getString("TABLE_NAME"));
                allTables.add(new DatabaseTable(rs.getString("TABLE_NAME"), "PostgreSQL"));
            }

        } catch (SQLException e) {
            System.err.println("Database connection error:");
            e.printStackTrace();
            // You might want to show an alert to the user here
        }
    }

    private void loadMySQLDatabaseTables() {

        String url = "jdbc:mysql://localhost:3306/world";
        String user = "root";
        String password = "samplePass";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {

            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getTables("world", null, "%", new String[]{"TABLE"});

            if (!rs.isBeforeFirst()) {
                System.out.println("No tables found in the database");
            }

            while (rs.next()) {
                System.out.println("Found table: " + rs.getString("TABLE_NAME"));
                allTables.add(new DatabaseTable(rs.getString("TABLE_NAME"), "MySQL"));
            }

        } catch (SQLException e) {
            System.err.println("Database connection error:");
            e.printStackTrace();
            // You might want to show an alert to the user here
        }

    }

    private void loadOracleTables() {

        String url = "jdbc:oracle:thin:@localhost:1521/free";
        String user = "system";
        String password = "samplePass";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {

            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getTables(null, "SYSTEM", "%", new String[]{"TABLE"});

            if (!rs.isBeforeFirst()) {
                System.out.println("No tables found in the database");
            }

            while (rs.next()) {

                String tableName = rs.getString("TABLE_NAME");

                if (tableName.startsWith("SYS_") ||
                        tableName.startsWith("WRH$") ||
                        tableName.startsWith("WRI$") ||
                        tableName.startsWith("AU$")) {
                    continue;
                }

                System.out.println("Found table: " + rs.getString("TABLE_NAME"));
                allTables.add(new DatabaseTable(rs.getString("TABLE_NAME"), "OracleDB"));
            }

        } catch (SQLException e) {
            System.err.println("Database connection error:");
            e.printStackTrace();
            // You might want to show an alert to the user here
        }
    }

    //remove feature
    public void handleRemoveTable(ActionEvent event) throws SQLException {
        DatabaseTable selectedTable = tablesTableView.getSelectionModel().getSelectedItem();
        if (selectedTable != null) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Confirm Deletion");
            confirmation.setHeaderText("Delete Table");
            confirmation.setContentText("Are you sure you want to delete the table '" +
                    selectedTable.getName() + "' from the database " +
                    selectedTable.getType() + "?");

            if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                if (deleteTableFromDatabase(selectedTable)) {
                    allTables.remove(selectedTable);
                    showAlert("Success", "Table deleted successfully", Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Error", "Failed to delete table", Alert.AlertType.ERROR);
                }
            }
        }
    }

    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
            DatabaseTable selectedTable = tablesTableView.getSelectionModel().getSelectedItem();
            if (selectedTable != null) {
                try {
                    handleRemoveTable(new ActionEvent());
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Error", "Failed to delete table: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        }
    }

    private boolean deleteTableFromDatabase(DatabaseTable table) {
        String tableName = table.getName();
        String dbType = table.getType();

        try {
            switch (dbType) {
                case "PostgreSQL":
                    return deletePostgresTable(tableName);
                case "MySQL":
                    return deleteMySQLTable(tableName);
                case "OracleDB":
                    return deleteOracleTable(tableName);
                default:
                    return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean deletePostgresTable(String tableName) throws SQLException {
        String url = "jdbc:postgresql://localhost:5432/TestD";
        String user = "postgres";
        String password = "samplePass";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE \"" + tableName + "\" CASCADE");
            return true;
        }
    }

    private boolean deleteMySQLTable(String tableName) throws SQLException {
        String url = "jdbc:mysql://localhost:3306/world";
        String user = "root";
        String password = "samplePass";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE `" + tableName + "`");
            return true;
        }
    }

    private boolean deleteOracleTable(String tableName) throws SQLException {
        String url = "jdbc:oracle:thin:@localhost:1521/free";
        String user = "system";
        String password = "samplePass";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            // Oracle requires uppercase table names
            stmt.execute("DROP TABLE \"" + tableName.toUpperCase() + "\" CASCADE CONSTRAINTS");
            return true;
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    //add table
    public void handleAddTable() {

        ObservableList<String> dbList = FXCollections.observableArrayList("PostgreSQL", "MySQL", "OracleDB");

        //(1)
        ChoiceDialog<String> dbEnter = new ChoiceDialog<>("PostgreSQL", dbList);

        dbEnter.setTitle("Select Database");
        dbEnter.setHeaderText("Choose Database Type");
        dbEnter.setContentText("Database:");

        //optional --> may or may not contain value (user may cancel)
        Optional<String> dbResult = dbEnter.showAndWait();

        if (!dbResult.isPresent()) {
            return; //exit method early
        }

        //get the picked db
        String dbType = dbResult.get();

        //(2) - table name input
        TextInputDialog tableEnter = new TextInputDialog();
        tableEnter.setTitle("Create Table");
        tableEnter.setHeaderText("Create table (" + dbType + ")");
        tableEnter.setContentText("Table Name:");

        Optional<String> tableResult = tableEnter.showAndWait();

        //check input
        if (!dbResult.isPresent()) {
            return; //exit early
        }

        String tableName = tableResult.get();

        //(3) - create table in dbs
        try {
            boolean success = false;

            //switch sees which db to pick

            switch (dbType) {
                case "PostgreSQL":
                    success = addPostgresTable(tableName);
                    handleRefresh();
                    break;
                case "MySQL":
                    success = addMySQLTable(tableName);
                    handleRefresh();
                    break;
                case "OracleDB":
                    success = addOracleTable(tableName);
                    handleRefresh();
                    break;
            }

            //success and error message handling
            if (success) {
                showAlert("Success", "Table " + tableName + "created successfully in " + dbType, Alert.AlertType.INFORMATION);
            } else {
                showAlert("Error", "Failed to create table", Alert.AlertType.ERROR);
            }

        } catch (SQLException e) {
            e.printStackTrace();

        }

    }


    private boolean addPostgresTable(String tableName) throws SQLException {

        String url = "jdbc:postgresql://localhost:5432/TestD";
        String user = "postgres";
        String password = "samplePass";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement statement = conn.createStatement()) {

            String sql = "CREATE TABLE \"" + tableName + "\" (" +
                    "id SERIAL PRIMARY KEY, " +
                    "name VARCHAR(255)" +
                    ")";

            statement.execute(sql);
            return true; //success (handleAdd table method)
        } catch (SQLException e) {

            System.err.println("PostgreSQL Error: " + e.getMessage());
            return false;

        }
    }

    private boolean addMySQLTable(String tableName) throws SQLException {

        String url = "jdbc:mysql://localhost:3306/world";
        String user = "root";
        String password = "samplePass";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement statement = conn.createStatement()) {

            String sql = "CREATE TABLE `" + tableName + "` (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " + // MySQL uses AUTO_INCREMENT, not SERIAL
                    "name VARCHAR(255)" +
                    ")";

            statement.execute(sql);
            return true; //success (handleAdd table method)
        } catch (SQLException e) {

            System.err.println("MySQL Error: " + e.getMessage());
            return false;

        }
    }

    private boolean addOracleTable(String tableName) throws SQLException {
        String url = "jdbc:oracle:thin:@localhost:1521/free";
        String user = "system";
        String password = "samplePass";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement statement = conn.createStatement()) {

            // Create table with regular NUMBER column instead of auto-generated identity
            String sql = "CREATE TABLE \"" + tableName.toUpperCase() + "\" (" +
                    "id NUMBER DEFAULT 1 PRIMARY KEY, " +  // Regular NUMBER column, editable
                    "name VARCHAR(255)" +
                    ")";

            statement.execute(sql);
            return true;
        } catch (SQLException e) {
            System.err.println("Oracle Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void handleRenameTable() {

        DatabaseTable selectedTable = tablesTableView.getSelectionModel().getSelectedItem();

        //dialog to get new table name
        TextInputDialog renameDialog = new TextInputDialog(selectedTable.getName());
        renameDialog.setTitle("Rename Table");
        renameDialog.setHeaderText("Rename table in " + selectedTable.getType());
        renameDialog.setContentText("New Table Name:");

        //show dialog and wait for input
        Optional<String> result = renameDialog.showAndWait();

        //process result
        result.ifPresent(newName -> {
            //validate new name
            if (newName.trim().isEmpty()) {
                showAlert("Invalid Name", "Table name cannot be empty", Alert.AlertType.ERROR);
                return;
            }

            if (newName.equals(selectedTable.getName())) {
                return; //do nothing
            }
            //i don't think we need this

            //perform rename operation
            try {
                boolean success = renameTableInDatabase(selectedTable, newName);

                if (success) {

                    //update table in observable list
                    selectedTable.setName(newName);
                    //refresh tableview to view changes
                    tablesTableView.refresh();
                    showAlert("Success", "Table renamed successfully", Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Error", "Failed to rename table", Alert.AlertType.ERROR);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Database Error", "Error renaming table: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private boolean renameTableInDatabase(DatabaseTable table, String newName) throws SQLException {

        String beforeName = table.getName();
        String dbType = table.getType();

        try {
            switch (dbType) {
                case "PostgreSQL":
                    return renamePostgreSQLTable(beforeName, newName);
                case "MySQL":
                    return renameMySQLTable(beforeName, newName);
                case "OracleDB":
                    return renameOracleDBTable(beforeName, newName);
                default:
                    return false;
            }
        } catch (SQLException e) {
            throw e;
        }
    }

    private boolean renamePostgreSQLTable(String beforeName, String newName) throws SQLException {
        String url = "jdbc:postgresql://localhost:5432/TestD";
        String user = "postgres";
        String password = "samplePass";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement statement = conn.createStatement()) {
            String sql = "ALTER TABLE \"" + beforeName + "\" RENAME TO \"" + newName + "\"";
            statement.execute(sql);
            return true;
        }
    }

    private boolean renameMySQLTable(String beforeName, String newName) throws SQLException {
        String url = "jdbc:mysql://localhost:3306/world";
        String user = "root";
        String password = "samplePass";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement statement = conn.createStatement()) {
            String sql = "RENAME TABLE `" + beforeName + "` TO `" + newName + "`";
            statement.execute(sql);
            return true;
        }
    }

    private boolean renameOracleDBTable(String beforeName, String newName) throws SQLException {
        String url = "jdbc:oracle:thin:@localhost:1521/free";
        String user = "system";
        String password = "samplePass";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement statement = conn.createStatement()) {
            String sql = "ALTER TABLE \"" + beforeName.toUpperCase() + "\" RENAME TO \"" + newName.toUpperCase() + "\"";
            statement.execute(sql);
            return true;
        }
    }

    public void handleEditTable() throws IOException {

        DatabaseTable selectedTable = tablesTableView.getSelectionModel().getSelectedItem();

        // pass data to EditTablesController class
        FXMLLoader loader = new FXMLLoader(getClass().getResource("editTables.fxml"));
        Parent root = loader.load();

        //get controller and pass the chosen table
        EditTablesController controller = loader.getController();
        controller.receiveTable(selectedTable);

        //switch scene
        Stage stage = (Stage) editButton.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.centerOnScreen();

    }

}