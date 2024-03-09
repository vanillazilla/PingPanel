import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class MainTab extends Tab {

    private VBox mainLayout = new VBox(10);
    private GridPane pingAreasGrid = new GridPane();
    private ChoiceBox<Integer> boxChoiceBox;

    public MainTab() {
        super("Main");
        setClosable(false);
        initializeUI();
    }

    private void initializeUI() {
        mainLayout.setAlignment(Pos.TOP_LEFT);
        mainLayout.setPadding(new Insets(10));

        // Initialize the ping areas grid
        pingAreasGrid.setHgap(10);
        pingAreasGrid.setVgap(10);
        updatePingBoxes(1);  // Start with 1 ping box

        // Options at the bottom
        VBox optionsContainer = new VBox(10);
        optionsContainer.setAlignment(Pos.TOP_LEFT);
        optionsContainer.setPadding(new Insets(0, 10, 10, 10));  // Add some left margin

        // Number of boxes selector
        HBox boxSelectionContainer = new HBox(10);
        boxSelectionContainer.setAlignment(Pos.CENTER_LEFT);
        Label boxLabel = new Label("Select Number of Boxes: ");
        boxChoiceBox = new ChoiceBox<>(FXCollections.observableArrayList(1, 2, 3, 4));
        boxChoiceBox.setValue(1);  // Default value
        boxChoiceBox.setOnAction(event -> updatePingBoxes(boxChoiceBox.getValue()));
        boxSelectionContainer.getChildren().addAll(boxLabel, boxChoiceBox);

        // Ping interval and amount of pings
        HBox pingIntervalContainer = new HBox(10);
        pingIntervalContainer.setAlignment(Pos.CENTER_LEFT);
        Label intervalLabel = new Label("Ping Interval (ms): ");
        TextField intervalField = new TextField("500");
        pingIntervalContainer.getChildren().addAll(intervalLabel, intervalField);

        HBox amountOfPingsContainer = new HBox(10);
        amountOfPingsContainer.setAlignment(Pos.CENTER_LEFT);
        Label amountLabel = new Label("Amount of Pings: ");
        TextField amountOfPingsField = new TextField();
        amountOfPingsContainer.getChildren().addAll(amountLabel, amountOfPingsField);

        // Ping all and stop all buttons
        HBox buttonContainer = new HBox(10);
        buttonContainer.setAlignment(Pos.CENTER_LEFT);
        Button pingAllButton = new Button("Ping All");
        Button stopAllButton = new Button("Stop All");
        buttonContainer.getChildren().addAll(pingAllButton, stopAllButton);

        // Adding all option elements to the container
        optionsContainer.getChildren().addAll(boxSelectionContainer, pingIntervalContainer, amountOfPingsContainer, buttonContainer);

        // Adding ping areas and options to the main layout
        mainLayout.getChildren().addAll(pingAreasGrid, optionsContainer);
        setContent(mainLayout);
    }


    private VBox setupControlButtons() {
        VBox controlButtons = new VBox(10);
        controlButtons.setAlignment(Pos.CENTER);
        controlButtons.setPadding(new Insets(10));

        // Number of boxes selection
        HBox boxSelectionBox = new HBox(10);
        boxSelectionBox.setAlignment(Pos.CENTER);
        Label boxLabel = new Label("Select Number of Boxes: ");
        ChoiceBox<Integer> boxChoiceBox = new ChoiceBox<>();
        boxChoiceBox.getItems().addAll(1, 2, 3, 4);
        boxChoiceBox.setValue(1);  // Default value
        boxChoiceBox.setOnAction(event -> updatePingBoxes(boxChoiceBox.getValue()));
        boxSelectionBox.getChildren().addAll(boxLabel, boxChoiceBox);

        // Other controls (interval input, amount of pings, buttons)
        HBox intervalBox = new HBox(10, new Label("Ping Interval (ms): "), new TextField("500"));
        intervalBox.setAlignment(Pos.CENTER);
        HBox amountOfPingsBox = new HBox(10, new Label("Amount of Pings: "), new TextField());
        amountOfPingsBox.setAlignment(Pos.CENTER);
        HBox buttonBox = new HBox(10, new Button("Ping All"), new Button("Stop All"));
        buttonBox.setAlignment(Pos.CENTER);

        controlButtons.getChildren().addAll(boxSelectionBox, intervalBox, amountOfPingsBox, buttonBox);
        return controlButtons;
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
        HBox buttonBox = new HBox(10, new Button("Ping"), new Button("Stop"));
        buttonBox.setAlignment(Pos.CENTER);
        Label infoLabel = new Label("Success: 0 | Failed: 0 | Data: -");
        TextArea outputTextArea = new TextArea();
        outputTextArea.setEditable(false);
        outputTextArea.setWrapText(true);

        pingArea.getChildren().addAll(titleLabel, ipField, buttonBox, infoLabel, outputTextArea);
        return pingArea;
    }
}
