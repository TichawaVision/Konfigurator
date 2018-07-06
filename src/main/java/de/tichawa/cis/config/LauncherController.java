package de.tichawa.cis.config;

import java.io.*;
import java.net.*;
import java.util.*;
import javafx.event.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.stage.*;

public class LauncherController implements Initializable
{

  @FXML
  private ComboBox selectCIS;
  @FXML
  private Button button;

  @Override
  public void initialize(URL url, ResourceBundle rb)
  {
    
  }
  
  @FXML
  private void handleContinue(ActionEvent event)
  {
    try
    {
      Parent root = FXMLLoader.load(getClass().getResource("/de/tichawa/cis/config/" + selectCIS.getSelectionModel().getSelectedItem().toString().toLowerCase() + "/Mask.fxml"));
      Scene scene = new Scene(root);
      Stage stage = new Stage();
      stage.setScene(scene);
      stage.setTitle((String) selectCIS.getSelectionModel().getSelectedItem());
      stage.getIcons().add(new Image(getClass().getResourceAsStream("TiViCC.png")));
      stage.centerOnScreen();
      stage.show();
    }
    catch(IOException e)
    {
      e.printStackTrace();
    }
  }
}
