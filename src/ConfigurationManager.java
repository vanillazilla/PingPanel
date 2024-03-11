import javafx.scene.paint.Color;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;

public class ConfigurationManager {
    private static final String PROPERTIES_FILE = "config.properties";
    private Properties props;

    public ConfigurationManager() {
        props = new Properties();
        loadProperties();
    }

    private void loadProperties() {
        try (FileInputStream in = new FileInputStream(PROPERTIES_FILE)) {
            props.load(in);
        } catch (IOException e) {
            System.out.println("Error loading the properties file: " + e.getMessage());
        }
    }

    public void saveProperties() {
        try (FileOutputStream out = new FileOutputStream(PROPERTIES_FILE)) {
            props.store(out, "Application Settings");
        } catch (IOException e) {
            System.out.println("Error saving the properties file: " + e.getMessage());
        }
    }

    public String getProperty(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public void setProperty(String key, String value) {
        props.setProperty(key, value);
    }

    public void removeProperty(String key) {
        props.remove(key);
        saveProperties();  // Save the properties file after removing the key
    }

    public Set<String> getKeys() {
        return props.stringPropertyNames();
    }
}
