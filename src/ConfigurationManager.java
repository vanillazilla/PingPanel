import java.awt.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

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
            setDefaultProperties();
        }
    }

    private void setDefaultProperties() {
        props.setProperty("windowWidth", "800");
        props.setProperty("windowHeight", "600");
        props.setProperty("reachableColor", Color.GREEN.toString());
        props.setProperty("unreachableColor", Color.RED.toString());
        saveProperties();
    }

    public void saveProperties() {
        try (FileOutputStream out = new FileOutputStream(PROPERTIES_FILE)) {
            props.store(out, "Application Settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getProperty(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public void setProperty(String key, String value) {
        props.setProperty(key, value);
    }
}
