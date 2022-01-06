package de.tichawa.cis.config;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import javafx.application.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.image.*;
import javafx.stage.*;

public class Launcher extends Application
{

  public static final Properties PROP = new Properties();
  public static Path ferixHome;

  @Override
  public void start(Stage stage) throws Exception
  {
    PROP.loadFromXML(Launcher.class.getResourceAsStream("properties.xml"));
    ferixHome = Paths.get(PROP.getProperty("ferixHome"));
    stage.setOnCloseRequest(w -> Platform.exit());

    stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("Launcher.fxml"))));
    stage.setTitle("CIS Configurator");
    stage.getIcons().add(new Image(getClass().getResourceAsStream("TiViCC.png")));
    stage.centerOnScreen();
    stage.show();
  }

  public static void main(String[] args)
  {
    launch(args);
  }
}
