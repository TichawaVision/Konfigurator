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

  public static final Properties prop = new Properties();
  public static Path tableHome;

  @Override
  public void start(Stage stage) throws Exception
  {
    prop.loadFromXML(Launcher.class.getResourceAsStream("properties.xml"));
    tableHome = Paths.get(prop.getProperty("tableHome"));
    stage.setOnCloseRequest((WindowEvent) ->
    {
      Platform.exit();
    });

    stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("Launcher.fxml"))));
    stage.setTitle("CIS Configurator");
    stage.getIcons().add(new Image(getClass().getResourceAsStream("TiViCC.png")));
    stage.centerOnScreen();
    stage.show();
  }

  public static void main(String[] args) throws IOException
  {
    launch(args);
  }
}
