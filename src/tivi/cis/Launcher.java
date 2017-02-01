package tivi.cis;

import javafx.application.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.image.*;
import javafx.stage.*;

public class Launcher extends Application
{

  @Override
  public void start(Stage stage) throws Exception
  {
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

  public static void main(String[] args)
  {
    launch(args);
  }
}
