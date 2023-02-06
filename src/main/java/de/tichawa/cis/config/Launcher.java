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

public class Launcher extends Application {

    public static final Properties PROP = new Properties();
    public static Path ferixHome;

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

    public static void main(String[] args) {
        launch(args);
    }
}
