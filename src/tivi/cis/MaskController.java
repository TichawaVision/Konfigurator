package tivi.cis;

import java.io.*;
import java.net.*;
import java.util.*;
import javafx.event.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.stage.*;
import tivi.cis.mxled.*;

public abstract class MaskController implements Initializable
{
  @FXML
  protected ComboBox<String> Color;
  @FXML
  protected ComboBox<String> Resolution;
  @FXML
  protected ComboBox<String> ScanWidth;
  @FXML
  protected Label PixelSize;
  @FXML
  protected Label DefectSize;
  @FXML
  protected Label MaxLineRate;
  @FXML
  protected Label CurrLineRate;
  @FXML
  protected Slider SelLineRate;
  @FXML
  protected Label Speedmms;
  @FXML
  protected Label Speedms;
  @FXML
  protected Label Speedmmin;
  @FXML
  protected Label Speedips;
  @FXML
  protected ComboBox<String> InternalLightSource;
  @FXML
  protected ComboBox<String> InternalLightColor;
  @FXML
  protected ComboBox<String> ExternalLightSource;
  @FXML
  protected ComboBox<String> ExternalLightColor;
  @FXML
  protected ComboBox<String> Interface;
  @FXML
  protected ComboBox<String> Cooling;
  @FXML
  protected CheckBox Trigger;
  @FXML
  protected Label Infotext;
  @FXML
  protected Button Calculation;
  @FXML
  protected Button PartList;
  @FXML
  protected Button DataSheet;
  @FXML
  protected Button OEMMode;

  protected CIS CIS_DATA;
  protected MXLED MXLED_DATA;

  public MaskController()
  {
  }

  @Override
  public abstract void initialize(URL url, ResourceBundle rb);

  @FXML
  public void handleCalculation(ActionEvent event)
  {
    if(CIS_DATA.getSpec("MXLED") == null && CIS_DATA.getSpec("External Light Source") > 0)
    {
      try
      {
        MXLED_DATA.calculate();
      }
      catch(CISException e)
      {
        new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        return;
      }

      try
      {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/tivi/cis/Calculation.fxml"));
        Scene scene = new Scene(loader.load());
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("MXLED Calculation");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/tivi/cis/TiViCC.png")));
        stage.centerOnScreen();
        stage.show();
        stage.setX(stage.getX() - 20);
        stage.setY(stage.getY() - 20);

        CalculationController controller = loader.<CalculationController>getController();
        controller.passData(MXLED_DATA);
      }
      catch(IOException e)
      {

      }
    }

    try
    {
      CIS_DATA.calculate();
    }
    catch(CISException e)
    {
      new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
      return;
    }

    try
    {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/tivi/cis/Calculation.fxml"));
      Scene scene = new Scene(loader.load());
      Stage stage = new Stage();
      stage.setScene(scene);
      stage.setTitle(CIS_DATA.CIS_NAME + " Calculation");
      stage.getIcons().add(new Image(getClass().getResourceAsStream("/tivi/cis/TiViCC.png")));
      stage.centerOnScreen();
      stage.show();
      CalculationController controller = loader.<CalculationController>getController();
      controller.passData(CIS_DATA);
    }
    catch(IOException e)
    {

    }
  }

  @FXML
  public void handlePartList(ActionEvent event)
  {
    try
    {
      CIS_DATA.calculate();
    }
    catch(CISException e)
    {
      new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
      return;
    }

    FileChooser f = new FileChooser();
    f.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "csv"));
    f.setInitialFileName(CIS_DATA.getTiViKey() + "_partList.csv");
    File file = f.showSaveDialog(null);

    if(file != null)
    {
      try(BufferedWriter writer = new BufferedWriter(new FileWriter(file)))
      {
        writer.write("Quantity,TiViKey");
        writer.newLine();
        writer.write("");
        writer.newLine();
        writer.flush();

        for(Map.Entry<Integer, Integer> entry : CIS_DATA.getElectConfig().entrySet())
        {
          writer.write("\"" + entry.getValue() + "\",\"" + CIS_DATA.getKey(entry.getKey()) + "\",\" \"");
          writer.newLine();
          writer.flush();
        }

        for(Map.Entry<Integer, Integer> entry : CIS_DATA.getMechaConfig().entrySet())
        {
          writer.write("\"" + entry.getValue() + "\",\"" + CIS_DATA.getKey(entry.getKey()) + "\",\" \"");
          writer.newLine();
          writer.flush();
        }

        writer.close();

        new Alert(Alert.AlertType.INFORMATION, ResourceBundle.getBundle("tivi.cis.Bundle", CIS_DATA.getLocale()).getString("File saved.")).show();
      }
      catch(IOException e)
      {
        new Alert(Alert.AlertType.ERROR, ResourceBundle.getBundle("tivi.cis.Bundle", CIS_DATA.getLocale()).getString("A fatal error occurred during the save attempt.Please close the target file and try again.")).show();
      }
    }
  }

  @FXML
  public void handleDataSheet(ActionEvent event)
  {
    if(CIS_DATA.getSpec("MXLED") == null && CIS_DATA.getSpec("External Light Source") > 0)
    {
      try
      {
        MXLED_DATA.calculate();
      }
      catch(CISException e)
      {
        new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        return;
      }

      try
      {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/tivi/cis/DataSheet.fxml"));
        Scene scene = new Scene(loader.load());
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("MXLED Datasheet");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/tivi/cis/TiViCC.png")));
        stage.centerOnScreen();
        stage.show();
        stage.setX(stage.getX() - 20);
        stage.setY(stage.getY() - 20);

        DataSheetController controller = loader.<DataSheetController>getController();
        controller.passData(MXLED_DATA);

        if(((Button) event.getSource()).equals(OEMMode))
        {
          controller.getHeader().setEditable(true);
          controller.getSpecs().setEditable(true);
          controller.getCLConfig().setEditable(true);
          controller.getProfilePic().setImage(new Image(getClass().getResourceAsStream("/tivi/cis/OEM_Profile.jpg")));
        }
      }
      catch(IOException e)
      {

      }
    }
    
    try
    {
      CIS_DATA.calculate();
    }
    catch(CISException e)
    {
      new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
      return;
    }

    try
    {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/tivi/cis/DataSheet.fxml"));
      Scene scene = new Scene(loader.load());
      Stage stage = new Stage();
      stage.setScene(scene);
      stage.setTitle(CIS_DATA.CIS_NAME + " Datasheet");
      stage.getIcons().add(new Image(getClass().getResourceAsStream("/tivi/cis/TiViCC.png")));
      stage.centerOnScreen();
      stage.show();

      DataSheetController controller = loader.<DataSheetController>getController();
      controller.passData(CIS_DATA);

      if(((Button) event.getSource()).equals(OEMMode))
      {
        controller.getHeader().setEditable(true);
        controller.getSpecs().setEditable(true);
        controller.getCLConfig().setEditable(true);
        controller.getProfilePic().setImage(new Image(getClass().getResourceAsStream("/tivi/cis/OEM_Profile.jpg")));
      }
    }
    catch(IOException e)
    {

    }
  }

  @FXML
  public void handleOEMMode(ActionEvent event)
  {
    handleDataSheet(event);
  }
}
