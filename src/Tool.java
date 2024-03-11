import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.InputStream;

public class Tool extends Application {

    @Override
    public void start(Stage primaryStage) {
        ConfigurationManager configManager = new ConfigurationManager();

        MainTab mainTab = new MainTab(configManager, null);
        PresetTab presetTab = new PresetTab(configManager, mainTab);
        mainTab.setPresetUpdateListener(presetTab);

        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(mainTab, new SettingsTab(configManager), presetTab);

        double width = Double.parseDouble(configManager.getProperty("windowWidth", "800"));
        double height = Double.parseDouble(configManager.getProperty("windowHeight", "600"));

        Scene scene = new Scene(tabPane, width, height);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Palamedes");

        InputStream logoStream = Tool.class.getResourceAsStream("/logo.png");
        if (logoStream != null) {
            primaryStage.getIcons().add(new Image(logoStream));
        }

        primaryStage.show();
    }

    public static void main(String[] args) {
        setupLogging();
        launch(args);
    }

    private static void setupLogging() {
        try {
            PrintStream console = System.out;
            FileOutputStream fileOut = new FileOutputStream("output.txt", true); // Append to the file
            PrintStream fileStream = new PrintStream(fileOut);
            PrintStream multiStream = new PrintStream(new OutputStream() {
                public void write(int b) throws IOException {
                    console.write(b);
                    fileStream.write(b);
                }
            });
            System.setOut(multiStream);
            System.setErr(multiStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
