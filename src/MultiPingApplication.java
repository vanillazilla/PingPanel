import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiPingApplication extends Application {

    private ExecutorService executorService = Executors.newCachedThreadPool();
    private List<PingTask> pingTasks = new ArrayList<>();
    private VBox pingAreasContainer = new VBox();
    private TextField intervalField;
    private TextField amountOfPingsField;
    private ChoiceBox<Integer> boxChoiceBox;
    private BorderPane root;

    private CheckBox tracert;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Ping Panel");

        root = new BorderPane();
        VBox controlButtons = new VBox(10);
        controlButtons.setPadding(new Insets(10));

        HBox boxSelectionBox = new HBox();
        Label boxLabel = new Label("Select Number of Boxes: ");
        boxChoiceBox = new ChoiceBox<>();
        boxChoiceBox.getItems().addAll(1, 2, 3, 4);
        boxChoiceBox.setValue(1); // Default value
        boxChoiceBox.setOnAction(event -> updatePingBoxes(boxChoiceBox.getValue())); // Event handler for choice box
        boxSelectionBox.getChildren().addAll(boxLabel, boxChoiceBox);

        HBox intervalBox = new HBox(new Label("Ping Interval (ms): "), intervalField = new TextField("500"));
        HBox amountOfPingsBox = new HBox(new Label("Amount of Pings: "), amountOfPingsField = new TextField());
        HBox buttonBox = new HBox(10);
        Button pingAllButton = new Button("Ping All");
        Button stopAllButton = new Button("Stop All");
        pingAllButton.setOnAction(event -> pingAll());
        stopAllButton.setOnAction(event -> stopAll());
        buttonBox.getChildren().addAll(pingAllButton, stopAllButton);

        controlButtons.getChildren().addAll(boxSelectionBox, intervalBox, amountOfPingsBox, buttonBox);

        pingAreasContainer = new VBox();
        pingAreasContainer.setPadding(new Insets(10));
        pingAreasContainer.setSpacing(10);

        // Ensure intervalField and amountOfPingsField are properly initialized before calling updatePingBoxes
        intervalField = new TextField("500");
        amountOfPingsField = new TextField();

        updatePingBoxes(1); // Initially, show one box
        ScrollPane scrollPane = new ScrollPane(pingAreasContainer);
        scrollPane.setFitToWidth(true); // Ensure the scroll pane resizes horizontally
        root.setCenter(scrollPane);

        // Set the controlButtons VBox as the bottom component of the BorderPane
        root.setBottom(controlButtons);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Adjust the stage size based on the content
        primaryStage.sizeToScene();
    }




    private void updatePingBoxes(int numBoxes) {
        pingAreasContainer.getChildren().clear();

        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);

        int rowIndex = 0;
        int colIndex = 0;

        for (int i = 1; i <= numBoxes; i++) {
            VBox pingArea = createPingArea(i);
            gridPane.add(pingArea, colIndex, rowIndex);

            colIndex++;
            if (colIndex == 2) {
                colIndex = 0;
                rowIndex++;
            }
        }

        pingAreasContainer.getChildren().add(gridPane);

        // Update Ping All button state
        boolean anyRunning = pingTasks.stream().anyMatch(PingTask::isRunning);
        if (root.getBottom() != null && root.getBottom() instanceof VBox) {
            VBox bottomVBox = (VBox) root.getBottom();
            for (Node node : bottomVBox.getChildren()) {
                if (node instanceof Button && ((Button) node).getText().equals("Ping All")) {
                    ((Button) node).setDisable(anyRunning);
                    break;
                }
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
        Label infoLabel = new Label("Success: 0 | Failed: 0 | Data: -");
        TextArea outputTextArea = new TextArea();
        outputTextArea.setEditable(false);
        outputTextArea.setWrapText(true);

        PingTask pingTask = null;

        pingButton.setOnAction(event -> {
            String ip = ipField.getText().trim();
            if (!ip.isEmpty()) {
                String intervalText = intervalField.getText().trim();
                String amountOfPingsText = amountOfPingsField.getText().trim();
                int amountOfPings = amountOfPingsText.isEmpty() ? 0 : Integer.parseInt(amountOfPingsText);
                if (!intervalText.isEmpty()) {
                    int interval = Integer.parseInt(intervalText);
                    final PingTask finalPingTask = createAndStartPingTask(ip, outputTextArea, infoLabel, interval, amountOfPings);
                    stopButton.setOnAction(e -> finalPingTask.stop()); // Use final reference
                } else {
                    showAlert("Interval field cannot be empty.");
                }
            } else {
                showAlert("IP Address field cannot be empty.");
            }
        });

        stopButton.setOnAction(event -> {
            final PingTask finalPingTask = pingTask; // Create final reference
            if (finalPingTask != null) {
                finalPingTask.stop(); // Stop the corresponding PingTask
            }
        });

        HBox buttonBox = new HBox(pingButton, stopButton); // Declare buttonBox locally
        buttonBox.setAlignment(Pos.CENTER);
        pingArea.getChildren().addAll(titleLabel, ipField, buttonBox, infoLabel, outputTextArea);
        return pingArea;
    }

    private PingTask createAndStartPingTask(String ipAddress, TextArea outputTextArea, Label infoLabel, int interval, int amountOfPings) {
        PingTask pingTask = new PingTask(ipAddress, outputTextArea, infoLabel, interval, amountOfPings);
        executorService.execute(pingTask);
        pingTasks.add(pingTask);
        return pingTask;
    }

    private void pingAll() {
        for (PingTask pingTask : pingTasks) {
            pingTask.start(); // Start the task
        }
    }



    private void stopAll() {
        for (PingTask pingTask : pingTasks) {
            pingTask.stop();
        }
    }



    private class PingTask implements Runnable {
        private String ipAddress;
        private TextArea outputTextArea;
        private Label infoLabel;
        private boolean running;
        private int interval;
        private int successCount;
        private int failCount;
        private long totalRTT;
        private int pingCount;
        private int amountOfPings;

        public void start() {
            running = true;
        }

        PingTask(String ipAddress, TextArea outputTextArea, Label infoLabel, int interval, int amountOfPings) {
            this.ipAddress = ipAddress;
            this.outputTextArea = outputTextArea;
            this.infoLabel = infoLabel;
            this.interval = interval;
            this.amountOfPings = amountOfPings;
            this.running = true;
        }

        private long minRTT = Long.MAX_VALUE;
        private long maxRTT = Long.MIN_VALUE;

        @Override
        public void run() {
            try {
                while (running && (amountOfPings == 0 || pingCount < amountOfPings)) {
                    pingCount++;
                    ProcessBuilder processBuilder = new ProcessBuilder("ping", "-n", "1", ipAddress);
                    Process process = processBuilder.start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    StringBuilder stringBuilder = new StringBuilder();
                    boolean isSummary = false;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                        if (line.contains("Packets: Sent = ")) {
                            isSummary = true;
                        } else if (isSummary && line.isEmpty()) {
                            break; // End of summary section
                        }
                    }
                    reader.close();

                    String result = stringBuilder.toString();
                    boolean isSuccessful = result.contains("Reply from");
                    if (isSuccessful) {
                        successCount++;
                    } else {
                        failCount++;
                    }

                    String[] parts = result.split("\n");
                    String summaryLine = "";
                    for (String part : parts) {
                        if (part.contains("Minimum")) {
                            summaryLine = part.trim();
                            break;
                        }
                    }
                    String[] summaryParts = summaryLine.split(",");
                    if (summaryParts.length >= 3) {
                        String responseTime = summaryParts[1].trim().replaceAll("[^\\d]", ""); // Remove non-digit characters
                        long rtt = Long.parseLong(responseTime); // Extract RTT
                        totalRTT += rtt; // Update totalRTT
                        minRTT = Math.min(minRTT, rtt); // Update minRTT
                        maxRTT = Math.max(maxRTT, rtt); // Update maxRTT
                        Platform.runLater(() -> outputTextArea.appendText("Pinging " + ipAddress + ", replied in " + responseTime + "ms\n"));
                    }

                    Platform.runLater(() -> {
                        infoLabel.setText(String.format("Success: %d | Failed: %d | Min: %d ms | Max: %d ms | Avg: %.2f ms",
                                successCount, failCount, minRTT, maxRTT, calculateAverageRTT()));
                    });

                    Thread.sleep(interval); // Using milliseconds for the ping interval
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        void stop() {
            running = false;
        }

        boolean isRunning() {
            return running;
        }

        private double calculateAverageRTT() {
            return successCount == 0 ? 0 : (double) totalRTT / successCount;
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void stop() {
        executorService.shutdownNow();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
