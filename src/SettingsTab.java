import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class SettingsTab extends Tab {
    private ConfigurationManager configManager;

    public SettingsTab(ConfigurationManager configManager) {
        super("Settings");
        this.configManager = configManager;
        setClosable(false);
        initializeUI();
    }

    private void initializeUI() {
        VBox mainLayout = new VBox(10);
        //mainLayout.getStyleClass().add("color-settings");
        mainLayout.setPadding(new Insets(15, 20, 20, 20));

        // Color Settings Section
        VBox colorSettingsLayout = new VBox(10);
        HBox reachableColorSetting = createColorSetting("Reachable Color:", "reachableColor", Color.GREEN.toString());
        HBox unreachableColorSetting = createColorSetting("Unreachable Color:", "unreachableColor", Color.RED.toString());
        colorSettingsLayout.getChildren().addAll(reachableColorSetting, unreachableColorSetting);

        TitledPane colorSettingsPane = new TitledPane("Color Settings", colorSettingsLayout);
        colorSettingsPane.setExpanded(true);

        mainLayout.getChildren().add(colorSettingsPane);
        setContent(mainLayout);
    }

    private HBox createColorSetting(String labelText, String propertyKey, String defaultColor) {
        Label label = new Label(labelText);
        label.setFont(new Font("Arial", 14));
        ColorPicker colorPicker = new ColorPicker(Color.valueOf(configManager.getProperty(propertyKey, defaultColor)));
        colorPicker.setOnAction(event -> {
            Color newColor = colorPicker.getValue();
            configManager.setProperty(propertyKey, newColor.toString());
            configManager.saveProperties();
        });

        HBox settingBox = new HBox(10);
        settingBox.getChildren().addAll(label, colorPicker);
        settingBox.setAlignment(Pos.CENTER_LEFT);
        return settingBox;
    }
}
