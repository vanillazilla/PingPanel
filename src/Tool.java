import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;
import java.net.URL;

public class Tool extends Application {

    @Override
    public void start(Stage primaryStage) {
        ConfigurationManager configManager = new ConfigurationManager();

        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(
                new MainTab(),
                new SettingsTab(configManager)
        );

        double width = Double.parseDouble(configManager.getProperty("windowWidth", "800"));
        double height = Double.parseDouble(configManager.getProperty("windowHeight", "600"));

        Scene scene = new Scene(tabPane, width, height);

        // Apply the CSS file
        /*URL cssResource = getClass().getResource("/dark-theme.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        } else {
            System.out.println("Could not load the CSS file.");
        }*/

        primaryStage.setScene(scene);
        primaryStage.setTitle("Tabbed Application");

        InputStream logoStream = Tool.class.getResourceAsStream("/logo.png");
        if (logoStream != null) {
            primaryStage.getIcons().add(new Image(logoStream));
        }

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
