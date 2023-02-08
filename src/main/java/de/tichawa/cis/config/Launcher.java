package de.tichawa.cis.config;

import javafx.application.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

/**
 * Class that launches the application. This class contains the main method.
 */
public class Launcher extends Application {

    public static final Properties PROP = new Properties();
    public static Path ferixHome;

    /**
     * Starts this application. Reads the property file and creates the first stage (Launcher.fxml)
     *
     * @param stage the first stage object that is automatically passed by this application
     * @throws Exception any exception that might occur
     */
    @Override
    public void start(Stage stage) throws Exception {
        PROP.loadFromXML(Launcher.class.getResourceAsStream("properties.xml"));
        ferixHome = Paths.get(PROP.getProperty("ferixHome"));
        stage.setOnCloseRequest(w -> Platform.exit());

        URL launcher = getClass().getResource("Launcher.fxml");
        if (launcher != null) {
            stage.setScene(new Scene(FXMLLoader.load(launcher)));
            stage.setTitle("CIS Configurator_" + ResourceBundle.getBundle("de.tichawa.cis.config.version").getString("version"));
            InputStream icon = getClass().getResourceAsStream("TiViCC.png");
            if (icon != null) {
                stage.getIcons().add(new Image(icon));
            }
            stage.centerOnScreen();
            stage.show();
        }
    }

    /**
     * Launches the application
     *
     * @param args the arguments for the main method that are passed to the launch method of this application
     */
    public static void main(String[] args) {
        launch(args);
    }
}
