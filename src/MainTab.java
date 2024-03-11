import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainTab extends Tab implements PresetUpdateListener {

    private PresetUpdateListener presetUpdateListener;
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private Map<Integer, Process> activePings = new HashMap<>();

    private VBox mainLayout = new VBox(10);
    private GridPane pingAreasGrid = new GridPane();
    private ChoiceBox<Integer> boxChoiceBox;
    private TextField intervalField, amountOfPingsField, presetNameField;
    private ComboBox<String> presetComboBox;
    private ConfigurationManager configManager;

    public MainTab(ConfigurationManager configManager, PresetUpdateListener listener) {
        super("Main");
        this.configManager = configManager;
        this.presetUpdateListener = listener;
        setClosable(false);
        initializeUI();
        loadPresets();
    }

    public void setPresetUpdateListener(PresetUpdateListener listener) {
        this.presetUpdateListener = listener;
    }

    @Override
    public void onPresetUpdate() {
        loadPresets();
    }

    @Override
    public void onMainTabUpdate() {
        // Placeholder for potential future use
    }

    private void initializeUI() {
        mainLayout.setAlignment(Pos.TOP_LEFT);
        mainLayout.setPadding(new Insets(10));

        pingAreasGrid.setHgap(10);
        pingAreasGrid.setVgap(10);
        updatePingBoxes(1);

        // Ensure the fields are initialized before being used
        intervalField = new TextField("500");
        amountOfPingsField = new TextField();
        presetNameField = new TextField();

        VBox optionsContainer = new VBox(10);
        optionsContainer.setAlignment(Pos.TOP_LEFT);
        optionsContainer.setPadding(new Insets(0, 10, 10, 10));

        HBox boxSelectionContainer = new HBox(10);
        boxSelectionContainer.setAlignment(Pos.CENTER_LEFT);
        Label boxLabel = new Label("Select Number of Boxes: ");
        boxChoiceBox = new ChoiceBox<>(FXCollections.observableArrayList(1, 2, 3, 4));
        boxChoiceBox.setValue(1);
        boxChoiceBox.setOnAction(event -> updatePingBoxes(boxChoiceBox.getValue()));
        boxSelectionContainer.getChildren().addAll(boxLabel, boxChoiceBox);

        HBox pingIntervalContainer = new HBox(10, new Label("Ping Interval (ms): "), intervalField);
        pingIntervalContainer.setAlignment(Pos.CENTER_LEFT);

        HBox amountOfPingsContainer = new HBox(10, new Label("Amount of Pings: "), amountOfPingsField);
        amountOfPingsContainer.setAlignment(Pos.CENTER_LEFT);

        Button pingAllButton = new Button("Ping All");
        pingAllButton.setOnAction(event -> pingAll());

        Button stopAllButton = new Button("Stop All");
        stopAllButton.setOnAction(event -> stopAll());

        HBox buttonContainer = new HBox(10, pingAllButton, stopAllButton);
        buttonContainer.setAlignment(Pos.CENTER_LEFT);

        optionsContainer.getChildren().addAll(boxSelectionContainer, pingIntervalContainer, amountOfPingsContainer, buttonContainer);

        VBox presetContainer = new VBox(10);
        presetContainer.setAlignment(Pos.TOP_LEFT);
        presetContainer.setPadding(new Insets(10));

        presetNameField.setPromptText("Preset Name");

        Button savePresetButton = new Button("Save Preset");
        savePresetButton.setOnAction(event -> savePreset());

        presetComboBox = new ComboBox<>();
        presetComboBox.setOnAction(event -> loadPreset(presetComboBox.getValue()));

        presetContainer.getChildren().addAll(new Label("Preset Name:"), presetNameField, savePresetButton, new Label("Load Preset:"), presetComboBox);

        HBox bottomContainer = new HBox(20);
        bottomContainer.setAlignment(Pos.TOP_LEFT);
        bottomContainer.setPadding(new Insets(10));
        bottomContainer.getChildren().addAll(optionsContainer, presetContainer);

        mainLayout.getChildren().addAll(pingAreasGrid, bottomContainer);
        setContent(mainLayout);
    }


    private void updatePingBoxes(int numBoxes) {
        pingAreasGrid.getChildren().clear();
        int rowIndex = 0;
        int colIndex = 0;

        for (int i = 1; i <= numBoxes; i++) {
            VBox pingArea = createPingArea(i);
            pingAreasGrid.add(pingArea, colIndex, rowIndex);
            colIndex++;
            if (colIndex > 1) {
                colIndex = 0;
                rowIndex++;
            }
        }
    }

    private VBox createPingArea(int index) {
        VBox pingArea = new VBox(5);
        pingArea.setPadding(new Insets(5));
        pingArea.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

        Label titleLabel = new Label("Ping " + index);
        TextField ipField = new TextField();
        ipField.setPromptText("Enter IP Address");
        Button pingButton = new Button("Ping");
        Button stopButton = new Button("Stop");
        HBox buttonBox = new HBox(10, pingButton, stopButton);
        buttonBox.setAlignment(Pos.CENTER);
        Label infoLabel = new Label("Success: 0 | Failed: 0 | Data: -");
        TextArea outputTextArea = new TextArea();
        outputTextArea.setEditable(false);
        outputTextArea.setWrapText(true);

        pingButton.setOnAction(event -> startPing(ipField.getText(), outputTextArea, infoLabel, pingButton, stopButton, index));
        stopButton.setOnAction(event -> stopPing(index));

        pingArea.getChildren().addAll(titleLabel, ipField, buttonBox, infoLabel, outputTextArea);
        return pingArea;
    }

    private void startPing2(String ipAddress, TextArea outputTextArea, Label infoLabel, Button pingButton, Button stopButton, int index) {
        pingButton.setDisable(true);
        stopButton.setDisable(false);

        int amountOfPings;
        try {
            amountOfPings = Integer.parseInt(amountOfPingsField.getText().trim());
            if (amountOfPings <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            amountOfPings = isWindows() ? Integer.MAX_VALUE : 4; // Default to continuous ping for Windows, 4 pings for others
        }
        final int[] successCount = {0};
        final int[] failureCount = {0};
        final long[] maxTime = {Long.MIN_VALUE};
        final long[] minTime = {Long.MAX_VALUE};
        final long[] totalTime = {0};

        int finalAmountOfPings = amountOfPings;
        executorService.submit(() -> {
            Process process = null;
            try {
                ProcessBuilder builder = new ProcessBuilder(isWindows() ? "ping" : "ping", isWindows() ? "-n" : "-c", String.valueOf(finalAmountOfPings), ipAddress);
                process = builder.start();
                activePings.put(index, process);

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    String finalLine = line;
                    Platform.runLater(() -> outputTextArea.appendText(finalLine + "\n"));
                        successCount[0]++;
                        long time = Long.parseLong(line.replaceAll(".*time=(\\d+).*", "$1"));
                        totalTime[0] += time;
                        if (time > maxTime[0]) maxTime[0] = time;
                        if (time < minTime[0]) minTime[0] = time;
                        long avgTime = totalTime[0] / successCount[0];
                        Platform.runLater(() -> {
                            infoLabel.setStyle("-fx-text-fill: green;");
                            infoLabel.setText("Success: " + successCount[0] + " | Failed: " + failureCount[0] + " | Data: Max: " + maxTime[0] + "ms. Min: " + minTime[0] + "ms. AVG: " + avgTime + "ms.");
                        });
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Platform.runLater(() -> {
                pingButton.setDisable(false);
                stopButton.setDisable(true);
            });
            activePings.remove(index);
        });
    }

    private String getCssColor(String configKey, String defaultColor) {
        String colorValue = configManager.getProperty(configKey, defaultColor);
        Color fxColor = Color.valueOf(colorValue);
        return String.format("-fx-text-fill: rgba(%d, %d, %d, %.2f);",
                (int) (fxColor.getRed() * 255),
                (int) (fxColor.getGreen() * 255),
                (int) (fxColor.getBlue() * 255),
                fxColor.getOpacity());
    }

    private void logPingResult(String ipAddress, String pingResult, String date, String sessionFileName) {
        String folderName = ipAddress.replaceAll("[^a-zA-Z0-9.-]", "_"); // Sanitize IP address for use in file paths

        // Creating a "ip logs" directory and then a subdirectory for the specific IP
        File ipLogsDir = new File("ip logs");
        if (!ipLogsDir.exists()) {
            ipLogsDir.mkdir(); // Create "ip logs" directory if it doesn't exist
        }

        File dir = new File(ipLogsDir, folderName + File.separator + date);
        if (!dir.exists()) {
            dir.mkdirs(); // Create the directory for the IP and date if it doesn't exist
        }

        File logFile = new File(dir, sessionFileName); // Use the session-specific file name

        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.write(date + ": "+ pingResult + "\n");
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }



    private void startPing(String ipAddress, TextArea outputTextArea, Label infoLabel, Button pingButton, Button stopButton, int index) {
        pingButton.setDisable(true);
        stopButton.setDisable(false);

        // Generate a unique file name for this ping session
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdfDateTime = new SimpleDateFormat("yyyy-MM-dd hh-mm-ss-a");
        String date = sdfDate.format(new Date());
        String dateTime = sdfDateTime.format(new Date());
        String sessionFileName = dateTime + ".txt";


        int amountOfPings;
        try {
            amountOfPings = Integer.parseInt(amountOfPingsField.getText().trim());
            if (amountOfPings <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            amountOfPings = isWindows() ? Integer.MAX_VALUE : 4; // Default to continuous ping for Windows, 4 pings for others
        }
        final int[] successCount = {0};
        final int[] failureCount = {0};
        final long[] maxTime = {Long.MIN_VALUE};
        final long[] minTime = {Long.MAX_VALUE};
        final long[] totalTime = {0};

        int finalAmountOfPings = amountOfPings;
        executorService.submit(() -> {
            Process process = null;
            try {
                ProcessBuilder builder = new ProcessBuilder("ping", isWindows() ? "-n" : "-c", String.valueOf(finalAmountOfPings), ipAddress);
                process = builder.start();
                activePings.put(index, process);

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null && !Thread.currentThread().isInterrupted()) {
                    String finalLine = line;
                    Platform.runLater(() -> {
                        outputTextArea.appendText(finalLine + "\n");
                        logPingResult(ipAddress, finalLine, dateTime, sessionFileName);
                    });
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
                    String timestamp = sdf.format(new Date());


                    //System.out.println(timestamp + ": IP "+ ipAddress +" - Ping Output: " + finalLine);
                    //logPingResult(ipAddress, timestamp + ": IP "+ ipAddress +" - Ping Output: " + finalLine);

                    if (line.contains("time=")) {
                        successCount[0]++;
                        long time = Long.parseLong(line.replaceAll(".*time=(\\d+).*", "$1"));
                        totalTime[0] += time;
                        if (time > maxTime[0]) maxTime[0] = time;
                        if (time < minTime[0]) minTime[0] = time;
                        long avgTime = totalTime[0] / successCount[0];
                        Platform.runLater(() -> {
                            String successColor = getCssColor("reachableColor", "GREEN");
                            infoLabel.setStyle(successColor);
                            outputTextArea.setStyle("-fx-control-inner-background:" + successColor.replace("-fx-text-fill:", "") + ";");
                            infoLabel.setText("Success: " + successCount[0] + " | Failed: " + failureCount[0] + " | Data: Max: " + maxTime[0] + "ms. Min: " + minTime[0] + "ms. AVG: " + avgTime + "ms.");
                        });
                    } else if (line.contains("Request timed out") || line.contains("Destination host unreachable")) {
                        failureCount[0]++;
                        Platform.runLater(() -> {
                            String failureColor = getCssColor("unreachableColor", "RED");
                            infoLabel.setStyle(failureColor);
                            outputTextArea.setStyle("-fx-control-inner-background:" + failureColor.replace("-fx-text-fill:", "") + ";");
                            infoLabel.setText("Success: " + successCount[0] + " | Failed: " + failureCount[0] + " | Data: -");
                        });
                    }
                }
                int exitValue = process.waitFor(); // Wait for the process to complete
                if (exitValue != 0) {
                    //System.err.println("Ping process exited with value: " + exitValue);
                }
                reader.close();
            } catch (IOException e) {
                System.err.println("Error during the ping process: " + e.getMessage());
                e.printStackTrace();
            } catch (InterruptedException e) {
                System.err.println("Ping process was interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
            } finally {
                Platform.runLater(() -> {
                    pingButton.setDisable(false);
                    stopButton.setDisable(true);
                });
                activePings.remove(index);
            }
        });
    }

    private void stopPing(int index) {
        Process process = activePings.get(index);
        if (process != null) {
            //System.out.println("Stopping ping process for index: " + index);
            process.destroy();
            activePings.remove(index);
        } else {
            System.err.println("No ping process found to stop for index: " + index);
        }
    }


    private void pingAll() {
        int index = 0;
        for (Node node : pingAreasGrid.getChildren()) {
            if (node instanceof VBox) {
                VBox vbox = (VBox) node;
                TextField ipField = (TextField) vbox.getChildren().get(1);
                TextArea outputTextArea = (TextArea) vbox.getChildren().get(4);
                Label infoLabel = (Label) vbox.getChildren().get(3);
                Button pingButton = (Button) ((HBox) vbox.getChildren().get(2)).getChildren().get(0);
                Button stopButton = (Button) ((HBox) vbox.getChildren().get(2)).getChildren().get(1);
                startPing(ipField.getText(), outputTextArea, infoLabel, pingButton, stopButton, ++index);
            }
        }
    }

    private void stopAll() {
        for (Integer index : new ArrayList<>(activePings.keySet())) {
            stopPing(index);
        }
    }

    private void savePreset() {
        String presetName = presetNameField.getText().trim();
        if (presetName.isEmpty()) {
            showAlert("Preset name cannot be empty.");
            return;
        }

        String encodedPresetName = "preset_" + presetName.replace(" ", "\\ ");

        StringBuilder sb = new StringBuilder();
        sb.append(boxChoiceBox.getValue()).append(";");
        sb.append(intervalField.getText().trim()).append(";");
        sb.append(amountOfPingsField.getText().trim()).append(";");

        for (Node node : pingAreasGrid.getChildren()) {
            if (node instanceof VBox) {
                TextField ipField = (TextField) ((VBox) node).getChildren().get(1);
                sb.append(ipField.getText().trim()).append(",");
            }
        }

        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ',') {
            sb.deleteCharAt(sb.length() - 1);
        }

        configManager.setProperty(encodedPresetName, sb.toString());
        configManager.saveProperties();

        presetComboBox.getItems().add(presetName);

        if (presetUpdateListener != null) {
            presetUpdateListener.onMainTabUpdate();
        }
    }

    private void loadPresets() {
        presetComboBox.getItems().clear();
        configManager.getKeys().stream()
                .filter(key -> key.startsWith("preset_"))
                .forEach(key -> presetComboBox.getItems().add(key.substring(7).replace("\\ ", " ")));
    }

    private void loadPreset(String presetName) {
        String encodedPresetName = "preset_" + presetName.replace(" ", "\\ ");
        String presetData = configManager.getProperty(encodedPresetName, "");
        if (presetData.isEmpty()) {
            return;
        }

        String[] parts = presetData.split(";");
        if (parts.length < 3) {
            return;
        }

        boxChoiceBox.setValue(Integer.parseInt(parts[0]));
        updatePingBoxes(Integer.parseInt(parts[0]));
        intervalField.setText(parts[1]);
        amountOfPingsField.setText(parts[2]);

        String[] ips = parts.length > 3 ? parts[3].split(",") : new String[0];
        int index = 0;
        for (Node node : pingAreasGrid.getChildren()) {
            if (node instanceof VBox && index < ips.length) {
                VBox vbox = (VBox) node;
                TextField ipField = (TextField) vbox.getChildren().get(1);
                ipField.setText(ips[index++]);
            }
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}