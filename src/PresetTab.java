import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

public class PresetTab extends Tab implements PresetUpdateListener {

    private PresetUpdateListener mainTabUpdateListener;

    private ConfigurationManager configManager;
    private VBox mainLayout = new VBox(10);
    private TextField presetNameField;
    private ChoiceBox<Integer> numberOfBoxesChoiceBox;
    private TextField intervalField, amountOfPingsField;
    private VBox presetsDisplayContainer;
    private VBox ipFieldsContainer;
    private List<TextField> ipTextFields = new ArrayList<>();

    public PresetTab(ConfigurationManager configManager, PresetUpdateListener listener) {
        super("Manage Presets");
        this.configManager = configManager;
        this.mainTabUpdateListener = listener;
        setClosable(false);
        initializeUI();
    }

    @Override
    public void onPresetUpdate() {
        displayPresets();
    }

    @Override
    public void onMainTabUpdate() {
        displayPresets();
    }

    private void initializeUI() {
        mainLayout.setAlignment(Pos.TOP_LEFT);
        mainLayout.setPadding(new Insets(10));

        TitledPane createPresetPane = new TitledPane();
        createPresetPane.setText("Create New Preset");
        createPresetPane.setCollapsible(true);
        createPresetPane.setExpanded(false);

        VBox createPresetContent = new VBox(10);
        createPresetContent.setPadding(new Insets(10));

        presetNameField = new TextField();
        presetNameField.setPromptText("Preset Name");

        numberOfBoxesChoiceBox = new ChoiceBox<>(FXCollections.observableArrayList(1, 2, 3, 4));
        numberOfBoxesChoiceBox.setValue(1);
        numberOfBoxesChoiceBox.setOnAction(event -> updateIpFields(numberOfBoxesChoiceBox.getValue()));

        intervalField = new TextField();
        intervalField.setPromptText("Ping Interval (ms)");

        amountOfPingsField = new TextField();
        amountOfPingsField.setPromptText("Amount of Pings");

        ipFieldsContainer = new VBox(5);
        updateIpFields(1);

        Button savePresetButton = new Button("Save Preset");
        savePresetButton.setOnAction(event -> savePreset());

        createPresetContent.getChildren().addAll(
                new Label("Preset Name:"), presetNameField,
                new Label("Number of Boxes:"), numberOfBoxesChoiceBox,
                new Label("Ping Interval:"), intervalField,
                new Label("Amount of Pings:"), amountOfPingsField,
                ipFieldsContainer,
                savePresetButton
        );
        createPresetPane.setContent(createPresetContent);

        presetsDisplayContainer = new VBox(10);
        presetsDisplayContainer.setPadding(new Insets(10));

        mainLayout.getChildren().addAll(createPresetPane, new Label("Existing Presets:"), presetsDisplayContainer);
        setContent(mainLayout);

        displayPresets();
    }

    private void updateIpFields(int numberOfFields) {
        ipFieldsContainer.getChildren().clear();
        ipTextFields.clear();

        for (int i = 0; i < numberOfFields; i++) {
            TextField ipField = new TextField();
            ipField.setPromptText("IP Address " + (i + 1));
            ipFieldsContainer.getChildren().add(ipField);
            ipTextFields.add(ipField);
        }
    }

    private void savePreset() {
        String presetName = presetNameField.getText().trim();
        if (presetName.isEmpty()) {
            showAlert("Preset name cannot be empty.");
            return;
        }

        String presetKey = "preset_" + presetName.replace(" ", "\\ ");

        StringBuilder sb = new StringBuilder();
        sb.append(numberOfBoxesChoiceBox.getValue()).append(";");
        sb.append(intervalField.getText().trim()).append(";");
        sb.append(amountOfPingsField.getText().trim()).append(";");

        for (TextField ipField : ipTextFields) {
            sb.append(ipField.getText().trim()).append(",");
        }

        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ',') {
            sb.deleteCharAt(sb.length() - 1);
        }

        // This will update the preset if it already exists or create a new one if it doesn't
        configManager.setProperty(presetKey, sb.toString());
        configManager.saveProperties();

        displayPresets();
        if (mainTabUpdateListener != null) {
            mainTabUpdateListener.onPresetUpdate();
        }
    }



    private void displayPresets() {
        presetsDisplayContainer.getChildren().clear();

        for (String key : configManager.getKeys()) {
            if (!key.startsWith("preset_")) continue;

            String presetData = configManager.getProperty(key, "");
            String[] parts = presetData.split(";");
            if (parts.length < 3) continue;

            TitledPane titledPane = new TitledPane();
            titledPane.setText(key.replace("preset_", ""));
            titledPane.setCollapsible(true);
            titledPane.setExpanded(false);

            VBox content = new VBox(5);
            content.getChildren().add(new Label("Number of Boxes: " + parts[0]));
            content.getChildren().add(new Label("Ping Interval: " + parts[1]));
            content.getChildren().add(new Label("Amount of Pings: " + parts[2]));

            if (parts.length > 3) {
                String[] ips = parts[3].split(",");
                for (String ip : ips) {
                    content.getChildren().add(new Label("IP: " + ip));
                }
            }

            Button deleteButton = new Button("Delete");
            int finalIndex = presetsDisplayContainer.getChildren().size();
            deleteButton.setOnAction(e -> deletePreset(key, finalIndex));
            content.getChildren().add(deleteButton);

            titledPane.setContent(content);
            presetsDisplayContainer.getChildren().add(titledPane);
        }
    }

    private void deletePreset(String key, int index) {
        configManager.removeProperty(key);
        presetsDisplayContainer.getChildren().remove(index);
        if (mainTabUpdateListener != null) {
            mainTabUpdateListener.onPresetUpdate();
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
